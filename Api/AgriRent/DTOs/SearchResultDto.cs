namespace AgriRent.DTOs
{
    public class SearchResultDto
    {
        public int EquipmentId { get; set; }
        public int ProductId { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public decimal? HourlyPrice { get; set; }
        public decimal? DailyPrice { get; set; }
        public string PriceType { get; set; } = string.Empty;
        public string Location { get; set; } = string.Empty;
        public string? ImageUrl { get; set; }
        public string Type { get; set; } = string.Empty; // "Equipment" or "Product"
        public string CategoryName { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
    }
}
