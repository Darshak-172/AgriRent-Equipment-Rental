using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Identity;
using AgriRent.Data;
using AgriRent.Models;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using AgriRent.DTOs;
using AgriRent.Services;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/admin")]
    public class AdminAuthController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _config;
        private readonly TranslateService _translate;
        private readonly PasswordHasher<Admin> _passwordHasher;
        private readonly JwtTokenService _jwtService;

        public AdminAuthController(AppDbContext context, IConfiguration config, TranslateService translate, JwtTokenService jwtService)
        {
            _context = context;
            _config = config;
            _translate = translate;
            _passwordHasher = new PasswordHasher<Admin>();
            _jwtService = jwtService;
        }

        // 🌍 Language helper
        private string GetLang()
        {
            var lang = Request.Headers["Accept-Language"].ToString();
            return string.IsNullOrWhiteSpace(lang) ? "en" : lang.Split(',')[0].Split('-')[0];
        }

        // 🔐 CREATE ADMIN (One-time use + Secret Key)
         [HttpPost("create")]
        public async Task<IActionResult> CreateAdmin([FromBody] CreateAdminDto dto)
        {
            var lang = GetLang();

            if (string.IsNullOrEmpty(dto.Username) ||
                string.IsNullOrEmpty(dto.Password) ||
                string.IsNullOrEmpty(dto.SecretKey))
            {
                return BadRequest(await _translate.Translate("Username, Password and SecretKey are required", lang));
            }

            // ✅ Validate Secret Key
            var secretKey = _config["AdminSettings:SecretKey"];
            if (dto.SecretKey != secretKey)
            {
                return Unauthorized(await _translate.Translate("Invalid Secret Key", lang));
            }

            // ✅ Check if Admin setup already exists
            if (await _context.Admins.AnyAsync(a => a.Username == dto.Username))
            {
                return BadRequest(await _translate.Translate("Admin with this username already exists", lang));
            }

            var admin = new Admin
            {
                Username = dto.Username,
                PasswordHash = "" // Placeholder, will be set below
            };
            admin.PasswordHash = _passwordHasher.HashPassword(admin, dto.Password);

            _context.Admins.Add(admin);
            await _context.SaveChangesAsync();

            return Ok(await _translate.Translate("Admin created successfully", lang));
        }



        // 🔐 ADMIN LOGIN
        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] AdminLoginDto dto)
        {
            var lang = GetLang();

            if (string.IsNullOrEmpty(dto.Username) || string.IsNullOrEmpty(dto.Password))
                return BadRequest(await _translate.Translate("Username and Password are required", lang));

            var admin = await _context.Admins.FirstOrDefaultAsync(a => a.Username == dto.Username);
            if (admin == null)
                return Unauthorized(await _translate.Translate("Invalid admin credentials", lang));

            var verifyResult = _passwordHasher.VerifyHashedPassword(admin, admin.PasswordHash, dto.Password);
            if (verifyResult == PasswordVerificationResult.Failed)
                return Unauthorized(await _translate.Translate("Invalid admin credentials", lang));

            var token = _jwtService.GenerateAdminToken(admin);

            return Ok(new
            {
                message = await _translate.Translate("Admin login successful", lang),
                token = token
            });
        }

        // 👥 GET ALL USERS – one row per user, roles as array, no duplicates
        [Authorize(Roles = "Admin")]
        [HttpGet("users")]
        public async Task<IActionResult> GetAllUsers(
            [FromQuery] string? role   = null,
            [FromQuery] string? status = null,
            [FromQuery] string? search = null,
            [FromQuery] int page = 1,
            [FromQuery] int pageSize = 10)
        {
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 1000) pageSize = 1000;

            // 1. Fetch all users
            var userQuery = _context.Users.AsNoTracking().AsQueryable();

            if (!string.IsNullOrEmpty(status))
                userQuery = userQuery.Where(u => u.Status.ToLower() == status.ToLower());

            if (!string.IsNullOrEmpty(search))
                userQuery = userQuery.Where(u => EF.Functions.Like(u.FullName, $"%{search}%") || EF.Functions.Like(u.MobileNumber, $"%{search}%"));

            if (!string.IsNullOrEmpty(role))
            {
                userQuery = userQuery.Where(u => _context.UserRoles
                    .Join(_context.Roles, ur => ur.RoleId, r => r.RoleId, (ur, r) => new { ur.UserId, r.RoleName })
                    .Any(r => r.UserId == u.UserId && r.RoleName == role));
            }

            var totalCount = await userQuery.CountAsync();
            var totalPages = (int)Math.Ceiling(totalCount / (double)pageSize);

            var users = await userQuery
                .OrderBy(u => u.UserId)
                .Skip((page - 1) * pageSize)
                .Take(pageSize)
                .Select(u => new { u.UserId, u.FullName, u.MobileNumber, u.Status })
                .ToListAsync();

            // 2. Fetch all user-role mappings for this page
            var userIds = users.Select(u => u.UserId).ToList();
            var roleMap = await (from ur in _context.UserRoles.AsNoTracking()
                                 join r in _context.Roles.AsNoTracking() on ur.RoleId equals r.RoleId
                                 where userIds.Contains(ur.UserId)
                                 select new { ur.UserId, r.RoleName })
                                .ToListAsync();

            // 3. Build result – one entry per user
            var result = users.Select(u =>
            {
                var roles = roleMap.Where(r => r.UserId == u.UserId).Select(r => r.RoleName).Distinct().ToList();
                return new
                {
                    u.UserId,
                    u.FullName,
                    u.MobileNumber,
                    u.Status,
                    Roles = roles.Count > 0 ? roles : new List<string> { "Unassigned" }
                };
            }).ToList();

            return Ok(new
            {
                data = result,
                pagination = new { page, pageSize, totalItems = totalCount, totalPages }
            });
        }

        // 📋 GET USER ACTIVITY (Admin) – all activity for a single user
        [Authorize(Roles = "Admin")]
        [HttpGet("users/{id}/activity")]
        public async Task<IActionResult> GetUserActivity(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return NotFound("User not found");

            // Roles
            var roles = await (from ur in _context.UserRoles.AsNoTracking()
                               join r in _context.Roles.AsNoTracking() on ur.RoleId equals r.RoleId
                               where ur.UserId == id
                               select r.RoleName).ToListAsync();

            // Equipments (as owner)
            var equipments = await _context.Equipments.AsNoTracking()
                .Where(e => e.OwnerId == id)
                .Select(e => new { e.EquipmentId, e.EquipmentName, e.Status, 
                    Price = e.HourlyPrice > 0 ? e.HourlyPrice : e.DailyPrice, 
                    PriceType = e.HourlyPrice > 0 ? "hr" : "day", 
                    e.Location })
                .ToListAsync();

            // Products (as seller)
            var products = await _context.Products.AsNoTracking()
                .Where(p => p.SellerId == id)
                .Select(p => new { p.ProductId, p.ProductName, p.Status, p.Price, p.Unit, p.Location })
                .ToListAsync();

            // Bookings (as farmer/renter)
            var bookings = await _context.EquipmentBookings.AsNoTracking()
                .Where(b => b.FarmerId == id)
                .Select(b => new { b.BookingId, b.EquipmentId, b.StartDate, b.EndDate, b.TotalPrice, b.Status })
                .ToListAsync();

            // Orders (as buyer)
            var orders = await _context.Orders.AsNoTracking()
                .Where(o => o.FarmerId == id)
                .Select(o => new { o.OrderId, o.TotalAmount, o.OrderStatus, o.CreatedAt })
                .ToListAsync();

            // Subscriptions
            var subscriptions = await _context.UserSubscriptions.AsNoTracking()
                .Where(s => s.UserId == id)
                .Select(s => new { s.SubscriptionId, s.PlanId, s.StartDate, s.EndDate, s.Status, s.PaymentStatus })
                .ToListAsync();

            // Complaints
            var complaints = await _context.Complaints.AsNoTracking()
                .Where(c => c.UserId == id)
                .Select(c => new { c.ComplaintId, c.Description, c.Status, c.CreatedAt })
                .ToListAsync();

            // Reviews (given by user)
            var reviews = await _context.Ratings.AsNoTracking()
                .Where(r => r.FromUserId == id)
                .Select(r => new { r.RatingId, r.RatingType, r.TargetId, r.RatingValue, r.Comment, r.CreatedAt })
                .ToListAsync();

            return Ok(new
            {
                UserId       = user.UserId,
                FullName     = user.FullName,
                MobileNumber = user.MobileNumber,
                Status       = user.Status,
                Roles        = roles,
                Equipments   = equipments,
                Products     = products,
                Bookings     = bookings,
                Orders       = orders,
                Subscriptions = subscriptions,
                Complaints   = complaints,
                Reviews      = reviews
            });
        }

        // 🔒 TOGGLE USER STATUS (Block/Unblock)
        [Authorize(Roles = "Admin")]
        [HttpPut("users/{id}/toggle-status")]
        public async Task<IActionResult> ToggleUserStatus(int id)
        {
            var user = await _context.Users.FindAsync(id);
            if (user == null) return NotFound(new { message = "User not found" });

            if (user.Status == "Blocked")
            {
                user.Status = "Active";
            }
            else
            {
                user.Status = "Blocked";
            }

            await _context.SaveChangesAsync();

            return Ok(new { message = $"User status updated to {user.Status}", status = user.Status });
        }
    }
}
