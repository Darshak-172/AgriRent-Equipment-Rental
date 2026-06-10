namespace AgriRent.ViewModels
{
    public class SellerDashboardViewModel
    {
        public string SellerName { get; set; } = "Seller";
        public string SellerMobile { get; set; } = "";
        
        public int TotalProducts { get; set; }
        public int ActiveProducts { get; set; }
        public int PendingProducts { get; set; }
        public int RejectedProducts { get; set; }
        
        public int TotalOrders { get; set; }
        public int PendingOrders { get; set; }
        public int AcceptedOrders { get; set; }
        public int RejectedOrders { get; set; }
        public decimal TotalEarnings { get; set; }

        public string SubscriptionStatus { get; set; } = "None"; // None, Active, Expired
        public string? SubscriptionPlanName { get; set; }
        public DateTime? SubscriptionExpiry { get; set; }

        public List<SellerProductItem> Products { get; set; } = new();
        public List<SellerOrderItem> Orders { get; set; } = new();

        // Complaints
        public List<SellerComplaintItem> MyComplaints { get; set; } = new();
        public List<SellerComplaintItem> IncomingComplaints { get; set; } = new();

        // Reviews
        public List<SellerReviewItem> MyReviews { get; set; } = new();
        public List<SellerReviewItem> IncomingReviews { get; set; } = new();
        public decimal AverageRating { get; set; }
    }

    public class SellerComplaintItem
    {
        public int ComplaintId { get; set; }
        public int TargetId { get; set; }
        public string TargetType { get; set; } = "";
        public string TargetName { get; set; } = "";
        public string FiledBy { get; set; } = "";
        public string Description { get; set; } = "";
        public string Status { get; set; } = "";
        public string? ResolutionNote { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime? ResolvedAt { get; set; }
    }

    public class SellerReviewItem
    {
        public int RatingId { get; set; }
        public int TargetId { get; set; }
        public string TargetType { get; set; } = "";
        public string TargetName { get; set; } = "";
        public string UserName { get; set; } = "";
        public int RatingValue { get; set; }
        public string? Comment { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    public class SellerProductItem
    {
        public int ProductId { get; set; }
        public string ProductName { get; set; } = "";
        public string Description { get; set; } = "";
        public string CategoryName { get; set; } = "";
        public int? CategoryId { get; set; }
        public int? SubCategoryId { get; set; }
        public decimal Price { get; set; }
        public int Stock { get; set; }
        public string Unit { get; set; } = "";
        public string Location { get; set; } = "";
        public string? ImageUrl { get; set; }
        public string Status { get; set; } = "Pending";
        public DateTime CreatedAt { get; set; }
    }

    public class SellerOrderItem
    {
        public int OrderId { get; set; }
        public int? ProductId { get; set; }
        public string ProductName { get; set; } = "";
        public string ProductDescription { get; set; } = "";
        public string ProductUnit { get; set; } = "";
        public string ProductLocation { get; set; } = "";
        public decimal ProductPrice { get; set; }
        public string? ProductImageUrl { get; set; }
        public List<string> ProductImages { get; set; } = new();
        public string FarmerName { get; set; } = "";
        public string DeliveryAddress { get; set; } = "";
        public string ContactNumber { get; set; } = "";
        public int Quantity { get; set; }
        public decimal UnitPrice { get; set; }
        public decimal TotalPrice { get; set; }
        public string Status { get; set; } = "Pending"; // Pending, Processing, Shipped, Delivered, Cancelled
        public string PaymentStatus { get; set; } = "Pending";
        public DateTime OrderDate { get; set; }
    }
}
