using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class CheckAvailabilityDto
    {
        [Required(ErrorMessage = "Equipment ID is required")]
        [Range(1, int.MaxValue, ErrorMessage = "Please select a valid equipment")]
        public int EquipmentId { get; set; }

        [Required(ErrorMessage = "Start date is required")]
        public DateTime StartDate { get; set; }  // booking start

        [Required(ErrorMessage = "End date is required")]
        public DateTime EndDate { get; set; }    // booking end
    }
}