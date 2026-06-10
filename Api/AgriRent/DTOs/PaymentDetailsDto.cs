namespace AgriRent.DTOs
{
    public class PaymentDetailsDto
    {
        public int PaymentId { get; set; }
        public int UserId { get; set; }
        public int SubscriptionId { get; set; }
        public int PlanId { get; set; }
        public string? RazorpayPaymentId { get; set; }
        public string? RazorpayOrderId { get; set; }
        public string? RazorpaySignature { get; set; }
        public decimal Amount { get; set; }
        public string? Currency { get; set; }
        public string? Status { get; set; }
        public DateTime PaymentDate { get; set; }
        public string? ReceiptNumber { get; set; }
        public string? ErrorMessage { get; set; }
        public string? PaymentMethod { get; set; }
        public DateTime? VerificationDate { get; set; }
        public string? Notes { get; set; }
        public string? PlanName { get; set; }
        public int DurationDays { get; set; }
        public int MaxEquipment { get; set; }
        public int MaxProducts { get; set; }
    }
}
