namespace AgriRent.DTOs
{
    public class PaymentHistoryDto
    {
        public int PaymentId { get; set; }
        public int SubscriptionId { get; set; }
        public string? RazorpayPaymentId { get; set; }
        public string? RazorpayOrderId { get; set; }
        public decimal Amount { get; set; }
        public string? Currency { get; set; }
        public string? Status { get; set; }
        public DateTime PaymentDate { get; set; }
        public string? ReceiptNumber { get; set; }
        public string? PaymentMethod { get; set; }
        public DateTime? VerificationDate { get; set; }
        public string? PlanName { get; set; }
        public int DurationDays { get; set; }
        public string? ErrorMessage { get; set; }
    }
}
