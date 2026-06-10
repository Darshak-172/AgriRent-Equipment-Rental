using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.DTOs;
using Microsoft.AspNetCore.Authorization;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class SearchController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;
        private const int ROLE_OWNER = 3;

        public SearchController(AppDbContext context, TranslateService translate, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
        }

        /// <summary>
        /// Search for equipment and products by keyword, location, and category
        /// </summary>
        /// <param name="keyword">Search term for name and description</param>
        /// <param name="location">Filter by location</param>
        /// <param name="categoryId">Filter by category ID</param>
        /// <param name="page">Page number (default: 1)</param>
        /// <param name="pageSize">Items per page (default: 10, max: 100)</param>
        /// <returns>Paginated list of search results</returns>
        [AllowAnonymous]
        [HttpGet]
        public async Task<IActionResult> Search(
            string? keyword, 
            string? location, 
            int? categoryId,
            int page = 1,
            int pageSize = 10)
        {
            // Validate and limit page size
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;
            // Start with all approved equipment and products
            var equipmentQuery = _context.Equipments.AsNoTracking()
                .Include(e => e.Images)
                .Include(e => e.SubCategory)
                    .ThenInclude(sc => sc!.Category)
                .Join(_context.UserRoles,
                    e => e.OwnerId,
                    ur => ur.UserId,
                    (e, ur) => new { Equipment = e, Role = ur })
                .Where(x => x.Role != null && x.Role.RoleId == ROLE_OWNER && x.Equipment.Status == "Active")
                .Select(x => x.Equipment);

            var productQuery = _context.Products.AsNoTracking()
                .Include(p => p.Images)
                .Include(p => p.SubCategory)
                    .ThenInclude(sc => sc!.Category)
                .Where(p => p.Status == "Active" && p.Stock > 0);

            // Apply keyword filter (search in name and description)
            if (!string.IsNullOrWhiteSpace(keyword))
            {
                var searchTerm = $"%{keyword}%";
                equipmentQuery = equipmentQuery.Where(e => 
                    EF.Functions.Like(e.EquipmentName, searchTerm) || 
                    EF.Functions.Like(e.Description, searchTerm));
                
                productQuery = productQuery.Where(p => 
                    EF.Functions.Like(p.ProductName, searchTerm) || 
                    EF.Functions.Like(p.Description, searchTerm));
            }

            // Apply location filter
            if (!string.IsNullOrWhiteSpace(location))
            {
                equipmentQuery = equipmentQuery.Where(e => EF.Functions.Like(e.Location, $"%{location}%"));
                productQuery = productQuery.Where(p => EF.Functions.Like(p.Location, $"%{location}%"));
            }

            // Apply category filter
            if (categoryId.HasValue && categoryId.Value > 0)
            {
                equipmentQuery = equipmentQuery.Where(e => e.SubCategory != null && e.SubCategory.CategoryId == categoryId.Value);
                productQuery = productQuery.Where(p => p.SubCategory != null && p.SubCategory.CategoryId == categoryId.Value);
            }

            // Get total counts for pagination
            var equipmentTotal = await equipmentQuery.CountAsync();
            var productTotal = await productQuery.CountAsync();
            var totalItems = equipmentTotal + productTotal;

            // Calculate pagination
            var totalPages = (int)Math.Ceiling(totalItems / (double)pageSize);
            var skip = (page - 1) * pageSize;

            // Execute queries with pagination
            var equipmentResults = await equipmentQuery
                .Skip(skip)
                .Take(pageSize)
                .Select(e => new SearchResultDto
                {
                    EquipmentId = e.EquipmentId,
                    ProductId = 0,
                    Name = e.EquipmentName,
                    Description = e.Description,
                    Price = e.DailyPrice,
                    HourlyPrice = e.HourlyPrice,
                    DailyPrice = e.DailyPrice,
                    PriceType = "Daily",
                    Location = e.Location,
                    ImageUrl = e.Images.FirstOrDefault() != null ? e.Images.FirstOrDefault()!.ImageUrl : null,
                    Type = "Equipment",
                    CategoryName = e.SubCategory != null && e.SubCategory.Category != null ? e.SubCategory.Category.Name : "Uncategorized",
                    Status = e.Status
                })
                .ToListAsync();

            // Combine results
            var allResults = new List<SearchResultDto>();
            allResults.AddRange(equipmentResults);
            
            // Add products if we haven't reached the limit
            var remainingSlots = pageSize - equipmentResults.Count;
            if (remainingSlots > 0)
            {
                var productResults = await productQuery
                    .Skip(Math.Max(0, skip - equipmentTotal))
                    .Take(remainingSlots)
                    .Select(p => new SearchResultDto
                    {
                        EquipmentId = 0,
                        ProductId = p.ProductId,
                        Name = p.ProductName,
                        Description = p.Description,
                        Price = p.Price,
                        HourlyPrice = null,
                        DailyPrice = null,
                        PriceType = "Fixed",
                        Location = p.Location,
                        ImageUrl = p.Images.FirstOrDefault() != null ? p.Images.FirstOrDefault()!.ImageUrl : null,
                        Type = "Product",
                        CategoryName = p.SubCategory != null && p.SubCategory.Category != null ? p.SubCategory.Category.Name : "Uncategorized",
                        Status = p.Status
                    })
                    .ToListAsync();

                allResults.AddRange(productResults);
            }

            // ⭐ TRANSLATE ALL RESULTS CONCURRENTLY
            var lang = _langHelper.GetLanguage();
            if (lang != "en")
            {
                var translationTasks = allResults.Select(async result => 
                {
                    result.Name = await _translate.TransliterateToNative(result.Name, lang);       
                    result.Description = await _translate.Translate(result.Description, lang);     
                    result.CategoryName = await _translate.Translate(result.CategoryName, lang);   
                    result.Type = await _translate.Translate(result.Type, lang);                   
                    result.PriceType = await _translate.Translate(result.PriceType, lang);         
                    result.Location = await _translate.TransliterateToNative(result.Location, lang);
                    return result;
                });
                
                allResults = (await Task.WhenAll(translationTasks)).ToList();
            }

            return Ok(new
            {
                data = allResults,
                pagination = new
                {
                    page,
                    pageSize,
                    totalItems,
                    totalPages,
                    hasNextPage = page < totalPages,
                    hasPreviousPage = page > 1
                }
            });
        }

    }
}
