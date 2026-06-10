using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.Models;

namespace AgriRent.Services
{
    public class SubscriptionExpiryService
    {
        private readonly AppDbContext _context;
        private readonly NotificationService _notificationService;

        public SubscriptionExpiryService(AppDbContext context, NotificationService notificationService)
        {
            _context = context;
            _notificationService = notificationService;
        }

        // ⭐ Check expired subscriptions, sync roles, and pause excess listings
        public async Task<int> DeactivateExpiredSubscriptionsAsync()
        {
            var expiredSubs = await _context.UserSubscriptions
                .Where(s => s.EndDate < DateTime.UtcNow && s.Status == "Active")
                .ToListAsync();

            if (!expiredSubs.Any())
                return 0;

            // Role IDs (fetch once)
            var farmerRole   = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == "Farmer");
            var ownerRoleDb  = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == "Owner");
            var sellerRoleDb = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == "Seller");

            var farmerRoleId = farmerRole?.RoleId ?? 0;
            var ownerRoleId  = ownerRoleDb?.RoleId ?? 0;
            var sellerRoleId = sellerRoleDb?.RoleId ?? 0;

            // 1. Mark expired subs as 'Expired'
            foreach (var sub in expiredSubs)
            {
                sub.Status = "Expired";
            }
            await _context.SaveChangesAsync();

            // 2. Process each affected user
            var userIds = expiredSubs.Select(s => s.UserId).Distinct().ToList();

            foreach (var userId in userIds)
            {
                // 3. Fetch remaining active plans with limits
                var activeSubs = await _context.UserSubscriptions
                    .Where(s => s.UserId == userId && s.Status == "Active" && s.EndDate > DateTime.UtcNow)
                    .Include(s => s.Plan)
                    .ToListAsync();

                // 4. Determine rights from surviving plans
                bool hasOwnerRights  = activeSubs.Any(s => s.Plan != null && s.Plan.MaxEquipment > 0);
                bool hasSellerRights = activeSubs.Any(s => s.Plan != null && s.Plan.MaxProducts > 0);

                // 5. Sync Owner role
                if (ownerRoleId > 0)
                {
                    var ownerRole = await _context.UserRoles
                        .FirstOrDefaultAsync(r => r.UserId == userId && r.RoleId == ownerRoleId);
                    if (hasOwnerRights && ownerRole == null)
                        _context.UserRoles.Add(new UserRole { UserId = userId, RoleId = ownerRoleId });
                    else if (!hasOwnerRights && ownerRole != null)
                        _context.UserRoles.Remove(ownerRole);
                }

                // 6. Sync Seller role
                if (sellerRoleId > 0)
                {
                    var sellerRole = await _context.UserRoles
                        .FirstOrDefaultAsync(r => r.UserId == userId && r.RoleId == sellerRoleId);
                    if (hasSellerRights && sellerRole == null)
                        _context.UserRoles.Add(new UserRole { UserId = userId, RoleId = sellerRoleId });
                    else if (!hasSellerRights && sellerRole != null)
                        _context.UserRoles.Remove(sellerRole);
                }

                // 🛡️ GUARANTEE FARMER ROLE — user always retains it
                if (farmerRoleId > 0 && !await _context.UserRoles
                        .AnyAsync(r => r.UserId == userId && r.RoleId == farmerRoleId))
                    _context.UserRoles.Add(new UserRole { UserId = userId, RoleId = farmerRoleId });

                // ── 7. PAUSE EXCESS EQUIPMENT ─────────────────────────────────────────────
                int newEquipLimit = activeSubs.Sum(s => s.Plan?.MaxEquipment ?? 0);

                var activeEquip = await _context.Equipments
                    .Where(e => e.OwnerId == userId && (e.Status == "Active" || e.Status == "Pending"))
                    .OrderByDescending(e => e.CreatedAt)
                    .ToListAsync();

                int pausedEquipCount = 0;
                if (activeEquip.Count > newEquipLimit)
                {
                    int excess = activeEquip.Count - newEquipLimit;
                    // Pause the newest excess items (newest first = most recently added)
                    foreach (var eq in activeEquip.Take(excess))
                    {
                        eq.Status = "Paused";
                        pausedEquipCount++;
                    }
                }

                // ── 8. PAUSE EXCESS PRODUCTS ──────────────────────────────────────────────
                int newProductLimit = activeSubs.Sum(s => s.Plan?.MaxProducts ?? 0);

                var activeProd = await _context.Products
                    .Where(p => p.SellerId == userId && (p.Status == "Active" || p.Status == "Pending"))
                    .OrderByDescending(p => p.CreatedAt)
                    .ToListAsync();

                int pausedProdCount = 0;
                if (activeProd.Count > newProductLimit)
                {
                    int excess = activeProd.Count - newProductLimit;
                    foreach (var prod in activeProd.Take(excess))
                    {
                        prod.Status = "Paused";
                        pausedProdCount++;
                    }
                }

                // ── 9. NOTIFY user if items were paused ───────────────────────────────────
                if (pausedEquipCount > 0 || pausedProdCount > 0)
                {
                    var parts = new List<string>();
                    if (pausedEquipCount > 0) parts.Add($"{pausedEquipCount} equipment listing(s)");
                    if (pausedProdCount   > 0) parts.Add($"{pausedProdCount} product listing(s)");

                    await _notificationService.SendNotificationAsync(
                        userId,
                        "Subscription Expired — Listings Paused",
                        $"Your plan expired. We have temporarily paused {string.Join(" and ", parts)} to match your new limits. Purchase an add-on plan to restore them.",
                        "Subscription");
                }
            }

            await _context.SaveChangesAsync();
            return expiredSubs.Count;
        }
    }
}
