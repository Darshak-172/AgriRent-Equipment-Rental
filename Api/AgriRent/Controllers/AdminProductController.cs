using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/admin/products")]
    [Authorize(Roles = "Admin")]
    public class AdminProductController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;
        private readonly NotificationService _notificationService;
        private readonly CloudinaryStorageService _storage;

        public AdminProductController(AppDbContext context, TranslateService translate, LanguageHelper langHelper, NotificationService notificationService, CloudinaryStorageService storage)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
            _notificationService = notificationService;
            _storage = storage;
        }



        // ⭐ GET ALL PENDING PRODUCTS
        [HttpGet("pending")]
        public async Task<IActionResult> GetPending()
        {
            var lang = _langHelper.GetLanguage();

            var list = await _context.Products.AsNoTracking()
                .Where(p => p.Status == "Pending")
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .ToListAsync();
            
            var translatedList = new List<object>();

            foreach (var p in list)
            {
                translatedList.Add(new
                {
                    p.ProductId,
                    p.SellerId,
                    Seller = p.Seller != null ? new
                    {
                        p.Seller.UserId,
                        Name = await _translate.TransliterateToNative(p.Seller.FullName, lang),
                        p.Seller.MobileNumber
                    } : null,
                    ProductName = await _translate.TransliterateToNative(p.ProductName, lang),
                    Description = await _translate.Translate(p.Description, lang),
                    Category = p.SubCategory != null ? await _translate.Translate(p.SubCategory.SubCategoryName, lang) : null,
                    p.Price,
                    p.Stock,
                    Unit = await _translate.Translate(p.Unit, lang),
                    Location = await _translate.TransliterateToNative(p.Location, lang),
                    ImageUrl = p.Images?.FirstOrDefault()?.ImageUrl,
                    Status = p.Status,
                    OwnerName = p.Seller != null ? await _translate.TransliterateToNative(p.Seller.FullName, lang) : "Unknown",
                    OwnerMobile = p.Seller?.MobileNumber ?? "N/A",
                    CreatedAt = p.CreatedAt
                });
            }

            return Ok(new
            {
                message = await _translate.Translate("Pending products list", lang),
                count = list.Count,
                data = translatedList
            });
        }

        // ⭐ APPROVE PRODUCT
        [HttpPut("approve/{id}")]
        public async Task<IActionResult> Approve(int id)
        {
            var lang = _langHelper.GetLanguage();

            var product = await _context.Products.FindAsync(id);
            if (product == null)
                return NotFound(await _translate.Translate("Product not found", lang));

            product.Status = "Active";
            await _context.SaveChangesAsync();

            await _notificationService.SendNotificationAsync(
                product.SellerId,
                "Product Approved",
                $"{product.ProductName} is now live",
                "Product"
            );

            return Ok(new
            {
                message = await _translate.Translate("Product Approved Successfully", lang),
                productId = product.ProductId
            });
        }

        // ⭐ REJECT PRODUCT
        [HttpPut("reject/{id}")]
        public async Task<IActionResult> Reject(int id)
        {
            var lang = _langHelper.GetLanguage();

            var product = await _context.Products.FindAsync(id);
            if (product == null)
                return NotFound(await _translate.Translate("Product not found", lang));

            product.Status = "Rejected";
            await _context.SaveChangesAsync();

            await _notificationService.SendNotificationAsync(
                product.SellerId,
                "Product Rejected",
                $"{product.ProductName} was not approved",
                "Product"
            );

            return Ok(new
            {
                message = await _translate.Translate("Product Rejected", lang),
                productId = product.ProductId
            });
        }

        // ⭐ DELETE PRODUCT
        [HttpDelete("delete/{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var lang = _langHelper.GetLanguage();

            var product = await _context.Products
                .Include(p => p.Images)
                .FirstOrDefaultAsync(p => p.ProductId == id);

            if (product == null)
                return NotFound(await _translate.Translate("Product not found", lang));

            // Delete all associated Cloudinary images first
            if (product.Images != null && product.Images.Any())
            {
                var deleteTasks = product.Images
                    .Where(img => !string.IsNullOrEmpty(img.ImageUrl))
                    .Select(img => _storage.DeleteImageAsync(img.ImageUrl));
                await Task.WhenAll(deleteTasks);
            }

            _context.Products.Remove(product);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = await _translate.Translate("Product Deleted Successfully", lang),
                productId = id
            });
        }

        // ⭐ GET ALL PRODUCTS (WITH FILTER)
        [HttpGet("all")]
        public async Task<IActionResult> GetAll(
            [FromQuery] string? status,
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 10)
        {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;

            var lang = _langHelper.GetLanguage();
            var query = _context.Products.AsNoTracking()
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .AsQueryable();

            if (!string.IsNullOrWhiteSpace(status))
                query = query.Where(p => p.Status == status);

            var totalCount = await query.CountAsync();
            var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

            var list = await query
                .OrderByDescending(p => p.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .ToListAsync();

            var translatedList = new List<object>();

            foreach (var p in list)
            {
                translatedList.Add(new
                {
                    p.ProductId,
                    ProductName = await _translate.TransliterateToNative(p.ProductName, lang),
                    Description = await _translate.Translate(p.Description, lang),
                    Category = p.SubCategory != null ? await _translate.Translate(p.SubCategory.SubCategoryName, lang) : null,
                    p.Price,
                    p.Stock,
                    Unit = await _translate.Translate(p.Unit, lang),
                    Location = await _translate.TransliterateToNative(p.Location, lang),
                    ImageUrl = p.Images != null ? p.Images.Select(i => i.ImageUrl).FirstOrDefault() : null,
                    p.Status,
                    p.CreatedAt,
                    OwnerName = p.Seller != null ? await _translate.TransliterateToNative(p.Seller.FullName, lang) : "Unknown",
                    OwnerMobile = p.Seller?.MobileNumber ?? "N/A"
                });
            }

            return Ok(new
            {
                data = translatedList,
                pagination = new { page, pageSize, totalItems = totalCount, totalPages }
            });
        }
    }
}
