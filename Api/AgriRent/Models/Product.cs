using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using AgriRent.Extensions;

namespace AgriRent.Models
{
    public class Product
    {
        [Key]
        public int ProductId { get; set; }

        public int SellerId { get; set; }
        public User? Seller { get; set; }

        [Required]
        [StringLength(200, MinimumLength = 3)]
        public required string ProductName { get; set; }

        [Required]
        [StringLength(2000, MinimumLength = 10)]
        public required string Description { get; set; }
        
        [Range(0.01, 1000000)]
        public decimal Price { get; set; }

        [Range(0, 1000000)]
        public int Stock { get; set; }

        [Required]
        [StringLength(50)]
        public required string Unit { get; set; } // kg, litre, piece, etc.

        [Required]
        [StringLength(200)]
        public required string Location { get; set; }

        public int SubCategoryId { get; set; }
        public SubCategory? SubCategory { get; set; }
        
        [StringLength(20)]
        public string Status { get; set; } = "Pending"; // Pending, Approved, Rejected
        public DateTime CreatedAt { get; set; } = IstHelper.Now;

        // Navigation properties
        public ICollection<ProductImage> Images { get; set; } = new List<ProductImage>();


    }
}
