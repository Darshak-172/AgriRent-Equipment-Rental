using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class RefreshTokenDto
    {
        [Required(ErrorMessage = "Refresh token is required")]
        [StringLength(500, MinimumLength = 10, ErrorMessage = "Invalid refresh token")]
        public required string RefreshToken { get; set; }
    }
}