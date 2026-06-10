using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class FirebaseTokenDto
    {
        [Required(ErrorMessage = "Firebase token is required")]
        [StringLength(500, MinimumLength = 10, ErrorMessage = "Invalid Firebase token")]
        public required string FirebaseToken { get; set; }
    }
}