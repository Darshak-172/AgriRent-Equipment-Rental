namespace AgriRent.ViewModels
{
    public class PendingProductViewModel
    {
        public int ProductId { get; set; }
        public string? ProductName { get; set; }
        public string? Description { get; set; }
        public decimal Price { get; set; }
        public int Stock { get; set; }
        public string? Unit { get; set; }
        public string? Location { get; set; }
        public string? Status { get; set; }
        public string? OwnerName { get; set; }
        public string? OwnerMobile { get; set; }
        public string? ImageUrl { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
