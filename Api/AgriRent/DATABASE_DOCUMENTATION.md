# 📊 AgriRent Database Documentation

**Database Name**: `harmish_agrirent`  
**Server**: `sql.bsite.net\MSSQL2016`  
**Database Type**: SQL Server  
**Normalization**: 3NF (Third Normal Form)  
**Created**: February 2, 2026  
**Version**: 1.0

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Entity Relationship Diagram](#entity-relationship-diagram)
3. [User Management Tables](#user-management-tables)
4. [Subscription Management Tables](#subscription-management-tables)
5. [Category & Classification Tables](#category--classification-tables)
6. [Equipment Management Tables](#equipment-management-tables)
7. [Product & Order Tables](#product--order-tables)
8. [Rating & Feedback Tables](#rating--feedback-tables)
9. [Utility Tables](#utility-tables)
10. [Indexes & Performance](#indexes--performance)
11. [Foreign Key Relationships](#foreign-key-relationships)

---

## 🎯 Overview

AgriRent is a comprehensive platform for agricultural equipment rental, product marketplace, and subscription-based services. The database supports:

- **Multi-role user system** (Farmer, Equipment Owner, Seller, Admin)
- **Subscription-based listing** (Equipment & Product limits based on plan)
- **Equipment rental** (Hourly & Daily booking)
- **Product marketplace** (With order management)
- **Admin approval workflow** (For equipment and products)
- **Rating & review system**
- **Complaint management**
- **Multi-image support**

**Total Tables**: 20  
**Total Relationships**: 19 Foreign Keys

---

## 🗺️ Entity Relationship Diagram

```
┌─────────────┐
│   USERS     │──────┐
└─────────────┘      │
       │             │
       ├─────────────┼──────────────┐
       │             │              │
       ▼             ▼              ▼
┌──────────┐  ┌──────────────┐  ┌──────────────┐
│UserRoles │  │UserSubscrip. │  │  Addresses   │
└──────────┘  └──────────────┘  └──────────────┘
       │             │
       ▼             ▼
┌──────────┐  ┌──────────────┐
│  Roles   │  │Subscr.Plans  │
└──────────┘  └──────────────┘

┌─────────────┐
│ Categories  │
└─────────────┘
       │
       ▼
┌──────────────┐
│SubCategories │
└──────────────┘
       │
       ├──────────────┬──────────────┐
       ▼              ▼
┌──────────────┐  ┌──────────────┐
│  Equipments  │  │   Products   │
└──────────────┘  └──────────────┘
       │              │
       ├──────┐       ├──────┐
       ▼      ▼       ▼      ▼
  ┌────────┐ ┌────────────┐ ┌────────────┐
  │Equip.  │ │Equipment   │ │Product     │
  │Bookings│ │Images      │ │Images      │
  └────────┘ └────────────┘ └────────────┘
       │              │
       ▼              ▼
  ┌─────────────┐ ┌──────────┐
  │Equip.Avail. │ │  Orders  │
  └─────────────┘ └──────────┘
                       │
                       ▼
                  ┌──────────┐
                  │OrderItems│
                  └──────────┘
```

---

## 👥 User Management Tables

### 1. **Users**

**Purpose**: Stores all user accounts (Farmers, Equipment Owners, Sellers)

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `UserId` | `INT` | PRIMARY KEY, IDENTITY | Unique user identifier |
| `FullName` | `NVARCHAR(100)` | NOT NULL | User's full name |
| `MobileNumber` | `NVARCHAR(13)` | NOT NULL, UNIQUE | International format (+91XXXXXXXXXX) |
| `PasswordHash` | `NVARCHAR(256)` | NOT NULL | SHA-256 hashed password |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Active' | Active, Blocked, Suspended |
| `RefreshToken` | `NVARCHAR(500)` | NULL | JWT refresh token |
| `RefreshTokenExpiry` | `DATETIME2` | NULL | Token expiration date |

**Indexes**:
- PRIMARY KEY on `UserId`
- UNIQUE on `MobileNumber`

**Sample Data**:
```sql
INSERT INTO Users (FullName, MobileNumber, PasswordHash, Status)
VALUES ('John Farmer', '+919876543210', 'hashed_password', 'Active');
```

---

### 2. **Roles**

**Purpose**: Defines available user roles in the system

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `RoleId` | `INT` | PRIMARY KEY, IDENTITY | Unique role identifier |
| `RoleName` | `NVARCHAR(MAX)` | NOT NULL | Role name |

**Default Roles**:
```sql
INSERT INTO Roles (RoleName) VALUES 
('Farmer'),            -- Can rent equipment & buy products
('EquipmentOwner'),    -- Can list equipment for rent
('Seller');            -- Can sell products
```

**Note**: Users can have multiple roles simultaneously via `UserRoles` table.

---

### 3. **UserRoles**

**Purpose**: Maps users to their roles (Many-to-Many relationship)

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `UserRoleId` | `INT` | PRIMARY KEY, IDENTITY | Unique mapping identifier |
| `UserId` | `INT` | NOT NULL, FK → Users | Reference to user |
| `RoleId` | `INT` | NOT NULL, FK → Roles | Reference to role |

**Indexes**:
- PRIMARY KEY on `UserRoleId`
- UNIQUE INDEX on `(UserId, RoleId)` - Prevents duplicate role assignments

**Foreign Keys**:
- `UserId` → `Users(UserId)`
- `RoleId` → `Roles(RoleId)`

**Example**:
```sql
-- User with both Equipment Owner and Seller roles
INSERT INTO UserRoles (UserId, RoleId) VALUES (1, 2); -- EquipmentOwner
INSERT INTO UserRoles (UserId, RoleId) VALUES (1, 3); -- Seller
```

---

### 4. **Admins**

**Purpose**: Separate admin accounts for platform management

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `AdminId` | `INT` | PRIMARY KEY, IDENTITY | Unique admin identifier |
| `Username` | `NVARCHAR(MAX)` | NOT NULL | Admin username (login) |
| `PasswordHash` | `NVARCHAR(MAX)` | NOT NULL | Hashed password |
| `CreatedAt` | `DATETIME2` | NOT NULL | Account creation date |

**Note**: Admins are separate from regular users and cannot perform user actions.

---

## 💳 Subscription Management Tables

### 5. **SubscriptionPlans**

**Purpose**: Defines available subscription tiers

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `PlanId` | `INT` | PRIMARY KEY, IDENTITY | Unique plan identifier |
| `PlanName` | `NVARCHAR(100)` | NOT NULL | Plan name (e.g., "Basic", "Premium") |
| `DurationDays` | `INT` | NOT NULL | Subscription duration in days |
| `Price` | `DECIMAL(18,2)` | NOT NULL | Subscription price in ₹ |
| `MaxEquipment` | `INT` | NOT NULL | Maximum equipment listings allowed |
| `MaxProducts` | `INT` | NOT NULL | Maximum product listings allowed |
| `Status` | `NVARCHAR(20)` | NOT NULL | Active, Inactive |

**Sample Plans**:
```sql
INSERT INTO SubscriptionPlans (PlanName, DurationDays, Price, MaxEquipment, MaxProducts, Status)
VALUES 
('Free', 365, 0.00, 1, 1, 'Active'),
('Basic', 30, 299.00, 5, 10, 'Active'),
('Premium', 30, 599.00, 20, 50, 'Active'),
('Enterprise', 30, 1499.00, 100, 200, 'Active');
```

---

### 6. **UserSubscriptions**

**Purpose**: Tracks active and historical subscriptions

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `SubscriptionId` | `INT` | PRIMARY KEY, IDENTITY | Unique subscription identifier |
| `UserId` | `INT` | NOT NULL, FK → Users | Reference to user |
| `PlanId` | `INT` | NOT NULL, FK → SubscriptionPlans | Reference to plan |
| `StartDate` | `DATETIME2` | NOT NULL | Subscription start date |
| `EndDate` | `DATETIME2` | NOT NULL | Subscription expiry date |
| `PaymentStatus` | `NVARCHAR(MAX)` | NOT NULL | Pending, Paid, Failed |
| `Status` | `NVARCHAR(MAX)` | NOT NULL | Active, Expired, Cancelled |
| `PaymentId` | `NVARCHAR(MAX)` | NOT NULL | Razorpay payment ID |

**Indexes**:
- PRIMARY KEY on `SubscriptionId`
- INDEX on `UserId`

**Business Rules**:
- User can have only ONE active subscription at a time (enforced in backend)
- When subscription expires, roles are automatically removed via background service
- Subscription grants EquipmentOwner and/or Seller roles based on plan limits

---

## 📂 Category & Classification Tables

### 7. **Categories**

**Purpose**: Main product/equipment categories

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `CategoryId` | `INT` | PRIMARY KEY, IDENTITY | Unique category identifier |
| `Name` | `NVARCHAR(100)` | NOT NULL | Category name |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Active' | Active, Inactive |
| `CreatedAt` | `DATETIME2` | NOT NULL | Creation timestamp |

**Sample Categories**:
```sql
INSERT INTO Categories (Name, Status)
VALUES 
('Tractors & Tillers', 'Active'),
('Harvesting Equipment', 'Active'),
('Seeds & Fertilizers', 'Active'),
('Irrigation Systems', 'Active');
```

---

### 8. **SubCategories**

**Purpose**: Detailed classification under main categories

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `SubCategoryId` | `INT` | PRIMARY KEY, IDENTITY | Unique subcategory identifier |
| `CategoryId` | `INT` | NOT NULL, FK → Categories | Parent category |
| `SubCategoryName` | `NVARCHAR(100)` | NOT NULL | Subcategory name |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Active' | Active, Inactive |

**Indexes**:
- PRIMARY KEY on `SubCategoryId`
- INDEX on `CategoryId`

**Foreign Keys**:
- `CategoryId` → `Categories(CategoryId)` ON DELETE CASCADE

**Example Hierarchy**:
```sql
-- Category: Tractors & Tillers (CategoryId = 1)
INSERT INTO SubCategories (CategoryId, SubCategoryName, Status)
VALUES 
(1, 'Mini Tractors', 'Active'),
(1, 'Power Tillers', 'Active'),
(1, 'Rotavators', 'Active');
```

---

## 🚜 Equipment Management Tables

### 9. **Equipments**

**Purpose**: Equipment available for rent

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `EquipmentId` | `INT` | PRIMARY KEY, IDENTITY | Unique equipment identifier |
| `OwnerId` | `INT` | NOT NULL, FK → Users | Equipment owner |
| `EquipmentName` | `NVARCHAR(200)` | NOT NULL | Equipment name |
| `Description` | `NVARCHAR(2000)` | NOT NULL | Detailed description |
| `PriceType` | `NVARCHAR(20)` | NOT NULL | "Hourly" or "Daily" |
| `Price` | `DECIMAL(18,2)` | NOT NULL | Rental price |
| `Location` | `NVARCHAR(200)` | NOT NULL | Equipment location |
| `SubCategoryId` | `INT` | NOT NULL, FK → SubCategories | Equipment category |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Pending' | Pending, Approved, Rejected |
| `CreatedAt` | `DATETIME2` | NOT NULL | Listing creation date |

**Indexes**:
- PRIMARY KEY on `EquipmentId`
- INDEX on `OwnerId`
- INDEX on `SubCategoryId`

**Foreign Keys**:
- `OwnerId` → `Users(UserId)` ON DELETE CASCADE
- `SubCategoryId` → `SubCategories(SubCategoryId)` ON DELETE CASCADE

**Business Rules**:
- Equipment requires admin approval before visible to renters
- Owner must have active subscription with available equipment slots
- PriceType determines booking calculation (hourly: 2-6 hours, daily: 1+ days)

---

### 10. **EquipmentImages**

**Purpose**: Multiple images per equipment

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `ImageId` | `INT` | PRIMARY KEY, IDENTITY | Unique image identifier |
| `EquipmentId` | `INT` | NOT NULL, FK → Equipments | Reference to equipment |
| `ImageUrl` | `NVARCHAR(255)` | NOT NULL | Image URL/path |

**Indexes**:
- PRIMARY KEY on `ImageId`
- INDEX on `EquipmentId`

**Foreign Keys**:
- `EquipmentId` → `Equipments(EquipmentId)` ON DELETE CASCADE

**Example**:
```sql
INSERT INTO EquipmentImages (EquipmentId, ImageUrl)
VALUES 
(1, 'https://cdn.agrirent.com/equipment/tractor1-front.jpg'),
(1, 'https://cdn.agrirent.com/equipment/tractor1-side.jpg'),
(1, 'https://cdn.agrirent.com/equipment/tractor1-back.jpg');
```

---

### 11. **EquipmentAvailability**

**Purpose**: Blocked dates when equipment is unavailable

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `AvailabilityId` | `INT` | PRIMARY KEY, IDENTITY | Unique availability identifier |
| `EquipmentId` | `INT` | NOT NULL, FK → Equipments | Reference to equipment |
| `StartDate` | `DATETIME2` | NOT NULL | Block start date |
| `EndDate` | `DATETIME2` | NOT NULL | Block end date |
| `Reason` | `NVARCHAR(MAX)` | NOT NULL | Reason for blocking (Maintenance, Personal Use, etc.) |

**Indexes**:
- PRIMARY KEY on `AvailabilityId`
- COMPOSITE INDEX on `(EquipmentId, StartDate, EndDate)` for fast availability checks

**Foreign Keys**:
- `EquipmentId` → `Equipments(EquipmentId)` ON DELETE CASCADE

**Usage**: Prevents double booking during owner-specified unavailable periods.

---

### 12. **EquipmentBookings**

**Purpose**: Rental bookings for equipment

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `BookingId` | `INT` | PRIMARY KEY, IDENTITY | Unique booking identifier |
| `EquipmentId` | `INT` | NOT NULL, FK → Equipments | Reference to equipment |
| `FarmerId` | `INT` | NOT NULL, FK → Users | Renter (farmer) |
| `StartDate` | `DATETIME2` | NOT NULL | Rental start date/time |
| `EndDate` | `DATETIME2` | NOT NULL | Rental end date/time |
| `TotalPrice` | `DECIMAL(18,2)` | NOT NULL | Total rental cost |
| `Status` | `NVARCHAR(MAX)` | NOT NULL | Pending, Confirmed, Completed, Cancelled |
| `CreatedAt` | `DATETIME2` | NOT NULL | Booking creation timestamp |

**Indexes**:
- PRIMARY KEY on `BookingId`
- INDEX on `EquipmentId`
- INDEX on `FarmerId`

**Foreign Keys**:
- `EquipmentId` → `Equipments(EquipmentId)` ON DELETE NO ACTION
- `FarmerId` → `Users(UserId)` ON DELETE NO ACTION

**Business Rules**:
- Hourly bookings: 2-6 hours duration
- Daily bookings: Minimum 1 day
- Cannot book during blocked availability periods
- Prevents overlapping bookings for same equipment

---

## 🛒 Product & Order Tables

### 13. **Products**

**Purpose**: Products available for sale

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `ProductId` | `INT` | PRIMARY KEY, IDENTITY | Unique product identifier |
| `SellerId` | `INT` | NOT NULL, FK → Users | Product seller |
| `ProductName` | `NVARCHAR(200)` | NOT NULL | Product name |
| `Description` | `NVARCHAR(2000)` | NOT NULL | Product description |
| `Price` | `DECIMAL(18,2)` | NOT NULL | Unit price |
| `Stock` | `INT` | NOT NULL | Available quantity |
| `Unit` | `NVARCHAR(50)` | NOT NULL | kg, litre, piece, etc. |
| `Location` | `NVARCHAR(200)` | NOT NULL | Seller location |
| `SubCategoryId` | `INT` | NOT NULL, FK → SubCategories | Product category |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Pending' | Pending, Approved, Rejected |
| `CreatedAt` | `DATETIME2` | NOT NULL | Listing creation date |

**Indexes**:
- PRIMARY KEY on `ProductId`
- INDEX on `SellerId`
- INDEX on `SubCategoryId`

**Foreign Keys**:
- `SellerId` → `Users(UserId)` ON DELETE CASCADE
- `SubCategoryId` → `SubCategories(SubCategoryId)` ON DELETE CASCADE

**Business Rules**:
- Products require admin approval
- Seller must have active subscription with available product slots
- Stock is automatically decremented on order placement

---

### 14. **ProductImages**

**Purpose**: Multiple images per product

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `ImageId` | `INT` | PRIMARY KEY, IDENTITY | Unique image identifier |
| `ProductId` | `INT` | NOT NULL, FK → Products | Reference to product |
| `ImageUrl` | `NVARCHAR(255)` | NOT NULL | Image URL/path |

**Indexes**:
- PRIMARY KEY on `ImageId`
- INDEX on `ProductId`

**Foreign Keys**:
- `ProductId` → `Products(ProductId)` ON DELETE CASCADE

---

### 15. **Orders**

**Purpose**: Order header information

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `OrderId` | `INT` | PRIMARY KEY, IDENTITY | Unique order identifier |
| `FarmerId` | `INT` | NOT NULL, FK → Users | Customer placing order |
| `TotalAmount` | `DECIMAL(18,2)` | NOT NULL | Total order amount |
| `OrderStatus` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Pending' | Pending, Confirmed, Delivered, Cancelled |
| `CreatedAt` | `DATETIME2` | NOT NULL | Order creation timestamp |

**Indexes**:
- PRIMARY KEY on `OrderId`
- INDEX on `FarmerId`

**Foreign Keys**:
- `FarmerId` → `Users(UserId)` ON DELETE NO ACTION

**Note**: Order contains multiple items via `OrderItems` table.

---

### 16. **OrderItems**

**Purpose**: Individual line items within an order

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `OrderItemId` | `INT` | PRIMARY KEY, IDENTITY | Unique item identifier |
| `OrderId` | `INT` | NOT NULL, FK → Orders | Parent order |
| `ProductId` | `INT` | NOT NULL, FK → Products | Product being ordered |
| `Quantity` | `INT` | NOT NULL | Quantity ordered |
| `Price` | `DECIMAL(18,2)` | NOT NULL | Unit price at time of order |

**Indexes**:
- PRIMARY KEY on `OrderItemId`
- INDEX on `OrderId`
- INDEX on `ProductId`

**Foreign Keys**:
- `OrderId` → `Orders(OrderId)` ON DELETE CASCADE
- `ProductId` → `Products(ProductId)` ON DELETE NO ACTION

**Business Rules**:
- Price is captured at order time (historical pricing)
- TotalAmount in Orders = SUM(Quantity × Price) for all items

---

## ⭐ Rating & Feedback Tables

### 17. **Ratings**

**Purpose**: Reviews for equipment and sellers

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `RatingId` | `INT` | PRIMARY KEY, IDENTITY | Unique rating identifier |
| `FromUserId` | `INT` | NOT NULL, FK → Users | User giving rating |
| `TargetId` | `INT` | NOT NULL | EquipmentId or SellerId |
| `RatingType` | `NVARCHAR(20)` | NOT NULL | "Equipment" or "Seller" |
| `RatingValue` | `INT` | NOT NULL, CHECK (1-5) | Star rating (1-5) |
| `Comment` | `NVARCHAR(255)` | NULL | Optional review comment |
| `CreatedAt` | `DATETIME2` | NOT NULL | Rating timestamp |

**Indexes**:
- PRIMARY KEY on `RatingId`
- INDEX on `FromUserId`

**Foreign Keys**:
- `FromUserId` → `Users(UserId)` ON DELETE CASCADE

**Example**:
```sql
-- Rate an equipment
INSERT INTO Ratings (FromUserId, TargetId, RatingType, RatingValue, Comment)
VALUES (2, 1, 'Equipment', 5, 'Excellent tractor, very well maintained!');

-- Rate a seller
INSERT INTO Ratings (FromUserId, TargetId, RatingType, RatingValue, Comment)
VALUES (2, 3, 'Seller', 4, 'Good quality seeds, fast delivery');
```

---

### 18. **Complaints**

**Purpose**: User complaints and feedback

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `ComplaintId` | `INT` | PRIMARY KEY, IDENTITY | Unique complaint identifier |
| `UserId` | `INT` | NOT NULL, FK → Users | User filing complaint |
| `Description` | `NVARCHAR(500)` | NOT NULL | Complaint details |
| `Status` | `NVARCHAR(20)` | NOT NULL, DEFAULT 'Pending' | Pending, Resolved, Rejected |
| `CreatedAt` | `DATETIME2` | NOT NULL | Complaint submission date |

**Indexes**:
- PRIMARY KEY on `ComplaintId`
- INDEX on `UserId`

**Foreign Keys**:
- `UserId` → `Users(UserId)` ON DELETE CASCADE

---

### 19. **Addresses**

**Purpose**: User delivery addresses

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `AddressId` | `INT` | PRIMARY KEY, IDENTITY | Unique address identifier |
| `UserId` | `INT` | NOT NULL, FK → Users | Address owner |
| `State` | `NVARCHAR(50)` | NOT NULL | State name |
| `District` | `NVARCHAR(50)` | NOT NULL | District name |
| `City` | `NVARCHAR(50)` | NOT NULL | City/Town name |
| `Village` | `NVARCHAR(50)` | NULL | Village name (optional) |
| `Pincode` | `NVARCHAR(10)` | NOT NULL | Postal code |

**Indexes**:
- PRIMARY KEY on `AddressId`
- INDEX on `UserId`

**Foreign Keys**:
- `UserId` → `Users(UserId)` ON DELETE CASCADE

---

## 🔔 Utility Tables

### 20. **Notifications**

**Purpose**: Push notifications for users

| Column Name | Data Type | Constraints | Description |
|-------------|-----------|-------------|-------------|
| `Id` | `INT` | PRIMARY KEY, IDENTITY | Unique notification identifier |
| `UserId` | `INT` | NULL | Target user (NULL = all users) |
| `Title` | `NVARCHAR(MAX)` | NOT NULL | Notification title |
| `Message` | `NVARCHAR(MAX)` | NOT NULL | Notification message |
| `IsRead` | `BIT` | NOT NULL, DEFAULT 0 | Read status |
| `CreatedAt` | `DATETIME2` | NOT NULL | Notification timestamp |

**Usage**: Stores notifications for booking confirmations, order updates, subscription expiry alerts, etc.

---

## 📊 Indexes & Performance

### Unique Indexes
```sql
-- Prevent duplicate mobile numbers
CREATE UNIQUE INDEX IX_Users_MobileNumber ON Users(MobileNumber);

-- Prevent duplicate role assignments
CREATE UNIQUE INDEX IX_UserRoles_UserId_RoleId ON UserRoles(UserId, RoleId);
```

### Foreign Key Indexes
```sql
-- Improve JOIN performance
CREATE INDEX IX_UserRoles_UserId ON UserRoles(UserId);
CREATE INDEX IX_UserRoles_RoleId ON UserRoles(RoleId);
CREATE INDEX IX_UserSubscriptions_UserId ON UserSubscriptions(UserId);
CREATE INDEX IX_UserSubscriptions_PlanId ON UserSubscriptions(PlanId);
CREATE INDEX IX_Equipments_OwnerId ON Equipments(OwnerId);
CREATE INDEX IX_Equipments_SubCategoryId ON Equipments(SubCategoryId);
CREATE INDEX IX_Products_SellerId ON Products(SellerId);
CREATE INDEX IX_Products_SubCategoryId ON Products(SubCategoryId);
CREATE INDEX IX_EquipmentBookings_EquipmentId ON EquipmentBookings(EquipmentId);
CREATE INDEX IX_EquipmentBookings_FarmerId ON EquipmentBookings(FarmerId);
CREATE INDEX IX_OrderItems_OrderId ON OrderItems(OrderId);
CREATE INDEX IX_OrderItems_ProductId ON OrderItems(ProductId);
```

### Composite Indexes
```sql
-- Fast availability checks
CREATE INDEX IX_EquipmentAvailability_EquipmentId_StartDate_EndDate 
ON EquipmentAvailability(EquipmentId, StartDate, EndDate);
```

---

## 🔗 Foreign Key Relationships

### Cascade Delete Rules

**ON DELETE CASCADE** (Child records deleted automatically):
- `UserRoles` → `Users`
- `UserRoles` → `Roles`
- `Addresses` → `Users`
- `Complaints` → `Users`
- `Ratings` → `Users`
- `Equipments` → `Users` (Owner)
- `Equipments` → `SubCategories`
- `EquipmentImages` → `Equipments`
- `EquipmentAvailability` → `Equipments`
- `Products` → `Users` (Seller)
- `Products` → `SubCategories`
- `ProductImages` → `Products`
- `SubCategories` → `Categories`
- `OrderItems` → `Orders`

**ON DELETE NO ACTION** (Prevents deletion if referenced):
- `EquipmentBookings` → `Equipments` (Preserve booking history)
- `EquipmentBookings` → `Users` (Farmer)
- `Orders` → `Users` (Farmer)
- `OrderItems` → `Products` (Preserve order history)

---

## 🔐 Security Considerations

### Password Storage
- Passwords stored as SHA-256 hash (256 characters)
- Salting implemented in application layer
- **Recommendation**: Migrate to BCrypt for enhanced security

### JWT Token Management
- Access tokens: 60 minutes expiry
- Refresh tokens: Stored in database with expiration
- Token rotation on refresh

### SQL Injection Prevention
- Entity Framework Core with parameterized queries
- No raw SQL execution without validation

### Data Validation
- Comprehensive validation via Data Annotations
- Server-side validation on all inputs
- Mobile number format: `+91XXXXXXXXXX` (13 characters)

---

## 📈 Performance Optimization

### Database-Level
1. **Proper indexing** on foreign keys and frequently queried columns
2. **Composite indexes** for multi-column searches
3. **DECIMAL(18,2)** for monetary values (prevents floating-point errors)

### Application-Level
1. **Async/Await** for all database operations
2. **Connection pooling** enabled (MultipleActiveResultSets=True)
3. **Background services** for subscription expiry checks
4. **Lazy loading** disabled (explicit eager loading with Include)

### Query Optimization
```csharp
// ❌ N+1 Query Problem
var equipments = await _context.Equipments.ToListAsync();
foreach(var e in equipments) {
    var images = e.Images; // Separate query for each
}

// ✅ Eager Loading
var equipments = await _context.Equipments
    .Include(e => e.Images)
    .Include(e => e.SubCategory)
        .ThenInclude(sc => sc.Category)
    .ToListAsync();
```

---

## 🔄 Migration History

### 20260201183254_CompleteSchemaAsPerDesign
**Applied**: February 1, 2026  
**Changes**:
- Created all 20 tables
- Established foreign key relationships
- Added indexes for performance
- Set up cascade delete rules

---

## 📝 Naming Conventions

### Tables
- PascalCase, Plural (e.g., `Users`, `Products`)
- Descriptive names indicating purpose

### Columns
- PascalCase, Singular (e.g., `UserId`, `ProductName`)
- ID columns: `{TableName}Id`
- Foreign keys match referenced column name

### Indexes
- Format: `IX_{TableName}_{ColumnName}`
- Composite: `IX_{TableName}_{Column1}_{Column2}`

---

## 🎯 Business Logic Examples

### Subscription Workflow
```sql
-- 1. User purchases subscription
INSERT INTO UserSubscriptions (UserId, PlanId, StartDate, EndDate, PaymentStatus, Status, PaymentId)
VALUES (1, 2, GETDATE(), DATEADD(DAY, 30, GETDATE()), 'Paid', 'Active', 'pay_xyz123');

-- 2. Grant roles based on plan
DECLARE @PlanMaxEquipment INT = (SELECT MaxEquipment FROM SubscriptionPlans WHERE PlanId = 2);
DECLARE @PlanMaxProducts INT = (SELECT MaxProducts FROM SubscriptionPlans WHERE PlanId = 2);

IF @PlanMaxEquipment > 0
    INSERT INTO UserRoles (UserId, RoleId) VALUES (1, 2); -- EquipmentOwner

IF @PlanMaxProducts > 0
    INSERT INTO UserRoles (UserId, RoleId) VALUES (1, 3); -- Seller
```

### Booking Validation
```sql
-- Check if equipment is available for booking
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM EquipmentBookings 
            WHERE EquipmentId = @EquipmentId 
            AND Status NOT IN ('Cancelled')
            AND (@StartDate BETWEEN StartDate AND EndDate 
                OR @EndDate BETWEEN StartDate AND EndDate)
        ) THEN 0 -- Not available (overlapping booking)
        WHEN EXISTS (
            SELECT 1 FROM EquipmentAvailability 
            WHERE EquipmentId = @EquipmentId 
            AND (@StartDate BETWEEN StartDate AND EndDate 
                OR @EndDate BETWEEN StartDate AND EndDate)
        ) THEN 0 -- Not available (owner blocked)
        ELSE 1 -- Available
    END AS IsAvailable;
```

### Average Rating Calculation
```sql
-- Get average rating for equipment
SELECT 
    TargetId AS EquipmentId,
    AVG(CAST(RatingValue AS DECIMAL(3,2))) AS AverageRating,
    COUNT(*) AS TotalReviews
FROM Ratings
WHERE RatingType = 'Equipment'
GROUP BY TargetId;
```

---

## 🛠️ Maintenance Queries

### Find Expired Subscriptions
```sql
SELECT 
    u.UserId, 
    u.FullName, 
    us.EndDate,
    sp.PlanName
FROM UserSubscriptions us
JOIN Users u ON us.UserId = u.UserId
JOIN SubscriptionPlans sp ON us.PlanId = sp.PlanId
WHERE us.EndDate < GETUTCDATE() 
AND us.Status = 'Active';
```

### Check Equipment Listing Limits
```sql
SELECT 
    u.UserId,
    u.FullName,
    sp.MaxEquipment,
    COUNT(e.EquipmentId) AS CurrentEquipment,
    sp.MaxEquipment - COUNT(e.EquipmentId) AS RemainingSlots
FROM Users u
JOIN UserSubscriptions us ON u.UserId = us.UserId
JOIN SubscriptionPlans sp ON us.PlanId = sp.PlanId
LEFT JOIN Equipments e ON u.UserId = e.OwnerId AND e.Status = 'Approved'
WHERE us.Status = 'Active'
GROUP BY u.UserId, u.FullName, sp.MaxEquipment;
```

### Clean Up Old Notifications
```sql
-- Delete read notifications older than 30 days
DELETE FROM Notifications
WHERE IsRead = 1 
AND CreatedAt < DATEADD(DAY, -30, GETDATE());
```

---

## 📞 Support Information

**Database Administrator**: AgriRent Team  
**Connection String**: Stored in `appsettings.json`  
**Backup Schedule**: Daily automated backups  
**Migration Tool**: Entity Framework Core 10.0.2

---

**Document Version**: 1.0  
**Last Updated**: February 2, 2026  
**Total Pages**: Comprehensive Database Reference
