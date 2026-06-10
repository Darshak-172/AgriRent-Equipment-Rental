# Database Schema Migration - Action Plan

## Models Updated ✅

1. **SubCategory** - NEW (links Categories to Equipment/Products)
2. **EquipmentImage** - NEW (multiple images per equipment)
3. **ProductImage** - NEW (multiple images per product)
4. **Order** - RESTRUCTURED (now just OrderId, FarmerId, TotalAmount, OrderStatus)
5. **OrderItem** - NEW (OrderId, ProductId, Quantity, Price)
6. **Rating** - NEW (review system)
7. **Complaint** - NEW (complaint management)
8. **Address** - NEW (delivery addresses)
9. **Equipment** - UPDATED (removed ImageUrl, CategoryId → added SubCategoryId, Images collection)
10. **Product** - UPDATED (removed Category, ImageUrl → added SubCategoryId, Images collection)
11. **Category** - UPDATED (added SubCategories navigation)

## Controllers That Need Updates

### High Priority (Breaking Changes):

1. **EquipmentController.cs**
   - Line 61: Remove `ImageUrl =` 
   - Line 62: Change `CategoryId` to `SubCategoryId`
   - Line 99: Remove `ImageUrl` access
   - Solution: Store images in EquipmentImages table

2. **ProductController.cs**
   - Line 43, 55, 90: Remove `Category` access
   - Line 60, 95: Remove `ImageUrl` access
   - Lines 126-134: Order model restructured (ProductId, Quantity, UnitPrice, etc. moved to OrderItem)
   - Line 159, 190: `o.Product` no longer exists
   - Solution: Use SubCategoryId, Images collection, OrderItems

3. **SellerController.cs**
   - Line 54, 93, 124: Remove `Category` access
   - Line 58, 98, 128: Remove `ImageUrl` access
   - Line 167, 199, 223, 224: `o.Product` no longer exists, `o.Status` → `o.OrderStatus`
   - Solution: Use SubCategoryId, Images collection, OrderItems with Include

4. **AdminProductController.cs**
   - Line 47, 143: Remove `Category` access
   - Line 52: Remove `ImageUrl` access
   - Solution: Use SubCategoryId, Images collection

5. **AdminEquipmentController.cs**
   - Line 50: Remove `ImageUrl` access
   - Solution: Use Images collection

6. **EquipmentPublicController.cs**
   - Line 58: Change `CategoryId` to `SubCategoryId`
   - Line 85: Remove `ImageUrl` access
   - Solution: Use SubCategoryId, Images collection

7. **AdminController.cs**
   - Line 193, 195, 237: Remove `ImageUrl` access
   - Solution: Use Images collection

### Views That Need Updates:

8. **Views/Home/Index.cshtml**
   - Line 175 (twice), 224 (twice): Remove `ImageUrl` access
   - Solution: Use Images collection, display first image

## Migration Strategy

**Phase 1**: Temporarily add back removed properties to avoid breaking changes
**Phase 2**: Update all controllers systematically
**Phase 3**: Remove temporary properties
**Phase 4**: Create migration and update database

## Quick Fix: Add Compatibility Properties

Add these to Equipment.cs and Product.cs temporarily:

```csharp
// Equipment.cs - Add temporary compatibility
[NotMapped]
public string? ImageUrl => Images?.FirstOrDefault()?.ImageUrl;

[NotMapped]
public int? CategoryId => SubCategory?.CategoryId;

// Product.cs - Add temporary compatibility
[NotMapped]
public string? ImageUrl => Images?.FirstOrDefault()?.ImageUrl;

[NotMapped]
public string? Category => SubCategory?.SubCategoryName;
```

This allows the code to compile while we migrate properly.
