using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.Services;       // 👈 ADD THIS
using System.Linq;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/admin/equipment")]
    [Authorize(Roles = "Admin")]  // 🔐 Only Admin
    public class AdminEquipmentController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;
        private readonly CloudinaryStorageService _storage;
        private readonly NotificationService _notificationService;

        public AdminEquipmentController(AppDbContext context, TranslateService translate, LanguageHelper langHelper, CloudinaryStorageService storage, NotificationService notificationService)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
            _storage = storage;
            _notificationService = notificationService;
        }

        // 🌍 Auto detect language


        // ⭐ 1) GET ALL EQUIPMENTS (With Status Filter)
        [HttpGet("")]  // Maps to /api/admin/equipment
        public async Task<IActionResult> GetEquipments(
            [FromQuery] string status = "Pending",
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 10)
        {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100;

            var lang = _langHelper.GetLanguage();

            var query = _context.Equipments.AsNoTracking().AsQueryable();

            if (!string.IsNullOrEmpty(status) && !status.Equals("All", StringComparison.OrdinalIgnoreCase))
            {
                // Case-insensitive filtering
                status = status.ToLower(); // normalized for check
                query = query.Where(e => e.Status.ToLower() == status);
            }

            var totalCount = await query.CountAsync();
            var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

            // ⭐ Use Projection (.Select) to ensure LEFT IOIN behavior for Owner/Images
            // This prevents "Inner Join" from hiding equipments with missing owners
            var list = await query
                .OrderByDescending(e => e.CreatedAt)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(e => new
                {
                    e.EquipmentId,
                    e.OwnerId,
                    OwnerName = e.Owner != null ? e.Owner.FullName : "Unknown",
                    OwnerMobile = e.Owner != null ? e.Owner.MobileNumber : "N/A",
                    e.EquipmentName,
                    e.Description,
                    e.HourlyPrice,
                    e.DailyPrice,
                    e.Location,
                    ImageUrl = e.Images.Select(i => i.ImageUrl).FirstOrDefault(), // EF Core compatible
                    e.Status,
                    e.CreatedAt
                }) // Executes SQL with LEFT JOINS
                .ToListAsync();

            var translatedList = new List<object>();

            foreach (var e in list)
            {
                var eqName = e.EquipmentName;
                var desc = e.Description;
                var loc = e.Location;

                try
                {
                    eqName = await _translate.TransliterateToNative(e.EquipmentName, lang);
                    desc = await _translate.Translate(e.Description, lang);
                    loc = await _translate.TransliterateToNative(e.Location, lang);
                }
                catch
                {
                    // Ignore translation errors, return original text
                }

                translatedList.Add(new
                {
                    e.EquipmentId,
                    e.OwnerId,
                    e.OwnerName,
                    e.OwnerMobile,
                    EquipmentName = eqName,
                    Description = desc,
                    e.HourlyPrice,
                    e.DailyPrice,
                    Location = loc,
                    e.ImageUrl,
                    e.Status,
                    e.CreatedAt
                });
            }

            return Ok(new
            {
                data = translatedList,
                pagination = new { page, pageSize, totalItems = totalCount, totalPages }
            });
        }


        // ⭐ 2) APPROVE EQUIPMENT
        [HttpPut("approve/{id}")]
        public async Task<IActionResult> Approve(int id)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments.FindAsync(id);
            if (equipment == null)
                return NotFound(await _translate.Translate("Equipment not found", lang));

            equipment.Status = "Active";
            await _context.SaveChangesAsync();

            await _notificationService.SendNotificationAsync(
                equipment.OwnerId,
                "Equipment Approved",
                $"{equipment.EquipmentName} is now live",
                "Equipment"
            );

            return Ok(new {
                message = await _translate.Translate("Equipment Approved Successfully", lang),
                equipmentId = equipment.EquipmentId
            });
        }


        // ⭐ 3) REJECT EQUIPMENT
        [HttpPut("reject/{id}")]
        public async Task<IActionResult> Reject(int id)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments.FindAsync(id);
            if (equipment == null)
                return NotFound(await _translate.Translate("Equipment not found", lang));

            equipment.Status = "Rejected";
            await _context.SaveChangesAsync();

            await _notificationService.SendNotificationAsync(
                equipment.OwnerId,
                "Equipment Rejected",
                $"{equipment.EquipmentName} was not approved",
                "Equipment"
            );

            return Ok(new {
                message = await _translate.Translate("Equipment Rejected", lang),
                equipmentId = equipment.EquipmentId
            });
        }


        // ⭐ 4) DELETE / REMOVE EQUIPMENT
        [HttpDelete("delete/{id}")]
        public async Task<IActionResult> Delete(int id)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments
                .Include(e => e.Images)
                .FirstOrDefaultAsync(e => e.EquipmentId == id);

            if (equipment == null)
                return NotFound(await _translate.Translate("Equipment not found", lang));

            // 🗑️ Delete all associated Cloudinary images first
            if (equipment.Images != null && equipment.Images.Any())
            {
                var deleteTasks = equipment.Images
                    .Where(img => !string.IsNullOrEmpty(img.ImageUrl))
                    .Select(img => _storage.DeleteImageAsync(img.ImageUrl));

                await Task.WhenAll(deleteTasks);
            }

            _context.Equipments.Remove(equipment);
            await _context.SaveChangesAsync();

            return Ok(new {
                message = await _translate.Translate("Equipment Removed Successfully", lang),
                equipmentId = id
            });
        }
    }
}