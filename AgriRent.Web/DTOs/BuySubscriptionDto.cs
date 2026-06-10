using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class BuySubscriptionDto
    {
        [Required(ErrorMessage = "Plan ID is required")]
        [Range(1, int.MaxValue, ErrorMessage = "Please select a valid subscription plan")]
        public int PlanId { get; set; }

        [Required(ErrorMessage = "Payment ID is required")]
        [StringLength(200, MinimumLength = 5, ErrorMessage = "Invalid payment ID")]
        public required string PaymentId { get; set; } // Razorpay Payment ID from App

        [Required(ErrorMessage = "Order ID is required")]
        [StringLength(200, MinimumLength = 5, ErrorMessage = "Invalid order ID")]
        public required string OrderId { get; set; }   // Razorpay Order ID

        [Required(ErrorMessage = "Signature is required")]
        public required string Signature { get; set; } // Razorpay Signature
    }
}