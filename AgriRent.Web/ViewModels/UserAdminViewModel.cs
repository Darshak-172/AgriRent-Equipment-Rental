namespace AgriRent.ViewModels
{
    public class UserAdminViewModel
    {
        public int UserId { get; set; }
        public string FullName { get; set; } = string.Empty;
        public string MobileNumber { get; set; } = string.Empty;
        public string Status { get; set; } = "Active";
        public List<string> Roles { get; set; } = new();
        // Convenience: primary role for display
        public string Role => Roles.Count > 0 ? Roles[0] : "Unassigned";
    }
}
