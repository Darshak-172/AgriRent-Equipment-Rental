using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using AgriRent.Extensions;

namespace AgriRent.Models
{
    public class Payment
    {
        [Key]
        public int PaymentId { get; set; }

        [Required]
        public int UserId { get; set; }  // Foreign key to Users

        [Required]
        public int SubscriptionId { get; set; }  // Foreign key to UserSubscriptions

        [Required]
        public int PlanId { get; set; }  // For tracking which plan was paid for

        [Required]
        [StringLength(100)]
        public required string RazorpayPaymentId { get; set; }  // Razorpay payment ID

        [Required]
        [StringLength(100)]
        public required string RazorpayOrderId { get; set; }  // Razorpay order ID

        [Required]
        [StringLength(255)]
        public required string RazorpaySignature { get; set; }  // Payment signature

        [Required]
        [Range(0, 9999999)]
        [Column(TypeName = "decimal(18,2)")]
        public decimal Amount { get; set; }  // Payment amount in rupees

        [Required]
        [StringLength(20)]
        public string Currency { get; set; } = "INR";  // Currency code

        [Required]
        [StringLength(50)]
        public string Status { get; set; } = "Pending";  // Pending, Successful, Failed

        [Required]
        public DateTime PaymentDate { get; set; } = IstHelper.Now;  // When payment was made

        public string? ReceiptNumber { get; set; }  // Internal receipt reference

        public string? ErrorMessage { get; set; }  // If payment failed, error details

        public string? PaymentMethod { get; set; }  // Credit Card, Debit Card, UPI, Wallet, etc.

        public DateTime? VerificationDate { get; set; }  // When signature was verified

        public string? Notes { get; set; }  // Additional notes

        // Navigation Properties
        [System.Text.Json.Serialization.JsonIgnore]
        public virtual User? User { get; set; }

        [System.Text.Json.Serialization.JsonIgnore]
        public virtual UserSubscription? Subscription { get; set; }

        [System.Text.Json.Serialization.JsonIgnore]
        public virtual SubscriptionPlan? Plan { get; set; }
    }
}
