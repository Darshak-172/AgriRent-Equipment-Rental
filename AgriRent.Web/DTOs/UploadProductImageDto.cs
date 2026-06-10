using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs;

public class UploadProductImageDto
{
    [Required(ErrorMessage = "Product ID is required")]
    public int ProductId { get; set; }

    [Required(ErrorMessage = "Image file is required")]
    public IFormFile? Image { get; set; }
}
