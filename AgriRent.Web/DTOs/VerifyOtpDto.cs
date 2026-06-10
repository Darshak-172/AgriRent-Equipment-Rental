using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class VerifyOtpDto
    {
        [Required(ErrorMessage = "Mobile number is required")]
        [StringLength(13, MinimumLength = 13, ErrorMessage = "Mobile number must be in format +91XXXXXXXXXX")]
        [RegularExpression(@"^\+91\d{10}$", ErrorMessage = "Please enter a valid mobile number with +91 prefix")]
        public required string MobileNumber { get; set; }

        [Required(ErrorMessage = "Session ID is required")]
        [StringLength(100, ErrorMessage = "Session ID is invalid")]
        public required string SessionId { get; set; }

        [Required(ErrorMessage = "OTP is required")]
        [StringLength(6, MinimumLength = 6, ErrorMessage = "OTP must be exactly 6 digits")]
        [RegularExpression(@"^\d{6}$", ErrorMessage = "OTP must be a 6-digit number")]
        public required string Otp { get; set; }

        public bool IsRealOtp { get; set; }
    }
}