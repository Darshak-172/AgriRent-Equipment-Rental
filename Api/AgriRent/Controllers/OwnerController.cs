using System.Security.Claims;
using AgriRent.Data;
using AgriRent.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/owner")]
    public class OwnerController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;

        public OwnerController(AppDbContext context, TranslateService translate, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
        }

        // 📊 OWNER DASHBOARD
        [Authorize(Roles = "Owner")]
        [HttpGet("dashboard")]
        public async Task<IActionResult> Dashboard()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var totalEquipment = await _context.Equipments.CountAsync(e => e.OwnerId == userId);
            var activeEquipment = await _context.Equipments.CountAsync(e => e.OwnerId == userId && e.Status == "Active");
            
            // Note: Equipment rentals are tracked via EquipmentAvailability
            var totalBookings = await _context.EquipmentAvailability.CountAsync(b => b.Equipment!.OwnerId == userId);
            
            // Availability records don't have a 'Status' field like "Pending" by default in this schema, 
            // so we'll just return total active blocks.
            var pendingBookings = 0;

            return Ok(new
            {
                message = await _translate.Translate("Owner Dashboard", lang),
                totalEquipment,
                activeEquipment,
                totalBookings,
                pendingBookings
            });
        }
    }
}