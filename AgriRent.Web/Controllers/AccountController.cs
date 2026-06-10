using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using AgriRent.DTOs;
using System.Text.Json;
using System.IdentityModel.Tokens.Jwt;

namespace AgriRent.Web.Controllers
{
    public class AccountController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ILogger<AccountController> _logger;

        public AccountController(ApiClient apiClient, ILogger<AccountController> logger)
        {
            _apiClient = apiClient;
            _logger = logger;
        }

        // GET: /Account/Login
        [Route("Account/Login")]
        [ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
        public IActionResult Login()
        {
            // Check if Remember Me cookies exist
            var dto = new LoginDto { MobileNumber = "", Password = "" };
            if (Request.Cookies.ContainsKey("UserMobile"))
            {
                dto.MobileNumber = Request.Cookies["UserMobile"] ?? "";
                if (Request.Cookies.ContainsKey("UserPass"))
                {
                    dto.Password = Request.Cookies["UserPass"] ?? "";
                }
                dto.RememberMe = true;
            }
            
            return View("~/Views/Auth/Login.cshtml", dto);
        }

        // POST: /Account/Login
        [HttpPost]
        [Route("Account/Login")]
        public async Task<IActionResult> Login(LoginDto dto)
        {
            try
            {
                // Call backend API for login
                var response = await _apiClient.PostAsyncRaw("/api/auth/login", dto);
                
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    
                    var result = JsonSerializer.Deserialize<JsonElement>(content, new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true
                    });

                    // Extract token and user data
                    if (result.TryGetProperty("data", out var data))
                    {
                        // Try to get token
                        if (!data.TryGetProperty("token", out var tokenElement))
                        {
                            _logger.LogWarning("Token not found in response");
                            ViewBag.Error = "Invalid response from server.";
                            return View("~/Views/Auth/Login.cshtml");
                        }
                        
                        var token = tokenElement.GetString();
                        
                        // Store JWT token
                        HttpContext.Session.SetString("JwtToken", token ?? "");
                        
                        // Extract user data from data object (not nested in user object)
                        // Get username
                        if (data.TryGetProperty("username", out var usernameProp))
                            HttpContext.Session.SetString("UserName", usernameProp.GetString() ?? "");
                        
                        // Get roles
                        if (data.TryGetProperty("roles", out var rolesProp) && rolesProp.ValueKind == JsonValueKind.Array)
                        {
                            var rolesArray = rolesProp.EnumerateArray().Select(r => r.GetString()).ToArray();
                            HttpContext.Session.SetString("UserRoles", string.Join(",", rolesArray));
                        }
                        
                        // Extract user ID from JWT token claims (Issue #9 - use JWT library)
                        if (!string.IsNullOrEmpty(token))
                        {
                            try
                            {
                                var tokenHandler = new JwtSecurityTokenHandler();
                                var jwtToken = tokenHandler.ReadToken(token) as JwtSecurityToken;
                                
                                if (jwtToken != null)
                                {
                                    // Extract userId from nameidentifier claim
                                    var userIdClaim = jwtToken.Claims.FirstOrDefault(c => 
                                        c.Type == "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier" ||
                                        c.Type == "sub" ||
                                        c.Type == "userid");
                                    
                                    if (userIdClaim != null)
                                    {
                                        HttpContext.Session.SetString("UserId", userIdClaim.Value);
                                    }
                                    
                                    // Extract mobile from mobilephone claim
                                    var mobileClaim = jwtToken.Claims.FirstOrDefault(c => 
                                        c.Type == "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone" ||
                                        c.Type == "mobilenumber");
                                    
                                    if (mobileClaim != null)
                                    {
                                        HttpContext.Session.SetString("UserMobile", mobileClaim.Value);
                                    }
                                }
                            }
                            catch (Exception)
                            {
                                // Log error but don't block login flow
                            }
                        }

                        // Note: Removed duplicate FetchUserProfile() call (Issue #6 fix)
                        // Profile data can be fetched on dashboard when needed, not on every login
                        // Store username as display name for now
                        var displayName = HttpContext.Session.GetString("UserName") ?? "User";
                        HttpContext.Session.SetString("DisplayName", displayName);

                        // Handle Remember Me Cookies
                        if (dto.RememberMe)
                        {
                            var cookieOptions = new CookieOptions
                            {
                                Expires = DateTime.Now.AddDays(30),
                                HttpOnly = true,
                                Secure = true
                            };
                            Response.Cookies.Append("UserMobile", dto.MobileNumber, cookieOptions);
                            Response.Cookies.Append("UserPass", dto.Password, cookieOptions);
                        }
                        else
                        {
                            Response.Cookies.Delete("UserMobile");
                            Response.Cookies.Delete("UserPass");
                        }

                        // Check if it's an AJAX request
                        if (Request.Headers["X-Requested-With"] == "XMLHttpRequest")
                        {
                            return Json(new { success = true, redirectUrl = Url.Action("Index", "Home") });
                        }

                        return RedirectToAction("Index", "Home");
                    }
                }

                // Login failed
                var errorContent = await response.Content.ReadAsStringAsync();
                var errorResult = JsonSerializer.Deserialize<JsonElement>(errorContent, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                var errorMsg = errorResult.TryGetProperty("message", out var msg) 
                    ? msg.GetString() 
                    : "Invalid mobile number or password.";

                if (Request.Headers["X-Requested-With"] == "XMLHttpRequest")
                {
                    return Json(new { success = false, message = errorMsg });
                }

                ViewBag.Error = errorMsg;
                return View("~/Views/Auth/Login.cshtml");
            }
            catch (Exception)
            {
                var errorMsg = "An error occurred. Please try again.";
                
                if (Request.Headers["X-Requested-With"] == "XMLHttpRequest")
                {
                    return Json(new { success = false, message = errorMsg });
                }

                ViewBag.Error = errorMsg;
                return View("~/Views/Auth/Login.cshtml");
            }
        }

        // GET: /Account/Logout
        [Route("Account/Logout")]
        public IActionResult Logout()
        {
            HttpContext.Session.Clear();
            
            // Note: Keep Remember Me cookies to allow auto-fill on next login
            // Cookies will expire after 30 days as configured
            
            return RedirectToAction("Index", "Home");
        }

        // GET: /Account/ForgotPassword
        [Route("Account/ForgotPassword")]
        [ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
        public IActionResult ForgotPassword()
        {
            return View("~/Views/Auth/ForgotPassword.cshtml");
        }

        // GET: /Account/Register
        [Route("Account/Register")]
        [ResponseCache(NoStore = true, Location = ResponseCacheLocation.None)]
        public IActionResult Register()
        {
            return View("~/Views/Auth/Register.cshtml");
        }

        // POST: /Account/Register
        [HttpPost]
        [Route("Account/Register")]
        public async Task<IActionResult> Register(RegisterDto dto)
        {
            try
            {
                var response = await _apiClient.PostAsyncRaw("/api/auth/register", dto);
                
                if (response.IsSuccessStatusCode)
                {
                    ViewBag.Success = "Registration successful! Please login.";
                    return RedirectToAction("Login");
                }

                var errorContent = await response.Content.ReadAsStringAsync();
                var errorResult = JsonSerializer.Deserialize<JsonElement>(errorContent, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                ViewBag.Error = errorResult.TryGetProperty("message", out var msg) 
                    ? msg.GetString() 
                    : "Registration failed. Please try again.";
                    
                return View("~/Views/Auth/Register.cshtml");
            }
            catch (Exception ex)
            {
                ViewBag.Error = "An error occurred. Please try again.";
                _logger.LogError(ex, "Register error");
                return View("~/Views/Auth/Register.cshtml");
            }
        }

        // Helper method to fetch user profile and store display name
        private async Task FetchUserProfile()
        {
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
                        // Get translated display name
                        if (data.TryGetProperty("name", out var nameProp))
                        {
                            HttpContext.Session.SetString("DisplayName", nameProp.GetString() ?? "");
                        }
                        
                        // Get translated roles
                        if (data.TryGetProperty("roles", out var rolesProp) && rolesProp.ValueKind == JsonValueKind.Array)
                        {
                            var rolesArray = rolesProp.EnumerateArray().Select(r => r.GetString()).ToArray();
                            HttpContext.Session.SetString("DisplayRoles", string.Join(", ", rolesArray));
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching profile");
                // Fallback to username if profile fetch fails
                var username = HttpContext.Session.GetString("UserName");
                if (!string.IsNullOrEmpty(username))
                {
                    HttpContext.Session.SetString("DisplayName", username);
                }
            }
        }
    }
}
