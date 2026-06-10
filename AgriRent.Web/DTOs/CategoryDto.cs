using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class CategoryDto
    {
        public int CategoryId { get; set; }

        [Required]
        [StringLength(100, MinimumLength = 2)]
        public required string Name { get; set; }


        [StringLength(20)]
        public string Status { get; set; } = "Active";

        [StringLength(20)]
        public string Type { get; set; } = "Equipment"; // Equipment/Product

        public List<SubCategoryDto> SubCategories { get; set; } = new List<SubCategoryDto>();
    }
}
