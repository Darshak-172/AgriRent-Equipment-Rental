namespace AgriRent.ViewModels
{
    public class AdminReviewViewModel
    {
        public int RatingId { get; set; }
        public int FromUserId { get; set; }
        public string UserName { get; set; } = "";
        public int TargetId { get; set; }
        public string TargetType { get; set; } = "";
        public string TargetName { get; set; } = "";
        public string? TargetImageUrl { get; set; }
        public string? OwnerOrSellerName { get; set; }
        public int RatingValue { get; set; }
        public string? Comment { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
