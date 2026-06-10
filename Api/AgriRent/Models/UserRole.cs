using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class UserRole
    {
        [Key]
        public int UserRoleId { get; set; }

        public int UserId { get; set; }
        public int RoleId { get; set; }
    }
}