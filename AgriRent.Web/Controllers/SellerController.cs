using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using AgriRent.ViewModels;
using System.Text.Json;
using AgriRent.DTOs;

namespace AgriRent.Web.Controllers
{
    public class SellerController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ICacheService _cacheService;
        private readonly ILogger<SellerController> _logger;

        public SellerController(ApiClient apiClient, ICacheService cacheService, ILogger<SellerController> logger)
        {
            _apiClient = apiClient;
            _cacheService = cacheService;
            _logger = logger;
        }

        private bool IsSeller()
        {
            var userId = HttpContext.Session.GetString("UserId");
            if (string.IsNullOrEmpty(userId)) return false;
            var roles = HttpContext.Session.GetString("UserRoles") ?? "";
            return roles.Contains("Seller");
        }

        // ─── Helpers ───────────────────────────────────────────────
        private static string SafeStr(JsonElement el, string key)
            => el.TryGetProperty(key, out var v) ? v.GetString() ?? "" : "";
        private static int SafeInt(JsonElement el, string key)
            => el.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.Number ? v.GetInt32() : 0;
        private static decimal SafeDec(JsonElement el, string key)
            => el.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.Number ? v.GetDecimal() : 0;
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

        // ──────────────────────────────────────────────────────────────────────
        // GET: /Seller/Dashboard
        // ──────────────────────────────────────────────────────────────────────
        [ResponseCache(Duration = 60, VaryByQueryKeys = new[] { "*" })]
        public async Task<IActionResult> Dashboard()
        {
            var userId = HttpContext.Session.GetString("UserId");
            if (string.IsNullOrEmpty(userId))
                return RedirectToAction("Login", "Account");

            if (!IsSeller())
            {
                TempData["Error"] = "Access denied. Seller role required.";
                return RedirectToAction("Index", "Home");
            }

            var vm = new SellerDashboardViewModel
            {
                SellerName   = HttpContext.Session.GetString("DisplayName") ?? "Seller",
                SellerMobile = HttpContext.Session.GetString("UserMobile") ?? ""
            };

            // Parallelize all API calls (Issue #1 fix - was sequential, now parallel)
            var statsTask = _apiClient.GetAsyncRaw("/api/seller/dashboard");
            var productsTask = _apiClient.GetAsyncRaw("/api/seller/my-products");
            var ordersTask = _apiClient.GetAsyncRaw("/api/seller/orders");
            var subscriptionTask = _apiClient.GetAsyncRaw("/api/subscription/my-subscription");
            var myComplaintsTask = _apiClient.GetAsyncRaw("/api/complaint/my");
            var incomingComplaintsTask = _apiClient.GetAsyncRaw("/api/complaint/incoming");
            var myReviewsTask = _apiClient.GetAsyncRaw("/api/review/my");
            var incomingReviewsTask = _apiClient.GetAsyncRaw("/api/review/incoming");

            try
            {
                await Task.WhenAll(statsTask, productsTask, ordersTask, subscriptionTask, myComplaintsTask, incomingComplaintsTask, myReviewsTask, incomingReviewsTask);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "One or more seller dashboard API calls failed; rendering partial data.");
            }

            var statsResp = await TryGetResponseAsync(statsTask);
            var productsResp = await TryGetResponseAsync(productsTask);
            var ordersResp = await TryGetResponseAsync(ordersTask);
            var subscriptionResp = await TryGetResponseAsync(subscriptionTask);
            var myComplaintsResp = await TryGetResponseAsync(myComplaintsTask);
            var incomingComplaintsResp = await TryGetResponseAsync(incomingComplaintsTask);
            var myReviewsResp = await TryGetResponseAsync(myReviewsTask);
            var incomingReviewsResp = await TryGetResponseAsync(incomingReviewsTask);

            // Process Stats - FIX: Use await instead of .Result
            await ProcessDashboardStatsAsync(statsResp, vm);

            // Process Products - FIX: Use await instead of .Result
            await ProcessProductsAsync(productsResp, vm);

            // Process Orders - FIX: Use await instead of .Result
            await ProcessOrdersAsync(ordersResp, vm);

            // Process Subscription - FIX: Use await instead of .Result
            await ProcessSubscriptionAsync(subscriptionResp, vm);

            // Process Complaints
            await ProcessComplaintsAsync(myComplaintsResp, incomingComplaintsResp, vm);

            // Process Reviews
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

        private async Task ProcessDashboardStatsAsync(HttpResponseMessage? resp, SellerDashboardViewModel vm)
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
                    var data = JsonSerializer.Deserialize<JsonElement>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    vm.TotalProducts  = SafeInt(data, "totalProducts");
                    vm.ActiveProducts = SafeInt(data, "activeProducts");
                    vm.TotalOrders    = SafeInt(data, "totalOrders");
                    vm.PendingOrders  = SafeInt(data, "pendingOrders");
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        private async Task ProcessProductsAsync(HttpResponseMessage? resp, SellerDashboardViewModel vm)
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

                    foreach (var p in items ?? new())
                    {
                        var item = new SellerProductItem
                        {
                            ProductId    = SafeInt(p, "productId"),
                            ProductName  = SafeStr(p, "productName"),
                            Description  = SafeStr(p, "description"),
                            CategoryName = SafeStr(p, "category"),
                            CategoryId   = p.TryGetProperty("categoryId", out var cid) && cid.ValueKind == JsonValueKind.Number ? cid.GetInt32() : (int?)null,
                            SubCategoryId = p.TryGetProperty("subCategoryId", out var scid) && scid.ValueKind == JsonValueKind.Number ? scid.GetInt32() : (int?)null,
                            Price        = SafeDec(p, "price"),
                            Stock        = SafeInt(p, "stock"),
                            Unit         = SafeStr(p, "unit"),
                            Location     = SafeStr(p, "location"),
                            Status       = SafeStr(p, "status"),
                            CreatedAt    = SafeDate(p, "createdAt"),
                        };

                        var iu = SafeStr(p, "imageUrl");
                        if (!string.IsNullOrEmpty(iu) && !iu.Contains("via.placeholder.com"))
                            item.ImageUrl = iu;

                        vm.Products.Add(item);
                    }

                    vm.TotalProducts  = vm.Products.Count;
                    vm.ActiveProducts = vm.Products.Count(x => x.Status == "Active");
                    vm.PendingProducts = vm.Products.Count(x => x.Status == "Pending");
                    vm.RejectedProducts = vm.Products.Count(x => x.Status == "Rejected");
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        private async Task ProcessOrdersAsync(HttpResponseMessage? resp, SellerDashboardViewModel vm)
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

                    foreach (var o in items ?? new())
                    {
                        var imgUrl = SafeStr(o, "imageUrl");
                        var productImages = new List<string>();
                        if (o.TryGetProperty("productImages", out var piProp) && piProp.ValueKind == JsonValueKind.Array)
                        {
                            foreach (var pi in piProp.EnumerateArray())
                            {
                                var u = pi.GetString();
                                if (!string.IsNullOrEmpty(u) && !u.Contains("via.placeholder.com"))
                                    productImages.Add(u);
                            }
                        }

                        vm.Orders.Add(new SellerOrderItem
                        {
                            OrderId             = SafeInt(o, "orderId"),
                            ProductId           = o.TryGetProperty("productId", out var pidEl) && pidEl.ValueKind == JsonValueKind.Number ? pidEl.GetInt32() : null,
                            ProductName         = SafeStr(o, "productName"),
                            ProductDescription  = SafeStr(o, "productDescription"),
                            ProductUnit         = SafeStr(o, "productUnit"),
                            ProductLocation     = SafeStr(o, "productLocation"),
                            ProductPrice        = SafeDec(o, "productPrice"),
                            ProductImageUrl     = !string.IsNullOrEmpty(imgUrl) && !imgUrl.Contains("via.placeholder.com") ? imgUrl : productImages.FirstOrDefault(),
                            ProductImages       = productImages,
                            FarmerName          = SafeStr(o, "farmerName"),
                            DeliveryAddress     = SafeStr(o, "deliveryAddress"),
                            ContactNumber       = SafeStr(o, "contactNumber"),
                            Quantity            = SafeInt(o, "quantity"),
                            UnitPrice           = SafeDec(o, "unitPrice"),
                            TotalPrice          = SafeDec(o, "totalPrice"),
                            Status              = SafeStr(o, "status"),
                            PaymentStatus       = SafeStr(o, "paymentStatus"),
                            OrderDate           = SafeDate(o, "orderDate")
                        });
                    }

                    vm.TotalOrders    = vm.Orders.Count;
                    vm.PendingOrders  = vm.Orders.Count(x => x.Status == "Pending");
                    vm.AcceptedOrders = vm.Orders.Count(x => x.Status is "Processing" or "Shipped" or "Delivered");
                    vm.TotalEarnings  = vm.Orders.Where(x => x.PaymentStatus == "Paid").Sum(x => x.TotalPrice);
                }
            }
            catch (Exception)
            {
                // Log error but don't block page load
            }
        }

        private async Task ProcessSubscriptionAsync(HttpResponseMessage? resp, SellerDashboardViewModel vm)
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
                        var subs = dataEl.EnumerateArray().ToList();
                        if (subs.Any())
                        {
                            var sub = subs.First();
                            vm.SubscriptionStatus   = SafeStr(sub, "status");
                            vm.SubscriptionPlanName = SafeStr(sub, "planName");
                            vm.SubscriptionExpiry   = SafeDate(sub, "endDate");
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
        // GET: /Seller/UploadImage?productId=X
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public IActionResult UploadImage(int productId)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");
            ViewBag.ProductId = productId;
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Seller/UploadImage
        // Sends multipart/form-data to POST /api/seller/upload-product-image
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> UploadImage(int productId, IFormFile? image)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            if (image == null || image.Length == 0)
            {
                TempData["Error"] = "Please select an image file.";
                return RedirectToAction("UploadImage", new { productId });
            }

            try
            {
                using var multipart = new MultipartFormDataContent();
                multipart.Add(new StringContent(productId.ToString()), "productId");

                var fileContent = new StreamContent(image.OpenReadStream());
                fileContent.Headers.ContentType =
                    new System.Net.Http.Headers.MediaTypeHeaderValue(image.ContentType);
                multipart.Add(fileContent, "image", image.FileName);

                var resp = await _apiClient.PostMultipartAsyncRaw("/api/seller/upload-product-image", multipart);
                var body = await resp.Content.ReadAsStringAsync();
                _logger.LogInformation("[SellerUploadImage] {StatusCode} — {Body}", resp.StatusCode, body);

                if (resp.IsSuccessStatusCode)
                {
                    TempData["Success"] = "✅ Image uploaded successfully!";
                    return RedirectToAction("Dashboard");
                }

                TempData["Error"] = $"Upload failed ({(int)resp.StatusCode}): {body}";
                return RedirectToAction("UploadImage", new { productId });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[SellerUploadImage] Exception");
                TempData["Error"] = "An error occurred during upload.";
                return RedirectToAction("UploadImage", new { productId });
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /Seller/AddProduct  — loads real categories
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> AddProduct()
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            ViewBag.Categories = await LoadCategoriesAsync();
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Seller/AddProduct
        // Flow: 1) create product (with placeholder imageUrl)
        //       2) upload real images via multipart
        //       3) delete placeholder image
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> AddProduct(
            string productName, string description,
            int? subCategoryId, string? subcategoryName,
            decimal price, int stock, string unit, string location,
            IFormFile[]? images)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            var validImages = images?.Where(f => f != null && f.Length > 0).ToArray() ?? Array.Empty<IFormFile>();
            if (validImages.Length == 0)
            {
                TempData["Error"] = "⚠ Please select at least one product photo before submitting.";
                return RedirectToAction("AddProduct");
            }

            int newProductId = 0;
            try
            {
                // ── Step 1: Create product ────────────────────────────────────
                // imageUrl and category are required fields in AddProductDto.
                // We pass a placeholder; the real image is uploaded in step 2.
                var payload = new
                {
                    productName,
                    description,
                    subCategoryId = subCategoryId ?? 1,
                    price,
                    stock,
                    unit,
                    location,
                    category = subcategoryName ?? "General",
                    imageUrl = "https://via.placeholder.com/400"
                };

                var res  = await _apiClient.PostAsyncRaw("/api/seller/add-product", payload);
                var body = await res.Content.ReadAsStringAsync();
                _logger.LogInformation("[SellerAddProduct] Create: {StatusCode} — {Body}", res.StatusCode, body);

                if (!res.IsSuccessStatusCode)
                {
                    TempData["Error"] = $"Failed to add product: {body}";
                    return RedirectToAction("AddProduct");
                }

                // Parse productId from { message, productId }
                try
                {
                    var addResult = JsonSerializer.Deserialize<JsonElement>(body,
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
                    newProductId = addResult.TryGetProperty("productId", out var pid) ? pid.GetInt32() : 0;
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "[SellerAddProduct] Could not parse productId");
                }

                // ── Step 2: Upload real images ────────────────────────────────
                int uploadedCount = 0;

                foreach (var img in validImages)
                {
                    try
                    {
                        using var multipart = new MultipartFormDataContent();
                        multipart.Add(new StringContent(newProductId.ToString()), "productId");

                        var fileContent = new StreamContent(img.OpenReadStream());
                        fileContent.Headers.ContentType =
                            new System.Net.Http.Headers.MediaTypeHeaderValue(img.ContentType);
                        multipart.Add(fileContent, "image", img.FileName);

                        var imgResp = await _apiClient.PostMultipartAsyncRaw("/api/seller/upload-product-image", multipart);
                        var imgBody = await imgResp.Content.ReadAsStringAsync();
                        _logger.LogInformation("[SellerAddProduct] Upload '{FileName}': {StatusCode} — {Body}", img.FileName, imgResp.StatusCode, imgBody);

                        if (imgResp.IsSuccessStatusCode) uploadedCount++;
                    }
                    catch (Exception ex)
                    {
                        _logger.LogError(ex, "[SellerAddProduct] Upload exception");
                    }
                }

                // ── Step 3: Delete placeholder image ─────────────────────────
                if (uploadedCount > 0 && newProductId > 0)
                {
                    try
                    {
                        var imagesResp = await _apiClient.GetAsyncRaw($"/api/seller/product-images/{newProductId}");
                        if (imagesResp.IsSuccessStatusCode)
                        {
                            var imagesBody = await imagesResp.Content.ReadAsStringAsync();
                            var imagesList = JsonSerializer.Deserialize<List<JsonElement>>(imagesBody,
                                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

                            foreach (var imgItem in imagesList ?? new())
                            {
                                var imgUrl = imgItem.TryGetProperty("imageUrl", out var u) ? u.GetString() : null;
                                if (imgUrl != null && imgUrl.Contains("via.placeholder.com"))
                                {
                                    var imgId = imgItem.TryGetProperty("imageId", out var iid) ? iid.GetInt32() : 0;
                                    if (imgId > 0)
                                    {
                                        await _apiClient.DeleteAsyncRaw($"/api/seller/delete-product-image/{imgId}");
                                        _logger.LogInformation("[SellerAddProduct] Deleted placeholder image id={ImageId}", imgId);
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        _logger.LogError(ex, "[SellerAddProduct] Placeholder cleanup error");
                    }
                }

                if (uploadedCount == 0)
                {
                    TempData["Error"] = "⚠ Product was listed but photo upload failed. Please upload a photo now.";
                    return RedirectToAction("UploadImage", new { productId = newProductId });
                }

                TempData["Success"] = $"✅ Product listed with {uploadedCount} photo(s)! Pending admin approval.";
                return RedirectToAction("Dashboard");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "[SellerAddProduct] Exception");
                TempData["Error"] = "An error occurred while adding product.";
                return RedirectToAction("AddProduct");
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // GET: /Seller/UpdateProduct/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpGet]
        public async Task<IActionResult> UpdateProduct(int id)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            var resp = await _apiClient.GetAsyncRaw("/api/seller/my-products");
            if (!resp.IsSuccessStatusCode)
            {
                TempData["Error"] = "Could not load product details.";
                return RedirectToAction("Dashboard");
            }

            var json = await resp.Content.ReadAsStringAsync();
            var arr  = JsonSerializer.Deserialize<JsonElement>(json,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            JsonElement? found = null;
            if (arr.ValueKind == JsonValueKind.Array)
                foreach (var item in arr.EnumerateArray())
                    if (SafeInt(item, "productId") == id) { found = item; break; }

            if (found == null)
            {
                TempData["Error"] = "Product not found.";
                return RedirectToAction("Dashboard");
            }

            ViewBag.ProductId  = id;
            var p = found.Value;
            var productItem = new SellerProductItem
            {
                ProductId     = SafeInt(p, "productId"),
                ProductName   = SafeStr(p, "productName"),
                Description   = SafeStr(p, "description"),
                CategoryName  = SafeStr(p, "category"),
                CategoryId    = p.TryGetProperty("categoryId", out var cid) && cid.ValueKind == JsonValueKind.Number ? cid.GetInt32() : (int?)null,
                SubCategoryId = p.TryGetProperty("subCategoryId", out var scid) && scid.ValueKind == JsonValueKind.Number ? scid.GetInt32() : (int?)null,
                Price         = SafeDec(p, "price"),
                Stock         = SafeInt(p, "stock"),
                Unit          = SafeStr(p, "unit"),
                Location      = SafeStr(p, "location"),
                Status        = SafeStr(p, "status"),
                CreatedAt     = SafeDate(p, "createdAt"),
            };

            ViewBag.Product    = productItem;
            ViewBag.Categories = await LoadCategoriesAsync();
            return View();
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Seller/UpdateProduct/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> UpdateProduct(
            int id,
            string productName, string description,
            int? subCategoryId, string? subcategoryName,
            decimal price, int stock, string unit, string location)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            // imageUrl and category are required in AddProductDto; update-product ignores imageUrl
            var payload = new
            {
                productName,
                description,
                subCategoryId = subCategoryId ?? 1,
                price,
                stock,
                unit,
                location,
                category = subcategoryName ?? "General",
                imageUrl = "https://via.placeholder.com/400"
            };

            try
            {
                var resp = await _apiClient.PutAsyncRaw($"/api/seller/update-product/{id}", payload);
                var body = await resp.Content.ReadAsStringAsync();
                _logger.LogInformation("[SellerUpdateProduct] {StatusCode} — {Body}", resp.StatusCode, body);

                if (resp.IsSuccessStatusCode)
                {
                    TempData["Success"] = "✅ Product updated successfully!";
                    return RedirectToAction("Dashboard");
                }

                TempData["Error"] = $"Update failed: {body}";
                return RedirectToAction("UpdateProduct", new { id });
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
                return RedirectToAction("UpdateProduct", new { id });
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Seller/DeleteProduct
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteProduct(int id)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            try
            {
                var resp = await _apiClient.DeleteAsyncRaw($"/api/seller/delete-product/{id}");
                var body = await resp.Content.ReadAsStringAsync();
                _logger.LogInformation("[SellerDeleteProduct] {StatusCode} — {Body}", resp.StatusCode, body);

                if (resp.IsSuccessStatusCode)
                    TempData["Success"] = "🗑 Product deleted successfully.";
                else
                    TempData["Error"] = $"Delete failed: {body}";
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // ──────────────────────────────────────────────────────────────────────
        // POST: /Seller/UpdateOrderStatus
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        public async Task<IActionResult> UpdateOrderStatus(int id, string status)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

            try
            {
                var resp = await _apiClient.PutAsyncRaw($"/api/seller/update-order-status/{id}", status);
                var body = await resp.Content.ReadAsStringAsync();

                TempData[resp.IsSuccessStatusCode ? "Success" : "Error"] =
                    resp.IsSuccessStatusCode
                        ? $"✅ Order #{id} marked as {status}."
                        : $"Failed to update order: {body}";
            }
            catch (Exception ex)
            {
                TempData["Error"] = $"Error: {ex.Message}";
            }

            return RedirectToAction("Dashboard");
        }

        // ─── Private: load categories from API ────────────────────────────────
        private async Task<List<object>> LoadCategoriesAsync()
        {
            // Issue #3 fix: Cache categories for 30 minutes
            var cacheKey = "product_categories";
            var cachedCategories = _cacheService.Get<List<object>>(cacheKey);
            if (cachedCategories != null)
                return cachedCategories;

            var categories = new List<object>();
            try
            {
                var resp = await _apiClient.GetAsyncRaw("/api/public/categories?type=Product");
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
                    
                    // Cache for 30 minutes
                    _cacheService.Set(cacheKey, categories, TimeSpan.FromMinutes(30));
                }
            }
            catch (Exception)
            {
                // Log error but return empty list (non-blocking)
            }
            return categories;
        }

        // ──────────────────────────────────────────────────────────────────────
        // Process Complaints
        // ──────────────────────────────────────────────────────────────────────
        private async Task ProcessComplaintsAsync(HttpResponseMessage? myResp, HttpResponseMessage? incomingResp, SellerDashboardViewModel vm)
        {
            if (myResp != null && myResp.IsSuccessStatusCode)
            {
                var body = await myResp.Content.ReadAsStringAsync();
                try
                {
                    vm.MyComplaints = JsonSerializer.Deserialize<List<SellerComplaintItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<SellerComplaintItem>();
                }
                catch { }
            }
            if (incomingResp != null && incomingResp.IsSuccessStatusCode)
            {
                var body = await incomingResp.Content.ReadAsStringAsync();
                try
                {
                    vm.IncomingComplaints = JsonSerializer.Deserialize<List<SellerComplaintItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<SellerComplaintItem>();
                }
                catch { }
            }
        }

        // ──────────────────────────────────────────────────────────────────────
        // Process Reviews
        // ──────────────────────────────────────────────────────────────────────
        private async Task ProcessReviewsAsync(HttpResponseMessage? myResp, HttpResponseMessage? incomingResp, SellerDashboardViewModel vm)
        {
            if (myResp != null && myResp.IsSuccessStatusCode)
            {
                var body = await myResp.Content.ReadAsStringAsync();
                try
                {
                    vm.MyReviews = JsonSerializer.Deserialize<List<SellerReviewItem>>(body, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<SellerReviewItem>();
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
                        vm.IncomingReviews = JsonSerializer.Deserialize<List<SellerReviewItem>>(revs.GetRawText(), new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<SellerReviewItem>();
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
        // POST: /Seller/ResolveComplaint/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> ResolveComplaint(int id, string resolutionNote)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

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
        // POST: /Seller/DeleteReview/{id}
        // ──────────────────────────────────────────────────────────────────────
        [HttpPost]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> DeleteReview(int id)
        {
            if (!IsSeller()) return RedirectToAction("Login", "Account");

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
