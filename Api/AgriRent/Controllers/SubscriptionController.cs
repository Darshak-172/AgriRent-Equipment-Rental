
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Razorpay.Api;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Extensions;
using AgriRent.Models;
using System.Security.Claims;
using AgriRent.Services; // Translation Support


namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/subscription")]
    public class SubscriptionController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _config;
        private readonly TranslateService _translate;
        private readonly JwtTokenService _jwtService;
        private readonly NotificationService _notificationService;

        public SubscriptionController(AppDbContext context, IConfiguration config, TranslateService translate, JwtTokenService jwtService, NotificationService notificationService)
        {
            _context = context;
            _config = config;
            _translate = translate;
            _jwtService = jwtService;
            _notificationService = notificationService;
        }

        // 🌍 AUTO-DETECT LANGUAGE FROM HEADER
        private string GetLang()
        {
            var lang = Request.Headers["Accept-Language"].ToString();

            if (string.IsNullOrWhiteSpace(lang))
                return "en";

            // ✅ Fix: "gu-IN" → "gu", "hi-IN" → "hi"
            return lang.Split(',')[0].Split('-')[0];
        }

        // ⭐ STEP 1: CREATE RAZORPAY ORDER
        [Authorize]
        [HttpPost("create-order/{planId}")]
        public async Task<IActionResult> CreateOrder(int planId)
        {
            var lang = GetLang();

            try
            {
                var plan = await _context.SubscriptionPlans.FirstOrDefaultAsync(p => p.PlanId == planId);
                if (plan == null)
                    return NotFound(new { success = false, message = await _translate.Translate("Plan not found", lang) });

                var razorKey    = _config["Razorpay:Key"];
                var razorSecret = _config["Razorpay:Secret"];

                if (string.IsNullOrWhiteSpace(razorKey) || string.IsNullOrWhiteSpace(razorSecret))
                    return StatusCode(500, new { success = false, message = "Payment gateway is not configured. Please contact support." });

                // Receipt ID must be <= 40 chars
                var receiptId = $"rcpt_{planId}_{IstHelper.Now:yyyyMMddHHmmss}";

                var client = new RazorpayClient(razorKey, razorSecret);

                var options = new Dictionary<string, object>
                {
                    { "amount",          (int)(plan.Price * 100) },
                    { "currency",        "INR" },
                    { "receipt",         receiptId },
                    { "payment_capture", 1 }
                };

                Razorpay.Api.Order order = client.Order.Create(options);

                return Ok(new
                {
                    orderId  = order["id"].ToString(),
                    key      = razorKey,
                    amount   = plan.Price,
                    planId   = plan.PlanId,
                    receipt  = receiptId
                });
            }
            catch (HttpRequestException ex)
            {
                // Network / connectivity issue reaching Razorpay
                return StatusCode(503, new { success = false, message = "Unable to reach payment gateway. Check internet connectivity.", detail = ex.Message });
            }
            catch (Exception ex)
            {
                // Razorpay SDK throws plain Exception with descriptive message
                return StatusCode(500, new
                {
                    success = false,
                    message = await _translate.Translate("Failed to create payment order. Please try again.", lang),
                    detail  = ex.Message
                });
            }
        }

        // ⭐ STEP 2: VERIFY PAYMENT + ACTIVATE SUBSCRIPTION
        [Authorize]
        [HttpPost("verify-payment")]
        public async Task<IActionResult> VerifyPayment([FromBody] BuySubscriptionDto dto)
        {
            var lang = GetLang();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            // 🔐 0️⃣ VERIFY RAZORPAY SIGNATURE
            try
            {
                var secret = _config["Razorpay:Secret"];
                if (string.IsNullOrEmpty(secret))
                {
                   return StatusCode(500, new { message = await _translate.Translate("Payment configuration error", lang) });
                }

                var generatedSignature = CalculateHmacSha256(dto.OrderId + "|" + dto.PaymentId, secret);

                if (generatedSignature != dto.Signature)
                {
                    return BadRequest(new { message = await _translate.Translate("Payment verification failed: Invalid Signature", lang) });
                }
            }
            catch (Exception)
            {
                return BadRequest(new { message = await _translate.Translate("Payment verification failed", lang) });
            }

            // 1️⃣ Get plan
            var plan = await _context.SubscriptionPlans
                .FirstOrDefaultAsync(p => p.PlanId == dto.PlanId);

            if (plan == null)
                return NotFound(await _translate.Translate("Plan not found", lang));

            // 2️⃣ Check for Existing Active Subscription for SAME Plan (Renewal)
            var existingSub = await _context.UserSubscriptions
                .FirstOrDefaultAsync(s => s.UserId == userId && s.PlanId == dto.PlanId && s.Status == "Active");

            DateTime validFrom, validTill;
            int subscriptionId;

            using var transaction = await _context.Database.BeginTransactionAsync();

            try
            {
                if (existingSub != null)
                {
                    // RENEWAL: Extend the existing subscription
                    var extensionStart = existingSub.EndDate > DateTime.UtcNow ? existingSub.EndDate : DateTime.UtcNow;
                    existingSub.EndDate = extensionStart.AddDays(plan.DurationDays);
                    existingSub.PaymentId     = dto.PaymentId;
                    existingSub.PaymentStatus = "Paid";

                    validFrom      = existingSub.StartDate;
                    validTill      = existingSub.EndDate;
                    subscriptionId = existingSub.SubscriptionId;
                }
                else
                {
                    // NEW SUBSCRIPTION
                    var newSub = new UserSubscription
                    {
                        UserId        = userId,
                        PlanId        = dto.PlanId,
                        StartDate     = IstHelper.Now,
                        EndDate       = IstHelper.Now.AddDays(plan.DurationDays),
                        PaymentId     = dto.PaymentId,
                        PaymentStatus = "Paid",
                        Status        = "Active"
                    };
                    _context.UserSubscriptions.Add(newSub);

                    // Save NOW so EF assigns the DB-generated SubscriptionId
                    await _context.SaveChangesAsync();

                    validFrom      = newSub.StartDate;
                    validTill      = newSub.EndDate;
                    subscriptionId = newSub.SubscriptionId;   // populated after SaveChanges
                }

            // 🔐 1.5️⃣ FETCH PAYMENT DETAILS FROM RAZORPAY (For Method & Error)
            string paymentMethod = "Online";
            string? errorMessage = null;

            try
            {
                var razorKey = _config["Razorpay:Key"];
                var razorSecret = _config["Razorpay:Secret"];
                if (!string.IsNullOrEmpty(razorKey) && !string.IsNullOrEmpty(razorSecret))
                {
                    var client = new RazorpayClient(razorKey, razorSecret);
                    Razorpay.Api.Payment rzpPayment = client.Payment.Fetch(dto.PaymentId);
                    if (rzpPayment != null)
                    {
                        var methodObj = rzpPayment.Attributes["method"];
                        if (methodObj != null) paymentMethod = methodObj.ToString();

                        var errObj = rzpPayment.Attributes["error_description"];
                        if (errObj != null) errorMessage = errObj.ToString();
                    }
                }
            }
            catch (Exception)
            {
                // Fallback to "Online" if Razorpay fetch fails to avoid breaking flow
                paymentMethod = "Online";
            }

                // 🔐 2️⃣ SAVE PAYMENT RECORD FOR TRACKING & AUDIT
                var paymentRecord = new Models.Payment
                {
                    UserId = userId,
                    SubscriptionId = subscriptionId,
                    PlanId = dto.PlanId,
                    RazorpayPaymentId = dto.PaymentId,
                    RazorpayOrderId = dto.OrderId,
                    RazorpaySignature = dto.Signature,
                    Amount = plan.Price,
                    Currency = "INR",
                    Status = "Successful",
                    PaymentDate = IstHelper.Now,
                    ReceiptNumber = $"rcpt_{dto.PlanId}_{IstHelper.Now:yyyyMMddHHmmss}",
                    PaymentMethod = paymentMethod,
                    ErrorMessage = errorMessage,
                    VerificationDate = IstHelper.Now,
                    Notes = $"Subscription payment for plan: {plan.PlanName}"
                };

                _context.Payments.Add(paymentRecord);
                // Removed intermediate SaveChangesAsync

                // 3️⃣ Load role IDs
                var ownerRoleId = await _context.Roles
                    .Where(r => r.RoleName == "Owner")
                    .Select(r => (int?)r.RoleId)
                    .FirstOrDefaultAsync();

                var sellerRoleId = await _context.Roles
                    .Where(r => r.RoleName == "Seller")
                    .Select(r => (int?)r.RoleId)
                    .FirstOrDefaultAsync();

                // 4️⃣ Assign Owner role
                if (plan.MaxEquipment > 0 && ownerRoleId.HasValue &&
                    !await _context.UserRoles.AnyAsync(x => x.UserId == userId && x.RoleId == ownerRoleId.Value))
                {
                    _context.UserRoles.Add(new UserRole
                    {
                        UserId = userId,
                        RoleId = ownerRoleId.Value
                    });
                }

                // 5️⃣ Assign Seller role
                if (plan.MaxProducts > 0 && sellerRoleId.HasValue &&
                    !await _context.UserRoles.AnyAsync(x => x.UserId == userId && x.RoleId == sellerRoleId.Value))
                {
                    _context.UserRoles.Add(new UserRole
                    {
                        UserId = userId,
                        RoleId = sellerRoleId.Value
                    });
                }

                // Final commit for all related entities
                await _context.SaveChangesAsync();
                await transaction.CommitAsync();

                // 🔑 6️⃣ FETCH UPDATED ROLES
                var roles = await (
                    from ur in _context.UserRoles
                    join role in _context.Roles on ur.RoleId equals role.RoleId
                    where ur.UserId == userId
                    select role.RoleName
                ).ToListAsync();

                // 👤 7️⃣ FETCH USER
                var user = await _context.Users.FindAsync(userId);

                // 🔐 8️⃣ GENERATE NEW JWT
                var newToken = _jwtService.GenerateToken(user!, roles);

                // 🔔 SEND FCM NOTIFICATION
                bool isRenewal = existingSub != null;
                await _notificationService.SendNotificationAsync(
                    userId,
                    isRenewal ? "Plan Extended" : "Plan Activated",
                    isRenewal
                        ? $"{plan.PlanName} extended till {validTill:dd MMM yyyy}"
                        : $"{plan.PlanName} active till {validTill:dd MMM yyyy}",
                    "Subscription"
                );

                // 🌍 9️⃣ RESPONSE
                var rolesTranslated = new List<string>();
                foreach (var role in roles)
                {
                    rolesTranslated.Add(lang == "en" ? role : await _translate.Translate(role, lang));
                }

                return Ok(new
                {
                    success = true,
                    message = await _translate.Translate(
                        "Subscription Activated & Access Updated Successfully!", lang),
                    token = newToken,
                    roles = rolesTranslated,
                    validFrom = validFrom,
                    validTill = validTill
                });
            }
            catch (Exception ex)
            {
                await transaction.RollbackAsync();
                return StatusCode(500, new { message = await _translate.Translate("Transaction failed: " + ex.Message, lang) });
            }
        }




        // ⭐ GET USER'S ACTIVE SUBSCRIPTIONS
        [Authorize]
        [HttpGet("my-subscription")]
        public async Task<IActionResult> GetMySubscriptions()
        {
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);
            var lang = GetLang();

            var activeSubs = await _context.UserSubscriptions
                .Include(s => s.Plan)
                .Where(s => s.UserId == userId && s.Status == "Active" && s.EndDate > DateTime.UtcNow)
                .Select(s => new
                {
                    s.SubscriptionId,
                    s.PlanId,
                    PlanName = s.Plan != null ? s.Plan.PlanName : "Unknown Plan",
                    s.StartDate,
                    s.EndDate,
                    s.Status
                })
                .ToListAsync();

            return Ok(new { success = true, data = activeSubs });
        }

        // ⭐ ADMIN → CHECK EXPIRED SUBSCRIPTIONS
        [Authorize(Roles = "Admin")]
        [HttpPost("check-expiry")]
        public async Task<IActionResult> CheckExpiry([FromServices] SubscriptionExpiryService service)
        {
            var lang = GetLang();
            var count = await service.DeactivateExpiredSubscriptionsAsync();
            var msg = await _translate.Translate($"{count} subscriptions expired and roles removed.", lang);

            return Ok(new { status = msg });
        }
        // Helper to calculate HMAC-SHA256
        private string CalculateHmacSha256(string data, string secret)
        {
            using (var hmac = new System.Security.Cryptography.HMACSHA256(System.Text.Encoding.ASCII.GetBytes(secret)))
            {
                var hash = hmac.ComputeHash(System.Text.Encoding.ASCII.GetBytes(data));
                return BitConverter.ToString(hash).Replace("-", "").ToLower();
            }
        }
    }
}