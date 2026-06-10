using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class AdminLoginDto
    {
        [Required(ErrorMessage = "Username is required")]
        [StringLength(50, MinimumLength = 3, ErrorMessage = "Username must be between 3 and 50 characters")]
        public required string Username { get; set; }

        [Required(ErrorMessage = "Password is required")]
        [StringLength(100, MinimumLength = 6, ErrorMessage = "Password must be at least 6 characters long")]
        public required string Password { get; set; }

        public bool RememberMe { get; set; }
    }
}
