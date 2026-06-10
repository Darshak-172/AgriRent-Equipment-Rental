using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class User
    {
        [Key]
        public int UserId { get; set; }

        [Required]
        [StringLength(100, MinimumLength = 2)]
        public required string FullName { get; set; }

        [Required]
        [StringLength(13, MinimumLength = 13)]
        public required string MobileNumber { get; set; }

        [Required]
        [StringLength(256)]
        public required string PasswordHash { get; set; }

        [StringLength(20)]
        public string Status { get; set; } = "Active";

        [StringLength(500)]
        public string? RefreshToken { get; set; }

        public DateTime? RefreshTokenExpiry { get; set; }
       
    }
}