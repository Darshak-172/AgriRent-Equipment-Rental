namespace AgriRent.ViewModels
{
    public class AdminComplaintViewModel
    {
        public int ComplaintId { get; set; }
        public int UserId { get; set; }
        public string FiledBy { get; set; } = "Unknown";
        public int TargetId { get; set; }
        public string TargetType { get; set; } = "Equipment";
        public string TargetName { get; set; } = "";
        public string? TargetImageUrl { get; set; }
        public string? OwnerOrSellerName { get; set; } = "N/A";
        public string Description { get; set; } = "";
        public string Status { get; set; } = "Pending";
        public string? ResolutionNote { get; set; }
        public int? ResolvedByUserId { get; set; }
        public string? ResolvedByName { get; set; }
        public string? ResolvedByRole { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime? ResolvedAt { get; set; }
    }
}
