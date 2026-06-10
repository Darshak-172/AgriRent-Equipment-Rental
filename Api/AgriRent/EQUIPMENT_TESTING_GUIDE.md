# 🧪 Equipment API - Step-by-Step Testing Guide

## Prerequisites
- API Testing Tool: Postman, Thunder Client, Insomnia, or similar
- Server running on: `http://localhost:5288`
- Valid JWT token with Owner role
- Valid JWT token with Farmer role (for public endpoints)

---

## 📝 Test Flow Overview

```
1. Login as Owner → Get JWT Token
2. Add Equipment → Get equipmentId
3. Upload Equipment Images (multiple)
4. View Own Equipment List
5. Block Dates for Equipment
6. View Blocked Dates
7. Login as Farmer → Get JWT Token
8. Check Equipment Availability (Farmer)
9. View Public Equipment List (Farmer)
10. Get Equipment Images (Farmer)
11. Login as Owner Again
12. Delete Equipment Image
```

---

## 🔐 Step 1: Login as Owner

**Endpoint:** `POST /api/auth/login`

**Headers:**
```
Content-Type: application/json
Accept-Language: en
```

**Request Body:**
```json
{
  "mobileNumber": "+919876543210",
  "password": "Owner@123"
}
```

**Expected Response:**
```json
{
  "message": "✔ Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "...",
  "userId": 1,
  "fullName": "John Owner",
  "roles": ["Owner"]
}
```

**Action:** Copy the `token` value for use in subsequent requests.

---

## ➕ Step 2: Add Equipment

**Endpoint:** `POST /api/equipment/add`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Request Body:**
```json
{
  "equipmentName": "John Deere Tractor 5050D",
  "description": "Powerful 50 HP tractor suitable for all farming operations. Well maintained, diesel engine, excellent condition.",
  "priceType": "Daily",
  "price": 1500.00,
  "location": "Ahmedabad",
  "subCategoryId": 1
}
```

**Expected Response:**
```json
{
  "message": "✔ Equipment added successfully (Pending Admin Approval)",
  "equipmentId": 1
}
```

**Action:** Copy the `equipmentId` for image upload.

**Note:** 
- Equipment status will be "Pending" until admin approves
- You must have an active subscription to add equipment
- equipmentName and description will be stored in English (auto-translated if needed)

---

## 📸 Step 3: Upload Equipment Images

**Endpoint:** `POST /api/equipment/upload-image`

**Headers:**
```
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Request Body:** (multipart/form-data)
```
equipmentId: 1
image: [Select file - tractor_front.jpg]
```

**Expected Response:**
```json
{
  "message": "✔ Image uploaded successfully",
  "imageId": 1,
  "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745623_a8f9d2e1.jpg"
}
```

**Repeat for Multiple Images:**
- Upload image 2: tractor_side.jpg → imageId: 2
- Upload image 3: tractor_back.jpg → imageId: 3

**Testing Notes:**
- Supported formats: JPG, JPEG, PNG, WEBP
- Maximum file size: 5MB
- Images stored in Cloudinary under `equipment/` folder
- Only equipment owner can upload images

---

## � How to Get Image URLs

There are **3 ways** to retrieve equipment image URLs:

### Method 1: Immediately After Upload
When you upload an image using `POST /api/equipment/upload-image`, the response includes the `imageUrl`:
```json
{
  "message": "✔ Image uploaded successfully",
  "imageId": 1,
  "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745623_a8f9d2e1.jpg"
}
```

### Method 2: Get Specific Equipment Images
Use `GET /api/equipment/{equipmentId}/images` to fetch all images for a specific equipment:
```json
[
  {
    "imageId": 1,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/..."
  },
  {
    "imageId": 2,
    "imageUrl": "https://..."
  }
]
```

### Method 3: In Equipment Lists
Both `GET /api/equipment/my-list` (Owner) and `GET /api/public/equipment/available` (Public) include an `images` array with all image URLs:
```json
{
  "equipmentId": 1,
  "equipmentName": "John Deere Tractor 5050D",
  "images": [
    { "imageId": 1, "imageUrl": "https://..." },
    { "imageId": 2, "imageUrl": "https://..." }
  ]
}
```

---

## �📋 Step 4: View Own Equipment List

**Endpoint:** `GET /api/equipment/my-list`

**Headers:**
```
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Expected Response:**
```json
[
  {
    "equipmentId": 1,
    "ownerId": 1,
    "owner": {
      "userId": 1,
      "fullName": "John Owner",
      "mobileNumber": "+919876543210"
    },
    "equipmentName": "John Deere Tractor 5050D",
    "description": "Powerful 50 HP tractor suitable for all farming operations...",
    "priceType": "Daily",
    "price": 1500.00,
    "location": "Ahmedabad",
    "subCategoryId": 1,
    "subCategoryName": "Tractors",
    "categoryId": 1,
    "categoryName": "Farm Equipment",
    "images": [
      {
        "imageId": 1,
        "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745623_a8f9d2e1.jpg"
      },
      {
        "imageId": 2,
        "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745645_b3c7e8f2.jpg"
      },
      {
        "imageId": 3,
        "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745678_d9a1f4c5.jpg"
      }
    ],
    "status": "Pending",
    "createdAt": "2026-02-05T10:30:00Z"
  }
]
```

**Verification Points:**
✅ Images array contains all uploaded images  
✅ SubCategory and Category information is populated  
✅ No `imageUrl` field in equipment (only in images array)  
✅ Status shows "Pending" (waiting for admin approval)

---

## 🚫 Step 5: Block Dates for Equipment

**Endpoint:** `POST /api/equipment/block-dates`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Request Body:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-10T00:00:00Z",
  "endDate": "2026-02-15T23:59:59Z",
  "reason": "Equipment under maintenance"
}
```

**Expected Response:**
```json
"🚫 Equipment marked unavailable for selected dates."
```

**Add Another Block:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-20T00:00:00Z",
  "endDate": "2026-02-22T23:59:59Z",
  "reason": "Already booked for another client"
}
```

**Testing Notes:**
- Only equipment owner can block dates
- Used to mark equipment unavailable for specific periods
- Farmers cannot book during blocked dates

---

## 📅 Step 6: View Blocked Dates

**Endpoint:** `GET /api/equipment/blocked-dates/1`

**Headers:**
```
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Expected Response:**
```json
[
  {
    "startDate": "2026-02-10T00:00:00Z",
    "endDate": "2026-02-15T23:59:59Z",
    "reason": "Equipment under maintenance"
  },
  {
    "startDate": "2026-02-20T00:00:00Z",
    "endDate": "2026-02-22T23:59:59Z",
    "reason": "Already booked for another client"
  }
]
```

---

## 👨‍🌾 Step 7: Login as Farmer

**Endpoint:** `POST /api/auth/login`

**Headers:**
```
Content-Type: application/json
Accept-Language: en
```

**Request Body:**
```json
{
  "mobileNumber": "+919123456789",
  "password": "Farmer@123"
}
```

**Expected Response:**
```json
{
  "message": "✔ Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "...",
  "userId": 5,
  "fullName": "Ramesh Patel",
  "roles": ["Farmer"]
}
```

**Action:** Copy the `token` value for farmer requests.

---

## ✅ Step 8: Check Equipment Availability (Farmer)

### Test Case 1: Available Dates

**Endpoint:** `POST /api/equipment/check-availability`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

**Request Body:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-16T08:00:00Z",
  "endDate": "2026-02-18T18:00:00Z"
}
```

**Expected Response:**
```json
"🟢 Available! You can proceed to booking."
```

### Test Case 2: Blocked Dates (Should Fail)

**Request Body:**
```json
{
  "equipmentId": 1,
  "startDate": "2026-02-10T08:00:00Z",
  "endDate": "2026-02-12T18:00:00Z"
}
```

**Expected Response:**
```json
"🚫 Equipment is not available on these dates (Owner Blocked)"
```

### Test Case 3: Hourly Booking (Less than 2 hours - Should Fail)

**Request Body:**
```json
{
  "equipmentId": 2,
  "startDate": "2026-02-16T08:00:00Z",
  "endDate": "2026-02-16T09:00:00Z"
}
```

**Note:** Equipment with `priceType: "Hourly"`

**Expected Response:**
```json
"⏳ Minimum 2 hours required for hourly booking"
```

---

## 🌍 Step 9: View Public Equipment List (Farmer)

### Test Case 1: Get All Approved Equipment

**Endpoint:** `GET /api/public/equipment/available`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

**Expected Response:**
```json
[
  {
    "equipmentId": 1,
    "equipmentName": "John Deere Tractor 5050D",
    "description": "Powerful 50 HP tractor suitable for all farming operations...",
    "priceType": "Daily",
    "price": 1500.00,
    "location": "Ahmedabad",
    "subCategoryId": 1,
    "subCategoryName": "Tractors",
    "categoryId": 1,
    "categoryName": "Farm Equipment",
    "images": [
      {
        "imageId": 1,
        "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/..."
      },
      {
        "imageId": 2,
        "imageUrl": "https://..."
      }
    ],
    "owner": "John Owner"
  }
]
```

**Note:** Only equipment with `Status = "Approved"` will appear.

### Test Case 2: Filter by Location

**Endpoint:** `GET /api/public/equipment/available?location=Ahmedabad`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

### Test Case 3: Filter by Category

**Endpoint:** `GET /api/public/equipment/available?categoryId=1`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

### Test Case 4: Filter by SubCategory

**Endpoint:** `GET /api/public/equipment/available?subCategoryId=1`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

### Test Case 5: Filter by Date Range (Exclude Blocked)

**Endpoint:** `GET /api/public/equipment/available?startDate=2026-02-10&endDate=2026-02-15`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

**Expected Result:** Equipment with ID 1 will NOT appear (blocked during these dates).

### Test Case 6: Multilingual Response (Gujarati)

**Endpoint:** `GET /api/public/equipment/available`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: gu
```

**Expected Response:**
```json
[
  {
    "equipmentId": 1,
    "equipmentName": "જોન ડીયર ટ્રેક્ટર 5050D",
    "description": "તમામ ખેતી કામગીરી માટે યોગ્ય શક્તિશાળી 50 એચપી ટ્રેક્ટર...",
    "priceType": "Daily",
    "price": 1500.00,
    "location": "Ahmedabad",
    "subCategoryId": 1,
    "subCategoryName": "Tractors",
    "categoryId": 1,
    "categoryName": "ખેતી સાધનો",
    "images": [...],
    "owner": "John Owner"
  }
]
```

**Supported Languages:** en, gu, hi, mr, ta, te, kn, ml, bn, pa

---

## 🖼️ Step 10: Get Equipment Images (Farmer)

**Endpoint:** `GET /api/equipment/1/images`

**Headers:**
```
Authorization: Bearer <FARMER_TOKEN>
Accept-Language: en
```

**Expected Response:**
```json
[
  {
    "imageId": 1,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745623_a8f9d2e1.jpg"
  },
  {
    "imageId": 2,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745645_b3c7e8f2.jpg"
  },
  {
    "imageId": 3,
    "imageUrl": "https://res.cloudinary.com/YOUR_CLOUD_NAME/image/upload/v1234567/equipment/1738745678_d9a1f4c5.jpg"
  }
]
```

---

## 🗑️ Step 11: Delete Equipment Image (Owner)

**Endpoint:** `DELETE /api/equipment/delete-image/2`

**Headers:**
```
Authorization: Bearer <OWNER_TOKEN>
Accept-Language: en
```

**Expected Response:**
```json
"✔ Image deleted successfully"
```

**Verify Deletion:**

**Endpoint:** `GET /api/equipment/1/images`

**Expected Response:**
```json
[
  {
    "imageId": 1,
    "imageUrl": "https://..."
  },
  {
    "imageId": 3,
    "imageUrl": "https://..."
  }
]
```

**Note:** imageId 2 should be removed.

---

## 🚨 Error Scenarios to Test

### 1. Upload Image Without Ownership
**Setup:** Use Farmer token to upload image for equipment owned by Owner  
**Expected:** `401 Unauthorized - "❌ You can only upload images for your own equipment."`

### 2. Delete Image Without Ownership
**Setup:** Use different Owner's token to delete image  
**Expected:** `401 Unauthorized - "❌ You can only delete images from your own equipment."`

### 3. Add Equipment Without Subscription
**Setup:** Owner without active subscription tries to add equipment  
**Expected:** `400 Bad Request - "❌ You must have an active subscription to add equipment."`

### 4. Upload Invalid Image Format
**Setup:** Upload .txt or .pdf file  
**Expected:** `400 Bad Request - "❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)"`

### 5. Upload Large Image (>5MB)
**Setup:** Upload image larger than 5MB  
**Expected:** `400 Bad Request - "❌ Invalid image. Supported: JPG, JPEG, PNG, WEBP (max 5MB)"`

### 6. Block Dates Without Ownership
**Setup:** Use different Owner's token to block dates  
**Expected:** `401 Unauthorized - "❌ You can only manage your own equipment."`

### 7. Check Availability for Non-existent Equipment
**Setup:** Use equipmentId that doesn't exist  
**Expected:** `404 Not Found - "❌ Equipment not found"`

---

## ✅ Validation Checklist

- [ ] Equipment added successfully with pending status
- [ ] Multiple images uploaded to Cloudinary
- [ ] Images array populated in equipment response
- [ ] No `imageUrl` field in Equipment table/response
- [ ] Category and SubCategory information properly retrieved
- [ ] Blocked dates prevent availability check
- [ ] Only approved equipment appears in public listing
- [ ] Filters work correctly (location, category, subcategory, date range)
- [ ] Multilingual translation works for equipment name, description, and category
- [ ] Image deletion removes from both Cloudinary and database
- [ ] Authorization checks prevent unauthorized access
- [ ] File validation rejects invalid formats and large files

---

## 🎯 Database Verification Queries

After testing, verify data in SQL Server:

```sql
-- Check Equipment (should NOT have ImageUrl column)
SELECT EquipmentId, EquipmentName, OwnerId, SubCategoryId, Status, CreatedAt
FROM Equipments;

-- Check Equipment Images
SELECT ImageId, EquipmentId, ImageUrl
FROM EquipmentImages
WHERE EquipmentId = 1;

-- Check Blocked Dates
SELECT * FROM EquipmentAvailability
WHERE EquipmentId = 1;

-- Check Category Relationship
SELECT 
    e.EquipmentId,
    e.EquipmentName,
    sc.SubCategoryName,
    c.Name AS CategoryName
FROM Equipments e
LEFT JOIN SubCategories sc ON e.SubCategoryId = sc.SubCategoryId
LEFT JOIN Categories c ON sc.CategoryId = c.CategoryId
WHERE e.EquipmentId = 1;
```

---

## 📊 Expected Data Flow

```
1. Owner adds equipment → Stored in Equipments table (Status: Pending)
2. Owner uploads images → Stored in EquipmentImages table + Cloudinary
3. Admin approves equipment → Status changed to "Approved"
4. Equipment appears in public listing (GET /api/public/equipment/available)
5. Farmer checks availability → Validates against blocked dates and bookings
6. Images retrieved from EquipmentImages table (NOT from Equipment table)
7. Category info accessed via SubCategoryId → SubCategory → CategoryId → Category
```

---

## 🔧 Troubleshooting

### Images not appearing?
- Check Cloudinary dashboard
- Verify API keys in appsettings.json
- Check EquipmentImages table has records

### Equipment not showing in public list?
- Verify Status = "Approved" (not "Pending")
- Check if date filter is excluding it due to blocked dates

### Category/SubCategory is null?
- Ensure SubCategoryId exists in SubCategories table
- Verify foreign key relationship is intact

### Translation not working?
- Check Accept-Language header is set
- Verify Azure Translator service is configured in appsettings.json

---

## 📌 Summary

This testing guide validates:
✅ Equipment creation without ImageUrl in database  
✅ Image storage in separate EquipmentImages table  
✅ Cloudinary integration  
✅ Category hierarchy (Equipment → SubCategory → Category)  
✅ Authorization and ownership validation  
✅ Date blocking and availability checking  
✅ Multilingual support  
✅ Public and private endpoint separation  

All endpoints follow the schema requirement: **Equipment table has NO ImageUrl column** - images are stored only in EquipmentImages table with proper foreign key relationship.
