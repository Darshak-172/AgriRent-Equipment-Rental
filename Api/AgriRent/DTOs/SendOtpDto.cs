using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class SendOtpDto
    {
        [Required(ErrorMessage = "Mobile number is required")]
        [StringLength(13, MinimumLength = 13, ErrorMessage = "Mobile number must be in format +91XXXXXXXXXX")]
        [RegularExpression(@"^\+91\d{10}$", ErrorMessage = "Please enter a valid mobile number with +91 prefix")]
        public required string MobileNumber { get; set; }
    }
}
