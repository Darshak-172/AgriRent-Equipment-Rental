namespace AgriRent.ViewModels
{
    public class UserSubscriptionAdminViewModel
    {
        public int SubscriptionId { get; set; }
        public int UserId { get; set; }
        public string UserName { get; set; } = "";
        public string UserMobile { get; set; } = "";
        public int PlanId { get; set; }
        public string PlanName { get; set; } = "";
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public string Status { get; set; } = "";
        public string PaymentStatus { get; set; } = "";
        public string PaymentId { get; set; } = "";
    }
}
