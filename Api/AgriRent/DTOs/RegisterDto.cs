using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class RegisterDto
    {
        [Required(ErrorMessage = "Full name is required")]
        [StringLength(100, MinimumLength = 2, ErrorMessage = "Full name must be between 2 and 100 characters")]
        // [RegularExpression(@"^[a-zA-Z\s]+$", ErrorMessage = "Full name can only contain letters and spaces")]
        public required string FullName { get; set; }

        [Required(ErrorMessage = "Mobile number is required")]
        [StringLength(13, MinimumLength = 13, ErrorMessage = "Mobile number must be in format +91XXXXXXXXXX")]
        [RegularExpression(@"^\+91\d{10}$", ErrorMessage = "Please enter a valid mobile number with +91 prefix")]
        public required string MobileNumber { get; set; }

        [Required(ErrorMessage = "Password is required")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long")]
        public required string Password { get; set; }
    }
}