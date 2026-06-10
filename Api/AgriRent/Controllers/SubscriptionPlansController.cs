using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Caching.Memory;
using AgriRent.Data;
using AgriRent.Services;
using System.Linq;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/subscription-plans")]
    public class SubscriptionPlansController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly IMemoryCache _cache;
        private readonly LanguageHelper _langHelper;

        public SubscriptionPlansController(AppDbContext context, TranslateService translate, IMemoryCache cache, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _cache = cache;
            _langHelper = langHelper;
        }



        // ⭐ VIEW ACTIVE PLANS (ALL USERS)
        // GET: /api/subscription-plans
        [HttpGet]
        [AllowAnonymous]
        public async Task<IActionResult> GetActivePlans()
        {
            var lang = _langHelper.GetLanguage();
            string cacheKey = $"subscription_plans_{lang}";

            if (_cache.TryGetValue(cacheKey, out object? cachedResponse))
            {
                return Ok(cachedResponse);
            }

            var plans = await _context.SubscriptionPlans.AsNoTracking()
                .Where(p => p.Status == "Active")
                .ToListAsync();

            var data = new List<object>();

            foreach (var plan in plans)
            {
                data.Add(new
                {
                    planId = plan.PlanId,
                    planName = await _translate.Translate(plan.PlanName, lang),
                    durationDays = plan.DurationDays,
                    price = plan.Price,
                    maxEquipment = plan.MaxEquipment,
                    maxProducts = plan.MaxProducts
                });
            }

            var response = new
            {
                message = await _translate.Translate("Subscription plans list", lang),
                total = plans.Count,
                data = data
            };

            // Cache for 24 hours
            _cache.Set(cacheKey, response, TimeSpan.FromHours(24));

            return Ok(response);
        }
    }
}
