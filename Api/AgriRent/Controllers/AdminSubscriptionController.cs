using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
using AgriRent.Data;
using AgriRent.Models;
using AgriRent.Services; // 👈 Translation Service
using System.Linq;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/admin/subscription")]
    [Authorize(Roles = "Admin")] // 🔐 Only admin access
    public class AdminSubscriptionController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;
        private readonly IMemoryCache _cache;

        public AdminSubscriptionController(AppDbContext context, TranslateService translate, LanguageHelper langHelper, IMemoryCache cache)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
            _cache = cache;
        }

        // 🔄 Clears all language variants of the subscription_plans cache
        private void ClearPlansCache()
        {
            // Clear all known language variants used in SubscriptionPlansController
            string[] langs = { "en", "hi", "gu", "mr", "pa", "bn", "ta", "te", "kn", "ml" };
            foreach (var lang in langs)
                _cache.Remove($"subscription_plans_{lang}");
        }



        // ⭐ 1. CREATE NEW PLAN
        [HttpPost("add-plan")]
        public async Task<IActionResult> AddPlan([FromBody] SubscriptionPlan plan)
        {
            var lang = _langHelper.GetLanguage();

            if (string.IsNullOrWhiteSpace(plan.PlanName) || plan.Price < 0)
                return BadRequest(await _translate.Translate("Invalid plan details", lang));

            _context.SubscriptionPlans.Add(plan);
            await _context.SaveChangesAsync();

            // ✅ Clear cache so users see the new plan immediately
            ClearPlansCache();

            return Ok(new
            {
                message = await _translate.Translate("Subscription plan added successfully", lang),
                planId = plan.PlanId
            });
        }


        // ⭐ 2. VIEW ALL PLANS
        [HttpGet("plans")]
        public async Task<IActionResult> GetAllPlans()
        {
            var lang = _langHelper.GetLanguage();

            var plans = await _context.SubscriptionPlans.ToListAsync();

            var translatedPlans = new List<object>();

            foreach (var plan in plans)
            {
                translatedPlans.Add(new
                {
                    planId = plan.PlanId,
                    planName = await _translate.Translate(plan.PlanName, lang),
                    durationDays = plan.DurationDays,
                    price = plan.Price,
                    maxEquipment = plan.MaxEquipment,
                    maxProducts = plan.MaxProducts,
                    status = plan.Status,
                });
            }

            return Ok(new
            {
                message = await _translate.Translate("Subscription plans list", lang),
                total = plans.Count,
                data = translatedPlans
            });
        }



        // ⭐ 3. CHANGE PLAN STATUS (Active / Inactive)
        [HttpPut("status/{id}")]
        public async Task<IActionResult> ChangeStatus(int id)
        {
            var lang = _langHelper.GetLanguage();

            var plan = await _context.SubscriptionPlans.FindAsync(id);
            if (plan == null)
                return NotFound(await _translate.Translate("Plan not found", lang));

            plan.Status = plan.Status == "Active" ? "Inactive" : "Active";
            await _context.SaveChangesAsync();

            // ✅ Clear cache so users see the updated status immediately
            ClearPlansCache();

            var response = plan.Status == "Active"
                ? await _translate.Translate("Plan activated successfully", lang)
                : await _translate.Translate("Plan deactivated successfully", lang);

            return Ok(new
            {
                planId = id,
                status = plan.Status,
                message = response
            });
        }
        // ⭐ 4. EDIT PLAN
        [HttpPut("edit-plan/{id}")]
        public async Task<IActionResult> EditPlan(int id, [FromBody] SubscriptionPlan updatedPlan)
        {
            var lang = _langHelper.GetLanguage();

            if (id != updatedPlan.PlanId || string.IsNullOrWhiteSpace(updatedPlan.PlanName) || updatedPlan.Price < 0)
                return BadRequest(await _translate.Translate("Invalid plan details", lang));

            var existingPlan = await _context.SubscriptionPlans.FindAsync(id);
            if (existingPlan == null)
                return NotFound(await _translate.Translate("Plan not found", lang));

            existingPlan.PlanName = updatedPlan.PlanName;
            existingPlan.Price = updatedPlan.Price;
            existingPlan.DurationDays = updatedPlan.DurationDays;
            existingPlan.MaxEquipment = updatedPlan.MaxEquipment;
            existingPlan.MaxProducts = updatedPlan.MaxProducts;
            
            await _context.SaveChangesAsync();

            // ✅ Clear cache so users see the edited plan immediately
            ClearPlansCache();

            return Ok(new
            {
                message = await _translate.Translate("Subscription plan updated successfully", lang),
                planId = id
            });
        }

        // ⭐ GET USER SUBSCRIPTIONS (with status filter)
        [HttpGet("user-subscriptions")]
        public async Task<IActionResult> GetUserSubscriptions([FromQuery] string? status = null)
        {
            var query = _context.UserSubscriptions
                .Include(s => s.Plan)
                .AsNoTracking()
                .AsQueryable();

            if (!string.IsNullOrWhiteSpace(status) && !status.Equals("All", StringComparison.OrdinalIgnoreCase))
            {
                if (status.Equals("Inactive", StringComparison.OrdinalIgnoreCase))
                    query = query.Where(s => s.Status != "Active");
                else
                    query = query.Where(s => s.Status == status);
            }

            var list = await query.OrderByDescending(s => s.SubscriptionId).ToListAsync();

            var userIds = list.Select(s => s.UserId).Distinct().ToList();
            var users   = await _context.Users.AsNoTracking()
                .Where(u => userIds.Contains(u.UserId))
                .ToDictionaryAsync(u => u.UserId, u => new { u.FullName, u.MobileNumber });

            var result = list.Select(s => new
            {
                s.SubscriptionId,
                s.UserId,
                UserName    = users.ContainsKey(s.UserId) ? users[s.UserId].FullName   : "Unknown",
                UserMobile  = users.ContainsKey(s.UserId) ? users[s.UserId].MobileNumber : "N/A",
                s.PlanId,
                PlanName    = s.Plan?.PlanName ?? "Unknown",
                s.StartDate,
                s.EndDate,
                s.Status,
                s.PaymentStatus,
                s.PaymentId
            }).ToList();

            return Ok(new { total = result.Count, data = result });
        }
    }
}