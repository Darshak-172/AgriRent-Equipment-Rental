using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using System.Text.Json;

namespace AgriRent.Web.Controllers
{
    public class ProfileController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ILogger<ProfileController> _logger;

        public ProfileController(ApiClient apiClient, ILogger<ProfileController> logger)
        {
            _apiClient = apiClient;
            _logger = logger;
        }

        // Middleware to check if user is logged in
        private bool IsLoggedIn()
        {
            return !string.IsNullOrEmpty(HttpContext.Session.GetString("UserId"));
        }

        // GET: /Profile (View Profile)
        [Route("Profile")]
        public async Task<IActionResult> Index()
        {
            if (!IsLoggedIn())
                return RedirectToAction("Login", "Account");

            try
            {
                var response = await _apiClient.GetAsyncRaw("/api/profile/me");
                
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var result = JsonSerializer.Deserialize<JsonElement>(content, new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    });

                    if (result.TryGetProperty("data", out var data))
                    {
                        ViewBag.UserId = HttpContext.Session.GetString("UserId");
                        ViewBag.DisplayName = data.TryGetProperty("name", out var name) ? name.GetString() : "";
                        ViewBag.Username = HttpContext.Session.GetString("UserName");
                        ViewBag.MobileNumber = HttpContext.Session.GetString("UserMobile");
                        ViewBag.Email = ""; // Add if available in profile API
                        
                        if (data.TryGetProperty("roles", out var roles) && roles.ValueKind == JsonValueKind.Array)
                        {
                            var rolesArray = roles.EnumerateArray().Select(r => r.GetString()).ToArray();
                            ViewBag.Roles = string.Join(", ", rolesArray);
                        }
                    }
                }

                return View();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error loading profile");
                ViewBag.Error = "Unable to load profile. Please try again.";
                return View();
            }
        }

        // GET: /Profile/Edit
        [Route("Profile/Edit")]
        public async Task<IActionResult> Edit()
        {
            if (!IsLoggedIn())
                return RedirectToAction("Login", "Account");

            try
            {
                var response = await _apiClient.GetAsyncRaw("/api/profile/me");
                
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var result = JsonSerializer.Deserialize<JsonElement>(content, new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    });

                    if (result.TryGetProperty("data", out var data))
                    {
                        ViewBag.DisplayName = data.TryGetProperty("name", out var name) ? name.GetString() : "";
                        ViewBag.Username = HttpContext.Session.GetString("UserName");
                        ViewBag.MobileNumber = HttpContext.Session.GetString("UserMobile");
                        ViewBag.Email = ""; // Add if available
                    }
                }

                return View();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error loading profile for edit");
                ViewBag.Error = "Unable to load profile. Please try again.";
                return View();
            }
        }

        // POST: /Profile/Edit
        [HttpPost]
        [Route("Profile/Edit")]
        public async Task<IActionResult> Edit(string fullName)
        {
            if (!IsLoggedIn())
                return RedirectToAction("Login", "Account");

            try
            {
                // Call API to update profile
                var updateData = new
                {
                    fullName = fullName
                };

                var response = await _apiClient.PutAsyncRaw("/api/profile/update", updateData);
                var content = await response.Content.ReadAsStringAsync();
                var result = JsonSerializer.Deserialize<JsonElement>(content, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                if (response.IsSuccessStatusCode && result.TryGetProperty("success", out var successVal) && successVal.GetBoolean())
                {
                    ViewBag.Success = result.TryGetProperty("message", out var msgVal) ? msgVal.GetString() : "Profile updated successfully!";
                    // Update session display name
                    HttpContext.Session.SetString("DisplayName", fullName);
                }
                else
                {
                    var errorMsg = result.TryGetProperty("message", out var errMsg) ? errMsg.GetString() : "Unable to update profile";
                    ViewBag.Error = errorMsg;
                }

                // Reload form data
                ViewBag.DisplayName = fullName;
                ViewBag.Username = HttpContext.Session.GetString("UserName");
                ViewBag.MobileNumber = HttpContext.Session.GetString("UserMobile");
                
                return View();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating profile");
                ViewBag.Error = "Unable to update profile. Please try again.";
                
                // Reload form data
                ViewBag.DisplayName = fullName;
                ViewBag.Username = HttpContext.Session.GetString("UserName");
                ViewBag.MobileNumber = HttpContext.Session.GetString("UserMobile");
                
                return View();
            }
        }

        // GET: /Profile/ChangePassword
        [Route("Profile/ChangePassword")]
        public IActionResult ChangePassword()
        {
            if (!IsLoggedIn())
                return RedirectToAction("Login", "Account");

            return View();
        }

        // POST: /Profile/ChangePassword
        [HttpPost]
        [Route("Profile/ChangePassword")]
        public async Task<IActionResult> ChangePassword(string oldPassword, string newPassword, string confirmPassword)
        {
            if (!IsLoggedIn())
                return RedirectToAction("Login", "Account");

            try
            {
                // Validate passwords
                if (string.IsNullOrEmpty(oldPassword) || string.IsNullOrEmpty(newPassword))
                {
                    ViewBag.Error = "All fields are required.";
                    return View();
                }

                if (newPassword != confirmPassword)
                {
                    ViewBag.Error = "New password and confirm password do not match.";
                    return View();
                }

                if (newPassword.Length < 6)
                {
                    ViewBag.Error = "Password must be at least 6 characters long.";
                    return View();
                }

                // Call API to change password
                var changePasswordData = new
                {
                    oldPassword = oldPassword,
                    newPassword = newPassword
                };

                var response = await _apiClient.PutAsyncRaw("/api/profile/change-password", changePasswordData);
                var content = await response.Content.ReadAsStringAsync();
                var result = JsonSerializer.Deserialize<JsonElement>(content, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                if (response.IsSuccessStatusCode && result.TryGetProperty("success", out var successVal) && successVal.GetBoolean())
                {
                    ViewBag.Success = result.TryGetProperty("message", out var msgVal) ? msgVal.GetString() : "Password changed successfully!";
                }
                else
                {
                    var errorMsg = result.TryGetProperty("message", out var errMsg) ? errMsg.GetString() : "Unable to change password";
                    ViewBag.Error = errorMsg;
                }
                
                return View();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error changing password");
                ViewBag.Error = "Unable to change password. Please try again.";
                return View();
            }
        }
    }
}
