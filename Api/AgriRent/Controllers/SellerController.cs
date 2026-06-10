using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Extensions;
using AgriRent.Models;
using AgriRent.Services;
using System.Security.Claims;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/seller")]
    public class SellerController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly CloudinaryStorageService _storage;
        private readonly LanguageHelper _langHelper;
        private readonly NotificationService _notificationService;

        public SellerController(AppDbContext context, TranslateService translate, CloudinaryStorageService storage, LanguageHelper langHelper, NotificationService notificationService)
        {
            _context = context;
            _translate = translate;
            _storage = storage;
            _langHelper = langHelper;
            _notificationService = notificationService;
        }



        // ⭐ ADD PRODUCT (SELLER + ACTIVE SUBSCRIPTION)
        [Authorize(Roles = "Seller")]
        [HttpPost("add-product")]
        public async Task<IActionResult> AddProduct(AddProductDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // 1. Sum MaxProducts across ALL active, non-expired plans
            int totalAllowed = await _context.UserSubscriptions
                .Include(s => s.Plan)
                .Where(s => s.UserId == userId && s.Status == "Active" && s.EndDate > DateTime.UtcNow)
                .SumAsync(s => s.Plan != null ? s.Plan.MaxProducts : 0);

            if (totalAllowed == 0)
                return BadRequest(await _translate.Translate(
                    "No active subscription with product slots. Please purchase a plan.", lang));

            // 2. Count Active + Pending products (slot usage includes queue)
            int currentCount = await _context.Products
                .CountAsync(p => p.SellerId == userId && (p.Status == "Active" || p.Status == "Pending"));

            if (currentCount >= totalAllowed)
                return BadRequest(await _translate.Translate(
                    $"Limit reached! You are using {currentCount}/{totalAllowed} product slots. Purchase an Add-on plan for more.", lang));

            // ⭐ INPUT TRANSLATION STRATEGY
            // 1. Names/Locations -> Transliterate (Phonetic)
            var nameEnglish = await _translate.TransliterateToEnglish(dto.ProductName, lang);
            var locationEnglish = await _translate.TransliterateToEnglish(dto.Location, lang);
            
            // 2. Description/Unit -> Translate (Meaning)
            var descEnglish = await _translate.ToEnglish(dto.Description, lang);
            var unitEnglish = dto.Unit != null ? await _translate.ToEnglish(dto.Unit, lang) : null;

            var product = new Product
            {
                SellerId = userId,
                ProductName = nameEnglish,
                Description = descEnglish,
                SubCategoryId = dto.SubCategoryId,
                Price = dto.Price,
                Stock = dto.Stock,
                Unit = unitEnglish ?? "",
                Location = locationEnglish,
                Status = "Pending",
                Images = (!string.IsNullOrEmpty(dto.ImageUrl) && !dto.ImageUrl.Contains("placeholder"))
                    ? new List<ProductImage> { new ProductImage { ImageUrl = dto.ImageUrl } }
                    : new List<ProductImage>()
            };

            _context.Products.Add(product);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = await _translate.Translate(
                    "Product added successfully (Pending Admin Approval)", lang),
                productId = product.ProductId
            });
        }

        // ⭐ SELLER → VIEW OWN PRODUCTS
        [Authorize(Roles = "Seller")]
        [HttpGet("my-products")]
        public async Task<IActionResult> MyProducts()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var products = await _context.Products.AsNoTracking()
                .Where(p => p.SellerId == userId)
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .OrderByDescending(p => p.CreatedAt)
                .ToListAsync();

            var tasks = products.Select(async p => (object)new
            {
                p.ProductId,
                p.SellerId,
                Seller = p.Seller != null ? new
                {
                    p.Seller.UserId,
                    FullName = await _translate.TransliterateToNative(p.Seller.FullName, lang),
                    p.Seller.MobileNumber
                } : null,
                ProductName = await _translate.TransliterateToNative(p.ProductName, lang),
                Description = await _translate.Translate(p.Description, lang),
                Category = p.SubCategory != null ? await _translate.Translate(p.SubCategory.SubCategoryName, lang) : null,
                p.SubCategoryId,
                CategoryId = p.SubCategory?.CategoryId,
                p.Price,
                p.Stock,
                Unit = await _translate.Translate(p.Unit, lang),
                Location = await _translate.TransliterateToNative(p.Location, lang),
                Images = p.Images?.Select(img => new { img.ImageId, img.ImageUrl }).ToList(),
                ImageUrl = p.Images?.FirstOrDefault(img => !img.ImageUrl.Contains("via.placeholder.com"))?.ImageUrl
                           ?? p.Images?.FirstOrDefault()?.ImageUrl,
                p.Status,
                p.CreatedAt
            });

            var list = (await Task.WhenAll(tasks)).ToList();

            return Ok(list);
        }

        // ⭐ UPDATE PRODUCT
        [Authorize(Roles = "Seller")]
        [HttpPut("update-product/{id}")]
        public async Task<IActionResult> UpdateProduct(int id, AddProductDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var product = await _context.Products
                .Include(p => p.Images)
                .FirstOrDefaultAsync(p => p.ProductId == id && p.SellerId == userId);

            if (product == null)
                return Unauthorized(await _translate.Translate(
                    "You can only update your own products.", lang));

            // ⭐ Name -> Transliterate
            product.ProductName = await _translate.TransliterateToEnglish(dto.ProductName, lang);
            // ⭐ Desc -> Translate
            product.Description = await _translate.ToEnglish(dto.Description, lang);
            // product.Category - readonly, use SubCategoryId
            product.Price = dto.Price;
            product.Stock = dto.Stock;
            product.Unit = dto.Unit;
            product.Location = dto.Location;
            product.Status = "Pending"; // Reset to pending after edit

            await _context.SaveChangesAsync();

            return Ok(await _translate.Translate("Product updated successfully", lang));
        }

        // ⭐ SELLER → TOGGLE PRODUCT STATUS
        [Authorize(Roles = "Seller")]
        [HttpPut("toggle-product-status/{id}")]
        public async Task<IActionResult> ToggleProductStatus(int id)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.ProductId == id && p.SellerId == userId);

            if (product == null)
                return NotFound(new { message = "Product not found or you are not the seller." });

            if (product.Status == "Pending" || product.Status == "Rejected")
                return BadRequest(new { message = $"Cannot toggle status. Product is currently {product.Status}." });

            product.Status = product.Status == "Active" ? "Blocked" : "Active";
            await _context.SaveChangesAsync();

            return Ok(new { message = $"Status changed to {product.Status}." });
        }

        // ⭐ DELETE PRODUCT
        [Authorize(Roles = "Seller")]
        [HttpDelete("delete-product/{id}")]
        public async Task<IActionResult> DeleteProduct(int id)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var product = await _context.Products
                .Include(p => p.Images)
                .FirstOrDefaultAsync(p => p.ProductId == id && p.SellerId == userId);

            if (product == null)
                return Unauthorized(await _translate.Translate(
                    "You can only delete your own products.", lang));

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

            return Ok(await _translate.Translate("Product deleted successfully", lang));
        }

        // ⭐ SELLER → VIEW ORDERS
        [Authorize(Roles = "Seller")]
        [HttpGet("orders")]
        public async Task<IActionResult> GetOrders()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var orders = await _context.Orders.AsNoTracking()
                .Include(o => o.OrderItems)
                .ThenInclude(i => i.Product)
                .ThenInclude(p => p!.Images)
                .Include(o => o.Farmer)
                .Where(o => o.OrderItems.Any(i => i.Product != null && i.Product.SellerId == userId))
                .OrderByDescending(o => o.CreatedAt)
                .ToListAsync();

            var tasks = orders.Select(async o => {
                var firstItem = o.OrderItems.FirstOrDefault();
                var product = firstItem?.Product;
                return (object)new
                {
                    o.OrderId,
                    ProductId = firstItem?.ProductId,
                    ProductName = product != null 
                        ? await _translate.TransliterateToNative(product.ProductName, lang) 
                        : "Unknown",
                    ProductDescription = product != null ? await _translate.Translate(product.Description, lang) : "",
                    ProductUnit = product != null ? await _translate.Translate(product.Unit, lang) : "",
                    ProductLocation = product != null ? await _translate.TransliterateToNative(product.Location, lang) : "",
                    ProductPrice = product?.Price ?? 0,
                    ProductImages = product?.Images?.Select(img => img.ImageUrl).ToList() ?? new List<string>(),
                    FarmerName = o.Farmer != null 
                        ? await _translate.TransliterateToNative(o.Farmer.FullName, lang) 
                        : "Unknown",
                    Quantity = firstItem?.Quantity ?? 0,
                    UnitPrice = firstItem?.Price ?? 0,
                    TotalPrice = o.TotalAmount,
                    o.DeliveryAddress,
                    o.ContactNumber,
                    Status = await _translate.Translate(o.OrderStatus, lang),
                    PaymentStatus = await _translate.Translate(o.PaymentStatus, lang),
                    OrderDate = o.CreatedAt,
                    ImageUrl = product?.Images?.FirstOrDefault(img => !img.ImageUrl.Contains("via.placeholder.com"))?.ImageUrl
                               ?? product?.Images?.FirstOrDefault()?.ImageUrl
                };
            });

            var result = (await Task.WhenAll(tasks)).ToList();

            return Ok(result);
        }

        // ⭐ SELLER → UPDATE ORDER STATUS
        [Authorize(Roles = "Seller")]
        [HttpPut("update-order-status/{orderId}")]
        public async Task<IActionResult> UpdateOrderStatus(int orderId, [FromBody] string status)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var order = await _context.Orders
                .Include(o => o.OrderItems)
                .ThenInclude(i => i.Product)
                .FirstOrDefaultAsync(o => o.OrderId == orderId && o.OrderItems.Any(i => i.Product != null && i.Product.SellerId == userId));

            if (order == null)
                return Unauthorized(await _translate.Translate(
                    "You can only update your own product orders.", lang));

            var validStatuses = new[] { "Pending", "Processing", "Shipped", "Delivered", "Canceled" };
            if (!validStatuses.Contains(status, StringComparer.OrdinalIgnoreCase))
            {
                return BadRequest(await _translate.Translate("Invalid order status.", lang));
            }

            status = char.ToUpper(status[0]) + status.Substring(1).ToLower();

            order.OrderStatus = status;
            if (status == "Delivered")
                order.DeliveryDate = IstHelper.Now;

            await _context.SaveChangesAsync();

            var notifMessages = new Dictionary<string, (string title, string body)>(StringComparer.OrdinalIgnoreCase)
            {
                ["Processing"] = ("Order Accepted",  $"Your order has been accepted and is being processed"),
                ["Shipped"]    = ("Order Shipped",    $"Your order has been shipped"),
                ["Delivered"]  = ("Order Delivered",  $"Your order has been delivered"),
                ["Canceled"]   = ("Order Canceled",   $"Your order has been canceled by the seller"),
            };

            if (notifMessages.TryGetValue(status, out var notif))
                await _notificationService.SendNotificationAsync(order.FarmerId, notif.title, notif.body, "Order");

            return Ok(await _translate.Translate("Order status updated", lang));
        }

        [Authorize(Roles = "Seller")]
        [HttpGet("dashboard")]
        public async Task<IActionResult> Dashboard()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var totalProducts = await _context.Products.CountAsync(p => p.SellerId == userId);
            var activeProducts = await _context.Products.CountAsync(p => p.SellerId == userId && p.Status == "Active");
            var totalOrders = await _context.Orders.CountAsync(o => o.OrderItems.Any(i => i.Product != null && i.Product.SellerId == userId));
            var pendingOrders = await _context.Orders.CountAsync(o => o.OrderItems.Any(i => i.Product != null && i.Product.SellerId == userId) && o.OrderStatus == "Pending");

            return Ok(new
            {
                message = await _translate.Translate("Seller Dashboard", lang),
                totalProducts,
                activeProducts,
                totalOrders,
                pendingOrders
            });
        }

        // 📸 UPLOAD PRODUCT IMAGE
        [Authorize(Roles = "Seller")]
        [HttpPost("upload-product-image")]
        public async Task<IActionResult> UploadProductImage([FromForm] int productId, [FromForm] IFormFile image)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // Validate product ownership
            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.ProductId == productId && p.SellerId == userId);

            if (product == null)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only upload images for your own products.", lang));

            // Validate image
            if (!_storage.IsValidImage(image))
                return BadRequest(await _translate.Translate(
                    "❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)", lang));

            try
            {
                // Upload to Cloudinary Storage
                var imageUrl = await _storage.UploadImageAsync(image, "product");

                // Save to database
                var productImage = new ProductImage
                {
                    ProductId = productId,
                    ImageUrl = imageUrl
                };

                _context.ProductImages.Add(productImage);
                await _context.SaveChangesAsync();

                return Ok(new
                {
                    message = await _translate.Translate("✔ Image uploaded successfully", lang),
                    imageId = productImage.ImageId,
                    imageUrl = imageUrl
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, await _translate.Translate(
                    $"❌ Upload failed: {ex.Message}", lang));
            }
        }

        // 📸 GET PRODUCT IMAGES
        [Authorize(Roles = "Seller")]
        [HttpGet("product-images/{productId}")]
        public async Task<IActionResult> GetProductImages(int productId)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.ProductId == productId && p.SellerId == userId);

            if (product == null)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only view images for your own products.", lang));

            var images = await _context.ProductImages.AsNoTracking()
                .Where(img => img.ProductId == productId)
                .Select(img => new
                {
                    img.ImageId,
                    img.ImageUrl
                })
                .ToListAsync();

            return Ok(images);
        }

        // 🗑️ DELETE PRODUCT IMAGE
        [Authorize(Roles = "Seller")]
        [HttpDelete("delete-product-image/{imageId}")]
        public async Task<IActionResult> DeleteProductImage(int imageId)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var image = await _context.ProductImages
                .Include(img => img.Product)
                .FirstOrDefaultAsync(img => img.ImageId == imageId);

            if (image == null)
                return NotFound(await _translate.Translate(
                    "❌ Image not found", lang));

            if (image.Product?.SellerId != userId)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only delete images from your own products.", lang));

            try
            {
                // Delete from Cloudinary Storage
                await _storage.DeleteImageAsync(image.ImageUrl);

                // Delete from database
                _context.ProductImages.Remove(image);
                await _context.SaveChangesAsync();

                return Ok(await _translate.Translate(
                    "✔ Image deleted successfully", lang));
            }
            catch (Exception ex)
            {
                return StatusCode(500, await _translate.Translate(
                    $"❌ Delete failed: {ex.Message}", lang));
            }
        }
    }
}
