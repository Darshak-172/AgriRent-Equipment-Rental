using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class AddEquipmentDto
    {
        [Required(ErrorMessage = "Equipment name is required")]
        [StringLength(200, MinimumLength = 3, ErrorMessage = "Equipment name must be between 3 and 200 characters")]
        public required string EquipmentName { get; set; }

        [Required(ErrorMessage = "Description is required")]
        [StringLength(2000, MinimumLength = 10, ErrorMessage = "Description must be between 10 and 2000 characters")]
        public required string Description { get; set; }

        [Required(ErrorMessage = "Price type is required")]
        [RegularExpression(@"^(Hourly|Daily)$", ErrorMessage = "Price type must be either 'Hourly' or 'Daily'")]
        public required string PriceType { get; set; } // Hourly / Daily

        [Required(ErrorMessage = "Price is required")]
        [Range(0.01, 1000000, ErrorMessage = "Price must be between 0.01 and 1,000,000")]
        public decimal Price { get; set; }

        [Required(ErrorMessage = "Location is required")]
        [StringLength(200, MinimumLength = 2, ErrorMessage = "Location must be between 2 and 200 characters")]
        public required string Location { get; set; }

        [Range(1, int.MaxValue, ErrorMessage = "Please select a valid subcategory")]
        public int? SubCategoryId { get; set; }
    }
}