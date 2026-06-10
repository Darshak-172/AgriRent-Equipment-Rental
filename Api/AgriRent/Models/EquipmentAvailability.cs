using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AgriRent.Models
{
    public class EquipmentAvailability
    {
        [Key] // 👈 THIS FIXES THE ERROR
        [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
        public int AvailabilityId { get; set; }

        public int EquipmentId { get; set; }
        public DateTime StartDate { get; set; }  
        public DateTime EndDate { get; set; }    
        public string Reason { get; set; } = "Unavailable";

        public Equipment? Equipment { get; set; }
    }
}