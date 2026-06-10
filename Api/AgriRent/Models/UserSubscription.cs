using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class UserSubscription
    {
        [Key] 
        public int SubscriptionId { get; set; }

        public int UserId { get; set; }
        public int PlanId { get; set; }
        
        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }

        public string PaymentStatus { get; set; } = "Pending"; // Paid / Pending
        public string Status { get; set; } = "Inactive";        // Active / Expired
        public required string PaymentId { get; set; }                   // Razorpay Trans ID

        // Navigation Property
        [System.Text.Json.Serialization.JsonIgnore]
        public virtual SubscriptionPlan? Plan { get; set; }
    }
}