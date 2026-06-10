using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs;

public class CreateOrderDto
{
    [Required(ErrorMessage = "Product ID is required")]
    [Range(1, int.MaxValue, ErrorMessage = "Invalid product ID")]
    public int ProductId { get; set; }

    [Required(ErrorMessage = "Quantity is required")]
    [Range(1, 100, ErrorMessage = "Quantity must be between 1 and 100")]
    public int Quantity { get; set; }

    [Required(ErrorMessage = "Delivery address is required")]
    [StringLength(200, MinimumLength = 10, ErrorMessage = "Address must be between 10 and 200 characters")]
    public required string DeliveryAddress { get; set; }

    [Required(ErrorMessage = "Contact number is required")]
    [StringLength(13, MinimumLength = 13, ErrorMessage = "Mobile number must be 13 characters (+91XXXXXXXXXX)")]
    [RegularExpression(@"^\+91\d{10}$", ErrorMessage = "Mobile number must be in +91XXXXXXXXXX format")]
    public required string ContactNumber { get; set; }
}
