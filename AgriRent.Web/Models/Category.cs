using System.ComponentModel.DataAnnotations;

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
        
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

        // Navigation property
        public ICollection<SubCategory> SubCategories { get; set; } = new List<SubCategory>();
    }
}
