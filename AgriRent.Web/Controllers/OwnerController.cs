using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using AgriRent.ViewModels;
using System.Text.Json;
using AgriRent.DTOs;

namespace AgriRent.Web.Controllers
{
    public class OwnerController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ICacheService _cacheService;
        private readonly ILogger<OwnerController> _logger;

        public OwnerController(ApiClient apiClient, ICacheService cacheService, ILogger<OwnerController> logger)
        {
            _apiClient = apiClient;
            _cacheService = cacheService;
            _logger = logger;
        }

        // ---------------------------------------------------------
        // Role guard: must be logged in + have Owner/EquipmentOwner role
        // The API assigns "Owner" role upon subscription purchase
        // ---------------------------------------------------------
        private bool IsOwner()
        {
            var userId = HttpContext.Session.GetString("UserId");
            if (string.IsNullOrEmpty(userId)) return false;
            var roles = HttpContext.Session.GetString("UserRoles") ?? "";
            return roles.Contains("Owner") || roles.Contains("EquipmentOwner");
        }

        // ─── Helpers ───────────────────────────────────────────────
        private static string SafeStr(JsonElement el, string key)
        {
            return el.TryGetProperty(key, out var v) ? v.GetString() ?? "" : "";
        }
        private static int SafeInt(JsonElement el, string key)
        {
            return el.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.Number
                ? v.GetInt32() : 0;
        }
        private static decimal SafeDec(JsonElement el, string key)
        {
            return el.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.Number
                ? v.GetDecimal() : 0;
        }
        private static DateTime SafeDate(JsonElement el, string key)
        {
            if (!el.TryGetProperty(key, out var v)) return DateTime.UtcNow;
            if (v.ValueKind == JsonValueKind.String)
            {
                var s = v.GetString() ?? "";
                return DateTime.TryParse(s, out var dt) ? dt : DateTime.UtcNow;
            }
            return DateTime.UtcNow;
        }

        // Loads equipment categories with 30-minute cache (Issue #3 - eliminates 800-1200ms)
        private async Task<List<object>> LoadCategoriesAsync()
        {
            var cacheKey = "equipment_categories";
            var cachedCategories = _cacheService.Get<List<object>>(cacheKey);
            if (cachedCategories != null) return cachedCategories;

            var categories = new List<object>();
            try
            {
                var resp = await _apiClient.GetAsyncRaw("/api/public/categories?type=Equipment");
                if (resp.IsSuccessStatusCode)
                {
                    var json = await resp.Content.ReadAsStringAsync();
                    var dtos = JsonSerializer.Deserialize<List<CategoryDto>>(json,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    if (dtos != null)
                        foreach (var cat in dtos)
                        {
                            var subs = cat.SubCategories
                                .Where(s => s.Status == "Active")
                                .Select(s => (object)new { id = s.SubCategoryId, name = s.SubCategoryName })
                                .ToList();
                            categories.Add(new { id = cat.CategoryId, name = cat.Name, subCategories = subs });
                        }
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }

            _cacheService.Set(cacheKey, categories, TimeSpan.FromMinutes(30));
            return categories;
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /EquipmentOwnerDashboard/Dashboard
        // ──────────────────────────────────────────────────────────────────────
        [ResponseCache(Duration = 60, VaryByQueryKeys = new[] { "*" })]
        public async Task<IActionResult> Dashboard()
        {
            var userId = HttpContext.Session.GetString("UserId");
            if (string.IsNullOrEmpty(userId))
                return RedirectToAction("Login", "Account");

            if (!IsOwner())
            {
                TempData["Error"] = "Access denied. Equipment Owner role required.";
                return RedirectToAction("Index", "Home");
            }

            var vm = new OwnerDashboardViewModel
            {
                OwnerName   = HttpContext.Session.GetString("DisplayName") ?? "Owner",
                OwnerMobile = HttpContext.Session.GetString("UserMobile") ?? ""
            };

            // FIX: Parallelize auth check with data fetching for better performance
            var authRespTask = _apiClient.GetAsyncRaw("/api/owner/dashboard");
            // FIX: Add pagination to prevent loading 100+ equipment items
            var equipmentTask = _apiClient.GetAsyncRaw("/api/equipment/my-list?pageSize=20&page=1");
            // FIX: Add pagination to prevent loading 100+ booking requests
            var bookingsTask = _apiClient.GetAsyncRaw("/api/booking/owner-requests?pageSize=20&page=1");
            var subscriptionTask = _apiClient.GetAsyncRaw("/api/subscription/my-subscription");
            
            var myComplaintsTask = _apiClient.GetAsyncRaw("/api/complaint/my");
            var incomingComplaintsTask = _apiClient.GetAsyncRaw("/api/complaint/incoming");
            var myReviewsTask = _apiClient.GetAsyncRaw("/api/review/my");
            var incomingReviewsTask = _apiClient.GetAsyncRaw("/api/review/incoming");

            try
            {
                await Task.WhenAll(authRespTask, equipmentTask, bookingsTask, subscriptionTask, myComplaintsTask, incomingComplaintsTask, myReviewsTask, incomingReviewsTask);
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }

            var authResp = await TryGetResponseAsync(authRespTask);
            if (authResp == null)
            {
                TempData["Error"] = "Unable to load dashboard right now. Please try again.";
                return View(vm);
            }

            // Check auth
            if (!authResp.IsSuccessStatusCode)
            {
                var authBody = await authResp.Content.ReadAsStringAsync();
                TempData["Error"] = $"Session token issue ({(int)authResp.StatusCode}). Please log out and log in again.";
                return RedirectToAction("Login", "Account");
            }

            var equipmentResp = await TryGetResponseAsync(equipmentTask);
            var bookingsResp = await TryGetResponseAsync(bookingsTask);
            var subscriptionResp = await TryGetResponseAsync(subscriptionTask);
            var myComplaintsResp = await TryGetResponseAsync(myComplaintsTask);
            var incomingComplaintsResp = await TryGetResponseAsync(incomingComplaintsTask);
            var myReviewsResp = await TryGetResponseAsync(myReviewsTask);
            var incomingReviewsResp = await TryGetResponseAsync(incomingReviewsTask);

            // Process all responses
            await ProcessEquipmentAsync(equipmentResp, vm);
            await ProcessBookingsAsync(bookingsResp, vm);
            await ProcessSubscriptionAsync(subscriptionResp, vm);
            await ProcessComplaintsAsync(myComplaintsResp, incomingComplaintsResp, vm);
            await ProcessReviewsAsync(myReviewsResp, incomingReviewsResp, vm);

            return View(vm);
        }

        private async Task<HttpResponseMessage?> TryGetResponseAsync(Task<HttpResponseMessage> task)
        {
            try
            {
                return await task;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Dashboard API request failed.");
                return null;
            }
        }

        private async Task ProcessEquipmentAsync(HttpResponseMessage? resp, OwnerDashboardViewModel vm)
        {
            try
            {
                if (resp == null)
                {
                    return;
                }

                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode && body.TrimStart().StartsWith("["))
                {
                    var items = JsonSerializer.Deserialize<List<JsonElement>>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    foreach (var e in items ?? new())
                    {
                        var dailyPrice = SafeDec(e, "dailyPrice");
                        var hourlyPrice = SafeDec(e, "hourlyPrice");

                        var item = new OwnerEquipmentItem
                        {
                            EquipmentId   = SafeInt(e, "equipmentId"),
                            EquipmentName = SafeStr(e, "equipmentName"),
                            Description   = SafeStr(e, "description"),
                            Location      = SafeStr(e, "location"),
                            Price         = dailyPrice > 0 ? dailyPrice : hourlyPrice,
                            PriceType     = dailyPrice > 0 ? "Day" : "Hour",
                            Status        = SafeStr(e, "status"),
                            CategoryName  = e.TryGetProperty("categoryName", out var cn) ? cn.GetString() : null,
                            CreatedAt     = SafeDate(e, "createdAt"),
                        };

                        // First image URL
                        if (e.TryGetProperty("images", out var imgs) && imgs.ValueKind == JsonValueKind.Array)
                        {
                            var firstImage = imgs.EnumerateArray().FirstOrDefault();
                            if (firstImage.ValueKind == JsonValueKind.Object && firstImage.TryGetProperty("imageUrl", out var u))
                            {
                                item.ImageUrl = u.GetString();
                            }
                        }
                        vm.Equipments.Add(item);
                    }

                    vm.TotalEquipments = vm.Equipments.Count;
                    vm.ApprovedCount   = vm.Equipments.Count(x => x.Status == "Active");
                    vm.PendingCount    = vm.Equipments.Count(x => x.Status == "Pending");
                    vm.RejectedCount   = vm.Equipments.Count(x => x.Status == "Rejected");
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        private async Task ProcessBookingsAsync(HttpResponseMessage? resp, OwnerDashboardViewModel vm)
        {
            try
            {
                if (resp == null)
                {
                    return;
                }

                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode && body.TrimStart().StartsWith("["))
                {
                    var items = JsonSerializer.Deserialize<List<JsonElement>>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    foreach (var b in items ?? new())
                    {
                        vm.Bookings.Add(new OwnerBookingItem
                        {
                            BookingId     = SafeInt(b, "bookingId"),
                            EquipmentName = SafeStr(b, "equipmentName"),
                            FarmerName    = SafeStr(b, "farmerName"),
                            TotalPrice    = SafeDec(b, "totalPrice"),
                            Status        = SafeStr(b, "status"),
                            StartDate     = SafeDate(b, "startDate"),
                            EndDate       = SafeDate(b, "endDate"),
                        });
                    }

                    vm.TotalBookings    = vm.Bookings.Count;
                    vm.PendingBookings  = vm.Bookings.Count(x => x.Status == "Pending");
                    vm.AcceptedBookings = vm.Bookings.Count(x => x.Status == "Accepted");
                    vm.RejectedBookings = vm.Bookings.Count(x => x.Status == "Rejected");
                    vm.TotalEarnings    = vm.Bookings.Where(x => x.Status == "Accepted").Sum(x => x.TotalPrice);
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        private async Task ProcessSubscriptionAsync(HttpResponseMessage? resp, OwnerDashboardViewModel vm)
        {
            try
            {
                if (resp == null)
                {
                    return;
                }

                if (resp.IsSuccessStatusCode)
                {
                    var body = await resp.Content.ReadAsStringAsync();
                    var root = JsonSerializer.Deserialize<JsonElement>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                    var dataEl = root.TryGetProperty("data", out var d) ? d : root;

                    if (dataEl.ValueKind == JsonValueKind.Array)
                    {
                        var subs = dataEl.EnumerateArray().FirstOrDefault();
                        if (subs.ValueKind == JsonValueKind.Object)
                        {
                            vm.SubscriptionStatus   = SafeStr(subs, "status");
                            vm.SubscriptionPlanName = SafeStr(subs, "planName");
                            vm.SubscriptionExpiry   = SafeDate(subs, "endDate");
                            if (vm.SubscriptionStatus == "") vm.SubscriptionStatus = "None";
                        }
                    }
                    else if (dataEl.ValueKind == JsonValueKind.Object)
                    {
                        vm.SubscriptionStatus   = SafeStr(dataEl, "status");
                        vm.SubscriptionPlanName = SafeStr(dataEl, "planName");
                        vm.SubscriptionExpiry   = SafeDate(dataEl, "endDate");
                        if (vm.SubscriptionStatus == "") vm.SubscriptionStatus = "None";
                    }
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/AcceptBooking
        // Endpoint: POST /api/booking/accept/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> AcceptBooking(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");
            try
            {
                var res = await _apiClient.PostAsyncRaw($"/api/booking/accept/{id}", new { });
                var body = await res.Content.ReadAsStringAsync();
                _logger.LogInformation("[EquipmentOwnerDashboard] AcceptBooking {BookingId}: {StatusCode} — {Body}", id, res.StatusCode, body);
                TempData[res.IsSuccessStatusCode ? "Success" : "Error"] =
                    res.IsSuccessStatusCode ? "✅ Booking accepted!" : $"Failed to accept booking: {body}";
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }
            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/RejectBooking
        // Endpoint: POST /api/booking/reject/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> RejectBooking(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");
            try
            {
                var res = await _apiClient.PostAsyncRaw($"/api/booking/reject/{id}", new { });
                var body = await res.Content.ReadAsStringAsync();
                _logger.LogInformation("[EquipmentOwnerDashboard] RejectBooking {BookingId}: {StatusCode} — {Body}", id, res.StatusCode, body);
                TempData[res.IsSuccessStatusCode ? "Success" : "Error"] =
                    res.IsSuccessStatusCode ? "Booking rejected." : $"Failed to reject booking: {body}";
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }
            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /EquipmentOwnerDashboard/Debug
        // Shows raw status + body from all 3 API endpoints — for diagnosing
        // exactly why equipment / subscription data is not showing.
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> Debug()
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            var sb = new System.Text.StringBuilder();
            sb.AppendLine("=== EQUIPMENT OWNER DASHBOARD — API DEBUG ===");
            sb.AppendLine($"Session UserId   : {HttpContext.Session.GetString("UserId")}");
            sb.AppendLine($"Session UserRoles: {HttpContext.Session.GetString("UserRoles")}");
            sb.AppendLine($"Session JwtToken : {(string.IsNullOrEmpty(HttpContext.Session.GetString("JwtToken")) ? "<EMPTY>" : "<SET, length=" + HttpContext.Session.GetString("JwtToken")!.Length + ">")}" );
            sb.AppendLine();

            var endpoints = new[]
            {
                "/api/owner/dashboard",
                "/api/equipment/my-list?pageSize=20&page=1",
                "/api/booking/owner-requests?pageSize=20&page=1",
                "/api/subscription/my-subscription",
                "/api/profile/me"
            };

            foreach (var ep in endpoints)
            {
                sb.AppendLine($"--- GET {ep} ---");
                try
                {
                    var resp = await _apiClient.GetAsyncRaw(ep);
                    var body = await resp.Content.ReadAsStringAsync();
                    sb.AppendLine($"Status : {(int)resp.StatusCode} {resp.StatusCode}");
                    sb.AppendLine($"Body   : {(body.Length > 500 ? body.Substring(0, 500) + "..." : body)}");
                }
                catch (Exception ex)
                {
                    sb.AppendLine($"Exception: {ex.Message}");
                }
                sb.AppendLine();
            }

            return Content(sb.ToString(), "text/plain");
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /EquipmentOwnerDashboard/UploadImage?equipmentId=X
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public IActionResult UploadImage(int equipmentId)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");
            ViewBag.EquipmentId = equipmentId;
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/UploadImage
        // Sends multipart/form-data to POST /api/equipment/upload-image
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> UploadImage(int equipmentId, IFormFile? image)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            if (image == null || image.Length == 0)
            {
                TempData["Error"] = "Please select an image file.";
                return RedirectToAction("UploadImage", new { equipmentId });
            }

            try
            {
                using var multipart = new MultipartFormDataContent();
                multipart.Add(new StringContent(equipmentId.ToString()), "equipmentId");

                var fileContent = new StreamContent(image.OpenReadStream());
                fileContent.Headers.ContentType =
                    new System.Net.Http.Headers.MediaTypeHeaderValue(image.ContentType);
                multipart.Add(fileContent, "image", image.FileName);

                var resp = await _apiClient.PostMultipartAsyncRaw("/api/equipment/upload-image", multipart);
                var body = await resp.Content.ReadAsStringAsync();
                _logger.LogInformation("[UploadImage] {StatusCode} — {Body}", resp.StatusCode, body);

                if (resp.IsSuccessStatusCode)
                {
                    TempData["Success"] = "✅ Image uploaded successfully!";
                    return RedirectToAction("Dashboard");
                }

                TempData["Error"] = $"Upload failed ({(int)resp.StatusCode}): {body}";
                return RedirectToAction("UploadImage", new { equipmentId });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[UploadImage] Exception");
                TempData["Error"] = "An error occurred during upload.";
                return RedirectToAction("UploadImage", new { equipmentId });
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /EquipmentOwnerDashboard/AddEquipment
        // Loads category/subcategory list for the form dropdown
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> AddEquipment()
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            var categories = await LoadCategoriesAsync();
            ViewBag.Categories = categories;
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/AddEquipment
        // Endpoint: POST /api/equipment/add  (Role: Owner)
        // Fields:   equipmentName, description, priceType(Hourly|Daily),
        //           price, location, subCategoryId
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> AddEquipment(
            string equipmentName, string description,
            decimal hourlyPrice, decimal dailyPrice,
            string location, int? subCategoryId,
            IFormFile[]? images)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            // ── Server-side: image is required ───────────────────────────────
            var validImages = images?.Where(f => f != null && f.Length > 0).ToArray() ?? Array.Empty<IFormFile>();
            if (validImages.Length == 0)
            {
                TempData["Error"] = "⚠ Please select at least one equipment photo before submitting.";
                return RedirectToAction("AddEquipment");
            }

            int newEquipmentId = 0;
            try
            {
                // ── Step 1: Create equipment (JSON) ──────────────────────────
                var payload = new
                {
                    equipmentName,
                    description,
                    hourlyPrice,
                    dailyPrice,
                    location,
                    subCategoryId = subCategoryId ?? 1
                };

                var res  = await _apiClient.PostAsyncRaw("/api/equipment/add", payload);
                var body = await res.Content.ReadAsStringAsync();
                _logger.LogInformation("[EquipmentOwnerDashboard] AddEquipment: {StatusCode} — {Body}", res.StatusCode, body);

                if (!res.IsSuccessStatusCode)
                {
                    string errMsg = body;
                    try
                    {
                        var errRoot = JsonSerializer.Deserialize<JsonElement>(body,
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (errRoot.TryGetProperty("message", out var m)) errMsg = m.GetString() ?? body;
                    }
                    catch { }

                    TempData["Error"] = $"Failed to add equipment: {errMsg}";
                    return RedirectToAction("AddEquipment");
                }

                // Parse equipmentId from { message, equipmentId }
                try
                {
                    var addResult = JsonSerializer.Deserialize<JsonElement>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    newEquipmentId = addResult.TryGetProperty("equipmentId", out var eid) ? eid.GetInt32() : 0;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "[EquipmentOwnerDashboard] Could not parse equipmentId");
                }

                // ── Step 2: Upload images — mandatory ────────────────────────
                // If upload fails, redirect to UploadImage page (NOT dashboard).
                // Equipment stays in DB but user must upload the photo before it makes sense.
                int uploadedCount = 0;
                string? uploadError = null;

                foreach (var img in validImages)
                {
                    try
                    {
                        using var multipart = new MultipartFormDataContent();
                        multipart.Add(new StringContent(newEquipmentId.ToString()), "equipmentId");

                        var fileContent = new StreamContent(img.OpenReadStream());
                        fileContent.Headers.ContentType =
                            new System.Net.Http.Headers.MediaTypeHeaderValue(img.ContentType);
                        multipart.Add(fileContent, "image", img.FileName);

                        var imgResp = await _apiClient.PostMultipartAsyncRaw("/api/equipment/upload-image", multipart);
                        var imgBody = await imgResp.Content.ReadAsStringAsync();
                        _logger.LogInformation("[EquipmentOwnerDashboard] UploadImage '{FileName}': {StatusCode} — {Body}", img.FileName, imgResp.StatusCode, imgBody);

                        if (imgResp.IsSuccessStatusCode)
                        {
                            uploadedCount++;
                        }
                        else
                        {
                            uploadError = $"Image upload failed ({(int)imgResp.StatusCode}): {imgBody}";
                            _logger.LogError("[EquipmentOwnerDashboard] Upload error: {UploadError}", uploadError);
                        }
                    }
                    catch (Exception ex)
                    {
                        uploadError = $"Image upload exception: {ex.Message}";
                        _logger.LogError(ex, "[EquipmentOwnerDashboard] UploadImage exception");
                    }
                }

                // ── Decide outcome ────────────────────────────────────────────
                if (uploadedCount == 0 && uploadError != null)
                {
                    // Equipment was created but NO image uploaded — send user to UploadImage page
                    TempData["Error"] = $"⚠ Equipment was listed but photo upload failed. Please upload your photo now to complete the listing. ({uploadError})";
                    return RedirectToAction("UploadImage", new { equipmentId = newEquipmentId });
                }

                // At least one image uploaded — consider this a success
                TempData["Success"] = uploadedCount > 1
                    ? $"✅ Equipment listed with {uploadedCount} photos! Pending admin approval."
                    : "✅ Equipment listed with photo! Pending admin approval.";
                return RedirectToAction("Dashboard");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[EquipmentOwnerDashboard] AddEquipment exception");
                TempData["Error"] = "An error occurred while adding equipment.";
                return RedirectToAction("AddEquipment");
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /EquipmentOwnerDashboard/UpdateEquipment/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> UpdateEquipment(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            var token = HttpContext.Session.GetString("JwtToken") ?? "";

            // Load equipment
            // FIX: Add pagination to prevent loading 100+ items
            var resp = await _apiClient.GetAsyncRaw($"/api/equipment/my-list?pageSize=20&page=1");
            if (!resp.IsSuccessStatusCode)
            {
                TempData["Error"] = "Could not load equipment details.";
                return RedirectToAction("Dashboard");
            }

            var json = await resp.Content.ReadAsStringAsync();
            var arr  = JsonSerializer.Deserialize<JsonElement>(json,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            JsonElement? found = null;
            if (arr.ValueKind == JsonValueKind.Array)
            {
                foreach (var item in arr.EnumerateArray())
                {
                    if (SafeInt(item, "equipmentId") == id) { found = item; break; }
                }
            }

            if (found == null)
            {
                TempData["Error"] = "Equipment not found.";
                return RedirectToAction("Dashboard");
            }

            // Load categories from cache (Issue #3 - eliminates 800-1200ms)
            var categories = await LoadCategoriesAsync();

            ViewBag.Equipment  = found.Value;
            ViewBag.Categories = categories;
            ViewBag.EquipmentId = id;
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/UpdateEquipment/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> UpdateEquipment(
            int id,
            string equipmentName, string description,
            decimal hourlyPrice, decimal dailyPrice,
            string location, int? subCategoryId)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            var payload = new { equipmentName, description, hourlyPrice, dailyPrice, location, subCategoryId = subCategoryId ?? 1 };

            try
            {
                var resp = await _apiClient.PutAsyncRaw($"/api/equipment/update/{id}", payload);
                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode)
                {
                    TempData["Success"] = "✅ Equipment updated successfully!";
                    return RedirectToAction("Dashboard");
                }
                else
                {
                    string msg = body;
                    try
                    {
                        var r = JsonSerializer.Deserialize<JsonElement>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (r.TryGetProperty("message", out var m)) msg = m.GetString() ?? body;
                    }
                    catch { }
                    TempData["Error"] = $"Update failed: {msg}";
                    return RedirectToAction("UpdateEquipment", new { id });
                }
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
                return RedirectToAction("UpdateEquipment", new { id });
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/ToggleEquipmentStatus/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> ToggleEquipmentStatus(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            try
            {
                var resp = await _apiClient.PutAsyncRaw($"/api/equipment/toggle-status/{id}", new { });
                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode)
                    TempData["Success"] = "Equipment status toggled successfully.";
                else
                {
                    string msg = body;
                    try
                    {
                        var r = JsonSerializer.Deserialize<JsonElement>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (r.TryGetProperty("message", out var m)) msg = m.GetString() ?? body;
                    }
                    catch { }
                    TempData["Error"] = $"Failed to toggle status: {msg}";
                }
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /EquipmentOwnerDashboard/DeleteEquipment/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteEquipment(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            try
            {
                var resp = await _apiClient.DeleteAsyncRaw($"/api/equipment/delete/{id}");
                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode)
                    TempData["Success"] = "🗑 Equipment deleted successfully.";
                else
                {
                    string msg = body;
                    try
                    {
                        var r = JsonSerializer.Deserialize<JsonElement>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (r.TryGetProperty("message", out var m)) msg = m.GetString() ?? body;
                    }
                    catch { }
                    TempData["Error"] = $"Delete failed: {msg}";
                }
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // Process Complaints
        // ──────────────────────────────────────────────────────────────────────
        private async Task ProcessComplaintsAsync(HttpResponseMessage? myResp, HttpResponseMessage? incomingResp, OwnerDashboardViewModel vm)
        {
            if (myResp != null && myResp.IsSuccessStatusCode)
            {
                var body = await myResp.Content.ReadAsStringAsync();
                try
                {
                    vm.MyComplaints = JsonSerializer.Deserialize<List<OwnerComplaintItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<OwnerComplaintItem>();
                }
                catch { }
            }
            if (incomingResp != null && incomingResp.IsSuccessStatusCode)
            {
                var body = await incomingResp.Content.ReadAsStringAsync();
                try
                {
                    vm.IncomingComplaints = JsonSerializer.Deserialize<List<OwnerComplaintItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<OwnerComplaintItem>();
                }
                catch { }
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // Process Reviews
        // ──────────────────────────────────────────────────────────────────────
        private async Task ProcessReviewsAsync(HttpResponseMessage? myResp, HttpResponseMessage? incomingResp, OwnerDashboardViewModel vm)
        {
            if (myResp != null && myResp.IsSuccessStatusCode)
            {
                var body = await myResp.Content.ReadAsStringAsync();
                try
                {
                    vm.MyReviews = JsonSerializer.Deserialize<List<OwnerReviewItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<OwnerReviewItem>();
                }
                catch { }
            }
            if (incomingResp != null && incomingResp.IsSuccessStatusCode)
            {
                var body = await incomingResp.Content.ReadAsStringAsync();
                try
                {
                    var doc = JsonDocument.Parse(body);
                    if (doc.RootElement.TryGetProperty("reviews", out var revs))
                    {
                        vm.IncomingReviews = JsonSerializer.Deserialize<List<OwnerReviewItem>>(revs.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<OwnerReviewItem>();
                    }
                    if (doc.RootElement.TryGetProperty("averageRating", out var avg))
                    {
                        vm.AverageRating = avg.GetDecimal();
                    }
                }
                catch { }
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Owner/ResolveComplaint/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> ResolveComplaint(int id, string resolutionNote)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            try
            {
                var payload = new { resolutionNote };
                var resp = await _apiClient.PutAsyncRaw($"/api/complaint/{id}/resolve", payload);
                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode)
                    TempData["Success"] = "Complaint resolved successfully.";
                else
                {
                    string msg = body;
                    try
                    {
                        var r = JsonSerializer.Deserialize<JsonElement>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (r.TryGetProperty("message", out var m)) msg = m.GetString() ?? body;
                    }
                    catch { }
                    TempData["Error"] = $"Failed to resolve complaint: {msg}";
                }
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Owner/DeleteReview/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteReview(int id)
        {
            if (!IsOwner()) return RedirectToAction("Login", "Account");

            try
            {
                var resp = await _apiClient.DeleteAsyncRaw($"/api/review/{id}");
                var body = await resp.Content.ReadAsStringAsync();

                if (resp.IsSuccessStatusCode)
                    TempData["Success"] = "Review deleted successfully.";
                else
                {
                    string msg = body;
                    try
                    {
                        var r = JsonSerializer.Deserialize<JsonElement>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                        if (r.TryGetProperty("message", out var m)) msg = m.GetString() ?? body;
                    }
                    catch { }
                    TempData["Error"] = $"Failed to delete review: {msg}";
                }
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

    }
}
