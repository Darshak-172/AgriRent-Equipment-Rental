namespace AgriRent.ViewModels
{
    public class OwnerDashboardViewModel
    {
        // Owner Info
        public string OwnerName { get; set; } = "";
        public string OwnerMobile { get; set; } = "";

        // API error (null = no error)
        public string? ApiError { get; set; }
        // Equipment Stats
        public int TotalEquipments { get; set; }
        public int ApprovedCount { get; set; }
        public int PendingCount { get; set; }
        public int RejectedCount { get; set; }

        // Booking Stats
        public int TotalBookings { get; set; }
        public int PendingBookings { get; set; }
        public int AcceptedBookings { get; set; }
        public int RejectedBookings { get; set; }
        public decimal TotalEarnings { get; set; }

        // Subscription
        public string SubscriptionStatus { get; set; } = "None";
        public string SubscriptionPlanName { get; set; } = "";
        public DateTime? SubscriptionExpiry { get; set; }
        public int MaxEquipment { get; set; }

        // Lists
        public List<OwnerEquipmentItem> Equipments { get; set; } = new();
        public List<OwnerBookingItem> Bookings { get; set; } = new();
        
        // Complaints
        public List<OwnerComplaintItem> MyComplaints { get; set; } = new();
        public List<OwnerComplaintItem> IncomingComplaints { get; set; } = new();

        // Reviews
        public List<OwnerReviewItem> MyReviews { get; set; } = new();
        public List<OwnerReviewItem> IncomingReviews { get; set; } = new();
        public decimal AverageRating { get; set; }
    }

    public class OwnerEquipmentItem
    {
        public int EquipmentId { get; set; }
        public string EquipmentName { get; set; } = "";
        public string Description { get; set; } = "";
        public string Location { get; set; } = "";
        public decimal Price { get; set; }
        public string PriceType { get; set; } = "";
        public string Status { get; set; } = "";
        public string? ImageUrl { get; set; }
        public string? CategoryName { get; set; }
        public DateTime CreatedAt { get; set; }
    }

    public class OwnerBookingItem
    {
        public int BookingId { get; set; }
        public string EquipmentName { get; set; } = "";
        public string FarmerName { get; set; } = "";
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public decimal TotalPrice { get; set; }
        public string Status { get; set; } = "";
    }

    public class OwnerComplaintItem
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

    public class OwnerReviewItem
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
}
