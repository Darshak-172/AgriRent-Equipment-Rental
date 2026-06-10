using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using AgriRent.Extensions;

namespace AgriRent.Models
{
    public class Equipment
    {
        [Key]
        public int EquipmentId { get; set; }

        public int OwnerId { get; set; }
        [ForeignKey("OwnerId")]
        public User? Owner { get; set; }

        [Required]
        [StringLength(200, MinimumLength = 3)]
        public required string EquipmentName { get; set; }

        [Required]
        [StringLength(2000, MinimumLength = 10)]
        public required string Description { get; set; }

        [Range(0.01, 1000000)]
        public decimal HourlyPrice { get; set; }

        [Range(0.01, 1000000)]
        public decimal DailyPrice { get; set; }

        [Required]
        [StringLength(200)]
        public required string Location { get; set; }

        public int SubCategoryId { get; set; }
        [ForeignKey("SubCategoryId")]
        public SubCategory? SubCategory { get; set; }

        [StringLength(20)]
        public string Status { get; set; } = "Pending"; // For Admin approval
        public DateTime CreatedAt { get; set; } = IstHelper.Now;

        // Navigation property
        public ICollection<EquipmentImage> Images { get; set; } = new List<EquipmentImage>();


    }
}