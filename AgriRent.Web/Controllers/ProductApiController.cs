using Microsoft.AspNetCore.Mvc;
using AgriRent.Models;
using AgriRent.Web.Services;
using System.Text.Json;

namespace AgriRent.Web.Controllers;

[ApiController]
[Route("api/products")]
public class ProductApiController : ControllerBase
{
    private readonly ApiClient _apiClient;

    public ProductApiController(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    // ⭐ PROXY: GET PRODUCT DETAILS FOR ADMIN
    [HttpGet("admin/details/{id}")]
    public async Task<IActionResult> GetProductDetailsAsync(int id)
    {
        try
        {
            var response = await _apiClient.GetAsyncRaw($"/api/products/admin/details/{id}");
            if (!response.IsSuccessStatusCode)
            {
                return StatusCode((int)response.StatusCode, await response.Content.ReadAsStringAsync());
            }
            var content = await response.Content.ReadAsStringAsync();
            return Ok(JsonSerializer.Deserialize<object>(content, new JsonSerializerOptions { PropertyNameCaseInsensitive = true }));
        }
        catch (Exception ex)
        {
            return BadRequest(new { message = ex.Message });
        }
    }
}

public class ProductsController : Controller
{
    private readonly ApiClient _apiClient;

    public ProductsController(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task<IActionResult> Index(int page = 1, int pageSize = 20)
    {
        var userSession = HttpContext.Session.GetString("UserId");
        if (string.IsNullOrEmpty(userSession))
        {
            return RedirectToAction("Login", "Account");
        }

        var model = new List<Product>();

        try
        {
            var productsResponse = await _apiClient.GetAsyncRaw($"/api/products/list?pageSize={pageSize}&page={page}");
                
            if (productsResponse.IsSuccessStatusCode)
            {
                var productsJson = await productsResponse.Content.ReadAsStringAsync();
                var productsRoot = JsonSerializer.Deserialize<JsonElement>(productsJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                List<JsonElement> productsData = new List<JsonElement>();

                if (productsRoot.ValueKind == JsonValueKind.Object && productsRoot.TryGetProperty("data", out var dataArray) && dataArray.ValueKind == JsonValueKind.Array)
                {
                    productsData = dataArray.EnumerateArray().ToList();
                }
                else if (productsRoot.ValueKind == JsonValueKind.Array)
                {
                    productsData = productsRoot.EnumerateArray().ToList();
                }

                foreach (var item in productsData)
                {
                    var product = new Product
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

                    model.Add(product);
                }
            }
        }
        catch (Exception)
        {
            // Log error but don't block
        }

        ViewBag.CurrentPage = page;
        ViewBag.PageSize = pageSize;
        ViewBag.HasMore = model.Count == pageSize;

        return View(model);
    }
}
