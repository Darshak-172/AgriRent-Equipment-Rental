using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class Rating
{
    [Key]
    public int RatingId { get; set; }

    public int FromUserId { get; set; }
    public User? FromUser { get; set; }

    public int TargetId { get; set; } // EquipmentId or SellerId

    [Required]
    [StringLength(20)]
    public required string RatingType { get; set; } // "Equipment" or "Seller"

    [Range(1, 5)]
    public int RatingValue { get; set; }

    [StringLength(255)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
