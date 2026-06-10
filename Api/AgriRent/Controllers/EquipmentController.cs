using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Models;
using System.Security.Claims;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/equipment")]
    public class EquipmentController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly CloudinaryStorageService _storage;
        private readonly LanguageHelper _langHelper;

        public EquipmentController(AppDbContext context, TranslateService translate, CloudinaryStorageService storage, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _storage = storage;
            _langHelper = langHelper;
        }



        // ⭐ ADD EQUIPMENT (OWNER + ACTIVE SUBSCRIPTION)
        [Authorize(Roles = "Owner")]
        [HttpPost("add")]
        public async Task<IActionResult> AddEquipment(AddEquipmentDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // 1. Sum MaxEquipment across ALL active, non-expired plans
            int totalAllowed = await _context.UserSubscriptions
                .Include(s => s.Plan)
                .Where(s => s.UserId == userId && s.Status == "Active" && s.EndDate > DateTime.UtcNow)
                .SumAsync(s => s.Plan != null ? s.Plan.MaxEquipment : 0);

            if (totalAllowed == 0)
                return BadRequest(await _translate.Translate(
                    "❌ No active subscription with equipment slots. Please purchase a plan.", lang));

            // 2. Count Active + Pending equipment (slot usage includes queue)
            int currentCount = await _context.Equipments
                .CountAsync(e => e.OwnerId == userId && (e.Status == "Active" || e.Status == "Pending"));

            if (currentCount >= totalAllowed)
                return BadRequest(await _translate.Translate(
                    $"❌ Limit reached! You are using {currentCount}/{totalAllowed} equipment slots. Purchase an Add-on plan for more.", lang));

            // ⭐ INPUT TRANSLATION STRATEGY
            // 1. Names/Locations -> Transliterate (Phonetic: "Ramesh" -> "Ramesh")
            var nameEnglish = await _translate.TransliterateToEnglish(dto.EquipmentName, lang);
            var locationEnglish = await _translate.TransliterateToEnglish(dto.Location, lang);

            // 2. Description/Types -> Translate (Meaning: "Kheti" -> "Farming")
            var descEnglish = await _translate.ToEnglish(dto.Description, lang);

            var equipment = new Equipment
            {
                OwnerId = userId,
                EquipmentName = nameEnglish,
                Description = descEnglish,
                HourlyPrice = dto.HourlyPrice,
                DailyPrice = dto.DailyPrice,
                Location = locationEnglish,
                SubCategoryId = dto.SubCategoryId ?? 1,
                Status = "Pending"
            };

            _context.Equipments.Add(equipment);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                message = await _translate.Translate(
                    "✔ Equipment added successfully (Pending Admin Approval)", lang),
                equipmentId = equipment.EquipmentId
            });
        }

        // ⭐ OWNER → UPDATE EQUIPMENT
        [Authorize(Roles = "Owner,EquipmentOwner")]
        [HttpPut("update/{id}")]
        public async Task<IActionResult> UpdateEquipment(int id, [FromBody] AddEquipmentDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var equipment = await _context.Equipments
                .FirstOrDefaultAsync(e => e.EquipmentId == id && e.OwnerId == userId);

            if (equipment == null)
                return NotFound(await _translate.Translate("❌ Equipment not found or you are not the owner.", lang));

            // Transliterate & translate same as add
            equipment.EquipmentName = await _translate.TransliterateToEnglish(dto.EquipmentName, lang);
            equipment.Location      = await _translate.TransliterateToEnglish(dto.Location, lang);
            equipment.Description   = await _translate.ToEnglish(dto.Description, lang);
            equipment.HourlyPrice   = dto.HourlyPrice;
            equipment.DailyPrice    = dto.DailyPrice;
            equipment.SubCategoryId = dto.SubCategoryId ?? equipment.SubCategoryId;

            await _context.SaveChangesAsync();

            return Ok(new { message = await _translate.Translate("✔ Equipment updated successfully.", lang) });
        }

        // ⭐ OWNER → TOGGLE EQUIPMENT STATUS
        [Authorize(Roles = "Owner,EquipmentOwner")]
        [HttpPut("toggle-status/{id}")]
        public async Task<IActionResult> ToggleEquipmentStatus(int id)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var equipment = await _context.Equipments
                .FirstOrDefaultAsync(e => e.EquipmentId == id && e.OwnerId == userId);

            if (equipment == null)
                return NotFound(new { message = "Equipment not found or you are not the owner." });

            if (equipment.Status == "Pending" || equipment.Status == "Rejected")
                return BadRequest(new { message = $"Cannot toggle status. Equipment is currently {equipment.Status}." });

            equipment.Status = equipment.Status == "Active" ? "Blocked" : "Active";
            await _context.SaveChangesAsync();

            return Ok(new { message = $"Status changed to {equipment.Status}." });
        }

        // ⭐ OWNER → DELETE EQUIPMENT
        [Authorize(Roles = "Owner,EquipmentOwner")]
        [HttpDelete("delete/{id}")]
        public async Task<IActionResult> DeleteEquipment(int id)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var equipment = await _context.Equipments
                .Include(e => e.Images)
                .FirstOrDefaultAsync(e => e.EquipmentId == id && e.OwnerId == userId);

            if (equipment == null)
                return NotFound(await _translate.Translate("❌ Equipment not found or you are not the owner.", lang));

            // 🗑️ Delete all associated Cloudinary images first
            if (equipment.Images != null && equipment.Images.Any())
            {
                var deleteTasks = equipment.Images
                    .Where(img => !string.IsNullOrEmpty(img.ImageUrl))
                    .Select(img => _storage.DeleteImageAsync(img.ImageUrl));

                await Task.WhenAll(deleteTasks);
            }

            if (equipment.Images != null)
            {
                _context.EquipmentImages.RemoveRange(equipment.Images);
            }
            _context.Equipments.Remove(equipment);
            await _context.SaveChangesAsync();

            return Ok(new { message = await _translate.Translate("🗑 Equipment deleted successfully.", lang) });
        }


        [Authorize(Roles = "EquipmentOwner,Owner")]
        [HttpGet("my-list")]
        public async Task<IActionResult> MyEquipments()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var equipments = await _context.Equipments.AsNoTracking()
                .Where(e => e.OwnerId == userId)
                .Include(e => e.Owner)
                .Include(e => e.Images)
                .Include(e => e.SubCategory!)
                    .ThenInclude(sc => sc.Category!)
                .OrderByDescending(e => e.CreatedAt)
                .ToListAsync();

            var tasks = equipments.Select(async e => new
            {
                e.EquipmentId,
                e.OwnerId,
                Owner = e.Owner != null ? new
                {
                    e.Owner.UserId,
                    FullName = await _translate.TransliterateToNative(e.Owner.FullName, lang),
                    e.Owner.MobileNumber
                } : null,
                EquipmentName = await _translate.TransliterateToNative(e.EquipmentName, lang),
                Description = await _translate.Translate(e.Description, lang),
                e.HourlyPrice,
                e.DailyPrice,
                Location = await _translate.TransliterateToNative(e.Location, lang),
                SubCategoryId = e.SubCategoryId,
                SubCategoryName = e.SubCategory != null 
                    ? await _translate.Translate(e.SubCategory.SubCategoryName, lang) 
                    : null,
                CategoryId = e.SubCategory?.CategoryId,
                CategoryName = e.SubCategory?.Category != null 
                    ? await _translate.Translate(e.SubCategory.Category.Name, lang) 
                    : null,
                Images = e.Images.Select(img => new
                {
                    img.ImageId,
                    img.ImageUrl
                }).ToList(),
                ImageUrl = e.Images.Select(i => i.ImageUrl).FirstOrDefault(),
                e.Status,
                e.CreatedAt
            });

            var list = await Task.WhenAll(tasks);

            return Ok(list);
        }

        // ⭐ OWNER → BLOCK DATES
        [Authorize(Roles = "EquipmentOwner,Owner")]
        [HttpPost("block-dates")]
        public async Task<IActionResult> BlockDates(BlockDateDto dto)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var equipment = await _context.Equipments
                .FirstOrDefaultAsync(e => e.EquipmentId == dto.EquipmentId && e.OwnerId == userId);

            if (equipment == null)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only manage your own equipment.", lang));

            var block = new EquipmentAvailability
            {
                EquipmentId = dto.EquipmentId,
                StartDate = dto.StartDate,
                EndDate = dto.EndDate,
                Reason = dto.Reason ?? "Unavailable"
            };

            _context.EquipmentAvailability.Add(block);
            await _context.SaveChangesAsync();

            return Ok(await _translate.Translate(
                "🚫 Equipment marked unavailable for selected dates.", lang));
        }

        // ⭐ CHECK AVAILABILITY (PUBLIC)
        [Authorize]
        [HttpPost("check-availability")]
        public async Task<IActionResult> CheckAvailability(CheckAvailabilityDto dto)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments.FirstOrDefaultAsync(e => e.EquipmentId == dto.EquipmentId);
            if (equipment == null)
                return NotFound(await _translate.Translate(
                    "❌ Equipment not found", lang));

            var hours = (dto.EndDate - dto.StartDate).TotalHours;
            if (hours < 24 && hours < 2)
            {
                return BadRequest(await _translate.Translate(
                    "⏳ Minimum 2 hours required for short bookings", lang));
            }

            bool isBlocked = await _context.EquipmentAvailability
                .AnyAsync(b => b.EquipmentId == dto.EquipmentId &&
                          dto.StartDate <= b.EndDate &&
                          dto.EndDate >= b.StartDate);

            if (isBlocked)
                return BadRequest(await _translate.Translate(
                    "🚫 Equipment is not available on these dates (Owner Blocked)", lang));

            bool isBooked = await _context.EquipmentBookings
                .AnyAsync(b => b.EquipmentId == dto.EquipmentId &&
                          dto.StartDate <= b.EndDate &&
                          dto.EndDate >= b.StartDate);

            if (isBooked)
                return BadRequest(await _translate.Translate(
                    "❌ Equipment already booked for the selected time", lang));

            return Ok(await _translate.Translate(
                "🟢 Available! You can proceed to booking.", lang));
        }

        // ⭐ VIEW BLOCKED DATES - Returns owner-blocked dates AND active customer bookings (public)
        [Microsoft.AspNetCore.Authorization.AllowAnonymous]
        [HttpGet("blocked-dates/{equipmentId}")]
        public async Task<IActionResult> GetBlockedDates(int equipmentId)
        {
            var equipment = await _context.Equipments.FirstOrDefaultAsync(e => e.EquipmentId == equipmentId);
            if (equipment == null)
                return Ok(new List<object>()); // Return empty array instead of 404

            // Owner-blocked date ranges
            var ownerBlocks = await _context.EquipmentAvailability.AsNoTracking()
                .Where(b => b.EquipmentId == equipmentId)
                .Select(b => new
                {
                    startDate = b.StartDate.ToString("yyyy-MM-ddTHH:mm:ss"),
                    endDate   = b.EndDate.ToString("yyyy-MM-ddTHH:mm:ss"),
                    reason    = b.Reason ?? "Unavailable",
                    type      = "blocked"
                })
                .ToListAsync<object>();

            // Active customer bookings (Only Accepted ones block the calendar)
            var customerBookings = await _context.EquipmentBookings.AsNoTracking()
                .Where(b => b.EquipmentId == equipmentId && b.Status == "Accepted")
                .Select(b => new
                {
                    startDate = b.StartDate.ToString("yyyy-MM-ddTHH:mm:ss"),
                    endDate   = b.EndDate.ToString("yyyy-MM-ddTHH:mm:ss"),
                    reason    = "Booked",
                    type      = "booked"
                })
                .ToListAsync<object>();

            var allDates = ownerBlocks.Concat(customerBookings).ToList();

            // Always return a JSON array so Android Retrofit can deserialize it properly
            return Ok(allDates);
        }

        // 📸 UPLOAD EQUIPMENT IMAGE
        [Authorize(Roles = "Owner")]
        [HttpPost("upload-image")]
        public async Task<IActionResult> UploadEquipmentImage([FromForm] int equipmentId, [FromForm] IFormFile image)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // Validate equipment ownership
            var equipment = await _context.Equipments
                .FirstOrDefaultAsync(e => e.EquipmentId == equipmentId && e.OwnerId == userId);

            if (equipment == null)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only upload images for your own equipment.", lang));

            // Validate image
            if (!_storage.IsValidImage(image))
                return BadRequest(await _translate.Translate(
                    "❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)", lang));

            try
            {
                // Upload to Cloudinary Storage
                var imageUrl = await _storage.UploadImageAsync(image, "equipment");

                // Save to database
                var equipmentImage = new EquipmentImage
                {
                    EquipmentId = equipmentId,
                    ImageUrl = imageUrl
                };

                _context.EquipmentImages.Add(equipmentImage);
                await _context.SaveChangesAsync();

                return Ok(new
                {
                    message = await _translate.Translate("✔ Image uploaded successfully", lang),
                    imageId = equipmentImage.ImageId,
                    imageUrl = imageUrl
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, await _translate.Translate(
                    $"❌ Upload failed: {ex.Message}", lang));
            }
        }

        // 📸 GET EQUIPMENT IMAGES
        [Authorize]
        [HttpGet("{equipmentId}/images")]
        public async Task<IActionResult> GetEquipmentImages(int equipmentId)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments
                .FirstOrDefaultAsync(e => e.EquipmentId == equipmentId);

            if (equipment == null)
                return NotFound(await _translate.Translate(
                    "❌ Equipment not found", lang));

            var images = await _context.EquipmentImages.AsNoTracking()
                .Where(img => img.EquipmentId == equipmentId)
                .Select(img => new
                {
                    img.ImageId,
                    img.ImageUrl
                })
                .ToListAsync();

            return Ok(images);
        }

        // 🗑️ DELETE EQUIPMENT IMAGE
        [Authorize(Roles = "Owner")]
        [HttpDelete("delete-image/{imageId}")]
        public async Task<IActionResult> DeleteEquipmentImage(int imageId)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var image = await _context.EquipmentImages
                .Include(img => img.Equipment)
                .FirstOrDefaultAsync(img => img.ImageId == imageId);

            if (image == null)
                return NotFound(await _translate.Translate(
                    "❌ Image not found", lang));

            if (image.Equipment?.OwnerId != userId)
                return Unauthorized(await _translate.Translate(
                    "❌ You can only delete images from your own equipment.", lang));

            try
            {
                // Delete from Cloudinary Storage
                await _storage.DeleteImageAsync(image.ImageUrl);

                // Delete from database
                _context.EquipmentImages.Remove(image);
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

        // ⭐ GET EQUIPMENT DETAILS BY ID (ADMIN - NO STATUS FILTER)
        [Authorize]
        [HttpGet("admin/details/{id}")]
        public async Task<IActionResult> GetEquipmentDetailsForAdmin(int id)
        {
            var lang = _langHelper.GetLanguage();

            var equipment = await _context.Equipments.AsNoTracking()
                .Include(e => e.Owner)
                .Include(e => e.SubCategory)
                .Include(e => e.Images)
                .FirstOrDefaultAsync(e => e.EquipmentId == id);

            if (equipment == null)
                return NotFound(new { message = "Equipment not found" });

            return Ok(new
            {
                equipment.EquipmentId,
                equipment.EquipmentName,
                equipment.Description,
                equipment.HourlyPrice,
                equipment.DailyPrice,
                equipment.Location,
                equipment.Status,
                Category = equipment.SubCategory?.SubCategoryName,
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