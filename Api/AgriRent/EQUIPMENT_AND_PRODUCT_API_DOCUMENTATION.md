# 🚜 Equipment & 🌾 Product API Documentation

Complete API reference for Equipment and Product management in AgriRent.

---

## 📑 Table of Contents

### Equipment APIs
1. [Public Equipment Listing](#1-public-equipment-listing)
2. [Add Equipment (Owner)](#2-add-equipment-owner)
3. [View My Equipment (Owner)](#3-view-my-equipment-owner)
4. [Block Equipment Dates](#4-block-equipment-dates)
5. [Check Equipment Availability](#5-check-equipment-availability)
6. [Get Blocked Dates](#6-get-blocked-dates)
7. [Upload Equipment Image](#7-upload-equipment-image)
8. [Get Equipment Images](#8-get-equipment-images)
9. [Delete Equipment Image](#9-delete-equipment-image)

### Product APIs
10. [Public Product Listing](#10-public-product-listing)
11. [Product Details](#11-product-details)
12. [Add Product (Seller)](#12-add-product-seller)
13. [View My Products (Seller)](#13-view-my-products-seller)
14. [Update Product (Seller)](#14-update-product-seller)
15. [Delete Product (Seller)](#15-delete-product-seller)
16. [Upload Product Image (Seller)](#16-upload-product-image-seller)
17. [Get Product Images (Seller)](#17-get-product-images-seller)
18. [Get Product Images (Public)](#18-get-product-images-public)
19. [Delete Product Image (Seller)](#19-delete-product-image-seller)
20. [Place Order (Farmer)](#20-place-order-farmer)
21. [View My Orders (Farmer)](#21-view-my-orders-farmer)
22. [Cancel Order (Farmer)](#22-cancel-order-farmer)

---

# 🚜 EQUIPMENT APIs

## 1. Public Equipment Listing

**Endpoint:** `GET /api/public/equipment/available`

**Authorization:** None (Public)

**Query Parameters:**
- `startDate` (DateTime, optional) - Start date for availability check
- `endDate` (DateTime, optional) - End date for availability check
- `location` (string, optional) - Filter by location
- `categoryId` (int, optional) - Filter by category ID

**Headers:**
- `Accept-Language` (optional) - Language code (en, gu, hi, etc.)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/public/equipment/available?location=Ahmedabad&categoryId=1" \
  -H "Accept-Language: gu"
```

**Success Response (200):**
```json
[
  {
    "equipmentId": 1,
    "equipmentName": "Tractor",
    "description": "Heavy duty farming tractor",
    "priceType": "Hourly",
    "price": 500.00,
    "location": "Ahmedabad",
    "imageUrl": "https://...legacy-image.jpg",
    "images": [
      {
        "imageId": 1,
        "imageUrl": "https://supabase.co/.../equipment/1738667000_abc123.jpg"
      },
      {
        "imageId": 2,
        "imageUrl": "https://supabase.co/.../equipment/1738667100_def456.jpg"
      }
    ],
    "owner": "John Farmer"
  }
]
```

**Notes:**
- If `startDate` and `endDate` are provided, blocked equipment will be excluded
- Translations are applied automatically based on `Accept-Language` header
- Only returns **Approved** equipment

---

## 2. Add Equipment (Owner)

**Endpoint:** `POST /api/equipment/add`

**Authorization:** Bearer Token (Owner role required)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "equipmentName": "Tractor",
  "description": "Heavy duty farming tractor",
  "priceType": "Hourly",
  "price": 500.00,
  "location": "Ahmedabad",
  "imageUrl": "https://example.com/image.jpg",
  "categoryId": 1
}
```

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/equipment/add" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{
    "equipmentName": "Tractor",
    "description": "Heavy duty tractor",
    "priceType": "Hourly",
    "price": 500,
    "location": "Ahmedabad",
    "categoryId": 1
  }'
```

**Success Response (200):**
```json
"✔ Equipment added successfully (Pending Admin Approval)"
```

**Error Response (400):**
```json
"❌ You must have an active subscription to add equipment."
```

**Notes:**
- Requires **active subscription**
- Equipment status will be set to **Pending** (requires admin approval)
- Name and description are auto-translated to English before storing

---

## 3. View My Equipment (Owner)

**Endpoint:** `GET /api/equipment/my-list`

**Authorization:** Bearer Token (Owner role required)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/equipment/my-list" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
[
  {
    "equipmentId": 1,
    "ownerId": 10,
    "owner": {
      "userId": 10,
      "fullName": "John Farmer",
      "mobileNumber": "+919876543210"
    },
    "equipmentName": "Tractor",
    "description": "Heavy duty farming tractor",
    "priceType": "Hourly",
    "price": 500.00,
    "location": "Ahmedabad",
    "imageUrl": "https://...",
    "status": "Approved",
    "createdAt": "2026-01-15T10:30:00Z"
  }
]
```

**Notes:**
- Returns only equipment owned by the authenticated user
- Ordered by creation date (newest first)

---

## 4. Block Equipment Dates

**Endpoint:** `POST /api/equipment/block-dates`

**Authorization:** Bearer Token (Owner role required)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-10T00:00:00Z",
  "endDate": "2026-02-15T23:59:59Z",
  "reason": "Maintenance"
}
```

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/equipment/block-dates" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "equipmentId": 1,
    "startDate": "2026-02-10T00:00:00Z",
    "endDate": "2026-02-15T23:59:59Z",
    "reason": "Maintenance"
  }'
```

**Success Response (200):**
```json
"🚫 Equipment marked unavailable for selected dates."
```

**Error Response (401):**
```json
"❌ You can only manage your own equipment."
```

**Notes:**
- Owner can only block their own equipment
- Prevents bookings during the blocked period

---

## 5. Check Equipment Availability

**Endpoint:** `POST /api/equipment/check-availability`

**Authorization:** None (Public)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-10T10:00:00Z",
  "endDate": "2026-02-10T16:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/equipment/check-availability" \
  -H "Content-Type: application/json" \
  -d '{
    "equipmentId": 1,
    "startDate": "2026-02-10T10:00:00Z",
    "endDate": "2026-02-10T16:00:00Z"
  }'
```

**Success Response (200):**
```json
"🟢 Available! You can proceed to booking."
```

**Error Responses:**
```json
"⏳ Minimum 2 hours required for hourly booking"
```
```json
"🚫 Equipment is not available on these dates (Owner Blocked)"
```
```json
"❌ Equipment already booked for the selected time"
```

**Notes:**
- Checks both owner-blocked dates and existing bookings
- For hourly equipment, minimum 2 hours required

---

## 6. Get Blocked Dates

**Endpoint:** `GET /api/equipment/blocked-dates/{equipmentId}`

**Authorization:** None (Public)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/equipment/blocked-dates/1"
```

**Success Response (200):**
```json
[
  {
    "startDate": "2026-02-10T00:00:00Z",
    "endDate": "2026-02-15T23:59:59Z",
    "reason": "Maintenance"
  }
]
```

**Empty Response:**
```json
"🟢 Equipment is fully available (no blocked dates)"
```

---

## 7. Upload Equipment Image

**Endpoint:** `POST /api/equipment/upload-image`

**Authorization:** Bearer Token (Owner role required)

**Content-Type:** `multipart/form-data`

**Request Body:**
- `equipmentId` (int) - Equipment ID
- `image` (file) - Image file (JPG, JPEG, PNG, WEBP, max 5MB)

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/equipment/upload-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "equipmentId=1" \
  -F "image=@/path/to/equipment.jpg"
```

**Android Kotlin Example:**
```kotlin
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
val equipmentIdPart = equipmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

val response = apiService.uploadEquipmentImage(
    token = "Bearer $token",
    equipmentId = equipmentIdPart,
    image = imagePart
)
```

**Success Response (200):**
```json
{
  "message": "✔ Image uploaded successfully",
  "imageId": 45,
  "imageUrl": "https://your-project.supabase.co/storage/v1/object/public/agrirent-images/equipment/1738667000_abc12345.jpg"
}
```

**Error Responses:**
```json
"❌ You can only upload images for your own equipment."
```
```json
"❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)"
```

---

## 8. Get Equipment Images

**Endpoint:** `GET /api/equipment/{equipmentId}/images`

**Authorization:** None (Public)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/equipment/1/images"
```

**Success Response (200):**
```json
[
  {
    "imageId": 45,
    "imageUrl": "https://supabase.co/.../equipment/1738667000_abc12345.jpg"
  },
  {
    "imageId": 46,
    "imageUrl": "https://supabase.co/.../equipment/1738667100_def67890.jpg"
  }
]
```

---

## 9. Delete Equipment Image

**Endpoint:** `DELETE /api/equipment/delete-image/{imageId}`

**Authorization:** Bearer Token (Owner role required)

**cURL Example:**
```bash
curl -X DELETE "https://your-api.com/api/equipment/delete-image/45" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
"✔ Image deleted successfully"
```

**Error Responses:**
```json
"❌ Image not found"
```
```json
"❌ You can only delete images from your own equipment."
```

**Notes:**
- Deletes image from both Supabase Storage and database
- Only owner can delete their equipment images

---

# 🌾 PRODUCT APIs

## 10. Public Product Listing

**Endpoint:** `GET /api/products/list`

**Authorization:** None (Public)

**Query Parameters:**
- `category` (string, optional) - Filter by category name
- `location` (string, optional) - Filter by location (partial match)

**Headers:**
- `Accept-Language` (optional) - Language code (en, gu, hi, etc.)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/products/list?category=Seeds&location=Ahmedabad" \
  -H "Accept-Language: gu"
```

**Success Response (200):**
```json
[
  {
    "productId": 10,
    "productName": "Wheat Seeds",
    "description": "High quality wheat seeds",
    "category": "Seeds",
    "price": 1200.00,
    "stock": 500,
    "unit": "Kg",
    "location": "Ahmedabad",
    "imageUrl": "https://...legacy-image.jpg",
    "images": [
      {
        "imageId": 78,
        "imageUrl": "https://supabase.co/.../product/1738667000_xyz123.jpg"
      }
    ],
    "seller": "AgriStore Pvt Ltd",
    "sellerMobile": "+919876543210"
  }
]
```

**Notes:**
- Only returns **Approved** products with `Stock > 0`
- Translations applied automatically based on `Accept-Language`
- Ordered by creation date (newest first)
- Both `imageUrl` (legacy) and `images` array are included

---

## 11. Product Details

**Endpoint:** `GET /api/products/{id}`

**Authorization:** None (Public)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/products/10" \
  -H "Accept-Language: en"
```

**Success Response (200):**
```json
{
  "productId": 10,
  "productName": "Wheat Seeds",
  "description": "High quality wheat seeds",
  "category": "Seeds",
  "price": 1200.00,
  "stock": 500,
  "unit": "Kg",
  "location": "Ahmedabad",
  "imageUrl": "https://...",
  "seller": {
    "fullName": "AgriStore Pvt Ltd",
    "mobileNumber": "+919876543210"
  }
}
```

**Error Response (404):**
```json
"Product not found"
```

---

## 12. Add Product (Seller)

**Endpoint:** `POST /api/seller/add-product`

**Authorization:** Bearer Token (Seller role required)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "productName": "Wheat Seeds",
  "description": "High quality wheat seeds",
  "price": 1200.00,
  "stock": 500,
  "unit": "Kg",
  "location": "Ahmedabad",
  "imageUrl": "https://example.com/image.jpg"
}
```

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/seller/add-product" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Wheat Seeds",
    "description": "Premium quality seeds",
    "price": 1200,
    "stock": 500,
    "unit": "Kg",
    "location": "Ahmedabad"
  }'
```

**Success Response (200):**
```json
"Product added successfully (Pending Admin Approval)"
```

**Error Response (400):**
```json
"You must have an active subscription to add products."
```

**Notes:**
- Requires **active subscription**
- Product status set to **Pending** (requires admin approval)
- Name and description auto-translated to English

---

## 13. View My Products (Seller)

**Endpoint:** `GET /api/seller/my-products`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/seller/my-products" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
[
  {
    "productId": 10,
    "sellerId": 15,
    "seller": {
      "userId": 15,
      "fullName": "AgriStore Pvt Ltd",
      "mobileNumber": "+919876543210"
    },
    "productName": "Wheat Seeds",
    "description": "High quality wheat seeds",
    "category": "Seeds",
    "price": 1200.00,
    "stock": 500,
    "unit": "Kg",
    "location": "Ahmedabad",
    "imageUrl": "https://...",
    "status": "Approved",
    "createdAt": "2026-01-20T14:30:00Z"
  }
]
```

**Notes:**
- Returns only products owned by authenticated seller
- Ordered by creation date (newest first)

---

## 14. Update Product (Seller)

**Endpoint:** `PUT /api/seller/update-product/{id}`

**Authorization:** Bearer Token (Seller role required)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "productName": "Premium Wheat Seeds",
  "description": "Updated description",
  "price": 1300.00,
  "stock": 600,
  "unit": "Kg",
  "location": "Ahmedabad",
  "imageUrl": "https://example.com/new-image.jpg"
}
```

**cURL Example:**
```bash
curl -X PUT "https://your-api.com/api/seller/update-product/10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Premium Wheat Seeds",
    "description": "Updated high quality seeds",
    "price": 1300,
    "stock": 600,
    "unit": "Kg",
    "location": "Ahmedabad"
  }'
```

**Success Response (200):**
```json
"Product updated successfully"
```

**Error Response (401):**
```json
"You can only update your own products."
```

**Notes:**
- Seller can only update their own products
- Status reset to **Pending** after update (requires re-approval)

---

## 15. Delete Product (Seller)

**Endpoint:** `DELETE /api/seller/delete-product/{id}`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X DELETE "https://your-api.com/api/seller/delete-product/10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
"Product deleted successfully"
```

**Error Response (401):**
```json
"You can only delete your own products."
```

---

## 16. Upload Product Image (Seller)

**Endpoint:** `POST /api/seller/upload-product-image`

**Authorization:** Bearer Token (Seller role required)

**Content-Type:** `multipart/form-data`

**Request Body:**
- `productId` (int) - Product ID
- `image` (file) - Image file (JPG, JPEG, PNG, WEBP, max 5MB)

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/seller/upload-product-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "productId=10" \
  -F "image=@/path/to/product.jpg"
```

**Android Kotlin Example:**
```kotlin
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
val productIdPart = productId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

val response = apiService.uploadProductImage(
    token = "Bearer $token",
    productId = productIdPart,
    image = imagePart
)
```

**Success Response (200):**
```json
{
  "message": "✔ Image uploaded successfully",
  "imageId": 78,
  "imageUrl": "https://your-project.supabase.co/storage/v1/object/public/agrirent-images/product/1738667000_xyz12345.jpg"
}
```

**Error Responses:**
```json
"❌ You can only upload images for your own products."
```
```json
"❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)"
```

---

## 17. Get Product Images (Seller)

**Endpoint:** `GET /api/seller/product-images/{productId}`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/seller/product-images/10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
[
  {
    "imageId": 78,
    "imageUrl": "https://supabase.co/.../product/1738667000_xyz12345.jpg"
  },
  {
    "imageId": 79,
    "imageUrl": "https://supabase.co/.../product/1738667100_abc67890.jpg"
  }
]
```

**Error Response (401):**
```json
"❌ You can only view images for your own products."
```

---

## 18. Get Product Images (Public)

**Endpoint:** `GET /api/products/{productId}/images`

**Authorization:** None (Public)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/products/10/images"
```

**Success Response (200):**
```json
[
  {
    "imageId": 78,
    "imageUrl": "https://supabase.co/.../product/1738667000_xyz12345.jpg"
  }
]
```

**Notes:**
- Public endpoint - no authentication required
- Anyone can view product images

---

## 19. Delete Product Image (Seller)

**Endpoint:** `DELETE /api/seller/delete-product-image/{imageId}`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X DELETE "https://your-api.com/api/seller/delete-product-image/78" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
"✔ Image deleted successfully"
```

**Error Responses:**
```json
"❌ Image not found"
```
```json
"❌ You can only delete images from your own products."
```

**Notes:**
- Deletes from both Supabase Storage and database
- Only seller can delete their product images

---

## 20. Place Order (Farmer)

**Endpoint:** `POST /api/products/place-order`

**Authorization:** Bearer Token (Farmer role required)

**Content-Type:** `application/json`

**Request Body:**
```json
{
  "productId": 10,
  "quantity": 50,
  "deliveryAddress": "123 Farm Road, Ahmedabad, Gujarat",
  "contactNumber": "+919876543210"
}
```

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/products/place-order" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 10,
    "quantity": 50,
    "deliveryAddress": "123 Farm Road, Ahmedabad",
    "contactNumber": "+919876543210"
  }'
```

**Success Response (200):**
```json
{
  "message": "Order placed successfully",
  "orderId": 155,
  "totalAmount": 60000.00
}
```

**Error Responses:**
```json
"Product not found"
```
```json
"Insufficient stock available"
```

**Notes:**
- Stock is automatically reduced
- Payment status set to **Pending**
- Order status set to **Pending**

---

## 21. View My Orders (Farmer)

**Endpoint:** `GET /api/products/my-orders`

**Authorization:** Bearer Token (Farmer role required)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/products/my-orders" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
[
  {
    "orderId": 155,
    "productName": "Wheat Seeds",
    "sellerName": "AgriStore Pvt Ltd",
    "quantity": 50,
    "unitPrice": 1200.00,
    "totalPrice": 60000.00,
    "status": "Pending",
    "paymentStatus": "Pending",
    "orderDate": "2026-02-04T10:30:00Z",
    "deliveryDate": null
  }
]
```

**Notes:**
- Returns only orders placed by authenticated farmer
- Ordered by date (newest first)

---

## 22. Cancel Order (Farmer)

**Endpoint:** `PUT /api/products/cancel-order/{orderId}`

**Authorization:** Bearer Token (Farmer role required)

**cURL Example:**
```bash
curl -X PUT "https://your-api.com/api/products/cancel-order/155" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Success Response (200):**
```json
"Order cancelled successfully"
```

**Error Responses:**
```json
"Order not found"
```
```json
"Only pending orders can be cancelled"
```

**Notes:**
- Only **Pending** orders can be cancelled
- Stock is automatically restored
- Farmer can only cancel their own orders

---

## 📱 Android Integration Examples

### Retrofit Interface Definition

```kotlin
interface ApiService {
    
    // ============ EQUIPMENT APIs ============
    
    @GET("api/public/equipment/available")
    suspend fun getAvailableEquipment(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("location") location: String? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Header("Accept-Language") language: String = "en"
    ): Response<List<Equipment>>
    
    @POST("api/equipment/add")
    suspend fun addEquipment(
        @Header("Authorization") token: String,
        @Body equipment: AddEquipmentDto
    ): Response<String>
    
    @GET("api/equipment/my-list")
    suspend fun getMyEquipment(
        @Header("Authorization") token: String
    ): Response<List<Equipment>>
    
    @POST("api/equipment/block-dates")
    suspend fun blockEquipmentDates(
        @Header("Authorization") token: String,
        @Body blockDate: BlockDateDto
    ): Response<String>
    
    @POST("api/equipment/check-availability")
    suspend fun checkEquipmentAvailability(
        @Body availability: CheckAvailabilityDto
    ): Response<String>
    
    @GET("api/equipment/blocked-dates/{equipmentId}")
    suspend fun getBlockedDates(
        @Path("equipmentId") equipmentId: Int
    ): Response<List<BlockedDate>>
    
    @Multipart
    @POST("api/equipment/upload-image")
    suspend fun uploadEquipmentImage(
        @Header("Authorization") token: String,
        @Part("equipmentId") equipmentId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
    
    @GET("api/equipment/{equipmentId}/images")
    suspend fun getEquipmentImages(
        @Path("equipmentId") equipmentId: Int
    ): Response<List<ImageResponse>>
    
    @DELETE("api/equipment/delete-image/{imageId}")
    suspend fun deleteEquipmentImage(
        @Header("Authorization") token: String,
        @Path("imageId") imageId: Int
    ): Response<String>
    
    // ============ PRODUCT APIs ============
    
    @GET("api/products/list")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
        @Header("Accept-Language") language: String = "en"
    ): Response<List<Product>>
    
    @GET("api/products/{id}")
    suspend fun getProductDetails(
        @Path("id") productId: Int,
        @Header("Accept-Language") language: String = "en"
    ): Response<Product>
    
    @POST("api/seller/add-product")
    suspend fun addProduct(
        @Header("Authorization") token: String,
        @Body product: AddProductDto
    ): Response<String>
    
    @GET("api/seller/my-products")
    suspend fun getMyProducts(
        @Header("Authorization") token: String
    ): Response<List<Product>>
    
    @PUT("api/seller/update-product/{id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("id") productId: Int,
        @Body product: AddProductDto
    ): Response<String>
    
    @DELETE("api/seller/delete-product/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") productId: Int
    ): Response<String>
    
    @Multipart
    @POST("api/seller/upload-product-image")
    suspend fun uploadProductImage(
        @Header("Authorization") token: String,
        @Part("productId") productId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
    
    @GET("api/seller/product-images/{productId}")
    suspend fun getMyProductImages(
        @Header("Authorization") token: String,
        @Path("productId") productId: Int
    ): Response<List<ImageResponse>>
    
    @GET("api/products/{productId}/images")
    suspend fun getProductImages(
        @Path("productId") productId: Int
    ): Response<List<ImageResponse>>
    
    @DELETE("api/seller/delete-product-image/{imageId}")
    suspend fun deleteProductImage(
        @Header("Authorization") token: String,
        @Path("imageId") imageId: Int
    ): Response<String>
    
    @POST("api/products/place-order")
    suspend fun placeOrder(
        @Header("Authorization") token: String,
        @Body order: CreateOrderDto
    ): Response<OrderResponse>
    
    @GET("api/products/my-orders")
    suspend fun getMyOrders(
        @Header("Authorization") token: String
    ): Response<List<Order>>
    
    @PUT("api/products/cancel-order/{orderId}")
    suspend fun cancelOrder(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: Int
    ): Response<String>
}
```

### Data Classes

```kotlin
data class Equipment(
    val equipmentId: Int,
    val equipmentName: String,
    val description: String,
    val priceType: String,
    val price: Double,
    val location: String,
    val imageUrl: String?,
    val images: List<ImageResponse> = emptyList(),
    val owner: String
)

data class Product(
    val productId: Int,
    val productName: String,
    val description: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val unit: String,
    val location: String,
    val imageUrl: String?,
    val images: List<ImageResponse> = emptyList(),
    val seller: String?,
    val sellerMobile: String?
)

data class ImageResponse(
    val imageId: Int,
    val imageUrl: String
)

data class ImageUploadResponse(
    val message: String,
    val imageId: Int,
    val imageUrl: String
)

data class AddEquipmentDto(
    val equipmentName: String,
    val description: String,
    val priceType: String,
    val price: Double,
    val location: String,
    val imageUrl: String? = null,
    val categoryId: Int
)

data class AddProductDto(
    val productName: String,
    val description: String,
    val price: Double,
    val stock: Int,
    val unit: String,
    val location: String,
    val imageUrl: String? = null
)

data class BlockDateDto(
    val equipmentId: Int,
    val startDate: String,
    val endDate: String,
    val reason: String?
)

data class CheckAvailabilityDto(
    val equipmentId: Int,
    val startDate: String,
    val endDate: String
)

data class CreateOrderDto(
    val productId: Int,
    val quantity: Int,
    val deliveryAddress: String,
    val contactNumber: String
)

data class OrderResponse(
    val message: String,
    val orderId: Int,
    val totalAmount: Double
)

data class Order(
    val orderId: Int,
    val productName: String,
    val sellerName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val status: String,
    val paymentStatus: String,
    val orderDate: String,
    val deliveryDate: String?
)
```

### Usage Examples

```kotlin
// Get equipment with filters
val equipmentList = apiService.getAvailableEquipment(
    location = "Ahmedabad",
    categoryId = 1,
    language = "gu"
)

// Upload equipment image
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
val equipmentIdPart = equipmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

val uploadResult = apiService.uploadEquipmentImage(
    token = "Bearer $jwtToken",
    equipmentId = equipmentIdPart,
    image = imagePart
)

// Get products
val products = apiService.getProducts(
    category = "Seeds",
    location = "Ahmedabad",
    language = "hi"
)

// Place order
val order = CreateOrderDto(
    productId = 10,
    quantity = 50,
    deliveryAddress = "123 Farm Road",
    contactNumber = "+919876543210"
)
val orderResult = apiService.placeOrder("Bearer $jwtToken", order)
```

---

## 🔐 Authentication & Authorization

### Equipment APIs
- **Public:** Available equipment listing, check availability, blocked dates, get images
- **Owner Role:** Add equipment, view my equipment, block dates, upload/delete images

### Product APIs
- **Public:** Product listing, product details, get product images
- **Seller Role:** Add/update/delete products, upload/delete product images
- **Farmer Role:** Place orders, view orders, cancel orders

### JWT Token Format
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🌐 Multi-Language Support

All equipment and product names/descriptions support automatic translation.

**Supported Languages:**
- `en` - English (default)
- `gu` - Gujarati
- `hi` - Hindi
- `mr` - Marathi
- And more via Azure Translator

**Usage:**
```bash
curl -H "Accept-Language: gu" https://api.com/api/products/list
```

---

## 📊 Status Values

### Equipment Status
- `Pending` - Awaiting admin approval
- `Approved` - Available for booking
- `Rejected` - Rejected by admin

### Product Status
- `Pending` - Awaiting admin approval
- `Approved` - Available for purchase
- `Rejected` - Rejected by admin

### Order Status
- `Pending` - Order placed, awaiting processing
- `Confirmed` - Order confirmed by seller
- `Delivered` - Order delivered
- `Cancelled` - Order cancelled

### Payment Status
- `Pending` - Payment not completed
- `Completed` - Payment successful
- `Failed` - Payment failed

---

## ⚠️ Error Handling

All endpoints follow consistent error response format:

**400 Bad Request:**
```json
"Error message in user's selected language"
```

**401 Unauthorized:**
```json
{
  "success": false,
  "statusCode": 401,
  "message": "Unauthorized access. Token is missing or invalid."
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "statusCode": 403,
  "message": "Access denied. Required role not found."
}
```

**404 Not Found:**
```json
"Resource not found"
```

**500 Internal Server Error:**
```json
"❌ Operation failed: {error details}"
```

---

## 📝 Best Practices

1. **Always include JWT token** for protected endpoints
2. **Use Accept-Language header** for multilingual support
3. **Upload multiple images** using the image upload endpoints
4. **Check availability** before booking equipment
5. **Validate stock** before placing product orders
6. **Handle both imageUrl and images array** for backward compatibility

---

## 🚀 Quick Start Checklist

- [ ] Configure Supabase credentials in appsettings.json
- [ ] Create `agrirent-images` bucket in Supabase
- [ ] Test authentication endpoints
- [ ] Test equipment listing and filtering
- [ ] Test product listing and ordering
- [ ] Test image upload functionality
- [ ] Implement error handling in Android app
- [ ] Add multi-language support in UI

---

**Last Updated:** February 4, 2026  
**API Version:** 1.0  
**Base URL:** `https://your-api-domain.com`
