using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using AgriRent.Extensions;

namespace AgriRent.Models;

public class Order
{
    [Key]
    public int OrderId { get; set; }

    public int FarmerId { get; set; }
    public User? Farmer { get; set; }

    [Range(0, 10000000)]
    public decimal TotalAmount { get; set; }

    [StringLength(20)]
    public string OrderStatus { get; set; } = "Pending";

    public DateTime CreatedAt { get; set; } = IstHelper.Now;

    // Navigation property
    public ICollection<OrderItem> OrderItems { get; set; } = new List<OrderItem>();



    public string? DeliveryAddress { get; set; }

    public string? ContactNumber { get; set; }

    public string? PaymentId { get; set; }



    public string? PaymentStatus { get; set; }



    public DateTime? DeliveryDate { get; set; }
}
