using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AgriRent.Models;

public class EquipmentImage
{
    [Key]
    public int ImageId { get; set; }

    public int EquipmentId { get; set; }
    [ForeignKey("EquipmentId")]
    public Equipment? Equipment { get; set; }

    [Required]
    [StringLength(255)]
    public required string ImageUrl { get; set; }
}
