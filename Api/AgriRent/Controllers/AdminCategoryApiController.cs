using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using AgriRent.Data;
using AgriRent.Extensions;
using AgriRent.Models;
using AgriRent.DTOs;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/admin/categories")]
    [Authorize(Roles = "Admin")]
    public class AdminCategoryApiController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;
        private readonly IMemoryCache _cache;

        private static readonly string[] _langs  = { "en", "hi", "gu" };
        private static readonly string[] _types  = { "Equipment", "Product", "all" };

        public AdminCategoryApiController(AppDbContext context, TranslateService translate, LanguageHelper langHelper, IMemoryCache cache)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
            _cache = cache;
        }

        private void InvalidateCategoryCache()
        {
            foreach (var lang in _langs)
                foreach (var type in _types)
                    _cache.Remove($"categories_{lang}_{type}");
        }



        // ⭐ 1. GET ALL CATEGORIES (Admin View)
        [HttpGet]
        public async Task<IActionResult> GetCategories()
        {
            var lang = _langHelper.GetLanguage();
            var categories = await _context.Categories.AsNoTracking()
                .Include(c => c.SubCategories)
                .OrderByDescending(c => c.CreatedAt)
                .ToListAsync();

            var tasks = categories.Select(async c => new CategoryDto
            {
                CategoryId = c.CategoryId,
                Name = await _translate.Translate(c.Name, lang),
                Status = c.Status,
                Type = c.Type,
                SubCategories = c.SubCategories.Select(sc => new SubCategoryDto
                {
                    SubCategoryId = sc.SubCategoryId,
                    CategoryId = sc.CategoryId,
                    SubCategoryName = sc.SubCategoryName, 
                    Status = sc.Status
                }).ToList()
            });

            var result = (await Task.WhenAll(tasks)).ToList();

            return Ok(result);
        }

        // ⭐ 2. ADD CATEGORY
        [HttpPost]
        public async Task<IActionResult> CreateCategory([FromBody] CategoryDto dto)
        {
            var lang = _langHelper.GetLanguage();
            
            // Translate input to English for DB storage
            var englishName = await _translate.ToEnglish(dto.Name, lang);

            var category = new Category
            {
                Name = englishName,
                Status = dto.Status ?? "Active",
                Type = dto.Type ?? "Equipment",
                CreatedAt = IstHelper.Now
            };

            _context.Categories.Add(category);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Category created successfully", lang), categoryId = category.CategoryId });
        }

        // ⭐ 3. EDIT CATEGORY
        [HttpPut("{id}")]
        public async Task<IActionResult> UpdateCategory(int id, [FromBody] CategoryDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var category = await _context.Categories.FindAsync(id);
            if (category == null) return NotFound(new { message = await _translate.Translate("Category not found", lang) });

            category.Name = await _translate.ToEnglish(dto.Name, lang);
            category.Status = dto.Status ?? category.Status;
            category.Type = dto.Type ?? category.Type;

            _context.Categories.Update(category);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Category updated successfully", lang) });
        }

        // ⭐ 4. DELETE CATEGORY
        [HttpDelete("{id}")]
        public async Task<IActionResult> DeleteCategory(int id)
        {
            var lang = _langHelper.GetLanguage();
            var category = await _context.Categories.FindAsync(id);
            if (category == null) return NotFound(new { message = await _translate.Translate("Category not found", lang) });

            _context.Categories.Remove(category);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Category deleted successfully", lang) });
        }

        // ==========================
        // SUB-CATEGORY OPERATIONS
        // ==========================

        // ⭐ 5. ADD SUB-CATEGORY
        [HttpPost("subcategories")]
        public async Task<IActionResult> CreateSubCategory([FromBody] SubCategoryDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var categoryExists = await _context.Categories.AnyAsync(c => c.CategoryId == dto.CategoryId);
            if (!categoryExists) return BadRequest(new { message = await _translate.Translate("Invalid Category ID", lang) });

            var subCategory = new SubCategory
            {
                CategoryId = dto.CategoryId,
                SubCategoryName = await _translate.ToEnglish(dto.SubCategoryName, lang),
                Status = dto.Status ?? "Active"
            };

            _context.SubCategories.Add(subCategory);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Sub-category created successfully", lang), subCategoryId = subCategory.SubCategoryId });
        }

        // ⭐ 6. EDIT SUB-CATEGORY
        [HttpPut("subcategories/{id}")]
        public async Task<IActionResult> UpdateSubCategory(int id, [FromBody] SubCategoryDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var subCategory = await _context.SubCategories.FindAsync(id);
            if (subCategory == null) return NotFound(new { message = await _translate.Translate("Sub-category not found", lang) });

            subCategory.SubCategoryName = await _translate.ToEnglish(dto.SubCategoryName, lang);
            subCategory.Status = dto.Status ?? subCategory.Status;
            
            if (dto.CategoryId > 0)
                subCategory.CategoryId = dto.CategoryId;

            _context.SubCategories.Update(subCategory);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Sub-category updated successfully", lang) });
        }

        // ⭐ 7. DELETE SUB-CATEGORY
        [HttpDelete("subcategories/{id}")]
        public async Task<IActionResult> DeleteSubCategory(int id)
        {
            var lang = _langHelper.GetLanguage();
            var subCategory = await _context.SubCategories.FindAsync(id);
            if (subCategory == null) return NotFound(new { message = await _translate.Translate("Sub-category not found", lang) });

            _context.SubCategories.Remove(subCategory);
            await _context.SaveChangesAsync();
            InvalidateCategoryCache();

            return Ok(new { message = await _translate.Translate("Sub-category deleted successfully", lang) });
        }
    }
}
