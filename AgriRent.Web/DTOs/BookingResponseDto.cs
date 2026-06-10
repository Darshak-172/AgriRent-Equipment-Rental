namespace AgriRent.DTOs
{
    public class BookingResponseDto
    {
        
        public int BookingId { get; set; }
        public required string EquipmentName { get; set; }
        public required string OwnerName { get; set; }
        public required string FarmerName { get; set; }
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }
        public decimal TotalPrice { get; set; }
        public required string Status { get; set; }
    }
}