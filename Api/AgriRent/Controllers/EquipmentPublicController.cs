using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/public/equipment")]
    public class EquipmentPublicController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;

        public EquipmentPublicController(AppDbContext context, TranslateService translate, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
        }



        // ⭐ GET AVAILABLE EQUIPMENT (Optional Date Filter)
        // Made public for homepage display - no auth required
        [HttpGet("available")]
        public async Task<IActionResult> GetAvailable(
            DateTime? startDate,
            DateTime? endDate,
            string? location = null,
            int? categoryId = null,
            int? subCategoryId = null,
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 10)
        {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;
            var lang = _langHelper.GetLanguage();

            IQueryable<AgriRent.Models.Equipment> query = _context.Equipments.AsNoTracking()
                .Where(e => e.Status == "Active")
                .Include(e => e.Owner)
                .Include(e => e.Images)
                .Include(e => e.SubCategory!)
                    .ThenInclude(sc => sc!.Category);

            // 🔍 Apply filters
            if (!string.IsNullOrWhiteSpace(location))
            {
                query = query.Where(e => EF.Functions.Like(e.Location, $"%{location}%"));
            }

            if (categoryId.HasValue)
            {
                query = query.Where(e => e.SubCategory != null && e.SubCategory.CategoryId == categoryId);
            }

            if (subCategoryId.HasValue)
            {
                query = query.Where(e => e.SubCategoryId == subCategoryId);
            }

            // 🛑 Exclude blocked equipment if dates provided
            if (startDate.HasValue && endDate.HasValue)
            {
                var blockedIds = await _context.EquipmentAvailability.AsNoTracking()
                    .Where(a =>
                        startDate <= a.EndDate &&
                        endDate >= a.StartDate)
                    .Select(a => a.EquipmentId)
                    .Distinct()
                    .ToListAsync();

                query = query.Where(e => !blockedIds.Contains(e.EquipmentId));
            }

            var totalCount = await query.CountAsync();
            var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

            // 🔽 Fetch from DB first
            var equipments = await query
                .OrderByDescending(e => e.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(e => new
                {
                    e.EquipmentId,
                    EquipmentName = e.EquipmentName,
                    Description = e.Description,
                    e.HourlyPrice,
                    e.DailyPrice,
                    e.Location,
                    SubCategoryId = e.SubCategoryId,
                    SubCategoryName = e.SubCategory != null ? e.SubCategory.SubCategoryName : null,
                    CategoryId = e.SubCategory != null ? e.SubCategory.CategoryId : (int?)null,
                    CategoryName = e.SubCategory != null && e.SubCategory.Category != null ? e.SubCategory.Category.Name : null,
                    ImageUrl = e.Images.Select(i => i.ImageUrl).FirstOrDefault(),
                    Images = e.Images.Select(img => new { img.ImageId, img.ImageUrl }).ToList(),
                    OwnerName = e.Owner != null ? e.Owner.FullName : "Unknown",
                    OwnerMobile = e.Owner != null ? e.Owner.MobileNumber : string.Empty,
                    AverageRating = _context.Ratings.Where(r => r.TargetId == e.EquipmentId && r.RatingType == "Equipment").Average(r => (double?)r.RatingValue) ?? 0.0,
                    ReviewCount = _context.Ratings.Count(r => r.TargetId == e.EquipmentId && r.RatingType == "Equipment")
                })
                .ToListAsync();

            // ⭐ TRANSLATE EQUIPMENT DATA
            if (lang != "en")
            {
                var tasks = equipments.Select(async e => (object)new
                {
                    e.EquipmentId,
                    EquipmentName = await _translate.TransliterateToNative(e.EquipmentName, lang),
                    Description = await _translate.Translate(e.Description, lang),
                    e.HourlyPrice,
                    e.DailyPrice,
                    Location = await _translate.TransliterateToNative(e.Location, lang),
                    e.SubCategoryId,
                    SubCategoryName = !string.IsNullOrEmpty(e.SubCategoryName) ? await _translate.Translate(e.SubCategoryName, lang) : null,
                    e.CategoryId,
                    CategoryName = !string.IsNullOrEmpty(e.CategoryName) ? await _translate.Translate(e.CategoryName, lang) : null,
                    e.ImageUrl,
                    e.Images,
                    OwnerName = await _translate.TransliterateToNative(e.OwnerName, lang),
                    e.OwnerMobile,
                    e.AverageRating,
                    e.ReviewCount
                });

                var translated = (await Task.WhenAll(tasks)).ToList();

                return Ok(new {
                    data = translated,
                    pagination = new { page, pageSize, totalItems = totalCount, totalPages }
                });
            }

            // 🟢 English response
            return Ok(new {
                data = equipments.Select(e => new
                {
                    e.EquipmentId,
                    e.EquipmentName,
                    e.Description,
                    e.HourlyPrice,
                    e.DailyPrice,
                    e.Location,
                    e.SubCategoryId,
                    e.SubCategoryName,
                    e.CategoryId,
                    e.CategoryName,
                    e.ImageUrl,
                    e.Images,
                    Owner = e.OwnerName,
                    e.OwnerMobile,
                    e.AverageRating,
                    e.ReviewCount
                }),
                pagination = new { page, pageSize, totalItems = totalCount, totalPages }
            });
        }

        // ⭐ GET EQUIPMENT DETAILS BY ID
        [HttpGet("{id}")]
        public async Task<IActionResult> GetEquipmentDetails(int id)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments.AsNoTracking()
                .Include(e => e.Owner)
                .Include(e => e.SubCategory)
                .Include(e => e.Images)
                .FirstOrDefaultAsync(e => e.EquipmentId == id && e.Status == "Active");

            if (equipment == null)
                return NotFound(await _translate.Translate("Equipment not found", lang));

            // ⭐ TRANSLATE EQUIPMENT DATA
            if (lang != "en")
            {
                return Ok(new
                {
                    equipment.EquipmentId,
                    EquipmentName = await _translate.TransliterateToNative(equipment.EquipmentName, lang), // Transliterate
                    Description = await _translate.Translate(equipment.Description, lang),                 // Translate
                    Category = equipment.SubCategory != null ? await _translate.Translate(equipment.SubCategory.SubCategoryName, lang) : null, // Translate
                    equipment.HourlyPrice,
                    equipment.DailyPrice,
                    Location = await _translate.TransliterateToNative(equipment.Location, lang),           // Transliterate
                    ImageUrl = equipment.Images?.FirstOrDefault()?.ImageUrl,
                    Owner = equipment.Owner != null ? new
                    {
                        FullName = await _translate.TransliterateToNative(equipment.Owner.FullName, lang), // Transliterate
                        equipment.Owner.MobileNumber
                    } : null
                });
            }

            return Ok(new
            {
                equipment.EquipmentId,
                equipment.EquipmentName,
                equipment.Description,
                Category = equipment.SubCategory?.SubCategoryName,
                equipment.HourlyPrice,
                equipment.DailyPrice,
                equipment.Location,
                ImageUrl = equipment.Images?.FirstOrDefault()?.ImageUrl,
                Owner = equipment.Owner != null ? new
                {
                    equipment.Owner.FullName,
                    equipment.Owner.MobileNumber
                } : null
            });
        }
    }
}