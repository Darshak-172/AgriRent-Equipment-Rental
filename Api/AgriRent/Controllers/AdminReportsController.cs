using AgriRent.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System;
using System.Linq;
using System.Threading.Tasks;

namespace AgriRent.Controllers
{
    [Route("api/admin/reports")]
    [ApiController]
    [Authorize(Roles = "Admin")]
    public class AdminReportsController : ControllerBase
    {
        private readonly AppDbContext _context;

        public AdminReportsController(AppDbContext context)
        {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetReports([FromQuery] string? startDate, [FromQuery] string? endDate)
        {
            DateTime start = string.IsNullOrEmpty(startDate) ? DateTime.MinValue : DateTime.Parse(startDate).Date;
            DateTime end = string.IsNullOrEmpty(endDate) ? DateTime.MaxValue : DateTime.Parse(endDate).Date.AddDays(1).AddTicks(-1);

            // 1. Revenue over time (Grouped by Date)
            var payments = await _context.Payments
                .Where(p => p.Status == "Successful" && p.PaymentDate >= start && p.PaymentDate <= end)
                .Select(p => new { Date = p.PaymentDate.Date, Amount = p.Amount })
                .ToListAsync();

            var revenueByDate = payments
                .GroupBy(p => p.Date.ToString("yyyy-MM-dd"))
                .Select(g => new { Date = g.Key, TotalRevenue = g.Sum(x => x.Amount) })
                .ToDictionary(k => k.Date, v => v.TotalRevenue);

            // 2. Equipment Added Over Time
            var equipments = await _context.Equipments
                .Where(e => e.CreatedAt >= start && e.CreatedAt <= end)
                .Select(e => new { Date = e.CreatedAt.Date })
                .ToListAsync();

            var equipmentByDate = equipments
                .GroupBy(e => e.Date.ToString("yyyy-MM-dd"))
                .ToDictionary(g => g.Key, g => g.Count());

            // 3. Products Added Over Time
            var products = await _context.Products
                .Where(p => p.CreatedAt >= start && p.CreatedAt <= end)
                .Select(p => new { Date = p.CreatedAt.Date })
                .ToListAsync();

            var productsByDate = products
                .GroupBy(p => p.Date.ToString("yyyy-MM-dd"))
                .ToDictionary(g => g.Key, g => g.Count());

            // 4. Summaries (Total Users, etc) in range
            // We can't filter Users by date because there is no CreatedAt, so we return totals.
            var totalUsersCount = await _context.Users.CountAsync();
            var totalRevenue = payments.Sum(p => p.Amount);
            var totalEquipments = equipments.Count;
            var totalProducts = products.Count;

            // 5. Subscription Breakdown (Subscriptions Sold within Date Range)
            var subscriptions = await _context.UserSubscriptions
                .Where(s => s.StartDate >= start && s.StartDate <= end)
                .Join(_context.SubscriptionPlans, s => s.PlanId, p => p.PlanId, (s, p) => new { p.PlanName })
                .ToListAsync();

            var subscriptionsByPlan = subscriptions
                .GroupBy(s => s.PlanName)
                .ToDictionary(g => g.Key, g => g.Count());

            // 6. Complaints by Status in Range
            var complaints = await _context.Complaints
                .Where(c => c.CreatedAt >= start && c.CreatedAt <= end)
                .Select(c => c.Status)
                .ToListAsync();

            var complaintsByStatus = complaints
                .GroupBy(s => s)
                .ToDictionary(g => g.Key, g => g.Count());

            // Aggregate all dates from all trend data to make it easy for frontend to plot
            var allDatesHashSet = new HashSet<string>(revenueByDate.Keys);
            allDatesHashSet.UnionWith(equipmentByDate.Keys);
            allDatesHashSet.UnionWith(productsByDate.Keys);

            var sortedDates = allDatesHashSet.OrderBy(d => d).ToList();

            var trendsData = sortedDates.Select(date => new
            {
                Date = date,
                Revenue = revenueByDate.ContainsKey(date) ? revenueByDate[date] : 0,
                Equipments = equipmentByDate.ContainsKey(date) ? equipmentByDate[date] : 0,
                Products = productsByDate.ContainsKey(date) ? productsByDate[date] : 0
            }).ToList();


            return Ok(new
            {
                success = true,
                summary = new
                {
                    totalUsers = totalUsersCount,
                    totalRevenue = totalRevenue,
                    totalEquipments = totalEquipments,
                    totalProducts = totalProducts
                },
                trends = trendsData,
                breakdowns = new
                {
                    subscriptions = subscriptionsByPlan,
                    complaints = complaintsByStatus
                }
            });
        }
    }
}
