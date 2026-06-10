# 📸 Image Upload API Documentation

## Overview
This document describes the image upload functionality for Equipment and Product images using Cloudinary.

---

## 🔧 Configuration

### 1. Update `appsettings.json`
Add your Cloudinary credentials:

```json
{
  "Cloudinary": {
    "CloudName": "YOUR_CLOUDINARY_CLOUD_NAME",
    "ApiKey": "YOUR_CLOUDINARY_API_KEY",
    "ApiSecret": "YOUR_CLOUDINARY_API_SECRET"
  }
}
```

### 2. Configure Cloudinary Account
1. Create a free account at [Cloudinary](https://cloudinary.com/).
2. Get your `Cloud Name`, `API Key`, and `API Secret` from your dashboard.
3. No need to manually create folders, they will be auto-generated upon upload.

---

## 📋 Equipment Image APIs

### 1️⃣ Upload Equipment Image

**Endpoint:** `POST /api/equipment/upload-image`

**Authorization:** Bearer Token (Owner role required)

**Content-Type:** `multipart/form-data`

**Request Body:**
- `equipmentId` (int) - The ID of the equipment
- `image` (file) - Image file (JPG, JPEG, PNG, WEBP, max 5MB)

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/equipment/upload-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Accept-Language: en" \
  -F "equipmentId=123" \
  -F "image=@/path/to/image.jpg"
```

**Android Kotlin Example:**
```kotlin
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
val equipmentIdPart = equipmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

val call = apiService.uploadEquipmentImage(
    token = "Bearer $token",
    equipmentId = equipmentIdPart,
    image = imagePart
)
```

**Success Response (200):**
```json
{
  "message": "✔ Image uploaded successfully",
  "imageId": 42,
  "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738666899_a1b2c3d4.jpg"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid/missing token or not the equipment owner
- `400 Bad Request` - Invalid image format or size exceeds 5MB
- `500 Internal Server Error` - Upload failed

---

### 2️⃣ Get Equipment Images

**Endpoint:** `GET /api/equipment/{equipmentId}/images`

**Authorization:** None (Public)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/equipment/123/images" \
  -H "Accept-Language: en"
```

**Success Response (200):**
```json
[
  {
    "imageId": 42,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738666899_a1b2c3d4.jpg"
  },
  {
    "imageId": 43,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738666920_e5f6g7h8.jpg"
  }
]
```

---

### 3️⃣ Delete Equipment Image

**Endpoint:** `DELETE /api/equipment/delete-image/{imageId}`

**Authorization:** Bearer Token (Owner role required)

**cURL Example:**
```bash
curl -X DELETE "https://your-api.com/api/equipment/delete-image/42" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Accept-Language: en"
```

**Success Response (200):**
```json
"✔ Image deleted successfully"
```

**Error Responses:**
- `401 Unauthorized` - Not the equipment owner
- `404 Not Found` - Image not found

---

## 📦 Product Image APIs

### 1️⃣ Upload Product Image (Seller)

**Endpoint:** `POST /api/seller/upload-product-image`

**Authorization:** Bearer Token (Seller role required)

**Content-Type:** `multipart/form-data`

**Request Body:**
- `productId` (int) - The ID of the product
- `image` (file) - Image file (JPG, JPEG, PNG, WEBP, max 5MB)

**cURL Example:**
```bash
curl -X POST "https://your-api.com/api/seller/upload-product-image" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Accept-Language: en" \
  -F "productId=456" \
  -F "image=@/path/to/product.jpg"
```

**Android Kotlin Example:**
```kotlin
val file = File(imagePath)
val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
val productIdPart = productId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

val call = apiService.uploadProductImage(
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
  "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/product/1738667000_i9j0k1l2.jpg"
}
```

---

### 2️⃣ Get Product Images (Seller)

**Endpoint:** `GET /api/seller/product-images/{productId}`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/seller/product-images/456" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Accept-Language: en"
```

**Success Response (200):**
```json
[
  {
    "imageId": 78,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/product/1738667000_i9j0k1l2.jpg"
  }
]
```

---

### 3️⃣ Delete Product Image (Seller)

**Endpoint:** `DELETE /api/seller/delete-product-image/{imageId}`

**Authorization:** Bearer Token (Seller role required)

**cURL Example:**
```bash
curl -X DELETE "https://your-api.com/api/seller/delete-product-image/78" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Accept-Language: en"
```

**Success Response (200):**
```json
"✔ Image deleted successfully"
```

---

### Public Product Images API

**Endpoint:** `GET /api/products/{productId}/images`

**Authorization:** None (Public access)

**cURL Example:**
```bash
curl -X GET "https://your-api.com/api/products/456/images"
```

**Success Response (200):**
```json
[
  {
    "imageId": 78,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/product/1738667000_i9j0k1l2.jpg"
  }
]
```

**Note:** This public endpoint allows anyone to view product images. Use the seller-specific endpoints (`/api/seller/*`) for managing your own product images.

---

## 🔐 Image Validation Rules

1. **Allowed Formats:** JPG, JPEG, PNG, WEBP
2. **Max File Size:** 5 MB
3. **Filename Generation:** `{timestamp}_{random8chars}.{extension}`
   - Example: `1738666899_a1b2c3d4.jpg`
4. **Storage Structure:**
   - Equipment images: `equipment/1738666899_a1b2c3d4.jpg`
   - Product images: `product/1738667000_i9j0k1l2.jpg`

---

## 🎯 Android Implementation Guide

### 1. Add Dependencies (build.gradle)
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
```

### 2. Create API Service Interface
```kotlin
interface ApiService {
    // Equipment Image APIs (Owner)
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

    // Product Image APIs (Seller)
    @Multipart
    @POST("api/seller/upload-product-image")
    suspend fun uploadProductImage(
        @Header("Authorization") token: String,
        @Part("productId") productId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    @GET("api/seller/product-images/{productId}")
    suspend fun getProductImagesForSeller(
        @Header("Authorization") token: String,
        @Path("productId") productId: Int
    ): Response<List<ImageResponse>>

    @DELETE("api/seller/delete-product-image/{imageId}")
    suspend fun deleteProductImage(
        @Header("Authorization") token: String,
        @Path("imageId") imageId: Int
    ): Response<String>

    // Public Product Image API
    @GET("api/products/{productId}/images")
    suspend fun getProductImages(
        @Path("productId") productId: Int
    ): Response<List<ImageResponse>>
}

data class ImageUploadResponse(
    val message: String,
    val imageId: Int,
    val imageUrl: String
)

data class ImageResponse(
    val imageId: Int,
    val imageUrl: String
)
```

### 3. Upload Image Function
```kotlin
suspend fun uploadEquipmentImage(
    equipmentId: Int,
    imageUri: Uri,
    context: Context
): Result<ImageUploadResponse> {
    return try {
        val file = getFileFromUri(context, imageUri)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val equipmentIdPart = equipmentId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val token = getAuthToken() // Your token retrieval method
        val response = apiService.uploadEquipmentImage(
            token = "Bearer $token",
            equipmentId = equipmentIdPart,
            image = imagePart
        )

        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception(response.errorBody()?.string() ?: "Upload failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun getFileFromUri(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    file.outputStream().use { outputStream ->
        inputStream?.copyTo(outputStream)
    }
    return file
}
```

### 4. Usage Example in Activity/Fragment
```kotlin
// Launch image picker
val pickImageLauncher = registerForActivityResult(
    ActivityResultContracts.GetContent()
) { uri: Uri? ->
    uri?.let { uploadImage(it) }
}

// Trigger picker
pickImageLauncher.launch("image/*")

// Upload function
private fun uploadImage(imageUri: Uri) {
    lifecycleScope.launch {
        try {
            showLoading(true)
            val result = uploadEquipmentImage(
                equipmentId = currentEquipmentId,
                imageUri = imageUri,
                context = requireContext()
            )
            
            result.onSuccess { response ->
                Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                // Load the uploaded image using response.imageUrl
                loadImage(response.imageUrl)
            }
            
            result.onFailure { error ->
                Toast.makeText(context, "Upload failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            showLoading(false)
        }
    }
}
```

---

## 🚀 Testing

### Using Postman
1. Set method to `POST`
2. Enter URL: `{{base_url}}/api/equipment/upload-image`
3. Go to **Authorization** → Type: Bearer Token → Enter your JWT
4. Go to **Body** → Select `form-data`
5. Add key `equipmentId` (Text) → Value: `123`
6. Add key `image` (File) → Select your image file
7. Click **Send**

### Using cURL
```bash
# Upload Equipment Image
curl -X POST "http://localhost:5000/api/equipment/upload-image" \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "equipmentId=123" \
  -F "image=@test_image.jpg"

# Get Equipment Images
curl -X GET "http://localhost:5000/api/equipment/123/images"

# Delete Equipment Image
curl -X DELETE "http://localhost:5000/api/equipment/delete-image/42" \
  -H "Authorization: Bearer eyJhbGc..."
```

---

## ⚠️ Common Issues & Solutions

### 1. 401 Unauthorized
- **Cause:** Invalid JWT token or user doesn't own the equipment/product
- **Solution:** Verify token is valid and user is the owner

### 2. 400 Bad Request - Invalid Image
- **Cause:** File format not supported or file size > 5MB
- **Solution:** Use JPG, PNG, WEBP and compress large images

### 3. 500 Internal Server Error
- **Cause:** Cloudinary credentials incorrect or upload limit reached
- **Solution:** Check `appsettings.json` and verify your dashboard

---

## 📊 Database Schema

### EquipmentImage Table
```sql
CREATE TABLE EquipmentImages (
    ImageId INT PRIMARY KEY IDENTITY(1,1),
    EquipmentId INT NOT NULL,
    ImageUrl NVARCHAR(255) NOT NULL,
    FOREIGN KEY (EquipmentId) REFERENCES Equipments(EquipmentId) ON DELETE CASCADE
);
```

### ProductImage Table
```sql
CREATE TABLE ProductImages (
    ImageId INT PRIMARY KEY IDENTITY(1,1),
    ProductId INT NOT NULL,
    ImageUrl NVARCHAR(255) NOT NULL,
    FOREIGN KEY (ProductId) REFERENCES Products(ProductId) ON DELETE CASCADE
);
```

---

## 🎨 Flow Diagram

```
┌─────────────┐
│ Android App │
└──────┬──────┘
       │ 1. Select Image from Gallery
       ▼
┌─────────────┐
│   Convert   │
│  Uri → File │
└──────┬──────┘
       │ 2. Create MultipartBody.Part
       ▼
┌─────────────────────┐
│ POST /upload-image  │
│ + JWT Token         │
│ + equipmentId       │
│ + image file        │
└──────┬──────────────┘
       │ 3. Backend validates
       ▼
┌─────────────────────┐
│ Check Ownership     │
│ Validate File Type  │
│ Validate File Size  │
└──────┬──────────────┘
       │ 4. Upload to Cloudinary
       ▼
┌─────────────────────┐
│ Cloudinary Storage  │
│ /equipment/         │
│ timestamp_rand.jpg  │
└──────┬──────────────┘
       │ 5. Get public URL
       ▼
┌─────────────────────┐
│ Save URL to DB      │
│ EquipmentImages     │
└──────┬──────────────┘
       │ 6. Return response
       ▼
┌─────────────┐
│ Android App │
│ Display URL │
└─────────────┘
```

---

## ✅ Implementation Checklist

- [x] Add CloudinaryDotNet NuGet package
- [x] Configure Cloudinary settings in appsettings.json
- [x] Create CloudinaryStorageService
- [x] Create image upload DTOs
- [x] Add Equipment image upload endpoint
- [x] Add Product image upload endpoint
- [x] Register service in Program.cs

---

## 📝 Notes

1. **File Naming:** Uses timestamp + random string to prevent collisions
2. **Storage Organization:** Images separated by folder (equipment/ and product/)
3. **Security:** Only owners can upload/delete their images
4. **Public Access:** Anyone can view images (GET endpoints)
5. **Cleanup:** Deleting image removes both database record and Cloudinary file
