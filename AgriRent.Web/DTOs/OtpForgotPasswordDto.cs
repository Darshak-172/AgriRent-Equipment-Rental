using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class OtpForgotPasswordDto : VerifyOtpDto
    {
        [Required(ErrorMessage = "New password is required")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long")]
        public required string NewPassword { get; set; }
    }
}