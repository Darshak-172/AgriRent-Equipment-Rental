using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using AgriRent.Data;
using AgriRent.Models;
using AgriRent.DTOs;
using AgriRent.Services;
using Microsoft.AspNetCore.Identity;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/auth")]
    public class AuthController : ControllerBase
    {
        private readonly AppDbContext _context;
        private readonly IConfiguration _config;
        private readonly TwoFactorService _otp;
        private readonly TranslateService _translate;
        private readonly PasswordHasher<User> _passwordHasher;
        private readonly JwtTokenService _jwtService;

        public AuthController(AppDbContext context, IConfiguration config, TwoFactorService otp, TranslateService translate, JwtTokenService jwtService)
        {
            _context = context;
            _config = config;
            _otp = otp;
            _translate = translate;
            _passwordHasher = new PasswordHasher<User>();
            _jwtService = jwtService;
        }


        // 🌍 Language helper
        private string GetLang()
        {
            var lang = Request.Headers["Accept-Language"].ToString();
            return string.IsNullOrWhiteSpace(lang) ? "en" : lang.Split(',')[0].Split('-')[0];
        }

        // ================= SEND OTP =================
        [HttpPost("send-otp")]
        public async Task<IActionResult> SendOtp(SendOtpDto dto)
        {

            if (string.IsNullOrWhiteSpace(dto.MobileNumber))
                return BadRequest(await _translate.Translate("Mobile required", GetLang()));

            var sessionId = await _otp.SendOtp(dto.MobileNumber);

            return Ok(new { message = await _translate.Translate("OTP sent successfully", GetLang()), sessionId });
        }

        // ================= PASSWORD LOGIN =================
        [HttpPost("login")]
        public async Task<IActionResult> LoginAsync(LoginDto dto)
        {

            var user = await _context.Users.FirstOrDefaultAsync(x => x.MobileNumber == dto.MobileNumber);
            if (user == null)
                return Unauthorized(await _translate.Translate("Invalid credentials", GetLang()));

            if (user.Status == "Blocked")
                return Unauthorized(await _translate.Translate("Account is blocked. Please contact support.", GetLang()));

            var verificationResult = _passwordHasher.VerifyHashedPassword(user, user.PasswordHash, dto.Password);
            if (verificationResult == PasswordVerificationResult.Failed)
                return Unauthorized(await _translate.Translate("Invalid credentials", GetLang()));

            return Ok(new { message = await _translate.Translate("Login successful", GetLang()), data = await BuildAuthAsync(user) });
        }

        // ================= OTP LOGIN =================
        [HttpPost("otp-login")]
        public async Task<IActionResult> OtpLogin(VerifyOtpDto dto)
        {
            if (!await _otp.VerifyOtp(dto.SessionId, dto.Otp))
                return BadRequest(await _translate.Translate("Invalid OTP", GetLang()));

            var user = await _context.Users.FirstOrDefaultAsync(x => x.MobileNumber == dto.MobileNumber);
            if (user == null) return NotFound(await _translate.Translate("User not found", GetLang()));

            if (user.Status == "Blocked")
                return Unauthorized(await _translate.Translate("Account is blocked. Please contact support.", GetLang()));

            return Ok(new { message = await _translate.Translate("Login successful", GetLang()), data = await BuildAuthAsync(user) });
        }

        // ================= OTP REGISTER =================
        [HttpPost("otp-register")]
        public async Task<IActionResult> OtpRegister(OtpRegisterDto dto)
        {
            if (!await _otp.VerifyOtp(dto.SessionId, dto.Otp))
                return BadRequest(await _translate.Translate("Invalid OTP", GetLang()));

            if (await _context.Users.AnyAsync(x => x.MobileNumber == dto.MobileNumber))
                return BadRequest(await _translate.Translate("Already registered", GetLang()));

            var user = new User
            {
                // ⭐ Name -> Transliterate for Database (Ramesh -> Ramesh)
                FullName = await _translate.TransliterateToEnglish(dto.FullName, GetLang()),
                MobileNumber = dto.MobileNumber,
                PasswordHash = _passwordHasher.HashPassword(null!, dto.Password)
            };

            _context.Users.Add(user);
            await _context.SaveChangesAsync();
            await AssignRoleAsync(user.UserId, "Farmer");

            return Ok(new
            {
                message = await _translate.Translate("Registration successful", GetLang()),
                data = await BuildAuthAsync(user, dto.FullName)
            });
        }

        // ================= FORGOT PASSWORD =================
        [HttpPost("otp-forgot")]
        public async Task<IActionResult> OtpForgot(OtpForgotPasswordDto dto)
        {
            if (!await _otp.VerifyOtp(dto.SessionId, dto.Otp))
                return BadRequest(await _translate.Translate("Invalid OTP", GetLang()));

            var user = await _context.Users.FirstOrDefaultAsync(x => x.MobileNumber == dto.MobileNumber);
            if (user == null)
                return NotFound(await _translate.Translate("User not found", GetLang()));

            if (user.Status == "Blocked")
                return Unauthorized(await _translate.Translate("Account is blocked. Please contact support.", GetLang()));

            user.PasswordHash = _passwordHasher.HashPassword(user, dto.NewPassword);
            await _context.SaveChangesAsync();

            return Ok(new { message = await _translate.Translate("Password reset successful", GetLang()) });
        }

        // ================= RESET PASSWORD =================
        [HttpPost("otp-reset")]
        public async Task<IActionResult> OtpReset(OtpResetPasswordDto dto)
        {
            if (!await _otp.VerifyOtp(dto.SessionId, dto.Otp))
                return BadRequest(await _translate.Translate("Invalid OTP", GetLang()));

            var user = await _context.Users.FirstOrDefaultAsync(x => x.MobileNumber == dto.MobileNumber);
            if (user == null) return NotFound(await _translate.Translate("User not found", GetLang()));

            if (user.Status == "Blocked")
                return Unauthorized(await _translate.Translate("Account is blocked. Please contact support.", GetLang()));

            var verificationResult = _passwordHasher.VerifyHashedPassword(user, user.PasswordHash, dto.OldPassword);
            if (verificationResult == PasswordVerificationResult.Failed)
                return BadRequest(await _translate.Translate("Old password wrong", GetLang()));

            user.PasswordHash = _passwordHasher.HashPassword(user, dto.NewPassword);
            await _context.SaveChangesAsync();

            return Ok(await _translate.Translate("Password changed", GetLang()));
        }

        // ================= REFRESH =================
        [HttpPost("refresh")]
        public async Task<IActionResult> Refresh(RefreshTokenDto dto)
        {
            var user = await _context.Users.FirstOrDefaultAsync(x =>
                x.RefreshToken == dto.RefreshToken &&
                x.RefreshTokenExpiry > DateTime.UtcNow);

            if (user == null) return Unauthorized("Expired");

            return Ok(await BuildAuthAsync(user));
        }

        // ================= HELPERS =================
        private async Task<object> BuildAuthAsync(User user, string? preferredName = null)
        {
            user.RefreshToken = Convert.ToBase64String(RandomNumberGenerator.GetBytes(64));
            user.RefreshTokenExpiry = DateTime.UtcNow.AddDays(30);
            await _context.SaveChangesAsync();
            
            var lang = GetLang();

            // 🎯 Get user roles
            var roles = await (from ur in _context.UserRoles
                         join r in _context.Roles on ur.RoleId equals r.RoleId
                         where ur.UserId == user.UserId
                         select r.RoleName).ToListAsync();

            // ⭐ TRANSLATE ROLES & TRANSLITERATE NAME
            var rolesTranslated = new List<string>();
            foreach (var role in roles)
            {
                rolesTranslated.Add(lang == "en" ? role : await _translate.Translate(role, lang));
            }

            // ⚡ UX: If we have the exact input name (preferredName) and it's not empty, use it!
            var username = !string.IsNullOrWhiteSpace(preferredName) 
                ? preferredName 
                : (lang == "en" ? user.FullName : await _translate.TransliterateToNative(user.FullName, lang));

            return new
            {
                userId = user.UserId,
                username = username,
                mobile = user.MobileNumber,
                roles = rolesTranslated,
                token = _jwtService.GenerateToken(user, roles),
                refreshToken = user.RefreshToken
            };
        }



        // ================= ROLE ASSIGNMENT =================
        private async Task AssignRoleAsync(int userId, string roleName)
        {
            var role = await _context.Roles.FirstOrDefaultAsync(r => r.RoleName == roleName);

            if (role == null)
                throw new Exception("Role not found in database: " + roleName);

            bool already = await _context.UserRoles.AnyAsync(x =>
                x.UserId == userId && x.RoleId == role.RoleId);

            if (already) return;

            _context.UserRoles.Add(new UserRole
            {
                UserId = userId,
                RoleId = role.RoleId
            });

            await _context.SaveChangesAsync();
        }

        // ================= UPDATE FCM TOKEN =================
        [Microsoft.AspNetCore.Authorization.Authorize]
        [HttpPost("update-fcm-token")]
        public async Task<IActionResult> UpdateFcmToken([FromBody] FcmTokenDto dto)
        {
            var userId = int.Parse(User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)!.Value);
            var user = await _context.Users.FindAsync(userId);
            if (user == null)
                return NotFound();

            user.FcmToken = dto.FcmToken;
            if (!string.IsNullOrEmpty(dto.Language))
                user.PreferredLanguage = dto.Language;
            await _context.SaveChangesAsync();

            return Ok(new { message = "FCM token updated successfully." });
        }
    }
}