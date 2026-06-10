using System.ComponentModel.DataAnnotations;

namespace AgriRent.Models;

public class OrderItem
{
    [Key]
    public int OrderItemId { get; set; }

    public int OrderId { get; set; }
    public Order? Order { get; set; }

    public int ProductId { get; set; }
    public Product? Product { get; set; }

    [Range(1, 10000)]
    public int Quantity { get; set; }

    [Range(0, 1000000)]
    public decimal Price { get; set; }
}
