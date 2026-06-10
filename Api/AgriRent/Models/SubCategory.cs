using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class SubCategory
{
    [Key]
    public int SubCategoryId { get; set; }

    public int CategoryId { get; set; }
    public Category? Category { get; set; }

    [Required]
    [StringLength(100)]
    public required string SubCategoryName { get; set; }

    [StringLength(20)]
    public string Status { get; set; } = "Active";

    // Navigation properties
    public ICollection<Equipment> Equipments { get; set; } = new List<Equipment>();
    public ICollection<Product> Products { get; set; } = new List<Product>();
}
