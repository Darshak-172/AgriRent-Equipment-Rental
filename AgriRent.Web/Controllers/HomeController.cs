using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using AgriRent.ViewModels;
using System.Text.Json;

namespace AgriRent.Web.Controllers;

public class HomeController : Controller
{
    private readonly ApiClient _apiClient;
    private readonly ICacheService _cacheService;
    private readonly ILogger<HomeController> _logger;

    public HomeController(ApiClient apiClient, ICacheService cacheService, ILogger<HomeController> logger)
    {
        _apiClient = apiClient;
        _cacheService = cacheService;
        _logger = logger;
    }

    private string CurrentLanguage()
    {
        return HttpContext?.Session.GetString("Language") ?? "en";
    }

    private sealed class HomeEquipmentCacheItem
    {
        public List<AgriRent.Models.Equipment> Equipments { get; init; } = new();
        public List<string> Locations { get; init; } = new();
    }

    public async Task<IActionResult> Index()
    {
        try
        {
            var model = new HomeViewModel
            {
                Equipments = new List<AgriRent.Models.Equipment>(),
                Products = new List<AgriRent.Models.Product>(),
                Categories = new List<AgriRent.Models.Category>(),
                Locations = new List<string>()
            };

            // Execute API calls in parallel for better performance
            var categoriesTask = FetchCategoriesAsync();
            var equipmentTask = FetchEquipmentAsync();
            var productsTask = FetchProductsAsync();

            // Wait for all tasks to complete
            await Task.WhenAll(categoriesTask, equipmentTask, productsTask);

            // Assign results - FIX: Use await instead of .Result for non-blocking
            model.Categories = await categoriesTask;
            var equipmentResult = await equipmentTask;
            model.Equipments = equipmentResult.Equipments;
            model.Locations = equipmentResult.Locations;
            model.Products = await productsTask;

            return View(model);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in Index");
            var model = new HomeViewModel
            {
                Equipments = new List<AgriRent.Models.Equipment>(),
                Products = new List<AgriRent.Models.Product>(),
                Categories = new List<AgriRent.Models.Category>(),
                Locations = new List<string>()
            };
            return View(model);
        }
    }

    private async Task<List<AgriRent.Models.Category>> FetchCategoriesAsync()
    {
        try
        {
            // Check cache first
            var cacheKey = $"categories_{CurrentLanguage()}";
            var cachedCategories = _cacheService.Get<List<AgriRent.Models.Category>>(cacheKey);
            if (cachedCategories != null && cachedCategories.Any())
            {
                return cachedCategories.Take(6).ToList();
            }

            var categoriesResponse = await _apiClient.GetAsyncRaw("/api/public/categories");
            if (categoriesResponse.IsSuccessStatusCode)
            {
                var categoriesJson = await categoriesResponse.Content.ReadAsStringAsync();
                var categories = JsonSerializer.Deserialize<List<AgriRent.Models.Category>>(categoriesJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });
                
                if (categories != null && categories.Any())
                {
                    // Cache for 1 hour
                    _cacheService.Set(cacheKey, categories, TimeSpan.FromHours(1));
                    return categories.Take(6).ToList();
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching categories");
        }

        return new List<AgriRent.Models.Category>();
    }

    private async Task<(List<AgriRent.Models.Equipment> Equipments, List<string> Locations)> FetchEquipmentAsync()
    {
        var cacheKey = $"home_equipment_{CurrentLanguage()}";
        var cached = _cacheService.Get<HomeEquipmentCacheItem>(cacheKey);
        if (cached != null)
        {
            return (cached.Equipments, cached.Locations);
        }

        var equipmentList = new List<AgriRent.Models.Equipment>();
        var locations = new List<string>();

        try
        {
            // FIX: Add pagination limit parameter to prevent loading all data
            var equipmentResponse = await _apiClient.GetAsyncRaw("/api/public/equipment/available?pageSize=8&page=1");
            
            if (equipmentResponse.IsSuccessStatusCode)
            {
                var equipmentJson = await equipmentResponse.Content.ReadAsStringAsync();
                
                var equipmentArray = JsonSerializer.Deserialize<JsonElement>(equipmentJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                if (equipmentArray.ValueKind == JsonValueKind.Object && equipmentArray.TryGetProperty("data", out var dataArray) && dataArray.ValueKind == JsonValueKind.Array)
                {
                    // Only take first 8 items as requested from API
                    var equipmentData = dataArray.EnumerateArray().Take(8).ToList();
                    
                    foreach (var item in equipmentData)
                    {
                        var equipment = new AgriRent.Models.Equipment
                        {
                            EquipmentId = item.GetProperty("equipmentId").GetInt32(),
                            EquipmentName = item.GetProperty("equipmentName").GetString() ?? "",
                            Description = item.GetProperty("description").GetString() ?? "",
                            HourlyPrice = item.TryGetProperty("hourlyPrice", out var hourlyVal) ? hourlyVal.GetDecimal() : 0,
                            DailyPrice = item.TryGetProperty("dailyPrice", out var dailyVal) ? dailyVal.GetDecimal() : 0,
                            Location = item.GetProperty("location").GetString() ?? "",
                            SubCategoryId = item.TryGetProperty("subCategoryId", out var subCatId) ? subCatId.GetInt32() : 0
                        };

                        // Extract image URL from images array
                        if (item.TryGetProperty("images", out var images) && images.ValueKind == JsonValueKind.Array)
                        {
                            var imageArray = images.EnumerateArray().FirstOrDefault();
                            if (imageArray.ValueKind == JsonValueKind.Object && imageArray.TryGetProperty("imageUrl", out var imageUrl))
                            {
                                equipment.ImageUrl = imageUrl.GetString();
                            }
                        }

                        equipmentList.Add(equipment);
                    }

                    // Extract unique locations
                    locations = equipmentList
                        .Select(e => e.Location)
                        .Where(l => !string.IsNullOrEmpty(l))
                        .Distinct()
                        .OrderBy(l => l)
                        .ToList();

                    _cacheService.Set(cacheKey, new HomeEquipmentCacheItem
                    {
                        Equipments = equipmentList,
                        Locations = locations
                    }, TimeSpan.FromMinutes(5));
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching equipment");
        }

        return (equipmentList, locations);
    }

    private async Task<List<AgriRent.Models.Product>> FetchProductsAsync()
    {
        var cacheKey = $"home_products_{CurrentLanguage()}";
        var cachedProducts = _cacheService.Get<List<AgriRent.Models.Product>>(cacheKey);
        if (cachedProducts != null)
        {
            return cachedProducts;
        }

        var productsList = new List<AgriRent.Models.Product>();

        try
        {
            // FIX: Add pagination limit parameter to prevent loading all data
            var productsResponse = await _apiClient.GetAsyncRaw("/api/products/list?pageSize=8&page=1");
            if (productsResponse.IsSuccessStatusCode)
            {
                var productsJson = await productsResponse.Content.ReadAsStringAsync();
                
                var productsRoot = JsonSerializer.Deserialize<JsonElement>(productsJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                List<JsonElement> productsData = new List<JsonElement>();

                if (productsRoot.ValueKind == JsonValueKind.Array)
                {
                    productsData = productsRoot.EnumerateArray().Take(8).ToList();
                }
                else if (productsRoot.ValueKind == JsonValueKind.Object && productsRoot.TryGetProperty("data", out var prodArray) && prodArray.ValueKind == JsonValueKind.Array)
                {
                    productsData = prodArray.EnumerateArray().Take(8).ToList();
                }

                foreach (var item in productsData)
                {
                    var product = new AgriRent.Models.Product
                    {
                        ProductId = item.TryGetProperty("productId", out var prodId) ? prodId.GetInt32() : 
                                     (item.TryGetProperty("equipmentId", out var eqId) ? eqId.GetInt32() : 0),
                        ProductName = item.TryGetProperty("productName", out var prodName) ? prodName.GetString() ?? "" :
                                       (item.TryGetProperty("equipmentName", out var eqName) ? eqName.GetString() ?? "" : ""),
                        Description = item.TryGetProperty("description", out var desc) ? desc.GetString() ?? "" : "",
                        Price = item.TryGetProperty("price", out var priceVal) ? priceVal.GetDecimal() : 0,
                        Unit = item.TryGetProperty("unit", out var unitVal) ? unitVal.GetString() ?? "kg" : "kg",
                        Location = item.TryGetProperty("location", out var locVal) ? locVal.GetString() ?? "" : "",
                        SubCategoryId = 0
                    };

                    // Extract image URL
                    if (item.TryGetProperty("imageUrl", out var imgUrl))
                    {
                        product.ImageUrl = imgUrl.GetString();
                    }
                    else if (item.TryGetProperty("images", out var images) && images.ValueKind == JsonValueKind.Array)
                    {
                        var imageArray = images.EnumerateArray().FirstOrDefault();
                        if (imageArray.ValueKind == JsonValueKind.Object && imageArray.TryGetProperty("imageUrl", out var imageUrl))
                        {
                            product.ImageUrl = imageUrl.GetString();
                        }
                    }

                    productsList.Add(product);
                }

                if (productsList.Count > 0)
                {
                    _cacheService.Set(cacheKey, productsList, TimeSpan.FromMinutes(5));
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching products");
        }

        return productsList;
    }

    public IActionResult Privacy()
    {
        return View();
    }

    public IActionResult Contact()
    {
        return View();
    }

    [HttpGet]
    [Route("api/search")]
    public async Task<IActionResult> Search(string? keyword, string? location, int? categoryId)
    {
        try
        {
            var queryParams = new List<string>();
            if (!string.IsNullOrEmpty(keyword)) queryParams.Add($"keyword={Uri.EscapeDataString(keyword)}");
            if (!string.IsNullOrEmpty(location)) queryParams.Add($"location={Uri.EscapeDataString(location)}");
            if (categoryId.HasValue) queryParams.Add($"categoryId={categoryId.Value}");
            
            var queryString = queryParams.Any() ? "?" + string.Join("&", queryParams) : "";
            var url = $"/api/search{queryString}";
            
            var response = await _apiClient.GetAsyncRaw(url);
            
            if (response.IsSuccessStatusCode)
            {
                var content = await response.Content.ReadAsStringAsync();
                return Content(content, "application/json");
            }
            return StatusCode((int)response.StatusCode, await response.Content.ReadAsStringAsync());
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in Search proxy");
            return StatusCode(500, new { error = "Internal server error" });
        }
    }

    [HttpPost]
    public async Task<IActionResult> SetLanguage(string lang)
    {
        // Validate language parameter
        if (string.IsNullOrEmpty(lang) || (lang != "en" && lang != "gu" && lang != "hi"))
        {
            lang = "en"; // Default to English
        }

        // Store selected language in session
        HttpContext.Session.SetString("Language", lang);

        // If user is logged in, refresh their profile data to get translated name
        var userId = HttpContext.Session.GetString("UserId");
        if (!string.IsNullOrEmpty(userId))
        {
            await RefreshUserProfile();
        }

        return Ok(new { success = true, language = lang });
    }

    // Helper method to refresh user profile with translated name
    private async Task RefreshUserProfile()
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
            _logger.LogError(ex, "Error refreshing profile");
        }
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View();
    }
}
