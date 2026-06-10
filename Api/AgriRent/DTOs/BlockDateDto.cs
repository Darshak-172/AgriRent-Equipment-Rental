using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs
{
    public class BlockDateDto
    {
        [Required(ErrorMessage = "Equipment ID is required")]
        [Range(1, int.MaxValue, ErrorMessage = "Please select a valid equipment")]
        public int EquipmentId { get; set; }          // Equipment to block

        [Required(ErrorMessage = "Start date is required")]
        public DateTime StartDate { get; set; }       // Not available from

        [Required(ErrorMessage = "End date is required")]
        public DateTime EndDate { get; set; }         // Not available till

        [StringLength(500, ErrorMessage = "Reason cannot exceed 500 characters")]
        public string? Reason { get; set; }           // Optional: Maintenance / Owner Out / Repair
    }
}