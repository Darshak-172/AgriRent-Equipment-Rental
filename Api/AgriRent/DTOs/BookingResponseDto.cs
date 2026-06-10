namespace AgriRent.DTOs
{
    public class BookingResponseDto
    {
        
        public int BookingId { get; set; }
        public int EquipmentId { get; set; }
        public required string EquipmentName { get; set; }
        public string? EquipmentImageUrl { get; set; }
        public required string EquipmentLocation { get; set; }
        public decimal EquipmentHourlyPrice { get; set; }
        public decimal EquipmentDailyPrice { get; set; }
        public required string OwnerName { get; set; }
        public required string OwnerMobile { get; set; }
        public required string FarmerName { get; set; }
        public required string FarmerMobile { get; set; }
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public decimal TotalPrice { get; set; }
        public required string Status { get; set; }
    }
}