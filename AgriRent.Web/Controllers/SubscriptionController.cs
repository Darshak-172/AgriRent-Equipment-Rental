using AgriRent.DTOs;
using AgriRent.Models;
using AgriRent.Web.Services;
using Microsoft.AspNetCore.Mvc;
using System.Text.Json;

namespace AgriRent.Web.Controllers
{
    public class SubscriptionController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ILogger<SubscriptionController> _logger;

        public SubscriptionController(ApiClient apiClient, ILogger<SubscriptionController> logger)
        {
            _apiClient = apiClient;
            _logger = logger;
        }

        // GET: /Subscription
        public async Task<IActionResult> Index()
        {
            // 1. Fetch active plans (public endpoint - returns { message, total, data: [...] })
            var plansResponse = await _apiClient.GetAsync<PlansApiResponse>("/api/subscription-plans");

            // 2. Fetch user's active subscriptions (auth endpoint - returns { success, data: [...] })
            //    Only call if user is logged in
            List<UserSubscriptionDto> activeSubs = new();
            if (HttpContext.Session.GetString("JwtToken") != null)
            {
                var mySubResponse = await _apiClient.GetAsync<MySubApiResponse>("/api/subscription/my-subscription");
                activeSubs = mySubResponse?.Data ?? new List<UserSubscriptionDto>();
            }

            var viewModel = new SubscriptionIndexViewModel
            {
                Plans = plansResponse?.Data ?? new List<SubscriptionPlan>(),
                ActiveSubscriptions = activeSubs
            };

            return View(viewModel);
        }

        // POST: /Subscription/CreateOrder
        [HttpPost]
        public async Task<IActionResult> CreateOrder(int planId)
        {
            try
            {
                var response = await _apiClient.PostAsyncRaw($"/api/subscription/create-order/{planId}", new { });
                if (response.IsSuccessStatusCode)
                {
                    var result = await response.Content.ReadAsStringAsync();
                    return Content(result, "application/json");
                }

                var errorBody = await response.Content.ReadAsStringAsync();
                _logger.LogWarning("CreateOrder failed: {Status} - {Body}", response.StatusCode, errorBody);
                return BadRequest(new { message = "Failed to create order. Please try again." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating order for planId={PlanId}", planId);
                return StatusCode(500, new { message = "Internal Server Error" });
            }
        }

        // POST: /Subscription/VerifyPayment
        [HttpPost]
        public async Task<IActionResult> VerifyPayment([FromBody] BuySubscriptionDto dto)
        {
            try
            {
                _logger.LogInformation("[VerifyPayment] PlanId={PlanId} PaymentId={PaymentId} OrderId={OrderId}",
                    dto.PlanId, dto.PaymentId?.Substring(0, Math.Min(12, dto.PaymentId?.Length ?? 0)) + "…",
                    dto.OrderId?.Substring(0, Math.Min(12, dto.OrderId?.Length ?? 0)) + "…");

                var response = await _apiClient.PostAsyncRaw("/api/subscription/verify-payment", dto);
                var result   = await response.Content.ReadAsStringAsync();

                _logger.LogInformation("[VerifyPayment] API returned {Status}: {Body}",
                    response.StatusCode, result.Length > 200 ? result.Substring(0, 200) : result);

                if (response.IsSuccessStatusCode)
                {
                    // ✅ Update JWT + UserRoles + Username in session so Owner role takes effect immediately
                    try
                    {
                        var parsed = JsonSerializer.Deserialize<JsonElement>(result,
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                        // Update JWT token
                        if (parsed.TryGetProperty("token", out var tokenProp))
                        {
                            var newToken = tokenProp.GetString();
                            if (!string.IsNullOrEmpty(newToken))
                            {
                                HttpContext.Session.SetString("JwtToken", newToken);
                                _logger.LogInformation("[VerifyPayment] Session JWT updated.");
                            }
                        }

                        // Update UserRoles so owner dashboard access works immediately (no re-login required)
                        if (parsed.TryGetProperty("roles", out var rolesProp) &&
                            rolesProp.ValueKind == JsonValueKind.Array)
                        {
                            var rolesStr = string.Join(",",
                                rolesProp.EnumerateArray().Select(r => r.GetString() ?? ""));
                            HttpContext.Session.SetString("UserRoles", rolesStr);
                            _logger.LogInformation("[VerifyPayment] Session roles updated: {Roles}", rolesStr);
                        }
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "[VerifyPayment] Could not update session from verify-payment response.");
                    }

                    return Content(result, "application/json");
                }

                // Return the API's error body so the frontend can show the actual message
                _logger.LogWarning("[VerifyPayment] Verification failed: {Status} {Body}", response.StatusCode, result);
                return StatusCode((int)response.StatusCode, result);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[VerifyPayment] Unexpected error");
                return StatusCode(500, new { message = "Internal server error during payment verification. Please contact support." });
            }
        }

        // GET: /Subscription/Success
        public IActionResult Success()
        {
            return View();
        }
    }

    // ─── Response Wrappers ────────────────────────────────────────────────────

    /// <summary>
    /// Matches: GET /api/subscription-plans → { message, total, data: [...] }
    /// </summary>
    public class PlansApiResponse
    {
        public string? Message { get; set; }
        public int Total { get; set; }
        public List<SubscriptionPlan>? Data { get; set; }
    }

    /// <summary>
    /// Matches: GET /api/subscription/my-subscription → { success, data: [...] }
    /// </summary>
    public class MySubApiResponse
    {
        public bool Success { get; set; }
        public List<UserSubscriptionDto>? Data { get; set; }
    }

    // ─── ViewModels ───────────────────────────────────────────────────────────

    public class SubscriptionIndexViewModel
    {
        public required List<SubscriptionPlan> Plans { get; set; }
        public required List<UserSubscriptionDto> ActiveSubscriptions { get; set; }
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public class UserSubscriptionDto
    {
        public int SubscriptionId { get; set; }
        public int PlanId { get; set; }
        public string PlanName { get; set; } = string.Empty;
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public string Status { get; set; } = string.Empty;
    }
}
