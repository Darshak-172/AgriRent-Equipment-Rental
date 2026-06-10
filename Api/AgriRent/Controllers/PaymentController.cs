using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using AgriRent.Data;
using AgriRent.DTOs;
using AgriRent.Services;
using System.Security.Claims;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/payment")]
    public class PaymentController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly TranslateService _translate;
        private readonly LanguageHelper _langHelper;

        public PaymentController(AppDbContext context, TranslateService translate, LanguageHelper langHelper)
        {
            _context = context;
            _translate = translate;
            _langHelper = langHelper;
        }



        // ⭐ GET USER'S PAYMENT HISTORY
        [Authorize]
        [HttpGet("history")]
        public async Task<IActionResult> GetPaymentHistory(
            [FromQuery] int pageSize = 10,
            [FromQuery] int pageNumber = 1,
            [FromQuery] string? status = null)  // Filter by status
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            if (pageSize <= 0 || pageSize > 100)
                pageSize = 10;
            if (pageNumber <= 0)
                pageNumber = 1;

            // Base query with filtering
            IQueryable<Models.Payment> query = _context.Payments.AsNoTracking()
                .Where(p => p.UserId == userId)
                .Include(p => p.Plan);

            // Filter by status if provided
            if (!string.IsNullOrWhiteSpace(status))
            {
                query = query.Where(p => p.Status == status);
            }

            // Order after all filtering
            var orderedQuery = query.OrderByDescending(p => p.PaymentDate);

            var totalCount = await orderedQuery.CountAsync();
            var totalPages = (int)Math.Ceiling((double)totalCount / pageSize);

            var payments = await orderedQuery
                .Skip((pageNumber - 1) * pageSize)
                .Take(pageSize)
                .Select(p => new PaymentHistoryDto
                {
                    PaymentId = p.PaymentId,
                    SubscriptionId = p.SubscriptionId,
                    RazorpayPaymentId = p.RazorpayPaymentId,
                    RazorpayOrderId = p.RazorpayOrderId,
                    Amount = p.Amount,
                    Currency = p.Currency,
                    Status = p.Status,
                    PaymentDate = p.PaymentDate,
                    ReceiptNumber = p.ReceiptNumber,
                    PaymentMethod = p.PaymentMethod,
                    VerificationDate = p.VerificationDate,
                    PlanName = p.Plan != null ? p.Plan.PlanName : "Unknown",
                    DurationDays = p.Plan != null ? p.Plan.DurationDays : 0,
                    ErrorMessage = p.ErrorMessage
                })
                .ToListAsync();

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("Payment history retrieved", lang),
                totalCount = totalCount,
                totalPages = totalPages,
                currentPage = pageNumber,
                pageSize = pageSize,
                data = payments
            });
        }

        // ⭐ GET SINGLE PAYMENT DETAILS
        [Authorize]
        [HttpGet("{paymentId}")]
        public async Task<IActionResult> GetPaymentDetails(int paymentId)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var payment = await _context.Payments.AsNoTracking()
                .Include(p => p.Plan)
                .Include(p => p.Subscription)
                .FirstOrDefaultAsync(p => p.PaymentId == paymentId && p.UserId == userId);

            if (payment == null)
                return NotFound(await _translate.Translate("Payment not found", lang));

            var details = new PaymentDetailsDto
            {
                PaymentId = payment.PaymentId,
                UserId = payment.UserId,
                SubscriptionId = payment.SubscriptionId,
                PlanId = payment.PlanId,
                RazorpayPaymentId = payment.RazorpayPaymentId,
                RazorpayOrderId = payment.RazorpayOrderId,
                RazorpaySignature = payment.RazorpaySignature,
                Amount = payment.Amount,
                Currency = payment.Currency,
                Status = payment.Status,
                PaymentDate = payment.PaymentDate,
                ReceiptNumber = payment.ReceiptNumber,
                ErrorMessage = payment.ErrorMessage,
                PaymentMethod = payment.PaymentMethod,
                VerificationDate = payment.VerificationDate,
                Notes = payment.Notes,
                PlanName = payment.Plan?.PlanName ?? "Unknown",
                DurationDays = payment.Plan?.DurationDays ?? 0,
                MaxEquipment = payment.Plan?.MaxEquipment ?? 0,
                MaxProducts = payment.Plan?.MaxProducts ?? 0
            };

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("Payment details retrieved", lang),
                data = details
            });
        }

        // ⭐ GET PAYMENT SUMMARY
        [Authorize]
        [HttpGet("summary/stats")]
        public async Task<IActionResult> GetPaymentSummary()
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            var totalSpent = await _context.Payments
                .Where(p => p.UserId == userId && p.Status == "Successful")
                .SumAsync(p => p.Amount);
                
            var totalPayments = await _context.Payments
                .CountAsync(p => p.UserId == userId && p.Status == "Successful");
                
            var lastPaymentDate = await _context.Payments
                .Where(p => p.UserId == userId && p.Status == "Successful")
                .MaxAsync(p => (DateTime?)p.PaymentDate);

            var failedPayments = await _context.Payments
                .CountAsync(p => p.UserId == userId && p.Status == "Failed");

            var pendingPayments = await _context.Payments
                .CountAsync(p => p.UserId == userId && p.Status == "Pending");

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("Payment summary retrieved", lang),
                data = new
                {
                    totalSpent = totalSpent,
                    totalPayments = totalPayments,
                    successfulPayments = totalPayments,
                    failedPayments = failedPayments,
                    pendingPayments = pendingPayments,
                    lastPaymentDate = lastPaymentDate,
                    averagePaymentValue = totalPayments > 0 ? (totalSpent / totalPayments) : 0
                }
            });
        }

        // ⭐ GET PAYMENTS BY DATE RANGE
        [Authorize]
        [HttpGet("range")]
        public async Task<IActionResult> GetPaymentsByDateRange(
            [FromQuery] DateTime startDate,
            [FromQuery] DateTime endDate)
        {
            var lang = _langHelper.GetLanguage();
            var userId = int.Parse(User.FindFirst(ClaimTypes.NameIdentifier)!.Value);

            if (startDate >= endDate)
                return BadRequest(await _translate.Translate("Invalid date range", lang));

            var payments = await _context.Payments.AsNoTracking()
                .Where(p => p.UserId == userId && 
                            p.PaymentDate >= startDate && 
                            p.PaymentDate <= endDate)
                .Include(p => p.Plan)
                .OrderByDescending(p => p.PaymentDate)
                .Select(p => new PaymentHistoryDto
                {
                    PaymentId = p.PaymentId,
                    SubscriptionId = p.SubscriptionId,
                    RazorpayPaymentId = p.RazorpayPaymentId,
                    RazorpayOrderId = p.RazorpayOrderId,
                    Amount = p.Amount,
                    Currency = p.Currency,
                    Status = p.Status,
                    PaymentDate = p.PaymentDate,
                    ReceiptNumber = p.ReceiptNumber,
                    PaymentMethod = p.PaymentMethod,
                    VerificationDate = p.VerificationDate,
                    PlanName = p.Plan != null ? p.Plan.PlanName : "Unknown",
                    DurationDays = p.Plan != null ? p.Plan.DurationDays : 0,
                    ErrorMessage = p.ErrorMessage
                })
                .ToListAsync();

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("Payments retrieved for date range", lang),
                totalCount = payments.Count,
                startDate = startDate,
                endDate = endDate,
                data = payments
            });
        }

        // ⭐ ADMIN: GET ALL PAYMENTS
        [Authorize(Roles = "Admin")]
        [HttpGet("admin/all")]
        public async Task<IActionResult> GetAllPayments(
            [FromQuery] int pageSize = 20,
            [FromQuery] int pageNumber = 1,
            [FromQuery] string? status = null)
        {
            var lang = _langHelper.GetLanguage();

            if (pageSize <= 0 || pageSize > 100)
                pageSize = 20;
            if (pageNumber <= 0)
                pageNumber = 1;

            var baseQuery = _context.Payments.AsNoTracking()
                .Include(p => p.User)
                .Include(p => p.Plan);

            IQueryable<Models.Payment> query = baseQuery;
            if (!string.IsNullOrWhiteSpace(status))
            {
                query = query.Where(p => p.Status == status);
            }

            var orderedQuery = query.OrderByDescending(p => p.PaymentDate);

            var totalCount = await orderedQuery.CountAsync();
            var totalPages = (int)Math.Ceiling((double)totalCount / pageSize);

            var payments = await orderedQuery
                .Skip((pageNumber - 1) * pageSize)
                .Take(pageSize)
                .Select(p => new
                {
                    p.PaymentId,
                    p.UserId,
                    UserName = p.User != null ? p.User.FullName : "Unknown",
                    UserMobile = p.User != null ? p.User.MobileNumber : "Unknown",
                    p.RazorpayPaymentId,
                    p.RazorpayOrderId,
                    p.Amount,
                    p.Currency,
                    p.Status,
                    p.PaymentDate,
                    p.ReceiptNumber,
                    p.PaymentMethod,
                    p.VerificationDate,
                    PlanName = p.Plan != null ? p.Plan.PlanName : "Unknown",
                    p.ErrorMessage
                })
                .ToListAsync();

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("All payments retrieved", lang),
                totalCount = totalCount,
                totalPages = totalPages,
                currentPage = pageNumber,
                pageSize = pageSize,
                data = payments
            });
        }

        // ⭐ ADMIN: GET PAYMENT REVENUE STATS
        [Authorize(Roles = "Admin")]
        [HttpGet("admin/stats")]
        public async Task<IActionResult> GetRevenueStats()
        {
            var lang = _langHelper.GetLanguage();

            var totalRevenue = await _context.Payments
                .Where(p => p.Status == "Successful")
                .SumAsync(p => p.Amount);
                
            var totalSuccessful = await _context.Payments
                .CountAsync(p => p.Status == "Successful");
            var totalFailed = await _context.Payments.CountAsync(p => p.Status == "Failed");
            var totalPending = await _context.Payments.CountAsync(p => p.Status == "Pending");

            var revenueByPlan = await _context.Payments.AsNoTracking()
                .Where(p => p.Status == "Successful")
                .GroupBy(p => p.Plan!.PlanName)
                .Select(g => new
                {
                    planName = g.Key,
                    revenue = g.Sum(p => p.Amount),
                    count = g.Count(),
                    avgAmount = g.Average(p => p.Amount)
                })
                .ToListAsync();

            return Ok(new
            {
                success = true,
                message = await _translate.Translate("Revenue statistics retrieved", lang),
                data = new
                {
                    totalRevenue = totalRevenue,
                    totalSuccessful = totalSuccessful,
                    totalFailed = totalFailed,
                    totalPending = totalPending,
                    successRate = totalSuccessful + totalFailed > 0 ? 
                        Math.Round((double)totalSuccessful / (totalSuccessful + totalFailed) * 100, 2) : 0,
                    revenueByPlan = revenueByPlan
                }
            });
        }
    }
}
