using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/public/categories")]
    public class CategoryPublicController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly IMemoryCache _cache;

        public CategoryPublicController(AppDbContext context, TranslateService translate, IMemoryCache cache)
        {
            _context = context;
            _translate = translate;
            _cache = cache;
        }

        private string GetLang()
        {
            var lang = Request.Headers["Accept-Language"].ToString();
            if (string.IsNullOrWhiteSpace(lang))
                return "en";
            return lang.Split(',')[0].Split('-')[0].ToLower();
        }

        // ⭐ GET ALL ACTIVE CATEGORIES (For Dropdowns/Browsing)
        [HttpGet]
        public async Task<IActionResult> GetCategories([FromQuery] string? type = null)
        {
            var lang = GetLang();
            string cacheKey = $"categories_{lang}_{type ?? "all"}";

            if (_cache.TryGetValue(cacheKey, out List<CategoryDto>? cachedCategories))
            {
                return Ok(cachedCategories);
            }

            var query = _context.Categories.AsNoTracking()
                .Where(c => c.Status == "Active");

            if (!string.IsNullOrWhiteSpace(type))
                query = query.Where(c => c.Type == type);

            var categories = await query
                .Include(c => c.SubCategories)
                .OrderBy(c => c.Name)
                .ToListAsync();

            var categoryTasks = categories.Select(async c =>
            {
                var subCategoryTasks = c.SubCategories.Where(s => s.Status == "Active").Select(async sc => new SubCategoryDto
                {
                    SubCategoryId = sc.SubCategoryId,
                    CategoryId = sc.CategoryId,
                    SubCategoryName = lang != "en" ? await _translate.Translate(sc.SubCategoryName, lang) : sc.SubCategoryName,
                    Status = sc.Status
                });

                var subCategories = (await Task.WhenAll(subCategoryTasks)).ToList();

                return new CategoryDto
                {
                    CategoryId = c.CategoryId,
                    Name = lang != "en" ? await _translate.Translate(c.Name, lang) : c.Name,
                    Status = c.Status,
                    Type = c.Type,
                    SubCategories = subCategories
                };
            });

            var result = (await Task.WhenAll(categoryTasks)).ToList();

            // Cache for 1 hour since categories rarely change
            var cacheOptions = new MemoryCacheEntryOptions()
                .SetAbsoluteExpiration(TimeSpan.FromHours(1));
            _cache.Set(cacheKey, result, cacheOptions);

            return Ok(result);
        }
    }
}
