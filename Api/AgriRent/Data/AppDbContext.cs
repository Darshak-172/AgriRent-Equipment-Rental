using AgriRent.Models;
using Microsoft.EntityFrameworkCore;

namespace AgriRent.Data
{
    public class AppDbContext : DbContext
    {
        public AppDbContext(DbContextOptions<AppDbContext> options)
            : base(options)
        {
        }

        // User & Auth
        public DbSet<User> Users { get; set; }
        public DbSet<Role> Roles { get; set; }
        public DbSet<UserRole> UserRoles { get; set; }
        public DbSet<Admin> Admins { get; set; }

        // Subscription
        public DbSet<SubscriptionPlan> SubscriptionPlans { get; set; }
        public DbSet<UserSubscription> UserSubscriptions { get; set; }
        public DbSet<Payment> Payments { get; set; }

        // Equipment
        public DbSet<Equipment> Equipments { get; set; }
        public DbSet<Category> Categories { get; set; }
        public DbSet<SubCategory> SubCategories { get; set; }
        public DbSet<EquipmentImage> EquipmentImages { get; set; }
        public DbSet<EquipmentAvailability> EquipmentAvailability { get; set; }
        public DbSet<EquipmentBooking> EquipmentBookings { get; set; }

        // Products & Orders
        public DbSet<Product> Products { get; set; }
        public DbSet<ProductImage> ProductImages { get; set; }
        public DbSet<Order> Orders { get; set; }
        public DbSet<OrderItem> OrderItems { get; set; }

        // Ratings & Complaints
        public DbSet<Rating> Ratings { get; set; }
        public DbSet<Complaint> Complaints { get; set; }
        public DbSet<Address> Addresses { get; set; }

        // Other
        public DbSet<Notification> Notifications { get; set; }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            // ============================
            // Indexes for Performance
            // ============================

            modelBuilder.Entity<UserRole>()
                .Property(ur => ur.UserRoleId)
                .ValueGeneratedOnAdd();

            modelBuilder.Entity<UserRole>()
                .HasIndex(ur => new { ur.UserId, ur.RoleId })
                .IsUnique();

            modelBuilder.Entity<UserSubscription>()
                .Property(us => us.SubscriptionId)
                .ValueGeneratedOnAdd();

            modelBuilder.Entity<UserSubscription>()
                .HasIndex(us => us.UserId);

            // ============================
            // Fix Multiple Cascade Paths
            // ============================

            modelBuilder.Entity<Complaint>()
                .HasOne(c => c.User)
                .WithMany()
                .HasForeignKey(c => c.UserId)
                .OnDelete(DeleteBehavior.NoAction);

            modelBuilder.Entity<Complaint>()
                .HasOne(c => c.ResolvedByUser)
                .WithMany()
                .HasForeignKey(c => c.ResolvedByUserId)
                .OnDelete(DeleteBehavior.NoAction);

            modelBuilder.Entity<EquipmentBooking>()
                .HasOne(b => b.Equipment)
                .WithMany()
                .HasForeignKey(b => b.EquipmentId)
                .OnDelete(DeleteBehavior.NoAction);

            modelBuilder.Entity<EquipmentBooking>()
                .HasOne(b => b.Farmer)
                .WithMany()
                .HasForeignKey(b => b.FarmerId)
                .OnDelete(DeleteBehavior.NoAction);

            modelBuilder.Entity<OrderItem>()
                .HasOne(oi => oi.Product)
                .WithMany()
                .HasForeignKey(oi => oi.ProductId)
                .OnDelete(DeleteBehavior.NoAction);

            modelBuilder.Entity<OrderItem>()
                .HasOne(oi => oi.Order)
                .WithMany(o => o.OrderItems)
                .HasForeignKey(oi => oi.OrderId)
                .OnDelete(DeleteBehavior.Cascade);

            modelBuilder.Entity<Order>()
                .HasOne(o => o.Farmer)
                .WithMany()
                .HasForeignKey(o => o.FarmerId)
                .OnDelete(DeleteBehavior.NoAction);

            // ============================
            // Decimal Precision
            // ============================

            modelBuilder.Entity<Equipment>()
                .Property(e => e.HourlyPrice)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<Equipment>()
                .Property(e => e.DailyPrice)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<EquipmentBooking>()
                .Property(b => b.TotalPrice)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<SubscriptionPlan>()
                .Property(s => s.Price)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<Product>()
                .Property(p => p.Price)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<Order>()
                .Property(o => o.TotalAmount)
                .HasColumnType("decimal(18,2)");

            modelBuilder.Entity<OrderItem>()
                .Property(oi => oi.Price)
                .HasColumnType("decimal(18,2)");

            // ============================
            // Indexes
            // ============================

            modelBuilder.Entity<EquipmentAvailability>()
                .HasIndex(ea => new { ea.EquipmentId, ea.StartDate, ea.EndDate });





            modelBuilder.Entity<Equipment>()
                .HasIndex(e => e.OwnerId);

            modelBuilder.Entity<Product>()
                .HasIndex(p => p.SellerId);



            modelBuilder.Entity<EquipmentBooking>()
                .HasIndex(b => b.FarmerId);

            modelBuilder.Entity<EquipmentBooking>()
                .HasIndex(b => new { b.EquipmentId, b.Status });

            modelBuilder.Entity<Payment>()
                .HasIndex(p => p.UserId);

            modelBuilder.Entity<Order>()
                .HasIndex(o => o.FarmerId);

            // One review per user per item (Equipment or Product)
            modelBuilder.Entity<Rating>()
                .HasIndex(r => new { r.FromUserId, r.TargetId, r.RatingType })
                .IsUnique();
        }
    }
}
