using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class Role
    {
        [Key]
        public int RoleId { get; set; }
        public required string RoleName { get; set; }
    }
}