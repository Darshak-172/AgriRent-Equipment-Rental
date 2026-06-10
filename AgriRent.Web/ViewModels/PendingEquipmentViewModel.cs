namespace AgriRent.ViewModels
{
    public class PendingEquipmentViewModel
    {
        public int EquipmentId { get; set; }
        public string? EquipmentName { get; set; }
        public string? Description { get; set; }
        
        // Price Properties
        public decimal HourlyPrice { get; set; }
        public decimal DailyPrice { get; set; }
        
        // Computed for backward compatibility with views
        public decimal Price => DailyPrice > 0 ? DailyPrice : HourlyPrice;
        public string PriceType => DailyPrice > 0 ? "Day" : "Hour";

        public string? Location { get; set; }
        public string? Status { get; set; }
        public string? OwnerName { get; set; }
        public string? OwnerMobile { get; set; }
        public string? ImageUrl { get; set; }
    }
}
