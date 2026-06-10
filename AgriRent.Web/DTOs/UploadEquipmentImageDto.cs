using System.ComponentModel.DataAnnotations;

namespace AgriRent.DTOs;

public class UploadEquipmentImageDto
{
    [Required(ErrorMessage = "Equipment ID is required")]
    public int EquipmentId { get; set; }

    [Required(ErrorMessage = "Image file is required")]
    public IFormFile? Image { get; set; }
}
