using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class UpdateProfileDto
    {
        [Required(ErrorMessage = "Full name is required")]
        [StringLength(100, MinimumLength = 2, ErrorMessage = "Full name must be between 2 and 100 characters")]
        public required string FullName { get; set; }
    }
}
