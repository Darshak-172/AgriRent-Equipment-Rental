# 🌾 AgriRent - Complete API Documentation

**Base URL:** `http://localhost:5288/api`  
**Production:** `https://agrirent.azurewebsites.net/api`

---

## 🔐 Authentication Endpoints

### Register (OTP-based)
**POST** `/auth/send-otp`
```json
{
  "mobileNumber": "+919876543210",
  "isRealOtp": false
}
```

**POST** `/auth/otp-register`
```json
{
  "mobileNumber": "+919876543210",
  "sessionId": "session_123",
  "otp": "123456",
  "fullName": "John Doe",
  "password": "password123",
  "isRealOtp": false
}
```
**Response:** Returns JWT token + refresh token

### Login
**POST** `/auth/login`
```json
{
  "mobileNumber": "+919876543210",
  "password": "password123"
}
```

**POST** `/auth/otp-login`
```json
{
  "mobileNumber": "+919876543210",
  "sessionId": "session_123",
  "otp": "123456",
  "isRealOtp": false
}
```

### Password Management
**POST** `/auth/otp-forgot`
**POST** `/auth/otp-reset`

### Token Refresh
**POST** `/auth/refresh`
```json
{
  "refreshToken": "..."
}
```

---

## 👤 User Profile

### Get Profile
**GET** `/profile/me`  
**Headers:** `Authorization: Bearer {token}`  
**Headers:** `Accept-Language: en|hi|gu`

---

## 🚜 Equipment Management (Owner)

### Add Equipment
**POST** `/equipment/add`  
**Role:** Owner  
**Requires:** Active subscription
```json
{
  "equipmentName": "Tractor",
  "description": "Heavy duty tractor",
  "priceType": "Daily",
  "price": 1500,
  "location": "Ahmedabad",
  "imageUrl": "https://...",
  "categoryId": 1
}
```

### View My Equipment
**GET** `/equipment/my-list`  
**Role:** Owner

### Block Dates
**POST** `/equipment/block-dates`
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-05",
  "endDate": "2026-02-10",
  "reason": "Maintenance"
}
```

---

## 🔍 Equipment Browsing (Public)

### View All Approved Equipment
**GET** `/equipment-public/list`  
**Query Params:** `?category=Tractor&location=Ahmedabad`

### Check Availability
**POST** `/equipment/check-availability`
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-05T10:00:00",
  "endDate": "2026-02-05T16:00:00"
}
```

### View Blocked Dates
**GET** `/equipment/blocked-dates/{equipmentId}`

---

## 📅 Booking System (Farmer)

### Create Booking
**POST** `/booking/create`  
**Role:** Farmer
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-05T10:00:00",
  "endDate": "2026-02-05T16:00:00"
}
```

### View My Bookings
**GET** `/booking/my-bookings`  
**Role:** Farmer

### Owner - View Requests
**GET** `/booking/owner-requests`  
**Role:** Owner

### Accept/Reject Booking
**POST** `/booking/accept/{bookingId}`  
**POST** `/booking/reject/{bookingId}`  
**Role:** Owner

### Filter Bookings
**GET** `/booking/filter?status=pending`

---

## 🛍️ Product Management (Seller)

### Add Product
**POST** `/seller/add-product`  
**Role:** Seller  
**Requires:** Active subscription
```json
{
  "productName": "Organic Fertilizer",
  "description": "High quality organic fertilizer",
  "category": "Fertilizers",
  "price": 500,
  "stock": 100,
  "unit": "kg",
  "imageUrl": "https://...",
  "location": "Rajkot"
}
```

### View My Products
**GET** `/seller/my-products`  
**Role:** Seller

### Update Product
**PUT** `/seller/update-product/{id}`  
**Role:** Seller

### Delete Product
**DELETE** `/seller/delete-product/{id}`  
**Role:** Seller

### View Orders
**GET** `/seller/orders`  
**Role:** Seller

### Update Order Status
**PUT** `/seller/update-order-status/{orderId}`  
**Role:** Seller
```json
"Confirmed" | "Delivered" | "Cancelled"
```

### Seller Dashboard
**GET** `/seller/dashboard`  
**Role:** Seller

---

## 🛒 Product Browsing & Orders (Farmer)

### View All Products
**GET** `/products/list`  
**Query Params:** `?category=Seeds&location=Surat`

### View Product Details
**GET** `/products/{id}`

### Place Order
**POST** `/products/place-order`  
**Role:** Farmer
```json
{
  "productId": 1,
  "quantity": 10,
  "deliveryAddress": "123 Main St, Ahmedabad",
  "contactNumber": "+919876543210"
}
```

### View My Orders
**GET** `/products/my-orders`  
**Role:** Farmer

### Cancel Order
**PUT** `/products/cancel-order/{orderId}`  
**Role:** Farmer

---

## 💳 Subscription System

### View Plans
**GET** `/subscription-plans/active`

### Create Razorpay Order
**POST** `/subscription/create-order/{planId}`  
**Requires:** JWT Token

### Verify Payment
**POST** `/subscription/verify-payment`
```json
{
  "planId": 1,
  "paymentId": "pay_123..."
}
```
**Response:** Returns updated JWT with new roles

---

## 🎛️ Admin - Equipment Management

### View Pending Equipment
**GET** `/admin/equipment/pending`  
**Role:** Admin

### Approve Equipment
**PUT** `/admin/equipment/approve/{id}`  
**Role:** Admin

### Reject Equipment
**PUT** `/admin/equipment/reject/{id}`  
**Role:** Admin

### Delete Equipment
**DELETE** `/admin/equipment/delete/{id}`  
**Role:** Admin

---

## 🎛️ Admin - Product Management

### View Pending Products
**GET** `/admin/products/pending`  
**Role:** Admin

### Approve Product
**PUT** `/admin/products/approve/{id}`  
**Role:** Admin

### Reject Product
**PUT** `/admin/products/reject/{id}`  
**Role:** Admin

### Delete Product
**DELETE** `/admin/products/delete/{id}`  
**Role:** Admin

### View All Products
**GET** `/admin/products/all?status=Approved`  
**Role:** Admin

---

## 🎛️ Admin - Subscription Plans

### Create Plan
**POST** `/admin/subscription/add-plan`  
**Role:** Admin
```json
{
  "planName": "Basic Plan",
  "price": 999,
  "durationDays": 30,
  "maxEquipment": 5,
  "maxProducts": 10,
  "status": "Active"
}
```

### View All Plans
**GET** `/admin/subscription/plans`

### Change Plan Status
**PUT** `/admin/subscription/status/{id}`  
**Role:** Admin

---

## 📱 Multi-Language Support

**All endpoints support multi-language:**
- Add header: `Accept-Language: en` (English)
- Add header: `Accept-Language: hi` (Hindi)
- Add header: `Accept-Language: gu` (Gujarati)

**How it works:**
- User input in Gujarati/Hindi is transliterated to English
- Database stores only English data
- Response data is translated back to requested language

---

## 🎛️ Admin - Category Management

### View All Categories (with Subcategories)
**GET** `/admin/categories`  
**Role:** Admin

### Add Category
**POST** `/admin/categories`  
**Role:** Admin
```json
{
  "name": "Tractors",
  "status": "Active"
}
```

### Edit Category
**PUT** `/admin/categories/{id}`  
**Role:** Admin

### Delete Category
**DELETE** `/admin/categories/{id}`  
**Role:** Admin

### Add SubCategory
**POST** `/admin/categories/subcategories`  
**Role:** Admin
```json
{
  "categoryId": 1,
  "subCategoryName": "Mini Tractor",
  "status": "Active"
}
```

### Edit SubCategory
**PUT** `/admin/categories/subcategories/{id}`  
**Role:** Admin

### Delete SubCategory
**DELETE** `/admin/categories/subcategories/{id}`  
**Role:** Admin

---

## 🌐 Public Category Browsing

### List All Categories
**GET** `/public/categories`  
**Description:** Get list of active categories and subcategories for app dropdowns.

---

## 🔒 Authorization

### Roles:
- **Farmer** - Default role on registration
- **Owner** - Assigned on subscription purchase (MaxEquipment > 0)
- **Seller** - Assigned on subscription purchase (MaxProducts > 0)
- **Admin** - Manually created

### Headers:
```
Authorization: Bearer {jwt_token}
Accept-Language: en|hi|gu
```

---

## 📊 Response Format

### Success Response:
```json
{
  "message": "Operation successful",
  "data": { ... }
}
```

### Error Response:
```json
{
  "message": "Error description"
}
```

**HTTP Status Codes:**
- 200: Success
- 400: Bad Request
- 401: Unauthorized
- 404: Not Found
- 500: Server Error

---

## 🎯 Complete Feature Checklist

✅ User Authentication (OTP + Password)  
✅ JWT Token Management  
✅ Role-Based Authorization  
✅ Multi-Language Support (English, Hindi, Gujarati)  
✅ Equipment Listing & Management  
✅ Equipment Booking System  
✅ Product Listing & Management  
✅ Order Management System  
✅ Subscription Plans  
✅ Razorpay Payment Integration  
✅ Admin Approval Workflows  
✅ Date Conflict Detection  
✅ Stock Management  
✅ Location-Based Filtering  

---

## 🚀 System Status

**Backend:** ASP.NET Core 10.0 ✅  
**Database:** SQL Server ✅  
**Authentication:** JWT Bearer ✅  
**Payment:** Razorpay Test Mode ✅  
**Translation:** Azure Translator API ✅  
**Server:** http://localhost:5288 ✅  

**Database Tables:**
- Users, Roles, UserRoles ✅
- Equipment, EquipmentAvailability, EquipmentBookings ✅
- Products, Orders ✅
- SubscriptionPlans, UserSubscriptions ✅
- Categories, Notifications, Admins ✅

---

## 📞 Android Development Notes

1. **Base URL Configuration:** Configure base URL as constant
2. **Token Storage:** Store JWT in SharedPreferences/SecureStorage
3. **Language Header:** Set Accept-Language based on app settings
4. **Refresh Token:** Implement auto-refresh on 401 responses
5. **Image Upload:** Use Cloudinary/ImageKit for image URLs
6. **Payment:** Integrate Razorpay Android SDK
7. **Offline Mode:** Cache approved equipment/products locally

---

**System Ready for Production! 🎉**
