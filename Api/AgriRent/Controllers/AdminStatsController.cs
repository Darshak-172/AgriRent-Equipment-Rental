using AgriRent.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace AgriRent.Controllers
{
    [Route("api/admin/dashboard")]
    [ApiController]
    [Authorize(Roles = "Admin")]
    public class AdminStatsController : ControllerBase
    {
        private readonly AppDbContext _context;

        public AdminStatsController(AppDbContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetDashboardStats()
        {
            // 1. Users Breakdown
            int totalUsers = await _context.Users.CountAsync();
            
            var roleCounts = await _context.UserRoles
                .Join(_context.Users, ur => ur.UserId, u => u.UserId, (ur, u) => ur)
                .Join(_context.Roles, ur => ur.RoleId, r => r.RoleId, (ur, r) => new { r.RoleName, ur.UserId })
                .GroupBy(r => r.RoleName)
                .Select(g => new { RoleName = g.Key, Count = g.Select(x => x.UserId).Distinct().Count() })
                .ToDictionaryAsync(x => x.RoleName, x => x.Count);

            int farmers = roleCounts.GetValueOrDefault("Farmer", 0);
            int owners = roleCounts.GetValueOrDefault("Owner", 0);
            int sellers = roleCounts.GetValueOrDefault("Seller", 0);

            // 2. Equipments (Grouped to reduce DB calls)
            var equipmentStats = await _context.Equipments
                .GroupBy(e => e.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Status, x => x.Count);

            int pendingEquipments = equipmentStats.GetValueOrDefault("Pending", 0);
            int activeEquipments  = equipmentStats.GetValueOrDefault("Active", 0);
            int pausedEquipments  = equipmentStats.GetValueOrDefault("Paused", 0);
            int rejectedEquipments = equipmentStats.GetValueOrDefault("Rejected", 0);

            // 3. Products
            var productStats = await _context.Products
                .GroupBy(p => p.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Status, x => x.Count);

            int pendingProducts  = productStats.GetValueOrDefault("Pending", 0);
            int activeProducts   = productStats.GetValueOrDefault("Active", 0);
            int pausedProducts   = productStats.GetValueOrDefault("Paused", 0);
            int rejectedProducts = productStats.GetValueOrDefault("Rejected", 0);

            // 4. Subscriptions
            var subStats = await _context.UserSubscriptions
                .GroupBy(s => s.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Status, x => x.Count);

            int activeSubs   = subStats.GetValueOrDefault("Active", 0);
            int totalSubs    = subStats.Values.Sum();
            int inactiveSubs = totalSubs - activeSubs;
            int subsUsers    = await _context.UserSubscriptions.Select(s => s.UserId).Distinct().CountAsync();

            // 5. Categories
            var catStats = await _context.Categories
                .GroupBy(c => c.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Status, x => x.Count);

            int activeCats   = catStats.GetValueOrDefault("Active", 0);
            int inactiveCats = catStats.GetValueOrDefault("Inactive", 0);
            int totalCats    = catStats.Values.Sum();

            // 6. Complaints
            var complaintStats = await _context.Complaints
                .GroupBy(c => c.Status)
                .Select(g => new { Status = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Status, x => x.Count);

            int pendingComplaints  = complaintStats.GetValueOrDefault("Pending", 0);
            int resolvedComplaints = complaintStats.GetValueOrDefault("Resolved", 0);
            int rejectedComplaints = complaintStats.GetValueOrDefault("Rejected", 0);
            int totalComplaints    = complaintStats.Values.Sum();

            // 7. Reviews
            var reviewStats = await _context.Ratings
                .GroupBy(r => r.RatingType)
                .Select(g => new { Type = g.Key, Count = g.Count() })
                .ToDictionaryAsync(x => x.Type, x => x.Count);

            int equipmentReviews = reviewStats.GetValueOrDefault("Equipment", 0);
            int productReviews   = reviewStats.GetValueOrDefault("Product", 0);
            int totalReviews     = reviewStats.Values.Sum();

            var stats = new
            {
                TotalUsers = totalUsers,
                TotalFarmers = farmers,
                TotalEquipmentOwners = owners,
                TotalSellers = sellers,

                PendingEquipments  = pendingEquipments,
                ActiveEquipments   = activeEquipments,
                PausedEquipments   = pausedEquipments,
                RejectedEquipments = rejectedEquipments,

                PendingProducts  = pendingProducts,
                ActiveProducts   = activeProducts,
                PausedProducts   = pausedProducts,
                RejectedProducts = rejectedProducts,
                TotalEquipments  = pendingEquipments + activeEquipments + pausedEquipments + rejectedEquipments,
                TotalProducts    = pendingProducts + activeProducts + pausedProducts + rejectedProducts,
                ActiveSubscriptions   = activeSubs,
                InactiveSubscriptions = inactiveSubs,
                TotalSubscriptions    = totalSubs,
                UsersWithSubscription = subsUsers,
                TotalCategories = totalCats,
                ActiveCategories   = activeCats,
                InactiveCategories = inactiveCats,
                PendingComplaints  = pendingComplaints,
                ResolvedComplaints = resolvedComplaints,
                RejectedComplaints = rejectedComplaints,
                TotalComplaints    = totalComplaints,
                TotalReviews     = totalReviews,
                EquipmentReviews = equipmentReviews,
                ProductReviews   = productReviews
            };

            return Ok(stats);
        }
    }
}
