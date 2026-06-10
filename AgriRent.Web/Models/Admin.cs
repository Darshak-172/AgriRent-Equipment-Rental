using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class Admin
    {
        [Key]
        public int AdminId { get; set; }

        [Required]
        public required string Username { get; set; }

        [Required]
        public required string PasswordHash { get; set; }

        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }
}
