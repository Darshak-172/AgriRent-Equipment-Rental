using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using System.Text.Json;
using AgriRent.DTOs;

namespace AgriRent.Web.Controllers
{
    public class AdminCategoryController : Controller
    {
        private readonly ApiClient _apiClient;
        private readonly ICacheService _cacheService;
        private readonly ILogger<AdminCategoryController> _logger;

        public AdminCategoryController(ApiClient apiClient, ICacheService cacheService, ILogger<AdminCategoryController> logger)
        {
            _apiClient = apiClient;
            _cacheService = cacheService;
            _logger = logger;
        }

        // Loads admin categories with 1-hour cache (Issue #3 - eliminates 800-1200ms per call)
        private async Task<List<CategoryDto>> LoadAdminCategoriesAsync()
        {
            var cacheKey = "admin_categories";
            var cachedCategories = _cacheService.Get<List<CategoryDto>>(cacheKey);
            if (cachedCategories != null) return cachedCategories;

            try
            {
                var response = await _apiClient.GetAsyncRaw("/api/admin/categories");
                if (response.IsSuccessStatusCode)
                {
                    var content = await response.Content.ReadAsStringAsync();
                    var categories = JsonSerializer.Deserialize<List<CategoryDto>>(content, 
                        new JsonSerializerOptions { PropertyNameCaseInsensitive = true }) ?? new List<CategoryDto>();
                    
                    _cacheService.Set(cacheKey, categories, TimeSpan.FromHours(1));
                    return categories;
                }
            }
            catch { }

            return new List<CategoryDto>();
        }

        // ==========================================
        // CATEGORY OPERATIONS
        // ==========================================

        // GET: /AdminCategory
        public async Task<IActionResult> Index(string? status = null)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            ViewBag.CurrentStatus = status ?? "All";

            try
            {
                var categories = await LoadAdminCategoriesAsync();

                if (!string.IsNullOrEmpty(status) && status != "All")
                    categories = categories.Where(c => c.Status == status).ToList();

                return View(categories);
            }
            catch (Exception)
            {
                TempData["Error"] = "Failed to load categories.";
            }

            return View(new List<CategoryDto>());
        }

        // GET: /AdminCategory/Create
        public IActionResult Create()
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            return View(new CategoryDto { Name = "" });
        }

        // POST: /AdminCategory/Create
        [HttpPost]
        public async Task<IActionResult> Create(CategoryDto dto)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            if (!ModelState.IsValid) return View(dto);

            try
            {
                // Check for duplicate category name (use cache)
                var categories = await LoadAdminCategoriesAsync();
                
                if (categories?.Any(c => c.Name.Equals(dto.Name, StringComparison.OrdinalIgnoreCase)) == true)
                {
                    ModelState.AddModelError("Name", "This category already exists.");
                    return View(dto);
                }

                var response = await _apiClient.PostAsyncRaw("/api/admin/categories", new
                {
                    name = dto.Name,
                    status = dto.Status ?? "Active",
                    type = dto.Type ?? "Equipment"
                });

                if (response.IsSuccessStatusCode)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "Category created successfully!";
                    return RedirectToAction(nameof(Index));
                }
                
                TempData["Error"] = "Failed to create category.";
            }
            catch (Exception)
            {
                TempData["Error"] = "An error occurred while creating category.";
            }

            return View(dto);
        }

        // GET: /AdminCategory/Edit/5
        public async Task<IActionResult> Edit(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                var categories = await LoadAdminCategoriesAsync();
                var category = categories?.FirstOrDefault(c => c.CategoryId == id);
                
                if (category != null)
                    return View(category);
            }
            catch (Exception)
            {
                // Log error but don't block
            }

            TempData["Error"] = "Category not found.";
            return RedirectToAction(nameof(Index));
        }

        // POST: /AdminCategory/Edit/5
        [HttpPost]
        public async Task<IActionResult> Edit(int id, CategoryDto dto)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            if (!ModelState.IsValid) return View(dto);

            try
            {
                // Check for duplicate category name (excluding self)
                var categories = await LoadAdminCategoriesAsync();
                
                if (categories?.Any(c => c.CategoryId != id && c.Name.Equals(dto.Name, StringComparison.OrdinalIgnoreCase)) == true)
                {
                    ModelState.AddModelError("Name", "This category already exists.");
                    return View(dto);
                }

                var response = await _apiClient.PutAsyncRaw($"/api/admin/categories/{id}", new
                {
                    name = dto.Name,
                    status = dto.Status,
                    type = dto.Type
                });

                if (response.IsSuccessStatusCode)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "Category updated successfully!";
                    return RedirectToAction(nameof(Index));
                }
                
                TempData["Error"] = "Failed to update category.";
            }
            catch (Exception)
            {
                TempData["Error"] = "An error occurred while updating category.";
            }

            return View(dto);
        }

        // POST: /AdminCategory/Delete/5
        [HttpPost]
        public async Task<IActionResult> Delete(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                // Fetch categories to check for dependencies (use cache)
                var categories = await LoadAdminCategoriesAsync();
                var category = categories?.FirstOrDefault(c => c.CategoryId == id);
                
                if (category == null)
                {
                    TempData["Error"] = "Category not found.";
                    return RedirectToAction(nameof(Index));
                }
                    
                // VALIDATION CHECK BEFORE DELETION (As requested by user constraints)
                if (category.SubCategories != null && category.SubCategories.Any())
                {
                    TempData["Error"] = $"Cannot delete category '{category.Name}' because it has {category.SubCategories.Count} subcategory(s). Please delete or move all subcategories first.";
                    return RedirectToAction(nameof(Index));
                }

                // 2. Perform deletion
                var success = await _apiClient.DeleteAsync($"/api/admin/categories/{id}");
                if (success)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "Category deleted successfully!";
                }
                else
                {
                    TempData["Error"] = "Failed to delete category.";
                }
            }
            catch (Exception)
            {
                TempData["Error"] = "An error occurred while deleting category.";
            }

            return RedirectToAction(nameof(Index));
        }

        // POST: /AdminCategory/ToggleStatus/5
        [HttpPost]
        public async Task<IActionResult> ToggleStatus(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                var categories = await LoadAdminCategoriesAsync();
                var category = categories?.FirstOrDefault(c => c.CategoryId == id);
                
                if (category != null)
                {
                    var newStatus = category.Status == "Active" ? "Inactive" : "Active";
                    var updateResponse = await _apiClient.PutAsyncRaw($"/api/admin/categories/{id}", new
                    {
                        name = category.Name,
                        status = newStatus,
                        type = category.Type
                    });

                    if (updateResponse.IsSuccessStatusCode)
                    {
                        _cacheService.Remove("admin_categories");
                        TempData["Success"] = $"Category '{category.Name}' is now {newStatus}.";
                    }
                }
            }
            catch (Exception)
            {
                // Log error but don't block
            }

            return RedirectToAction(nameof(Index));
        }

        // ==========================================
        // SUB-CATEGORY OPERATIONS
        // ==========================================

        // GET: /AdminCategory/SubCategories/5
        public async Task<IActionResult> SubCategories(int id)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                var categories = await LoadAdminCategoriesAsync();
                if (categories != null)
                {
                    var category = categories.FirstOrDefault(c => c.CategoryId == id);
                    
                    if (category != null)
                    {
                        ViewBag.CategoryId = category.CategoryId;
                        ViewBag.CategoryName = category.Name;
                        return View(category.SubCategories ?? new List<SubCategoryDto>());
                    }
                }
            }
            catch (Exception)
            {
                TempData["Error"] = "Failed to load subcategories.";
            }

            return RedirectToAction(nameof(Index));
        }
        
        // GET: /AdminCategory/SubCategoryCreate?categoryId=5
        public IActionResult SubCategoryCreate(int categoryId, string categoryName)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            ViewBag.CategoryName = categoryName;
            return View(new SubCategoryDto { CategoryId = categoryId, SubCategoryName = "" });
        }
        
        // POST: /AdminCategory/SubCategoryCreate
        [HttpPost]
        public async Task<IActionResult> SubCategoryCreate(SubCategoryDto dto)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            if (!ModelState.IsValid) return View(dto);

            try
            {
                // Check for duplicate subcategory name within the same category
                var categories = await LoadAdminCategoriesAsync();
                if (categories.Count > 0)
                {
                    var category = categories?.FirstOrDefault(c => c.CategoryId == dto.CategoryId);
                    
                    if (category?.SubCategories?.Any(sc => sc.SubCategoryName.Equals(dto.SubCategoryName, StringComparison.OrdinalIgnoreCase)) == true)
                    {
                        ModelState.AddModelError("SubCategoryName", "This subcategory already exists in this category.");
                        // Re-populate ViewBag so view doesn't break
                        ViewBag.CategoryName = category.Name;
                        return View(dto);
                    }
                }

                var response = await _apiClient.PostAsyncRaw("/api/admin/categories/subcategories", new 
                { 
                    categoryId = dto.CategoryId,
                    subCategoryName = dto.SubCategoryName, 
                    status = dto.Status ?? "Active" 
                });

                if (response.IsSuccessStatusCode)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "SubCategory created successfully!";
                    return RedirectToAction(nameof(SubCategories), new { id = dto.CategoryId });
                }
                
                TempData["Error"] = "Failed to create SubCategory.";
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating subcategory");
                TempData["Error"] = "An error occurred while creating SubCategory.";
            }

            return View(dto);
        }

        // GET: /AdminCategory/SubCategoryEdit?id=5&categoryId=2
        public async Task<IActionResult> SubCategoryEdit(int id, int categoryId, string categoryName)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                var categories = await LoadAdminCategoriesAsync();
                if (categories.Count > 0)
                {
                    var category = categories?.FirstOrDefault(c => c.CategoryId == categoryId);
                    var subCat = category?.SubCategories?.FirstOrDefault(sc => sc.SubCategoryId == id);
                    
                    if (subCat != null)
                    {
                        ViewBag.CategoryName = categoryName ?? category!.Name;
                        return View(subCat);
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching subcategory for edit");
            }

            TempData["Error"] = "SubCategory not found.";
            return RedirectToAction(nameof(SubCategories), new { id = categoryId });
        }
        
        // POST: /AdminCategory/SubCategoryEdit/5
        [HttpPost]
        public async Task<IActionResult> SubCategoryEdit(int id, SubCategoryDto dto)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            if (!ModelState.IsValid) return View(dto);

            try
            {
                // Check for duplicate subcategory name within the same category (excluding self)
                var categories = await LoadAdminCategoriesAsync();
                if (categories.Count > 0)
                {
                    var category = categories?.FirstOrDefault(c => c.CategoryId == dto.CategoryId);
                    
                    if (category?.SubCategories?.Any(sc => sc.SubCategoryId != id && sc.SubCategoryName.Equals(dto.SubCategoryName, StringComparison.OrdinalIgnoreCase)) == true)
                    {
                        ModelState.AddModelError("SubCategoryName", "This subcategory already exists in this category.");
                        // Re-populate ViewBag so view doesn't break
                        ViewBag.CategoryName = category.Name;
                        return View(dto);
                    }
                }

                var response = await _apiClient.PutAsyncRaw($"/api/admin/categories/subcategories/{id}", new 
                { 
                    categoryId = dto.CategoryId,
                    subCategoryName = dto.SubCategoryName, 
                    status = dto.Status 
                });

                if (response.IsSuccessStatusCode)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "SubCategory updated successfully!";
                    return RedirectToAction(nameof(SubCategories), new { id = dto.CategoryId });
                }
                
                TempData["Error"] = "Failed to update SubCategory.";
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating subcategory");
                TempData["Error"] = "An error occurred while updating SubCategory.";
            }

            return View(dto);
        }

        // POST: /AdminCategory/SubCategoryDelete/5
        [HttpPost]
        public async Task<IActionResult> SubCategoryDelete(int id, int categoryId, string subCategoryName)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                // VALIDATION CHECK BEFORE DELETION (As requested by user constraints)
                bool hasDependencies = false;

                // Check #1: Does it have Equipment?
                var eqResponse = await _apiClient.GetAsyncRaw($"/api/public/equipment/available?subCategoryId={id}");
                if (eqResponse.IsSuccessStatusCode)
                {
                    var eqContent = await eqResponse.Content.ReadAsStringAsync();
                    var equipments = JsonSerializer.Deserialize<JsonElement>(eqContent);
                    if (equipments.ValueKind == JsonValueKind.Object && equipments.TryGetProperty("data", out var eqDataArray))
                    {
                        if (eqDataArray.ValueKind == JsonValueKind.Array && eqDataArray.GetArrayLength() > 0)
                        {
                            hasDependencies = true;
                        }
                    }
                    else if (equipments.ValueKind == JsonValueKind.Array && equipments.GetArrayLength() > 0)
                    {
                        hasDependencies = true;
                    }
                }

                // Check #2: Does it have Products?
                if (!hasDependencies && !string.IsNullOrEmpty(subCategoryName))
                {
                    var prResponse = await _apiClient.GetAsyncRaw($"/api/products/list?category={Uri.EscapeDataString(subCategoryName)}");
                    if (prResponse.IsSuccessStatusCode)
                    {
                        var prContent = await prResponse.Content.ReadAsStringAsync();
                        var products = JsonSerializer.Deserialize<JsonElement>(prContent);
                        if (products.ValueKind == JsonValueKind.Array && products.GetArrayLength() > 0)
                        {
                            hasDependencies = true;
                        }
                    }
                }

                if (hasDependencies)
                {
                    TempData["Error"] = $"Cannot delete SubCategory '{subCategoryName}' because it currently has Equipment or Products linked to it. Delete them first.";
                    return RedirectToAction(nameof(SubCategories), new { id = categoryId });
                }

                // Actually Perform Deletion
                var success = await _apiClient.DeleteAsync($"/api/admin/categories/subcategories/{id}");
                if (success)
                {
                    _cacheService.Remove("admin_categories");
                    TempData["Success"] = "SubCategory deleted successfully!";
                }
                else
                {
                    TempData["Error"] = "Failed to delete SubCategory.";
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error deleting subcategory");
                TempData["Error"] = "An error occurred while deleting SubCategory.";
            }
            return RedirectToAction(nameof(SubCategories), new { id = categoryId });
        }

        // POST: /AdminCategory/SubCategoryToggleStatus/5?categoryId=2
        [HttpPost]
        public async Task<IActionResult> SubCategoryToggleStatus(int id, int categoryId)
        {
            if (string.IsNullOrEmpty(HttpContext.Session.GetString("AdminToken")))
                return RedirectToAction("Login", "Admin");

            try
            {
                var categories = await LoadAdminCategoriesAsync();
                if (categories.Count > 0)
                {
                    var category = categories?.FirstOrDefault(c => c.CategoryId == categoryId);
                    var subCat = category?.SubCategories?.FirstOrDefault(sc => sc.SubCategoryId == id);
                    
                    if (subCat != null)
                    {
                        var newStatus = subCat.Status == "Active" ? "Inactive" : "Active";
                        var updateResponse = await _apiClient.PutAsyncRaw($"/api/admin/categories/subcategories/{id}", new 
                        { 
                            categoryId = categoryId,
                            subCategoryName = subCat.SubCategoryName, 
                            status = newStatus 
                        });

                        if (updateResponse.IsSuccessStatusCode)
                        {
                            _cacheService.Remove("admin_categories");
                            TempData["Success"] = $"SubCategory '{subCat.SubCategoryName}' is now {newStatus}.";
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error toggling subcategory status");
            }

            return RedirectToAction(nameof(SubCategories), new { id = categoryId });
        }
    }
}
