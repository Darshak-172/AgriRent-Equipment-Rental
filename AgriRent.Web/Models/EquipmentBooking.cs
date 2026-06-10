using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models
{
    public class EquipmentBooking
    {
        [Key]                      // 👈 Add this line to fix the error
        public int BookingId { get; set; }

        public int EquipmentId { get; set; }
        public int FarmerId { get; set; }

        public DateTime StartDate { get; set; }
        public DateTime EndDate { get; set; }

        public decimal TotalPrice { get; set; }
        public string Status { get; set; } = "Pending";
        public DateTime CreatedAt { get; set; } = DateTime.Now;

        public Equipment? Equipment { get; set; }
        public User? Farmer { get; set; }
    }
}