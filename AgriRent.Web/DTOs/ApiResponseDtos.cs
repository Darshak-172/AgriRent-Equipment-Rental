namespace AgriRent.Web.DTOs
{
    /// <summary>
    /// Generic API response wrapper for paginated data
    /// </summary>
    public class ApiResponse<T>
    {
        public bool Success { get; set; }
        public string Message { get; set; } = string.Empty;
        public T? Data { get; set; }
    }

    /// <summary>
    /// Generic API response for single items
    /// </summary>
    public class ApiSingleResponse<T>
    {
        public T? Data { get; set; }
        public string Message { get; set; } = string.Empty;
    }

    /// <summary>
    /// Equipment/Product item DTO
    /// </summary>
    public class EquipmentDto
    {
        public int EquipmentId { get; set; }
        public string EquipmentName { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public decimal HourlyPrice { get; set; }
        public decimal DailyPrice { get; set; }
        public string Location { get; set; } = string.Empty;
        public int SubCategoryId { get; set; }
        public List<EquipmentImageDto> Images { get; set; } = new();
    }

    /// <summary>
    /// Equipment image DTO
    /// </summary>
    public class EquipmentImageDto
    {
        public int ImageId { get; set; }
        public string ImageUrl { get; set; } = string.Empty;
    }

    /// <summary>
    /// Product item DTO
    /// </summary>
    public class ProductDto
    {
        public int ProductId { get; set; }
        public string ProductName { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public string Location { get; set; } = string.Empty;
        public string ImageUrl { get; set; } = string.Empty;
    }

    /// <summary>
    /// User profile DTO
    /// </summary>
    public class UserProfileDto
    {
        public string Id { get; set; } = string.Empty;
        public string Name { get; set; } = string.Empty;
        public string Username { get; set; } = string.Empty;
        public string MobileNumber { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public List<string> Roles { get; set; } = new();
    }

    /// <summary>
    /// Login response DTO
    /// </summary>
    public class LoginResponseDto
    {
        public string Token { get; set; } = string.Empty;
        public string Username { get; set; } = string.Empty;
        public List<string> Roles { get; set; } = new();
        public UserProfileDto User { get; set; } = new();
    }

    /// <summary>
    /// Dashboard stats DTO
    /// </summary>
    public class DashboardStatsDto
    {
        public int TotalEquipment { get; set; }
        public int TotalOrders { get; set; }
        public int PendingOrders { get; set; }
        public decimal TotalRevenue { get; set; }
    }

    /// <summary>
    /// Order item DTO
    /// </summary>
    public class OrderDto
    {
        public int OrderId { get; set; }
        public string Status { get; set; } = string.Empty;
        public DateTime OrderDate { get; set; }
        public string EquipmentName { get; set; } = string.Empty;
        public decimal TotalAmount { get; set; }
    }

    /// <summary>
    /// Subscription plan DTO
    /// </summary>
    public class SubscriptionPlanDto
    {
        public int PlanId { get; set; }
        public string PlanName { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public int DurationDays { get; set; }
    }
}
