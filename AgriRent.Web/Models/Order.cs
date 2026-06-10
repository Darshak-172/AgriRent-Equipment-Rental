using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

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

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation property
    public ICollection<OrderItem> OrderItems { get; set; } = new List<OrderItem>();

    // ⚠️ TEMPORARY COMPATIBILITY - Remove after migration
    [NotMapped]
    public Product? Product => OrderItems?.FirstOrDefault()?.Product;

    [NotMapped]
    public int ProductId
    {
        get => OrderItems?.FirstOrDefault()?.ProductId ?? 0;
        set
        {
            // Handled by OrderItems
        }
    }

    [NotMapped]
    public int Quantity
    {
        get => OrderItems?.FirstOrDefault()?.Quantity ?? 0;
        set
        {
            // Handled by OrderItems
        }
    }

    [NotMapped]
    public decimal UnitPrice
    {
        get => OrderItems?.FirstOrDefault()?.Price ?? 0;
        set
        {
            // Handled by OrderItems
        }
    }

    [NotMapped]
    public decimal TotalPrice
    {
        get => TotalAmount;
        set => TotalAmount = value;
    }

    [NotMapped]
    public string? DeliveryAddress { get; set; }

    [NotMapped]
    public string? ContactNumber { get; set; }

    [NotMapped]
    public string? PaymentId { get; set; }

    [NotMapped]
    public string Status
    {
        get => OrderStatus;
        set => OrderStatus = value;
    }

    [NotMapped]
    public string? PaymentStatus { get; set; }

    [NotMapped]
    public DateTime OrderDate
    {
        get => CreatedAt;
        set => CreatedAt = value;
    }

    [NotMapped]
    public DateTime? DeliveryDate { get; set; }
}
