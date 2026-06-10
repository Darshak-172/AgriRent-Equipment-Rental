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
    [Route("api/booking")]
    public class BookingController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translator;
        private readonly NotificationService _notificationService;

        public BookingController(AppDbContext context, TranslateService translator, NotificationService notificationService)
        {
            _context = context;
            _translator = translator;
            _notificationService = notificationService;
        }

        // 🌍 Language helper
        private string GetLang()
        {
            var lang = Request.Headers["Accept-Language"].ToString();
            return string.IsNullOrWhiteSpace(lang) ? "en" : lang.Split(',')[0].Split('-')[0];
        }

        // ======================================================
        // ⭐ CREATE BOOKING REQUEST (FARMER)
        // ======================================================
        [Authorize(Roles = "Farmer")]
        [HttpPost("create")]
        public async Task<IActionResult> CreateBooking(CreateBookingDto dto)
        {
            var lang = GetLang();
            var farmerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            if (dto.StartDate >= dto.EndDate)
                return BadRequest(await _translator.Translate("Invalid booking dates", lang));
            
            if (dto.StartDate < DateTime.UtcNow)
                return BadRequest(await _translator.Translate("Start date cannot be in the past", lang));

            var equipment = await _context.Equipments
                .Include(e => e.Owner)
                .FirstOrDefaultAsync(e => e.EquipmentId == dto.EquipmentId);

            if (equipment == null)
                return NotFound(await _translator.Translate("Equipment not found", lang));

            bool blocked = await _context.EquipmentAvailability.AnyAsync(b =>
                b.EquipmentId == dto.EquipmentId &&
                dto.StartDate < b.EndDate &&
                dto.EndDate > b.StartDate);

            if (blocked)
                return BadRequest(await _translator.Translate(
                    "Equipment unavailable for selected dates", lang));

            bool booked = await _context.EquipmentBookings.AnyAsync(b =>
                b.EquipmentId == dto.EquipmentId &&
                b.Status == "Accepted" &&
                dto.StartDate < b.EndDate &&
                dto.EndDate > b.StartDate);

            if (booked)
                return BadRequest(await _translator.Translate(
                    "Equipment already booked in this period", lang));

            decimal totalPrice;
            var duration = dto.EndDate - dto.StartDate;

            if (duration.TotalHours < 2)
            {
                return BadRequest(await _translator.Translate("Minimum 2 hours required", lang));
            }

            if (duration.TotalHours < 24)
            {
                totalPrice = (decimal)duration.TotalHours * equipment.HourlyPrice;
            }
            else
            {
                var days = Math.Max((int)Math.Ceiling(duration.TotalDays), 1);
                totalPrice = days * equipment.DailyPrice;
            }

            var booking = new EquipmentBooking
            {
                EquipmentId = dto.EquipmentId,
                FarmerId = farmerId,
                StartDate = dto.StartDate,
                EndDate = dto.EndDate,
                TotalPrice = totalPrice,
                Status = "Pending"
            };

            _context.EquipmentBookings.Add(booking);
            await _context.SaveChangesAsync();

            if (equipment.Owner != null)
            {
                await _notificationService.SendNotificationAsync(
                    equipment.OwnerId,
                    "New Booking",
                    $"Booking request for {equipment.EquipmentName}",
                    "Booking"
                );
            }

            return Ok(new
            {
                message = await _translator.Translate("Booking request sent to owner", lang),
                bookingId = booking.BookingId,
                totalAmount = totalPrice
            });
        }

        // ======================================================
        // ⭐ FARMER BOOKINGS
        // ======================================================
        [Authorize(Roles = "Farmer")]
        [HttpGet("my-bookings")]
        public async Task<IActionResult> GetMyBookings()
        {
            var lang = GetLang();
            var farmerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var bookings = await _context.EquipmentBookings.AsNoTracking()
                .Include(b => b.Equipment)
                    .ThenInclude(e => e!.Owner)
                .Include(b => b.Equipment)
                    .ThenInclude(e => e!.Images)
                .Include(b => b.Farmer)
                .Where(b => b.FarmerId == farmerId)
                .OrderByDescending(b => b.BookingId)
                .ToListAsync();

            var tasks = bookings.Select(async b => new BookingResponseDto
            {
                BookingId = b.BookingId,
                EquipmentId = b.EquipmentId,

                EquipmentName = b.Equipment == null ? "N/A" :
                    lang == "en"
                    ? b.Equipment.EquipmentName
                    : await _translator.TransliterateToNative(b.Equipment.EquipmentName, lang),

                FarmerName = b.Farmer?.FullName != null 
                    ? await _translator.TransliterateToNative(b.Farmer.FullName, lang) 
                    : "N/A",
                FarmerMobile = b.Farmer?.MobileNumber ?? "N/A",
                OwnerName = b.Equipment?.Owner?.FullName != null 
                    ? await _translator.TransliterateToNative(b.Equipment.Owner.FullName, lang) 
                    : "N/A",
                OwnerMobile = b.Equipment?.Owner?.MobileNumber ?? "N/A",
                
                EquipmentImageUrl = b.Equipment?.Images?.FirstOrDefault()?.ImageUrl,
                EquipmentLocation = lang == "en" ? (b.Equipment?.Location ?? "N/A") : await _translator.TransliterateToNative(b.Equipment?.Location ?? "N/A", lang),
                EquipmentHourlyPrice = b.Equipment?.HourlyPrice ?? 0m,
                EquipmentDailyPrice = b.Equipment?.DailyPrice ?? 0m,

                StartDate = b.StartDate,
                EndDate = b.EndDate,
                TotalPrice = b.TotalPrice,
                Status = b.Status
            });

            var result = (await Task.WhenAll(tasks)).ToList();

            return Ok(result);
        }

        // ======================================================
        // ⭐ OWNER VIEW REQUESTS
        // ======================================================
        [Authorize(Roles = "EquipmentOwner,Owner")]
        [HttpGet("owner-requests")]
        public async Task<IActionResult> OwnerRequests()
        {
            var lang = GetLang();
            var ownerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var bookings = await _context.EquipmentBookings.AsNoTracking()
                .Include(b => b.Equipment)
                    .ThenInclude(e => e!.Owner)
                .Include(b => b.Equipment)
                    .ThenInclude(e => e!.Images)
                .Include(b => b.Farmer)
                .Where(b => b.Equipment != null && b.Equipment.OwnerId == ownerId)
                .OrderByDescending(b => b.BookingId)
                .ToListAsync();

            var tasks = bookings.Select(async b => new BookingResponseDto
            {
                BookingId = b.BookingId,
                EquipmentId = b.EquipmentId,

                EquipmentName = b.Equipment == null ? "N/A" :
                    lang == "en"
                    ? b.Equipment.EquipmentName
                    : await _translator.TransliterateToNative(b.Equipment.EquipmentName, lang),

                FarmerName = b.Farmer?.FullName != null 
                    ? await _translator.TransliterateToNative(b.Farmer.FullName, lang) 
                    : "N/A",
                FarmerMobile = b.Farmer?.MobileNumber ?? "N/A",
                OwnerName = b.Equipment?.Owner?.FullName != null 
                    ? await _translator.TransliterateToNative(b.Equipment.Owner.FullName, lang) 
                    : "N/A",
                OwnerMobile = b.Equipment?.Owner?.MobileNumber ?? "N/A",

                EquipmentImageUrl = b.Equipment?.Images?.FirstOrDefault()?.ImageUrl,
                EquipmentLocation = lang == "en" ? (b.Equipment?.Location ?? "N/A") : await _translator.TransliterateToNative(b.Equipment?.Location ?? "N/A", lang),
                EquipmentHourlyPrice = b.Equipment?.HourlyPrice ?? 0m,
                EquipmentDailyPrice = b.Equipment?.DailyPrice ?? 0m,

                StartDate = b.StartDate,
                EndDate = b.EndDate,
                TotalPrice = b.TotalPrice,
                Status = b.Status
            });

            var result = (await Task.WhenAll(tasks)).ToList();

            return Ok(result);
        }

        // ======================================================
        // ⭐ OWNER ACCEPT
        // ======================================================
        [Authorize(Roles = "EquipmentOwner,Owner")]
        [HttpPost("accept/{bookingId}")]
        public async Task<IActionResult> AcceptBooking(int bookingId)
        {
            var lang = GetLang();
            var ownerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var booking = await _context.EquipmentBookings
                .Include(b => b.Equipment)
                .Where(b => b.Equipment != null)
                .FirstOrDefaultAsync(b =>
                    b.BookingId == bookingId &&
                    b.Equipment!.OwnerId == ownerId);

            if (booking == null)
                return Unauthorized(await _translator.Translate(
                    "You can only manage your own equipment requests", lang));

            booking.Status = "Accepted";

            // Find overlapping "Pending" bookings for the same equipment and automatically reject them
            var overlappingPendingBookings = await _context.EquipmentBookings
                .Where(b => 
                    b.EquipmentId == booking.EquipmentId && 
                    b.Status == "Pending" && 
                    b.BookingId != bookingId &&
                    booking.StartDate < b.EndDate && 
                    booking.EndDate > b.StartDate)
                .ToListAsync();

            foreach(var pendingBooking in overlappingPendingBookings)
            {
                pendingBooking.Status = "Rejected";
            }

            await _context.SaveChangesAsync();

            var equipmentName = booking.Equipment?.EquipmentName ?? "Equipment";

            await _notificationService.SendNotificationAsync(
                booking.FarmerId,
                "Booking Accepted",
                $"{equipmentName} booking accepted",
                "Booking"
            );

            foreach (var pendingBooking in overlappingPendingBookings)
            {
                await _notificationService.SendNotificationAsync(
                    pendingBooking.FarmerId,
                    "Booking Rejected",
                    $"{equipmentName} booking not available",
                    "Booking"
                );
            }

            return Ok(await _translator.Translate("Booking accepted successfully", lang));
        }

        // ======================================================
        // ⭐ OWNER REJECT
        // ======================================================
        [Authorize(Roles = "EquipmentOwner,Owner")]
        [HttpPost("reject/{bookingId}")]
        public async Task<IActionResult> RejectBooking(int bookingId)
        {
            var lang = GetLang();
            var ownerId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var booking = await _context.EquipmentBookings
                .Include(b => b.Equipment)
                .Where(b => b.Equipment != null)
                .FirstOrDefaultAsync(b =>
                    b.BookingId == bookingId &&
                    b.Equipment!.OwnerId == ownerId);

            if (booking == null)
                return Unauthorized(await _translator.Translate(
                    "You can only manage your own equipment requests", lang));

            booking.Status = "Rejected";
            await _context.SaveChangesAsync();

            var equipmentName = booking.Equipment?.EquipmentName ?? "Equipment";

            await _notificationService.SendNotificationAsync(
                booking.FarmerId,
                "Booking Rejected",
                $"{equipmentName} booking rejected",
                "Booking"
            );

            return Ok(await _translator.Translate("Booking rejected", lang));
        }

        // ======================================================
        // ⭐ FILTER BOOKINGS
        // ======================================================
        [Authorize]
        [HttpGet("filter")]
        public async Task<IActionResult> FilterBookings(string status)
        {
            var lang = GetLang();
            status = status.ToLower();

            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);
            bool isOwner = User.IsInRole("EquipmentOwner") || User.IsInRole("Owner");
            bool isFarmer = User.IsInRole("Farmer");

            var query = _context.EquipmentBookings.AsNoTracking()
                .Include(b => b.Equipment)
                    .ThenInclude(e => e!.Owner)
                .AsQueryable();

            if (isFarmer) query = query.Where(b => b.FarmerId == userId);
            if (isOwner) query = query.Where(b => b.Equipment != null && b.Equipment.OwnerId == userId);

            var bookings = await query
                .Where(b => b.Status.ToLower() == status)
                .OrderByDescending(b => b.BookingId)
                .ToListAsync();

            var tasks = bookings.Select(async b => (object)new
            {
                b.BookingId,
                EquipmentName = b.Equipment == null ? "N/A" : await _translator.TransliterateToNative(b.Equipment.EquipmentName, lang),
                b.Status,
                b.StartDate,
                b.EndDate,
                b.TotalPrice
            });

            var result = (await Task.WhenAll(tasks)).ToList();

            return Ok(result);
        }
    }
}