using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class Address
{
    [Key]
    public int AddressId { get; set; }

    public int UserId { get; set; }
    public User? User { get; set; }

    [Required]
    [StringLength(50)]
    public required string State { get; set; }

    [Required]
    [StringLength(50)]
    public required string District { get; set; }

    [Required]
    [StringLength(50)]
    public required string City { get; set; }

    [StringLength(50)]
    public string? Village { get; set; }

    [Required]
    [StringLength(10)]
    public required string Pincode { get; set; }
}
