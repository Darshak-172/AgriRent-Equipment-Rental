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
    [Route("api/review")]
    public class ReviewController : ControllerBase
    {
        private readonly AppDbContext _context;

        public ReviewController(AppDbContext context)
        {
            _context = context;
        }

        // ─────────────────────────────────────────────
        // POST /api/review — Submit a review
        // ─────────────────────────────────────────────
        [HttpPost]
        [Authorize]
        public async Task<IActionResult> SubmitReview([FromBody] SubmitReviewRequest req)
        {
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // Validate rating value
            if (req.RatingValue < 1 || req.RatingValue > 5)
                return BadRequest(new { message = "Rating value must be between 1 and 5." });

            // Validate target type
            if (req.TargetType != "Equipment" && req.TargetType != "Product")
                return BadRequest(new { message = "TargetType must be 'Equipment' or 'Product'." });

            // Check if target item exists and is Active
            if (req.TargetType == "Equipment")
            {
                var exists = await _context.Equipments
                    .AnyAsync(e => e.EquipmentId == req.TargetId && e.Status == "Active");
                if (!exists)
                    return NotFound(new { message = "Equipment not found or not available." });
            }
            else
            {
                var exists = await _context.Products
                    .AnyAsync(p => p.ProductId == req.TargetId && p.Status == "Active");
                if (!exists)
                    return NotFound(new { message = "Product not found or not available." });
            }

            // Enforce one review per user per item
            var alreadyReviewed = await _context.Ratings.AnyAsync(r =>
                r.FromUserId == userId &&
                r.TargetId == req.TargetId &&
                r.RatingType == req.TargetType);

            if (alreadyReviewed)
                return Conflict(new { message = "You have already reviewed this item." });

            var rating = new Rating
            {
                FromUserId = userId,
                TargetId = req.TargetId,
                RatingType = req.TargetType,
                RatingValue = req.RatingValue,
                Comment = req.Comment?.Trim(),
                CreatedAt = IstHelper.Now
            };

            _context.Ratings.Add(rating);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Review submitted successfully.", ratingId = rating.RatingId });
        }

        // ─────────────────────────────────────────────
        // GET /api/review/equipment/{id} — Reviews for an equipment
        // ─────────────────────────────────────────────
        [HttpGet("equipment/{id}")]
        [AllowAnonymous]
        public async Task<IActionResult> GetEquipmentReviews(int id)
        {
            var reviews = await _context.Ratings
                .Include(r => r.FromUser)
                .Where(r => r.TargetId == id && r.RatingType == "Equipment")
                .OrderByDescending(r => r.CreatedAt)
                .Select(r => new
                {
                    r.RatingId,
                    userName = r.FromUser != null ? r.FromUser.FullName : "Anonymous",
                    r.RatingValue,
                    r.Comment,
                    r.CreatedAt
                })
                .ToListAsync();

            double average = reviews.Count > 0 ? reviews.Average(r => r.RatingValue) : 0;

            return Ok(new
            {
                equipmentId = id,
                averageRating = Math.Round(average, 1),
                totalReviews = reviews.Count,
                reviews
            });
        }

        // ─────────────────────────────────────────────
        // GET /api/review/product/{id} — Reviews for a product
        // ─────────────────────────────────────────────
        [HttpGet("product/{id}")]
        [AllowAnonymous]
        public async Task<IActionResult> GetProductReviews(int id)
        {
            var reviews = await _context.Ratings
                .Include(r => r.FromUser)
                .Where(r => r.TargetId == id && r.RatingType == "Product")
                .OrderByDescending(r => r.CreatedAt)
                .Select(r => new
                {
                    r.RatingId,
                    userName = r.FromUser != null ? r.FromUser.FullName : "Anonymous",
                    r.RatingValue,
                    r.Comment,
                    r.CreatedAt
                })
                .ToListAsync();

            double average = reviews.Count > 0 ? reviews.Average(r => r.RatingValue) : 0;

            return Ok(new
            {
                productId = id,
                averageRating = Math.Round(average, 1),
                totalReviews = reviews.Count,
                reviews
            });
        }

        // ─────────────────────────────────────────────
        // GET /api/review/my — My submitted reviews
        // ─────────────────────────────────────────────
        [HttpGet("my")]
        [Authorize]
        public async Task<IActionResult> GetMyReviews()
        {
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var reviews = await _context.Ratings
                .Where(r => r.FromUserId == userId)
                .OrderByDescending(r => r.CreatedAt)
                .Select(r => new
                {
                    r.RatingId,
                    r.TargetId,
                    targetType = r.RatingType,
                    r.RatingValue,
                    r.Comment,
                    r.CreatedAt
                })
                .ToListAsync();

            return Ok(reviews);
        }

        // ─────────────────────────────────────────────
        // GET /api/review/incoming — Reviews for my items
        // ─────────────────────────────────────────────
        [HttpGet("incoming")]
        [Authorize]
        public async Task<IActionResult> GetIncomingReviews()
        {
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var myEquipmentIds = await _context.Equipments
                .Where(e => e.OwnerId == userId)
                .Select(e => e.EquipmentId)
                .ToListAsync();

            var myProductIds = await _context.Products
                .Where(p => p.SellerId == userId)
                .Select(p => p.ProductId)
                .ToListAsync();

            var reviews = await _context.Ratings
                .Include(r => r.FromUser)
                .Where(r =>
                    (r.RatingType == "Equipment" && myEquipmentIds.Contains(r.TargetId)) ||
                    (r.RatingType == "Product"   && myProductIds.Contains(r.TargetId)))
                .OrderByDescending(r => r.CreatedAt)
                .Select(r => new
                {
                    r.RatingId,
                    r.TargetId,
                    targetType = r.RatingType,
                    userName = r.FromUser != null ? r.FromUser.FullName : "Unknown",
                    r.RatingValue,
                    r.Comment,
                    r.CreatedAt
                })
                .ToListAsync();

            double average = reviews.Count > 0 ? reviews.Average(r => r.RatingValue) : 0;

            return Ok(new
            {
                averageRating = Math.Round(average, 1),
                totalReviews = reviews.Count,
                reviews
            });
        }

        // ─────────────────────────────────────────────
        // DELETE /api/review/{id} — Delete (own or admin)
        // ─────────────────────────────────────────────
        [HttpDelete("{id}")]
        [Authorize]
        public async Task<IActionResult> DeleteReview(int id)
        {
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);
            var isAdmin = User.IsInRole("Admin");

            var review = await _context.Ratings.FindAsync(id);
            if (review == null)
                return NotFound(new { message = "Review not found." });

            if (!isAdmin && review.FromUserId != userId)
                return Forbid();

            _context.Ratings.Remove(review);
            await _context.SaveChangesAsync();

            return Ok(new { message = "Review deleted." });
        }

        // ─────────────────────────────────────────────
        // GET /api/review/admin/all — All reviews (admin)
        // ─────────────────────────────────────────────
        [HttpGet("admin/all")]
        [Authorize(Roles = "Admin")]
        public async Task<IActionResult> GetAllReviews(
            [FromQuery] string? targetType = null,
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 20)
        {
            var query = _context.Ratings
                .Include(r => r.FromUser)
                .AsQueryable();

            if (!string.IsNullOrEmpty(targetType))
                query = query.Where(r => r.RatingType == targetType);

            var total = await query.CountAsync();

            var raw = await query
                .OrderByDescending(r => r.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .ToListAsync();

            // Batch-load equipment and product info for target names + owner/seller names
            var eqIds = raw.Where(r => r.RatingType == "Equipment").Select(r => r.TargetId).ToHashSet();
            var prIds = raw.Where(r => r.RatingType == "Product").Select(r => r.TargetId).ToHashSet();

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

            var reviews = raw.Select(r =>
            {
                string targetName       = "Unknown";
                string? ownerSellerName = null;
                string? targetImageUrl = null;

                if (r.RatingType == "Equipment" && equipments.TryGetValue(r.TargetId, out var eq))
                {
                    targetName      = eq.EquipmentName;
                    ownerSellerName = eq.Owner?.FullName;
                    targetImageUrl = eq.Images?.FirstOrDefault()?.ImageUrl;
                }
                else if (r.RatingType == "Product" && products.TryGetValue(r.TargetId, out var pr))
                {
                    targetName      = pr.ProductName;
                    ownerSellerName = pr.Seller?.FullName;
                    targetImageUrl = pr.Images?.FirstOrDefault()?.ImageUrl;
                }

                return new
                {
                    r.RatingId,
                    r.FromUserId,
                    userName = r.FromUser?.FullName ?? "Unknown",
                    r.TargetId,
                    targetType  = r.RatingType,
                    targetName,
                    targetImageUrl,
                    ownerOrSellerName = ownerSellerName,
                    r.RatingValue,
                    r.Comment,
                    r.CreatedAt
                };
            });

            return Ok(new { total, page, pageSize, reviews });
        }
    }

    public class SubmitReviewRequest
    {
        public int TargetId { get; set; }
        public string TargetType { get; set; } = "Equipment"; // "Equipment" or "Product"
        public int RatingValue { get; set; }                  // 1–5
        public string? Comment { get; set; }
    }
}
