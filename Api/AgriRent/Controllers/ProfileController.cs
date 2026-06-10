using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Security.Claims;
using AgriRent.Services;
using AgriRent.Data;
using AgriRent.DTOs;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Identity;
using AgriRent.Models;

namespace AgriRent.Controllers
{
    [ApiController]
    [Route("api/profile")]
    public class ProfileController : ControllerBase
    {
        private readonly TranslateService _translator;
        private readonly AppDbContext _context;
        private readonly PasswordHasher<User> _passwordHasher;
        private readonly LanguageHelper _langHelper;

        public ProfileController(TranslateService translator, AppDbContext context, LanguageHelper langHelper)
        {
            _translator = translator;
            _context = context;
            _passwordHasher = new PasswordHasher<User>();
            _langHelper = langHelper;
        }



        // ⭐ PROFILE API (NAME + ROLES TRANSLATED)
        [Authorize]
        [HttpGet("me")]
        public async Task<IActionResult> Me()
        {
            var lang = _langHelper.GetLanguage();

            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            var nameEn = User.Identity?.Name ?? "";

            // 🔹 Translate Name for UI
            var displayName = lang == "en"
                ? nameEn
                : await _translator.TransliterateToNative(nameEn, lang); // Transliterate Name

            // 🔹 Get Roles from JWT
            var rolesEn = User.Claims
                .Where(c => c.Type == ClaimTypes.Role)
                .Select(c => c.Value)
                .ToList();

            // 🔹 Translate Roles for UI
            var rolesTranslated = new List<string>();

            foreach (var role in rolesEn)
            {
                var translatedRole = lang == "en"
                    ? role
                    : await _translator.Translate(role, lang);

                rolesTranslated.Add(translatedRole);
            }

            return Ok(new
            {
                success = true,
                message = await _translator.Translate("Profile loaded successfully", lang),
                data = new
                {
                    userId,
                    name = displayName,
                    roles = rolesTranslated
                }
            });
        }

        // ⭐ UPDATE PROFILE
        [Authorize]
        [HttpPut("update")]
        public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileDto dto)
        {
            var lang = _langHelper.GetLanguage();

            if (!ModelState.IsValid)
                return BadRequest(new
                {
                    success = false,
                    message = await _translator.Translate("Invalid data", lang)
                });

            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (string.IsNullOrEmpty(userId) || !int.TryParse(userId, out int uid))
                return Unauthorized(new
                {
                    success = false,
                    message = await _translator.Translate("Unauthorized", lang)
                });

            var user = await _context.Users.FindAsync(uid);
            if (user == null)
                return NotFound(new
                {
                    success = false,
                    message = await _translator.Translate("User not found", lang)
                });

            // Update user details
            user.FullName = dto.FullName;
            await _context.SaveChangesAsync();

            return Ok(new
            {
                success = true,
                message = await _translator.Translate("Profile updated successfully", lang)
            });
        }

        // ⭐ CHANGE PASSWORD
        [Authorize]
        [HttpPut("change-password")]
        public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordDto dto)
        {
            var lang = _langHelper.GetLanguage();

            if (!ModelState.IsValid)
                return BadRequest(new
                {
                    success = false,
                    message = await _translator.Translate("Invalid data", lang)
                });

            var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (string.IsNullOrEmpty(userId) || !int.TryParse(userId, out int uid))
                return Unauthorized(new
                {
                    success = false,
                    message = await _translator.Translate("Unauthorized", lang)
                });

            var user = await _context.Users.FindAsync(uid);
            if (user == null)
                return NotFound(new
                {
                    success = false,
                    message = await _translator.Translate("User not found", lang)
                });

            // Verify old password
            var verifyResult = _passwordHasher.VerifyHashedPassword(user, user.PasswordHash, dto.OldPassword);
            if (verifyResult == PasswordVerificationResult.Failed)
                return BadRequest(new
                {
                    success = false,
                    message = await _translator.Translate("Old password is incorrect", lang)
                });

            // Hash and update new password
            user.PasswordHash = _passwordHasher.HashPassword(user, dto.NewPassword);
            await _context.SaveChangesAsync();

            return Ok(new
            {
                success = true,
                message = await _translator.Translate("Password changed successfully", lang)
            });
        }
    }
}