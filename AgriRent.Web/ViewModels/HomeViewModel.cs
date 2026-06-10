using AgriRent.Models;

namespace AgriRent.ViewModels
{
    public class HomeViewModel
    {
        public List<Equipment> Equipments { get; set; } = new List<Equipment>();
        public List<Product> Products { get; set; } = new List<Product>();
        public List<Category> Categories { get; set; } = new List<Category>();
        public List<string> Locations { get; set; } = new List<string>();
    }
}
