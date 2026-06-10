namespace AgriRent.ViewModels
{
    public class AdminDashboardViewModel
    {
        // USERS
        public int TotalUsers { get; set; }
        public int TotalFarmers { get; set; }
        public int TotalEquipmentOwners { get; set; }
        public int TotalSellers { get; set; }

        // EQUIPMENTS
        public int PendingEquipments { get; set; }
        public int ActiveEquipments { get; set; }
        public int PausedEquipments { get; set; }
        public int RejectedEquipments { get; set; }

        // PRODUCTS
        public int PendingProducts { get; set; }
        public int ActiveProducts { get; set; }
        public int PausedProducts { get; set; }
        public int RejectedProducts { get; set; }

        public int TotalEquipments { get; set; }
        public int TotalProducts { get; set; }
        public int ActiveSubscriptions { get; set; }
        public int InactiveSubscriptions { get; set; }
        public int TotalSubscriptions { get; set; }
        public int UsersWithSubscription { get; set; }

        // CATEGORIES
        public int ActiveCategories { get; set; }
        public int InactiveCategories { get; set; }
        public int TotalCategories { get; set; }

        // COMPLAINTS
        public int PendingComplaints { get; set; }
        public int ResolvedComplaints { get; set; }
        public int RejectedComplaints { get; set; }
        public int TotalComplaints { get; set; }

        // REVIEWS
        public int TotalReviews { get; set; }
        public int EquipmentReviews { get; set; }
        public int ProductReviews { get; set; }
    }
}
