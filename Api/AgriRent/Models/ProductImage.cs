using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class ProductImage
{
    [Key]
    public int ImageId { get; set; }

    public int ProductId { get; set; }
    public Product? Product { get; set; }

    [Required]
    [StringLength(255)]
    public required string ImageUrl { get; set; }
}
