using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class Complaint
{
    [Key]
    public int ComplaintId { get; set; }

    // Who filed the complaint
    public int UserId { get; set; }
    public User? User { get; set; }

    // What it's about
    public int TargetId { get; set; }             // EquipmentId or ProductId

    [StringLength(20)]
    public string TargetType { get; set; } = "Equipment"; // "Equipment" or "Product"

    [Required]
    [StringLength(1000)]
    public required string Description { get; set; }

    [StringLength(20)]
    public string Status { get; set; } = "Pending"; // Pending, Resolved, Rejected

    [StringLength(1000)]
    public string? ResolutionNote { get; set; }

    // Who resolved the complaint (Admin, Owner, or Seller)
    public int? ResolvedByUserId { get; set; }
    public User? ResolvedByUser { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? ResolvedAt { get; set; }
}
