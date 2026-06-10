using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class CreateAdminDto
    {
        [Required(ErrorMessage = "Username is required")]
        [StringLength(50, MinimumLength = 3, ErrorMessage = "Username must be between 3 and 50 characters")]
        [RegularExpression(@"^[a-zA-Z0-9_]+$", ErrorMessage = "Username can only contain letters, numbers, and underscores")]
        public required string Username { get; set; }

        [Required(ErrorMessage = "Password is required")]
        [StringLength(100, MinimumLength = 8, ErrorMessage = "Password must be at least 8 characters long")]
        public required string Password { get; set; }

        [Required(ErrorMessage = "Secret key is required")]
        [StringLength(100, MinimumLength = 10, ErrorMessage = "Secret key must be at least 10 characters long")]
        public required string SecretKey { get; set; }
    }
}
