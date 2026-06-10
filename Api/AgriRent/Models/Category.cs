using System.ComponentModel.DataAnnotations;
using AgriRent.Extensions;

namespace AgriRent.Models
{
    public class Category
    {
        [Key]
        public int CategoryId { get; set; }

        [Required]
        [StringLength(100, MinimumLength = 2)]
        public required string Name { get; set; }

        [StringLength(20)]
        public string Status { get; set; } = "Active"; // Active/Inactive

        [StringLength(20)]
        public string Type { get; set; } = "Equipment"; // Equipment/Product

        public DateTime CreatedAt { get; set; } = IstHelper.Now;

        // Navigation property
        public ICollection<SubCategory> SubCategories { get; set; } = new List<SubCategory>();
    }
}
