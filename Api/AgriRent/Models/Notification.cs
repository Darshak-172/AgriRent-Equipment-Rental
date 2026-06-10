using System;
using System.ComponentModel.DataAnnotations;
using AgriRent.Extensions;

namespace AgriRent.Models
{
    public class Notification
    {
        [Key]
        public int Id { get; set; }

        public int? UserId { get; set; } // null = admin/global

        [Required]
        public required string Title { get; set; }

        [Required]
        public required string Message { get; set; }

        // e.g. "Booking", "Subscription", "Equipment", "Product", "General"
        public string Type { get; set; } = "General";

        public bool IsRead { get; set; } = false;

        public DateTime CreatedAt { get; set; } = IstHelper.Now;
    }
}
