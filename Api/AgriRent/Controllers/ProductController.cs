using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Models;
using AgriRent.Services;
using System.Security.Claims;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/products")]
    public class ProductController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;

        public ProductController(AppDbContext context, TranslateService translate, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
        }



        // ⭐ VIEW ALL APPROVED PRODUCTS (PUBLIC)
        [HttpGet("list")]
        public async Task<IActionResult> GetProducts(
            [FromQuery] string? category, 
            [FromQuery] string? location,
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 10)
        {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;
            var lang = _langHelper.GetLanguage();

            var query = _context.Products.AsNoTracking()
                .Where(p => p.Status == "Active" && p.Stock > 0)
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .AsQueryable();

            if (!string.IsNullOrWhiteSpace(category))
                query = query.Where(p => p.SubCategory != null && p.SubCategory.SubCategoryName == category);


            if (!string.IsNullOrWhiteSpace(location))
                query = query.Where(p => EF.Functions.Like(p.Location, $"%{location}%"));

            var totalCount = await query.CountAsync();
            var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

            var products = await query
                .OrderByDescending(p => p.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(p => new
                {
                    p.ProductId,
                    ProductName = p.ProductName,
                    Description = p.Description,
                    Category = p.SubCategory != null ? p.SubCategory.SubCategoryName : null,
                    p.Price,
                    p.Stock,
                    p.Unit,
                    p.Location,
                    ImageUrl = p.Images.Select(i => i.ImageUrl).FirstOrDefault(),
                    Images = p.Images.Select(img => new { img.ImageId, img.ImageUrl }).ToList(),
                    SellerName = p.Seller != null ? p.Seller.FullName : "Unknown",
                    SellerMobile = p.Seller != null ? p.Seller.MobileNumber : null,
                    AverageRating = _context.Ratings.Where(r => r.TargetId == p.ProductId && r.RatingType == "Product").Average(r => (double?)r.RatingValue) ?? 0.0,
                    ReviewCount = _context.Ratings.Count(r => r.TargetId == p.ProductId && r.RatingType == "Product")
                })
                .ToListAsync();

            // 🌐 Translate if not English
            if (lang != "en")
            {
                var tasks = products.Select(async p => (object)new
                {
                    p.ProductId,
                    ProductName = await _translate.TransliterateToNative(p.ProductName, lang), 
                    Description = await _translate.Translate(p.Description, lang),             
                    Category = await _translate.Translate(p.Category, lang),                   
                    p.Price,
                    p.Stock,
                    Unit = await _translate.Translate(p.Unit, lang),                           
                    Location = await _translate.TransliterateToNative(p.Location, lang),       
                    ImageUrl = p.ImageUrl,
                    p.Images,
                    Seller = await _translate.TransliterateToNative(p.SellerName, lang),       
                    SellerMobile = p.SellerMobile,
                    p.AverageRating,
                    p.ReviewCount
                });
                
                var translated = (await Task.WhenAll(tasks)).ToList();
                return Ok(new {
                    data = translated,
                    pagination = new { page, pageSize, totalItems = totalCount, totalPages }
                });
            }

            return Ok(new {
                data = products.Select(p => new
                {
                    p.ProductId,
                    p.ProductName,
                    p.Description,
                    p.Category,
                    p.Price,
                    p.Stock,
                    p.Unit,
                    p.Location,
                    p.ImageUrl,
                    p.Images,
                    Seller = p.SellerName,
                    SellerMobile = p.SellerMobile,
                    p.AverageRating,
                    p.ReviewCount
                }),
                pagination = new { page, pageSize, totalItems = totalCount, totalPages }
            });
        }

        // ⭐ VIEW PRODUCT DETAILS
        [HttpGet("{id}")]
        public async Task<IActionResult> GetProductDetails(int id)
        {
            var lang = _langHelper.GetLanguage();

            var product = await _context.Products.AsNoTracking()
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .FirstOrDefaultAsync(p => p.ProductId == id && p.Status == "Active");

            if (product == null)
                return NotFound(await _translate.Translate("Product not found", lang));

            // ⭐ TRANSLATE PRODUCT DATA
            if (lang != "en")
            {
                return Ok(new
                {
                    product.ProductId,
                    ProductName = await _translate.TransliterateToNative(product.ProductName, lang), // Transliterate
                    Description = await _translate.Translate(product.Description, lang),             // Translate
                    Category = product.SubCategory != null ? await _translate.Translate(product.SubCategory.SubCategoryName, lang) : null, // Translate
                    product.Price,
                    product.Stock,
                    Unit = await _translate.Translate(product.Unit, lang),                           // Translate
                    Location = await _translate.TransliterateToNative(product.Location, lang),       // Transliterate
                    ImageUrl = product.Images?.FirstOrDefault()?.ImageUrl,
                    Seller = product.Seller != null ? new
                    {
                        FullName = await _translate.TransliterateToNative(product.Seller.FullName, lang), // Transliterate
                        product.Seller.MobileNumber
                    } : null
                });
            }

            return Ok(new
            {
                product.ProductId,
                product.ProductName,
                product.Description,
                Category = product.SubCategory?.SubCategoryName,
                product.Price,
                product.Stock,
                product.Unit,
                product.Location,
                ImageUrl = product.Images?.FirstOrDefault()?.ImageUrl,
                Seller = product.Seller != null ? new
                {
                    product.Seller.FullName,
                    product.Seller.MobileNumber
                } : null
            });
        }

        // ⭐ PLACE ORDER (FARMER)
        [Authorize(Roles = "Farmer")]
        [HttpPost("place-order")]
        public async Task<IActionResult> PlaceOrder(CreateOrderDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var farmerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var product = await _context.Products
                .FirstOrDefaultAsync(p => p.ProductId == dto.ProductId && p.Status == "Active");

            if (product == null)
                return NotFound(await _translate.Translate("Product not found", lang));

            if (product.Stock < dto.Quantity)
                return BadRequest(await _translate.Translate("Insufficient stock available", lang));

            var totalPrice = product.Price * dto.Quantity;

            var order = new Order
            {
                FarmerId = farmerId,
                TotalAmount = totalPrice,
                DeliveryAddress = dto.DeliveryAddress,
                ContactNumber = dto.ContactNumber,
                PaymentId = $"ORD_{DateTime.Now:yyyyMMddHHmmss}_{farmerId}",
                OrderStatus = "Pending",
                PaymentStatus = "Pending",
                OrderItems = new List<OrderItem>
                {
                    new OrderItem
                    {
                        ProductId = dto.ProductId,
                        Quantity = dto.Quantity,
                        Price = product.Price
                    }
                }
            };

            // Reduce stock
            product.Stock -= dto.Quantity;

            _context.Orders.Add(order);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = await _translate.Translate("Order placed successfully", lang),
                orderId = order.OrderId,
                totalAmount = totalPrice
            });
        }

        // ⭐ VIEW MY ORDERS (FARMER)
        [Authorize(Roles = "Farmer")]
        [HttpGet("my-orders")]
        public async Task<IActionResult> MyOrders()
        {
            var lang = _langHelper.GetLanguage();
            var farmerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var orders = await _context.Orders.AsNoTracking()
                .Include(o => o.OrderItems)
                .ThenInclude(i => i.Product)
                .ThenInclude(p => p!.Seller)
                .Include(o => o.OrderItems)
                .ThenInclude(i => i.Product)
                .ThenInclude(p => p!.Images)
                .Where(o => o.FarmerId == farmerId)
                .OrderByDescending(o => o.CreatedAt)
                .ToListAsync();

            var result = new List<object>();

            foreach (var o in orders)
            {
                var firstItem = o.OrderItems.FirstOrDefault();
                result.Add(new
                {
                    o.OrderId,
                    ProductId = firstItem?.ProductId,
                    ProductName = firstItem?.Product != null 
                        ? await _translate.TransliterateToNative(firstItem.Product.ProductName, lang) 
                        : "Unknown",
                    SellerName = firstItem?.Product?.Seller != null 
                        ? await _translate.TransliterateToNative(firstItem.Product.Seller.FullName, lang) 
                        : "Unknown",
                    Quantity = firstItem?.Quantity ?? 0,
                    UnitPrice = firstItem?.Price ?? 0,
                    TotalPrice = o.TotalAmount,
                    Status = await _translate.Translate(o.OrderStatus, lang),
                    PaymentStatus = await _translate.Translate(o.PaymentStatus, lang),
                    OrderDate = o.CreatedAt,
                    o.DeliveryDate,
                    ImageUrl = firstItem?.Product?.Images?.FirstOrDefault()?.ImageUrl
                });
            }

            return Ok(result);
        }

        // ⭐ CANCEL ORDER (FARMER - if pending)
        [Authorize(Roles = "Farmer")]
        [HttpPut("cancel-order/{orderId}")]
        public async Task<IActionResult> CancelOrder(int orderId)
        {
            var lang = _langHelper.GetLanguage();
            var farmerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var order = await _context.Orders
                .Include(o => o.OrderItems)
                .ThenInclude(i => i.Product)
                .FirstOrDefaultAsync(o => o.OrderId == orderId && o.FarmerId == farmerId);

            if (order == null)
                return NotFound(await _translate.Translate("Order not found or access denied.", lang));

            if (order.OrderStatus != "Pending")
                return BadRequest(await _translate.Translate("Only pending orders can be canceled.", lang));

            order.OrderStatus = "Canceled";

            // Restore product stock
            var firstItem = order.OrderItems.FirstOrDefault();
            if (firstItem?.Product != null)
            {
                firstItem.Product.Stock += firstItem.Quantity;
            }

            await _context.SaveChangesAsync();

            return Ok(await _translate.Translate("Order cancelled successfully", lang));
        }

        // 📸 GET PRODUCT IMAGES (PUBLIC)
        [Authorize]
        [HttpGet("{productId}/images")]
        public async Task<IActionResult> GetProductImages(int productId)
        {
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

        // ⭐ GET PRODUCT DETAILS BY ID (ADMIN - NO STATUS FILTER)
        [Authorize]
        [HttpGet("admin/details/{id}")]
        public async Task<IActionResult> GetProductDetailsForAdmin(int id)
        {
            var lang = _langHelper.GetLanguage();

            var product = await _context.Products.AsNoTracking()
                .Include(p => p.Seller)
                .Include(p => p.SubCategory)
                .Include(p => p.Images)
                .FirstOrDefaultAsync(p => p.ProductId == id);

            if (product == null)
                return NotFound(new { message = "Product not found" });

            return Ok(new
            {
                product.ProductId,
                product.ProductName,
                product.Description,
                product.Price,
                product.Stock,
                product.Unit,
                product.Location,
                product.Status,
                Category = product.SubCategory?.SubCategoryName,
                ImageUrl = product.Images?.FirstOrDefault()?.ImageUrl,
                Seller = product.Seller != null ? new
                {
                    product.Seller.FullName,
                    product.Seller.MobileNumber
                } : null
            });
        }
    }
}
