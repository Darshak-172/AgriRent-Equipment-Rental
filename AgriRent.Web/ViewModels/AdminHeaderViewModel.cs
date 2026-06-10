namespace AgriRent.ViewModels
{
    public class AdminHeaderViewModel
    {
        public string AdminName { get; set; } = "Admin";

        public int NotificationCount { get; set; }

        public required string CurrentDateTime { get; set; }
    }
}
