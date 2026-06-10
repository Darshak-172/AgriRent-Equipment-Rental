using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class SubCategoryDto
    {
        public int SubCategoryId { get; set; }

        [Required]
        public int CategoryId { get; set; }

        [Required]
        [StringLength(100)]
        public required string SubCategoryName { get; set; }

        [StringLength(20)]
        public string Status { get; set; } = "Active";
    }
}
