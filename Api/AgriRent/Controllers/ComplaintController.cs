using AgriRent.Data;
using AgriRent.Extensions;
using AgriRent.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/complaint")]
    public class ComplaintController : ControllerBase
    {
        private readonly AppDbContext _context;

        public ComplaintController(AppDbContext context)
        {
            _context = context;
        }

        private int GetUserId() =>
            int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

        private bool IsAdmin() => User.IsInRole("Admin");

        // ─────────────────────────────────────────────────────────
        // POST /api/complaint  —  File a complaint
        // Rule: user must have booked the equipment OR bought the product
        // ─────────────────────────────────────────────────────────
        [HttpPost]
        [Authorize]
        public async Task<IActionResult> FileComplaint([FromBody] FileComplaintRequest req)
        {
            var userId = GetUserId();

            if (req.TargetType != "Equipment" && req.TargetType != "Product")
                return BadRequest(new { message = "TargetType must be 'Equipment' or 'Product'." });

            if (string.IsNullOrWhiteSpace(req.Description))
                return BadRequest(new { message = "Description is required." });

            // ── Verify the user actually booked / bought the item ──
            if (req.TargetType == "Equipment")
            {
                var hasBooked = await _context.EquipmentBookings
                    .AnyAsync(b => b.FarmerId == userId && b.EquipmentId == req.TargetId);

                if (!hasBooked)
                    return StatusCode(403, new { message = "You can only complain about equipment you have booked." });
            }
            else
            {
                var hasBought = await _context.Orders
                    .AnyAsync(o => o.FarmerId == userId &&
                                   o.OrderItems.Any(oi => oi.ProductId == req.TargetId));

                if (!hasBought)
                    return StatusCode(403, new { message = "You can only complain about products you have purchased." });
            }

            var complaint = new Complaint
            {
                UserId     = userId,
                TargetId   = req.TargetId,
                TargetType = req.TargetType,
                Description = req.Description.Trim(),
                Status     = "Pending",
                CreatedAt  = IstHelper.Now
            };

            _context.Complaints.Add(complaint);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Complaint filed successfully.", complaintId = complaint.ComplaintId });
        }

        // ─────────────────────────────────────────────────────────
        // GET /api/complaint/my  —  Complaints I filed
        // ─────────────────────────────────────────────────────────
        [HttpGet("my")]
        [Authorize]
        public async Task<IActionResult> GetMyComplaints()
        {
            var userId = GetUserId();

            var raw = await _context.Complaints
                .Where(c => c.UserId == userId)
                .OrderByDescending(c => c.CreatedAt)
                .ToListAsync();

            var eqIds  = raw.Where(c => c.TargetType == "Equipment").Select(c => c.TargetId).ToHashSet();
            var prIds  = raw.Where(c => c.TargetType == "Product").Select(c => c.TargetId).ToHashSet();

            var equipments = await _context.Equipments
                .Where(e => eqIds.Contains(e.EquipmentId))
                .ToDictionaryAsync(e => e.EquipmentId);

            var products = await _context.Products
                .Where(p => prIds.Contains(p.ProductId))
                .ToDictionaryAsync(p => p.ProductId);

            var list = raw.Select(c =>
            {
                string targetName = "Unknown";
                if (c.TargetType == "Equipment" && equipments.TryGetValue(c.TargetId, out var eq)) targetName = eq.EquipmentName ?? "Unknown Equipment";
                else if (c.TargetType == "Product" && products.TryGetValue(c.TargetId, out var pr)) targetName = pr.ProductName ?? "Unknown Product";

                return new
                {
                    c.ComplaintId,
                    c.TargetId,
                    targetType     = c.TargetType,
                    targetName,
                    c.Description,
                    c.Status,
                    c.ResolutionNote,
                    c.CreatedAt,
                    c.ResolvedAt
                };
            }).ToList();

            return Ok(list);
        }

        // ─────────────────────────────────────────────────────────
        // GET /api/complaint/incoming  —  Complaints about my items
        // Visible to equipment owners / product sellers
        // ─────────────────────────────────────────────────────────
        [HttpGet("incoming")]
        [Authorize]
        public async Task<IActionResult> GetIncomingComplaints()
        {
            var userId = GetUserId();

            var myEquipmentIds = await _context.Equipments
                .Where(e => e.OwnerId == userId)
                .Select(e => e.EquipmentId)
                .ToListAsync();

            var myProductIds = await _context.Products
                .Where(p => p.SellerId == userId)
                .Select(p => p.ProductId)
                .ToListAsync();

            var raw = await _context.Complaints
                .Include(c => c.User)
                .Where(c =>
                    (c.TargetType == "Equipment" && myEquipmentIds.Contains(c.TargetId)) ||
                    (c.TargetType == "Product"   && myProductIds.Contains(c.TargetId)))
                .OrderByDescending(c => c.CreatedAt)
                .ToListAsync();

            var eqIds  = raw.Where(c => c.TargetType == "Equipment").Select(c => c.TargetId).ToHashSet();
            var prIds  = raw.Where(c => c.TargetType == "Product").Select(c => c.TargetId).ToHashSet();

            var equipments = await _context.Equipments
                .Where(e => eqIds.Contains(e.EquipmentId))
                .ToDictionaryAsync(e => e.EquipmentId);

            var products = await _context.Products
                .Where(p => prIds.Contains(p.ProductId))
                .ToDictionaryAsync(p => p.ProductId);

            var list = raw.Select(c =>
            {
                string targetName = "Unknown";
                if (c.TargetType == "Equipment" && equipments.TryGetValue(c.TargetId, out var eq)) targetName = eq.EquipmentName ?? "Unknown Equipment";
                else if (c.TargetType == "Product" && products.TryGetValue(c.TargetId, out var pr)) targetName = pr.ProductName ?? "Unknown Product";

                return new
                {
                    c.ComplaintId,
                    c.TargetId,
                    targetType   = c.TargetType,
                    targetName,
                    filedBy      = c.User != null ? c.User.FullName : "Unknown",
                    c.UserId,
                    c.Description,
                    c.Status,
                    c.ResolutionNote,
                    c.CreatedAt,
                    c.ResolvedAt
                };
            }).ToList();

            return Ok(list);
        }

        // ─────────────────────────────────────────────────────────
        // GET /api/complaint/{id}  —  View a single complaint
        // Visible to: filer, equipment owner / product seller, admin
        // ─────────────────────────────────────────────────────────
        [HttpGet("{id}")]
        [Authorize]
        public async Task<IActionResult> GetComplaint(int id)
        {
            var userId = GetUserId();

            var complaint = await _context.Complaints
                .Include(c => c.User)
                .Include(c => c.ResolvedByUser)
                .FirstOrDefaultAsync(c => c.ComplaintId == id);

            if (complaint == null)
                return NotFound(new { message = "Complaint not found." });

            // Check visibility
            bool canView = IsAdmin() || complaint.UserId == userId;

            if (!canView)
            {
                canView = complaint.TargetType == "Equipment"
                    ? await _context.Equipments.AnyAsync(e => e.EquipmentId == complaint.TargetId && e.OwnerId == userId)
                    : await _context.Products.AnyAsync(p => p.ProductId == complaint.TargetId && p.SellerId == userId);
            }

            if (!canView)
                return StatusCode(403, new { message = "You are not allowed to view this complaint." });

            // Load target information (equipment/product + owner/seller)
            string targetName = "Unknown";
            string? ownerOrSellerName = null;
            string? targetImageUrl = null;

            if (complaint.TargetType == "Equipment")
            {
                var equipment = await _context.Equipments
                    .Include(e => e.Owner)
                    .Include(e => e.Images)
                    .FirstOrDefaultAsync(e => e.EquipmentId == complaint.TargetId);
                
                if (equipment != null)
                {
                    targetName = equipment.EquipmentName ?? "Unknown Equipment";
                    ownerOrSellerName = equipment.Owner?.FullName ?? "N/A";
                    targetImageUrl = equipment.Images?.FirstOrDefault()?.ImageUrl;
                }
            }
            else if (complaint.TargetType == "Product")
            {
                var product = await _context.Products
                    .Include(p => p.Seller)
                    .Include(p => p.Images)
                    .FirstOrDefaultAsync(p => p.ProductId == complaint.TargetId);
                
                if (product != null)
                {
                    targetName = product.ProductName ?? "Unknown Product";
                    ownerOrSellerName = product.Seller?.FullName ?? "N/A";
                    targetImageUrl = product.Images?.FirstOrDefault()?.ImageUrl;
                }
            }

            // Determine resolver info
            string? resolvedByRole = null;
            string? resolvedByName = null;

            if (complaint.ResolvedByUserId.HasValue)
            {
                // Get admin role ID first
                var adminRole = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == "Admin");
                var adminRoleId = adminRole?.RoleId ?? 1; // Fallback to 1 if not found

                // Get all admin user IDs
                var adminIds = await _context.UserRoles
                    .Where(ur => ur.RoleId == adminRoleId)
                    .Select(ur => ur.UserId)
                    .ToHashSetAsync();

                if (complaint.Status == "Rejected")
                {
                    resolvedByRole = "Admin";
                    resolvedByName = complaint.ResolvedByUser?.FullName ?? "Admin";
                }
                else if (complaint.Status == "Resolved")
                {
                    resolvedByRole = adminIds.Contains(complaint.ResolvedByUserId.Value) ? "Admin" : "Owner/Seller";
                    resolvedByName = complaint.ResolvedByUser?.FullName ?? (resolvedByRole == "Admin" ? "Admin" : "Owner/Seller");
                }
            }

            return Ok(new
            {
                complaint.ComplaintId,
                complaint.UserId,
                filedBy           = complaint.User?.FullName ?? "Unknown",
                complaint.TargetId,
                targetType        = complaint.TargetType,
                targetName,
                targetImageUrl,
                ownerOrSellerName,
                complaint.Description,
                complaint.Status,
                complaint.ResolutionNote,
                complaint.ResolvedByUserId,
                resolvedByName,
                resolvedByRole,
                complaint.CreatedAt,
                complaint.ResolvedAt
            });
        }

        // ─────────────────────────────────────────────────────────
        // PUT /api/complaint/{id}/resolve  —  Resolve a complaint
        // Only: equipment owner / product seller / admin
        // ─────────────────────────────────────────────────────────
        [HttpPut("{id}/resolve")]
        [Authorize]
        public async Task<IActionResult> ResolveComplaint(int id, [FromBody] ResolutionRequest req)
        {
            var userId = GetUserId();

            var complaint = await _context.Complaints.FindAsync(id);
            if (complaint == null)
                return NotFound(new { message = "Complaint not found." });

            if (complaint.Status != "Pending")
                return BadRequest(new { message = $"Complaint is already {complaint.Status}." });

            // Check authority
            bool canResolve = IsAdmin();
            if (!canResolve)
            {
                canResolve = complaint.TargetType == "Equipment"
                    ? await _context.Equipments.AnyAsync(e => e.EquipmentId == complaint.TargetId && e.OwnerId == userId)
                    : await _context.Products.AnyAsync(p => p.ProductId == complaint.TargetId && p.SellerId == userId);
            }

            if (!canResolve)
                return StatusCode(403, new { message = "Only the equipment owner, product seller, or admin can resolve this complaint." });

            complaint.Status           = "Resolved";
            complaint.ResolutionNote   = req.ResolutionNote?.Trim();
            complaint.ResolvedByUserId = userId;
            complaint.ResolvedAt       = IstHelper.Now;

            await _context.SaveChangesAsync();

            return Ok(new { message = "Complaint resolved." });
        }

        // ─────────────────────────────────────────────────────────
        // PUT /api/complaint/{id}/reject  —  Reject (admin only)
        // ─────────────────────────────────────────────────────────
        [HttpPut("{id}/reject")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> RejectComplaint(int id, [FromBody] ResolutionRequest req)
        {
            var complaint = await _context.Complaints.FindAsync(id);
            if (complaint == null)
                return NotFound(new { message = "Complaint not found." });

            if (complaint.Status != "Pending")
                return BadRequest(new { message = $"Complaint is already {complaint.Status}." });

            complaint.Status           = "Rejected";
            complaint.ResolutionNote   = req.ResolutionNote?.Trim();
            complaint.ResolvedByUserId = GetUserId();
            complaint.ResolvedAt       = IstHelper.Now;

            await _context.SaveChangesAsync();

            return Ok(new { message = "Complaint rejected." });
        }

        // ─────────────────────────────────────────────────────────
        // GET /api/complaint/admin/all  —  All complaints (admin)
        // ─────────────────────────────────────────────────────────
        [HttpGet("admin/all")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> GetAllComplaints(
            [FromQuery] string? status     = null,
            [FromQuery] string? targetType = null,
            [FromQuery] int     page       = 1,
            [FromQuery] int     pageSize   = 20)
        {
            // Get admin role ID first
            var adminRole = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == "Admin");
            var adminRoleId = adminRole?.RoleId ?? 1; // Fallback to 1 if not found

            // Get all admin user IDs
            var adminIds = await _context.UserRoles
                .Where(ur => ur.RoleId == adminRoleId)
                .Select(ur => ur.UserId)
                .ToHashSetAsync();

            var query = _context.Complaints
                .Include(c => c.User)
                .Include(c => c.ResolvedByUser)
                .AsQueryable();

            if (!string.IsNullOrEmpty(status))
                query = query.Where(c => c.Status == status);

            if (!string.IsNullOrEmpty(targetType))
                query = query.Where(c => c.TargetType == targetType);

            var total = await query.CountAsync();

            var raw = await query
                .OrderByDescending(c => c.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .ToListAsync();

            // Batch-load equipment and product info for target names + owner/seller names
            var eqIds  = raw.Where(c => c.TargetType == "Equipment").Select(c => c.TargetId).ToHashSet();
            var prIds  = raw.Where(c => c.TargetType == "Product").Select(c => c.TargetId).ToHashSet();

            var equipments = await _context.Equipments
                .Include(e => e.Owner)
                .Include(e => e.Images)
                .Where(e => eqIds.Contains(e.EquipmentId))
                .ToDictionaryAsync(e => e.EquipmentId);

            var products = await _context.Products
                .Include(p => p.Seller)
                .Include(p => p.Images)
                .Where(p => prIds.Contains(p.ProductId))
                .ToDictionaryAsync(p => p.ProductId);

            var list = raw.Select(c =>
            {
                string targetName       = "Unknown";
                string? ownerOrSellerName = null;
                string? targetImageUrl = null;

                if (c.TargetType == "Equipment" && equipments.TryGetValue(c.TargetId, out var eq))
                {
                    targetName       = eq.EquipmentName ?? "Unknown Equipment";
                    ownerOrSellerName  = eq.Owner?.FullName ?? "N/A";
                    targetImageUrl = eq.Images?.FirstOrDefault()?.ImageUrl;
                }
                else if (c.TargetType == "Product" && products.TryGetValue(c.TargetId, out var pr))
                {
                    targetName       = pr.ProductName ?? "Unknown Product";
                    ownerOrSellerName  = pr.Seller?.FullName ?? "N/A";
                    targetImageUrl = pr.Images?.FirstOrDefault()?.ImageUrl;
                }

                // Determine resolver role and name
                string? resolvedByRole = null;
                string? resolvedByName = null;

                if (c.ResolvedByUserId.HasValue)
                {
                    if (c.Status == "Rejected")
                    {
                        resolvedByRole = "Admin";
                        resolvedByName = c.ResolvedByUser?.FullName ?? "Admin";
                    }
                    else if (c.Status == "Resolved")
                    {
                        resolvedByRole = adminIds.Contains(c.ResolvedByUserId.Value) ? "Admin" : "Owner/Seller";
                        resolvedByName = c.ResolvedByUser?.FullName ?? (resolvedByRole == "Admin" ? "Admin" : "Owner/Seller");
                    }
                }

                return new
                {
                    c.ComplaintId,
                    c.UserId,
                    filedBy          = c.User?.FullName ?? "Unknown",
                    c.TargetId,
                    targetType       = c.TargetType,
                    targetName,
                    targetImageUrl,
                    ownerOrSellerName,
                    c.Description,
                    c.Status,
                    c.ResolutionNote,
                    c.ResolvedByUserId,
                    resolvedByName,
                    resolvedByRole,
                    c.CreatedAt,
                    c.ResolvedAt
                };
            });

            return Ok(new { total, page, pageSize, complaints = list });
        }
    }

    // ── Request models ──────────────────────────────────────────

    public class FileComplaintRequest
    {
        public int    TargetId   { get; set; }
        public string TargetType { get; set; } = "Equipment"; // "Equipment" or "Product"
        public string Description { get; set; } = "";
    }

    public class ResolutionRequest
    {
        public string? ResolutionNote { get; set; }
    }
}
