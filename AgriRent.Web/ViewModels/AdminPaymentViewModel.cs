namespace AgriRent.ViewModels
{
    public class AdminPaymentViewModel
    {
        public int PaymentId { get; set; }
        public int UserId { get; set; }
        public string UserName { get; set; } = string.Empty;
        public string UserMobile { get; set; } = string.Empty;
        public string RazorpayPaymentId { get; set; } = string.Empty;
        public string RazorpayOrderId { get; set; } = string.Empty;
        public decimal Amount { get; set; }
        public string Currency { get; set; } = "INR";
        public string Status { get; set; } = string.Empty;
        public DateTime PaymentDate { get; set; }
        public string ReceiptNumber { get; set; } = string.Empty;
        public string PaymentMethod { get; set; } = string.Empty;
        public DateTime? VerificationDate { get; set; }
        public string PlanName { get; set; } = string.Empty;
        public string ErrorMessage { get; set; } = string.Empty;
    }
}
