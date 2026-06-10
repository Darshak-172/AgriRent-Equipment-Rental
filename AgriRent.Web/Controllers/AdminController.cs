using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using System.Text.Json;
using AgriRent.DTOs;
using AgriRent.ViewModels;

namespace AgriRent.Web.Controllers
{
    public class AdminController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ILogger<AdminController> _logger;

        public AdminController(ApiClient apiClient, ILogger<AdminController> logger)
        {
            _apiClient = apiClient;
            _logger = logger;
        }

        // GET: /Admin/Login
        [HttpGet]
        public IActionResult Login()
        {
            if (HttpContext.Session.GetString("AdminToken") != null)
                return RedirectToAction("Dashboard");

            var dto = new AdminLoginDto { Username = "", Password = "" };
            if (Request.Cookies.ContainsKey("AdminUser"))
            {
                dto.Username = Request.Cookies["AdminUser"] ?? "";
                if (Request.Cookies.ContainsKey("AdminPass"))
                {
                    dto.Password = Request.Cookies["AdminPass"] ?? "";
                }
                dto.RememberMe = true;
            }

            return View(dto);
        }

        // POST: /Admin/Login
        [HttpPost]
        public async Task<IActionResult> Login(AdminLoginDto dto)
        {
            if (!ModelState.IsValid)
                return View(dto);

            try
            {
                var response = await _apiClient.PostAsyncRaw("/api/admin/login", dto);

                if (!response.IsSuccessStatusCode)
                {
                    ViewBag.Error = "Invalid Username or Password";
                    return View(dto);
                }

                var result = await response.Content.ReadAsStringAsync();
                var data = JsonSerializer.Deserialize<JsonElement>(result, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                // Extract token
                if (data.TryGetProperty("token", out var token))
                {
                    HttpContext.Session.SetString("AdminToken", token.GetString() ?? "");
                    HttpContext.Session.SetString("AdminUsername", dto.Username);

                    // Handle Remember Me Cookies
                    if (dto.RememberMe)
                    {
                        var cookieOptions = new CookieOptions
                        {
                            Expires = DateTime.Now.AddDays(30),
                            HttpOnly = true,
                            Secure = true
                        };
                        Response.Cookies.Append("AdminUser", dto.Username, cookieOptions);
                        Response.Cookies.Append("AdminPass", dto.Password, cookieOptions);
                    }
                    else
                    {
                        Response.Cookies.Delete("AdminUser");
                        Response.Cookies.Delete("AdminPass");
                    }

                    return RedirectToAction("Dashboard");
                }

                ViewBag.Error = "Login failed: token not received";
                return View(dto);
            }
            catch (Exception ex)
            {
                ViewBag.Error = "An error occurred. Please try again.";
                _logger.LogError(ex, "Admin login error");
                return View(dto);
            }
        }

        // GET: /Admin/Dashboard
        public async Task<IActionResult> Dashboard()
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                // Call backend API to get dashboard data
                var response = await _apiClient.GetAsyncRaw("/api/admin/dashboard");
                
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    _logger.LogInformation("Dashboard API Response: {Content}", content);
                    
                    try 
                    {
                        var jsonDoc = JsonDocument.Parse(content);
                        if (jsonDoc.RootElement.TryGetProperty("data", out var dataElement))
                        {
                            var dataFromWrapper = JsonSerializer.Deserialize<ViewModels.AdminDashboardViewModel>(dataElement.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                            return View(dataFromWrapper ?? new ViewModels.AdminDashboardViewModel());
                        }
                    }
                    catch { /* Fallback to direct deserialization */ }

                    // Explicitly use camelCase naming policy
                    var options = new JsonSerializerOptions
                    {
                        PropertyNameCaseInsensitive = true,
                        PropertyNamingPolicy = JsonNamingPolicy.CamelCase
                    };
                    
                    var data = JsonSerializer.Deserialize<ViewModels.AdminDashboardViewModel>(content, options);

                    return View(data ?? new ViewModels.AdminDashboardViewModel());
                }

                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError("Dashboard API failed. Status: {Status}, Content: {Content}", response.StatusCode, errorContent);
                // If API call fails, return empty view model
                return View(new ViewModels.AdminDashboardViewModel());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Dashboard error");
                return View(new ViewModels.AdminDashboardViewModel());
            }
        }

        // GET: /Admin/PendingEquipments
        public async Task<IActionResult> PendingEquipments(string status = "Pending", int page = 1, int pageSize = 10)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            if (page < 1) page = 1;

            try
            {
                var url = $"/api/admin/equipment?status={Uri.EscapeDataString(status)}&page={page}&pageSize={pageSize}";
                var response = await _apiClient.GetAsyncRaw(url);

                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var jsonDoc = JsonDocument.Parse(content);

                    if (jsonDoc.RootElement.TryGetProperty("data", out var dataElement))
                    {
                        var equipments = JsonSerializer.Deserialize<List<ViewModels.PendingEquipmentViewModel>>(
                            dataElement.GetRawText(),
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                        if (jsonDoc.RootElement.TryGetProperty("pagination", out var pag))
                        {
                            ViewBag.CurrentPage = pag.GetProperty("page").GetInt32();
                            ViewBag.TotalPages  = pag.GetProperty("totalPages").GetInt32();
                            ViewBag.TotalItems  = pag.GetProperty("totalItems").GetInt32();
                            ViewBag.PageSize    = pageSize;
                        }
                        else { ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize; }

                        ViewBag.SelectedStatus = status;
                        return View(equipments ?? new List<ViewModels.PendingEquipmentViewModel>());
                    }
                }

                ViewBag.SelectedStatus = status;
                ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize;
                return View(new List<ViewModels.PendingEquipmentViewModel>());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "PendingEquipments error");
                ViewBag.SelectedStatus = status;
                ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize;
                return View(new List<ViewModels.PendingEquipmentViewModel>());
            }
        }

        // POST: /Admin/ApproveEquipment
        [HttpPost]
        public async Task<IActionResult> ApproveEquipment(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/equipment/approve/{id}", new { });
                
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction("PendingEquipments", new { status = "Pending" });
                }

                return BadRequest("Failed to approve equipment");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ApproveEquipment error");
                return BadRequest("An error occurred");
            }
        }

        // POST: /Admin/RejectEquipment
        [HttpPost]
        public async Task<IActionResult> RejectEquipment(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/equipment/reject/{id}", new { });
                
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction("PendingEquipments", new { status = "Pending" });
                }

                return BadRequest("Failed to reject equipment");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "RejectEquipment error");
                return BadRequest("An error occurred");
            }
        }

        // GET: /Admin/PendingProducts
        public async Task<IActionResult> PendingProducts(string status = "Pending", int page = 1, int pageSize = 10)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            if (page < 1) page = 1;
            ViewBag.SelectedStatus = status;

            try
            {
                var url = status.Equals("All", StringComparison.OrdinalIgnoreCase)
                    ? $"/api/admin/products/all?page={page}&pageSize={pageSize}"
                    : $"/api/admin/products/all?status={Uri.EscapeDataString(status)}&page={page}&pageSize={pageSize}";

                var response = await _apiClient.GetAsyncRaw(url);
                var content = await response.Content.ReadAsStringAsync();

                _logger.LogInformation("[PendingProducts] Status: {StatusCode} | URL: {Url}", response.StatusCode, url);

                if (response.IsSuccessStatusCode)
                {
                    var root = JsonDocument.Parse(content).RootElement;

                    if (root.TryGetProperty("data", out var dataElement))
                    {
                        var products = JsonSerializer.Deserialize<List<ViewModels.PendingProductViewModel>>(
                            dataElement.GetRawText(),
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                        if (root.TryGetProperty("pagination", out var pag))
                        {
                            ViewBag.CurrentPage = pag.GetProperty("page").GetInt32();
                            ViewBag.TotalPages  = pag.GetProperty("totalPages").GetInt32();
                            ViewBag.TotalItems  = pag.GetProperty("totalItems").GetInt32();
                            ViewBag.PageSize    = pageSize;
                        }
                        else { ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize; }

                        return View(products ?? new List<ViewModels.PendingProductViewModel>());
                    }
                }

                _logger.LogError($"[PendingProducts] API call failed or no 'data' key. Status: {response.StatusCode}");
                ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize;
                return View(new List<ViewModels.PendingProductViewModel>());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "PendingProducts error");
                ViewBag.CurrentPage = 1; ViewBag.TotalPages = 1; ViewBag.TotalItems = 0; ViewBag.PageSize = pageSize;
                return View(new List<ViewModels.PendingProductViewModel>());
            }
        }

        // POST: /Admin/ApproveProduct
        [HttpPost]
        public async Task<IActionResult> ApproveProduct(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/products/approve/{id}", new { });
                
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction("PendingProducts", new { status = "Pending" });
                }

                return BadRequest("Failed to approve product");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ApproveProduct error");
                return BadRequest("An error occurred");
            }
        }

        // POST: /Admin/RejectProduct
        [HttpPost]
        public async Task<IActionResult> RejectProduct(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/products/reject/{id}", new { });
                
                if (response.IsSuccessStatusCode)
                {
                    return RedirectToAction("PendingProducts", new { status = "Pending" });
                }

                return BadRequest("Failed to reject product");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "RejectProduct error");
                return BadRequest("An error occurred");
            }
        }

        // GET: /Admin/Users
        [ResponseCache(Location = ResponseCacheLocation.None, NoStore = true)]
        public async Task<IActionResult> Users(string? role = null, string? search = null, string? status = null)
        {
            Response.Headers.Append("Cache-Control", "no-cache, no-store, must-revalidate");
            Response.Headers.Append("Pragma", "no-cache");
            Response.Headers.Append("Expires", "0");

            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var url = "/api/admin/users";
                var queryParams = new List<string>();
                queryParams.Add("pageSize=1000"); // fetch all to avoid pagination cut-off
                if (!string.IsNullOrEmpty(role)) queryParams.Add($"role={Uri.EscapeDataString(role)}");
                if (!string.IsNullOrEmpty(search)) queryParams.Add($"search={Uri.EscapeDataString(search)}");
                if (!string.IsNullOrEmpty(status)) queryParams.Add($"status={Uri.EscapeDataString(status)}");
                url += "?" + string.Join("&", queryParams);

                var response = await _apiClient.GetAsyncRaw(url);

                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var result = System.Text.Json.JsonSerializer.Deserialize<System.Text.Json.JsonElement>(content);

                    if (result.TryGetProperty("data", out var dataProp))
                    {
                        var users = System.Text.Json.JsonSerializer.Deserialize<List<ViewModels.UserAdminViewModel>>(
                            dataProp.GetRawText(),
                            new System.Text.Json.JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                        // Read totalItems from the nested pagination object
                        int totalItems = 0;
                        if (result.TryGetProperty("pagination", out var paginationProp) &&
                            paginationProp.TryGetProperty("totalItems", out var totalProp))
                        {
                            totalItems = totalProp.GetInt32();
                        }

                        ViewBag.SelectedRole = role;
                        ViewBag.SelectedStatus = status;
                        ViewBag.SearchQuery = search;
                        ViewBag.Total = totalItems;
                        return View(users ?? new List<ViewModels.UserAdminViewModel>());
                    }
                }

                ViewBag.SelectedRole = role;
                ViewBag.SelectedStatus = status;
                ViewBag.SearchQuery = search;
                ViewBag.Total = 0;
                return View(new List<ViewModels.UserAdminViewModel>());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Users error");
                ViewBag.Total = 0;
                return View(new List<ViewModels.UserAdminViewModel>());
            }
        }

        // POST: /Admin/ToggleUserStatus
        [HttpPost]
        public async Task<IActionResult> ToggleUserStatus(int id, string? returnRole, string? returnStatus, string? search)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/users/{id}/toggle-status", new { });
                if (response.IsSuccessStatusCode)
                {
                    TempData["Success"] = "User status toggled successfully.";
                }
                else
                {
                    TempData["Error"] = "Failed to toggle user status.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ToggleUserStatus error");
                TempData["Error"] = "An error occurred while toggling status.";
            }

            return RedirectToAction("Users", new { role = returnRole, status = returnStatus, search = search });
        }

        // GET /Admin/UserActivity/{id}  – proxy to backend API (avoids CORS)
        [HttpGet]
        public async Task<IActionResult> UserActivity(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return Unauthorized();
            try
            {
                var response = await _apiClient.GetAsyncRaw($"/api/admin/users/{id}/activity");
                var content  = await response.Content.ReadAsStringAsync();
                return Content(content, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, ex.Message);
            }
        }


        [HttpPost]
        public async Task<IActionResult> CheckExpiry()
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PostAsyncRaw("/api/subscription/check-expiry", new { });
                if (response.IsSuccessStatusCode)
                {
                    TempData["Success"] = "Subscription expiry check completed successfully.";
                }
                else
                {
                    TempData["Error"] = "Failed to check subscription expiry.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "CheckExpiry error");
                TempData["Error"] = $"An error occurred during expiry check: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // GET: /Admin/Subscriptions
        public async Task<IActionResult> Subscriptions(string status = "All")
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            ViewBag.SelectedStatus = status;

            try
            {
                var response = await _apiClient.GetAsyncRaw("/api/admin/subscription/plans");
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var result = JsonSerializer.Deserialize<JsonElement>(content);

                    if (result.TryGetProperty("data", out var dataProp))
                    {
                        var plans = JsonSerializer.Deserialize<List<Models.SubscriptionPlan>>(dataProp.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true })
                                    ?? new List<Models.SubscriptionPlan>();

                        if (!status.Equals("All", StringComparison.OrdinalIgnoreCase))
                            plans = plans.Where(p => p.Status.Equals(status, StringComparison.OrdinalIgnoreCase)).ToList();

                        return View(plans);
                    }
                }

                TempData["Error"] = "Failed to load subscription plans.";
                return View(new List<Models.SubscriptionPlan>());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Subscriptions error");
                TempData["Error"] = "An error occurred while loading plans.";
                return View(new List<Models.SubscriptionPlan>());
            }
        }

        // GET: /Admin/SubscriptionUsers
        public async Task<IActionResult> SubscriptionUsers(string status = "All")
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            ViewBag.SelectedStatus = status;

            var vm = new ViewModels.SubscriptionUsersPageViewModel();
            var jsonOpts = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };

            try
            {
                // ── 1. Subscriptions ──────────────────────────────────────────
                var subUrl  = $"/api/admin/subscription/user-subscriptions?status={Uri.EscapeDataString(status)}";
                var subResp = await _apiClient.GetAsyncRaw(subUrl);
                if (subResp.IsSuccessStatusCode)
                {
                    var root = JsonSerializer.Deserialize<JsonElement>(await subResp.Content.ReadAsStringAsync());
                    if (root.TryGetProperty("data", out var dataProp))
                        vm.Subscriptions = JsonSerializer.Deserialize<List<ViewModels.UserSubscriptionAdminViewModel>>(dataProp.GetRawText(), jsonOpts)
                                           ?? new();
                }

                // ── 2. All payment records (up to 100 per page, fetch page 1) ─
                var payResp = await _apiClient.GetAsyncRaw("/api/payment/admin/all?pageSize=100&pageNumber=1");
                if (payResp.IsSuccessStatusCode)
                {
                    var root = JsonSerializer.Deserialize<JsonElement>(await payResp.Content.ReadAsStringAsync());
                    if (root.TryGetProperty("data", out var dataProp))
                        vm.Payments = JsonSerializer.Deserialize<List<ViewModels.AdminPaymentViewModel>>(dataProp.GetRawText(), jsonOpts)
                                      ?? new();
                }

                return View(vm);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "SubscriptionUsers error");
                TempData["Error"] = "An error occurred.";
                return View(vm);
            }
        }

        // POST: /Admin/AddPlan
        [HttpPost]
        public async Task<IActionResult> AddPlan(Models.SubscriptionPlan plan)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            if (!ModelState.IsValid)
            {
                TempData["Error"] = "Invalid plan details.";
                return RedirectToAction("Subscriptions");
            }

            try
            {
                var response = await _apiClient.PostAsyncRaw("/api/admin/subscription/add-plan", plan);
                if (response.IsSuccessStatusCode)
                {
                    TempData["Success"] = "Plan added successfully!";
                }
                else
                {
                    TempData["Error"] = "Failed to add plan.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "AddPlan error");
                TempData["Error"] = "An error occurred while adding the plan.";
            }

            return RedirectToAction("Subscriptions");
        }

        // POST: /Admin/TogglePlanStatus
        [HttpPost]
        public async Task<IActionResult> TogglePlanStatus(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/subscription/status/{id}", new { });
                if (response.IsSuccessStatusCode)
                {
                    TempData["Success"] = "Plan status updated successfully!";
                }
                else
                {
                    TempData["Error"] = "Failed to update plan status.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "TogglePlanStatus error");
                TempData["Error"] = "An error occurred while updating status.";
            }

            return RedirectToAction("Subscriptions");
        }

        // POST: /Admin/EditPlan
        [HttpPost]
        public async Task<IActionResult> EditPlan(Models.SubscriptionPlan plan)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            if (!ModelState.IsValid)
            {
                TempData["Error"] = "Invalid plan details.";
                return RedirectToAction("Subscriptions");
            }

            try
            {
                var response = await _apiClient.PutAsyncRaw($"/api/admin/subscription/edit-plan/{plan.PlanId}", plan);
                if (response.IsSuccessStatusCode)
                {
                    TempData["Success"] = "Plan updated successfully!";
                }
                else
                {
                    TempData["Error"] = "Failed to update plan.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "EditPlan error");
                TempData["Error"] = "An error occurred while updating the plan.";
            }

            return RedirectToAction("Subscriptions");
        }

        // GET: /Admin/Payments
        public async Task<IActionResult> Payments()
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                decimal actualTotalRevenue = 0;
                var statsResponse = await _apiClient.GetAsyncRaw("/api/payment/admin/stats");
                if (statsResponse.IsSuccessStatusCode)
                {
                    var statsContent = await statsResponse.Content.ReadAsStringAsync();
                    var statsJson = JsonSerializer.Deserialize<JsonElement>(statsContent);
                    if (statsJson.TryGetProperty("data", out var statsData) && statsData.TryGetProperty("totalRevenue", out var trProp))
                    {
                        actualTotalRevenue = trProp.GetDecimal();
                    }
                }
                ViewBag.TotalRevenue = actualTotalRevenue;

                var response = await _apiClient.GetAsyncRaw("/api/payment/admin/all?pageSize=500");
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var result = JsonSerializer.Deserialize<JsonElement>(content);
                    
                    if (result.TryGetProperty("data", out var dataProp))
                    {
                        var payments = JsonSerializer.Deserialize<List<ViewModels.AdminPaymentViewModel>>(
                            dataProp.GetRawText(), 
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
                        );
                        return View(payments ?? new List<ViewModels.AdminPaymentViewModel>());
                    }
                }
                
                TempData["Error"] = "Failed to load payments.";
                return View(new List<ViewModels.AdminPaymentViewModel>());
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Payments error");
                TempData["Error"] = "An error occurred while loading payments.";
                return View(new List<ViewModels.AdminPaymentViewModel>());
            }
        }

        // GET: /Admin/Logout
        public IActionResult Logout()
        {
            HttpContext.Session.Clear();
            
            // Clear Remember Me cookies
            Response.Cookies.Delete("AdminUser");
            Response.Cookies.Delete("AdminPass");
            
            return RedirectToAction("Login");
        }

        // ──────────────────────────────────────────────────────
        // COMPLAINTS
        // ──────────────────────────────────────────────────────

        // GET: /Admin/Complaints
        public async Task<IActionResult> Complaints(string? status = null)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            ViewBag.CurrentStatus = status ?? "All";

            try
            {
                // Always fetch ALL to get accurate header counts
                var response = await _apiClient.GetAsyncRaw("/api/complaint/admin/all?pageSize=500");
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var root    = JsonSerializer.Deserialize<JsonElement>(content);
                    if (root.TryGetProperty("complaints", out var arr))
                    {
                        var allList = JsonSerializer.Deserialize<List<ViewModels.AdminComplaintViewModel>>(
                            arr.GetRawText(),
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true })
                            ?? new List<ViewModels.AdminComplaintViewModel>();

                        ViewBag.PendingCount  = allList.Count(c => c.Status == "Pending");
                        ViewBag.ResolvedCount = allList.Count(c => c.Status == "Resolved");
                        ViewBag.RejectedCount = allList.Count(c => c.Status == "Rejected");

                        var display = (!string.IsNullOrEmpty(status) && status != "All")
                            ? allList.Where(c => c.Status == status).ToList()
                            : allList;

                        return View(display);
                    }
                }
                TempData["Error"] = "Failed to load complaints.";
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Complaints error");
                TempData["Error"] = "An error occurred while loading complaints.";
            }

            ViewBag.PendingCount  = 0;
            ViewBag.ResolvedCount = 0;
            ViewBag.RejectedCount = 0;
            return View(new List<ViewModels.AdminComplaintViewModel>());
        }

        // POST: /Admin/ResolveComplaint
        [HttpPost]
        public async Task<IActionResult> ResolveComplaint(int id, string? resolutionNote, string? returnStatus)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                await _apiClient.PutAsyncRaw($"/api/complaint/{id}/resolve",
                    new { resolutionNote });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ResolveComplaint error");
            }

            return RedirectToAction("Complaints", new { status = returnStatus });
        }

        // POST: /Admin/RejectComplaint
        [HttpPost]
        public async Task<IActionResult> RejectComplaint(int id, string? resolutionNote, string? returnStatus)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                await _apiClient.PutAsyncRaw($"/api/complaint/{id}/reject",
                    new { resolutionNote });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "RejectComplaint error");
            }

            return RedirectToAction("Complaints", new { status = returnStatus });
        }

        // ──────────────────────────────────────────────────────
        // REVIEWS
        // ──────────────────────────────────────────────────────

        // GET: /Admin/Reviews
        public async Task<IActionResult> Reviews(string? type = null)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            ViewBag.CurrentType = type ?? "All";

            try
            {
                // Always fetch ALL to get accurate header counts
                var response = await _apiClient.GetAsyncRaw("/api/review/admin/all?pageSize=500");
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var root    = JsonSerializer.Deserialize<JsonElement>(content);
                    if (root.TryGetProperty("reviews", out var arr))
                    {
                        var allList = JsonSerializer.Deserialize<List<ViewModels.AdminReviewViewModel>>(
                            arr.GetRawText(),
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true })
                            ?? new List<ViewModels.AdminReviewViewModel>();

                        ViewBag.TotalCount     = allList.Count;
                        ViewBag.EquipmentCount = allList.Count(r => r.TargetType == "Equipment");
                        ViewBag.ProductCount   = allList.Count(r => r.TargetType == "Product");

                        var display = (!string.IsNullOrEmpty(type) && type != "All")
                            ? allList.Where(r => r.TargetType == type).ToList()
                            : allList;

                        return View(display);
                    }
                }
                TempData["Error"] = "Failed to load reviews.";
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Reviews error");
                TempData["Error"] = "An error occurred while loading reviews.";
            }

            ViewBag.TotalCount     = 0;
            ViewBag.EquipmentCount = 0;
            ViewBag.ProductCount   = 0;
            return View(new List<ViewModels.AdminReviewViewModel>());
        }

        // POST: /Admin/DeleteReview
        [HttpPost]
        public async Task<IActionResult> DeleteReview(int id, string? returnType)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            try
            {
                await _apiClient.DeleteAsync($"/api/review/{id}");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "DeleteReview error");
            }

            return RedirectToAction("Reviews", new { type = returnType });
        }
        // ──────────────────────────────────────────────────────
        // REPORTS
        // ──────────────────────────────────────────────────────

        // GET: /Admin/Reports
        public IActionResult Reports()
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login");

            return View();
        }

        // GET: /Admin/ReportsData
        [HttpGet]
        public async Task<IActionResult> ReportsData(string? startDate, string? endDate)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return Unauthorized();

            try
            {
                var qs = "";
                if (!string.IsNullOrEmpty(startDate) && !string.IsNullOrEmpty(endDate))
                {
                    qs = $"?startDate={startDate}&endDate={endDate}";
                }
                var response = await _apiClient.GetAsyncRaw($"/api/admin/reports{qs}");
                var content = await response.Content.ReadAsStringAsync();
                return Content(content, "application/json");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ReportsData error");
                return StatusCode(500, new { success = false, message = ex.Message });
            }
        }
    }
}
