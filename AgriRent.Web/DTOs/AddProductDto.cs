using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs;

public class AddProductDto
{
    [Required(ErrorMessage = "Product name is required")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Product name must be between 3 and 100 characters")]
    public required string ProductName { get; set; }

    [Required(ErrorMessage = "Description is required")]
    [StringLength(500, MinimumLength = 10, ErrorMessage = "Description must be between 10 and 500 characters")]
    public required string Description { get; set; }

    [Required(ErrorMessage = "Price is required")]
    [Range(1, 1000000, ErrorMessage = "Price must be between ₹1 and ₹10,00,000")]
    public decimal Price { get; set; }

    [Required(ErrorMessage = "Stock quantity is required")]
    [Range(1, 10000, ErrorMessage = "Stock must be between 1 and 10,000 units")]
    public int Stock { get; set; }

    [Required(ErrorMessage = "Category is required")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Category must be between 3 and 100 characters")]
    public required string Category { get; set; }

    [Required(ErrorMessage = "Unit is required")]
    [StringLength(50, MinimumLength = 1, ErrorMessage = "Unit must be between 1 and 50 characters")]
    public required string Unit { get; set; }

    [Required(ErrorMessage = "Location is required")]
    [StringLength(100, MinimumLength = 3, ErrorMessage = "Location must be between 3 and 100 characters")]
    public required string Location { get; set; }

    [Required(ErrorMessage = "Image URL is required")]
    [Url(ErrorMessage = "Invalid image URL format")]
    public required string ImageUrl { get; set; }
}
