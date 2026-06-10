using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class SubscriptionPlan
    {
        [Key] 
        public int PlanId { get; set; }
        
        [Required]
        [StringLength(100, MinimumLength = 3)]
        public required string PlanName { get; set; }

        [Range(1, 3650)] // 1 day to 10 years
        public int DurationDays { get; set; }   // 30, 90, 180

        [Range(0, 1000000)]
        public decimal Price { get; set; }      // ₹99, ₹199, etc.

        [Range(0, 1000)]
        public int MaxEquipment { get; set; }   // equipment posting limit

        [Range(0, 1000)]
        public int MaxProducts { get; set; }    // product posting limit
        
        [StringLength(20)]
        public string Status { get; set; } = "Active"; // Active/Inactive
    }
}