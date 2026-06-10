-- Drop all existing tables in correct order (respecting foreign keys)
IF OBJECT_ID('dbo.OrderItems', 'U') IS NOT NULL DROP TABLE dbo.OrderItems;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.ProductImages', 'U') IS NOT NULL DROP TABLE dbo.ProductImages;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.EquipmentImages', 'U') IS NOT NULL DROP TABLE dbo.EquipmentImages;
IF OBJECT_ID('dbo.EquipmentBookings', 'U') IS NOT NULL DROP TABLE dbo.EquipmentBookings;
IF OBJECT_ID('dbo.EquipmentAvailability', 'U') IS NOT NULL DROP TABLE dbo.EquipmentAvailability;
IF OBJECT_ID('dbo.Equipments', 'U') IS NOT NULL DROP TABLE dbo.Equipments;
IF OBJECT_ID('dbo.SubCategories', 'U') IS NOT NULL DROP TABLE dbo.SubCategories;
IF OBJECT_ID('dbo.Categories', 'U') IS NOT NULL DROP TABLE dbo.Categories;
IF OBJECT_ID('dbo.Ratings', 'U') IS NOT NULL DROP TABLE dbo.Ratings;
IF OBJECT_ID('dbo.Complaints', 'U') IS NOT NULL DROP TABLE dbo.Complaints;
IF OBJECT_ID('dbo.Addresses', 'U') IS NOT NULL DROP TABLE dbo.Addresses;
IF OBJECT_ID('dbo.Notifications', 'U') IS NOT NULL DROP TABLE dbo.Notifications;
IF OBJECT_ID('dbo.UserSubscriptions', 'U') IS NOT NULL DROP TABLE dbo.UserSubscriptions;
IF OBJECT_ID('dbo.SubscriptionPlans', 'U') IS NOT NULL DROP TABLE dbo.SubscriptionPlans;
IF OBJECT_ID('dbo.UserRoles', 'U') IS NOT NULL DROP TABLE dbo.UserRoles;
IF OBJECT_ID('dbo.Roles', 'U') IS NOT NULL DROP TABLE dbo.Roles;
IF OBJECT_ID('dbo.Users', 'U') IS NOT NULL DROP TABLE dbo.Users;
IF OBJECT_ID('dbo.Admins', 'U') IS NOT NULL DROP TABLE dbo.Admins;
IF OBJECT_ID('dbo.__EFMigrationsHistory', 'U') IS NOT NULL DROP TABLE dbo.__EFMigrationsHistory;
GO
