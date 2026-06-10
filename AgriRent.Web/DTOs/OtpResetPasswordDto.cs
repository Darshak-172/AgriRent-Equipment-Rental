using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class OtpResetPasswordDto : VerifyOtpDto
    {
        [Required(ErrorMessage = "Old password is required")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long")]
        public required string OldPassword { get; set; }

        [Required(ErrorMessage = "New password is required")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long")]
        public required string NewPassword { get; set; }
    }
}