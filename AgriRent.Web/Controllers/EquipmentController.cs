using Microsoft.AspNetCore.Mvc;
using AgriRent.Web.Services;
using System.Text.Json;
using AgriRent.Models;

namespace AgriRent.Web.Controllers;

[ApiController]
[Route("api/equipment")]
public class EquipmentApiController : ControllerBase
{
    private readonly ApiClient _apiClient;

    public EquipmentApiController(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    // ⭐ PROXY: GET EQUIPMENT DETAILS FOR ADMIN
    [HttpGet("admin/details/{id}")]
    public async Task<IActionResult> GetEquipmentDetailsAsync(int id)
    {
        try
        {
            var response = await _apiClient.GetAsyncRaw($"/api/equipment/admin/details/{id}");
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

public class EquipmentController : Controller
{
    private readonly ApiClient _apiClient;

    public EquipmentController(ApiClient apiClient)
    {
        _apiClient = apiClient;
    }

    public async Task<IActionResult> Index()
    {
        // Require login to view equipment
        var userSession = HttpContext.Session.GetString("UserId");
        if (string.IsNullOrEmpty(userSession))
        {
            return RedirectToAction("Login", "Account");
        }

        var model = new List<Equipment>();

        try
        {
            // Fetch equipment from API
            // FIX: Add pagination to prevent loading 100+ items (1-3s delay)
            var equipmentResponse = await _apiClient.GetAsyncRaw("/api/public/equipment/available?pageSize=20&page=1");
                
            if (equipmentResponse.IsSuccessStatusCode)
            {
                var equipmentJson = await equipmentResponse.Content.ReadAsStringAsync();
                var equipmentArray = JsonSerializer.Deserialize<JsonElement>(equipmentJson, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });

                if (equipmentArray.ValueKind == JsonValueKind.Object && equipmentArray.TryGetProperty("data", out var dataArray) && dataArray.ValueKind == JsonValueKind.Array)
                {
                    foreach (var item in dataArray.EnumerateArray())
                    {
                        var equipment = new Equipment
                        {
                            EquipmentId = item.GetProperty("equipmentId").GetInt32(),
                            EquipmentName = item.GetProperty("equipmentName").GetString() ?? "",
                            Description = item.GetProperty("description").GetString() ?? "",
                            HourlyPrice = item.TryGetProperty("hourlyPrice", out var hourlyVal) ? hourlyVal.GetDecimal() : 0,
                            DailyPrice = item.TryGetProperty("dailyPrice", out var dailyVal) ? dailyVal.GetDecimal() : 0,
                            Location = item.GetProperty("location").GetString() ?? "",
                            SubCategoryId = item.TryGetProperty("subCategoryId", out var subCatId) ? subCatId.GetInt32() : 0
                        };

                        if (item.TryGetProperty("images", out var images) && images.ValueKind == JsonValueKind.Array)
                        {
                            var firstImage = images.EnumerateArray().FirstOrDefault();
                            if (firstImage.ValueKind == JsonValueKind.Object && firstImage.TryGetProperty("imageUrl", out var imageUrl))
                            {
                                equipment.ImageUrl = imageUrl.GetString();
                            }
                        }

                        model.Add(equipment);
                    }
                }
                else if (equipmentArray.ValueKind == JsonValueKind.Array)
                {
                    foreach (var item in equipmentArray.EnumerateArray())
                    {
                        var equipment = new Equipment
                        {
                            EquipmentId = item.GetProperty("equipmentId").GetInt32(),
                            EquipmentName = item.GetProperty("equipmentName").GetString() ?? "",
                            Description = item.GetProperty("description").GetString() ?? "",
                            HourlyPrice = item.TryGetProperty("hourlyPrice", out var hourlyVal) ? hourlyVal.GetDecimal() : 0,
                            DailyPrice = item.TryGetProperty("dailyPrice", out var dailyVal) ? dailyVal.GetDecimal() : 0,
                            Location = item.GetProperty("location").GetString() ?? "",
                            SubCategoryId = item.TryGetProperty("subCategoryId", out var subCatId) ? subCatId.GetInt32() : 0
                        };

                        if (item.TryGetProperty("images", out var images) && images.ValueKind == JsonValueKind.Array)
                        {
                            var firstImage = images.EnumerateArray().FirstOrDefault();
                            if (firstImage.ValueKind == JsonValueKind.Object && firstImage.TryGetProperty("imageUrl", out var imageUrl))
                            {
                                equipment.ImageUrl = imageUrl.GetString();
                            }
                        }

                        model.Add(equipment);
                    }
                }
            }
        }
        catch (Exception)
        {
            // Log error but don't block
        }

        return View(model);
    }
}
