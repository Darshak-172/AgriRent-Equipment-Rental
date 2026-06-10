namespace AgriRent.ViewModels
{
    public class SubscriptionUsersPageViewModel
    {
        public List<UserSubscriptionAdminViewModel> Subscriptions { get; set; } = new();
        public List<AdminPaymentViewModel> Payments { get; set; } = new();
    }
}
