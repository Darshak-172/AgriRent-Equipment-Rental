# 💳 Subscription API Documentation

## Overview

The Subscription API handles the complete subscription lifecycle in AgriRent, including plan management, payment processing via Razorpay, role-based access control, and automatic expiry management. This system enables users to upgrade from Farmers to Equipment Owners and/or Sellers through paid subscription plans.

---

## 🏗️ Architecture

### Three Main Controllers

1. **SubscriptionPlansController** - View subscription plans (Public)
2. **SubscriptionController** - Payment processing and subscription activation (User)
3. **AdminSubscriptionController** - Plan management (Admin only)

### Key Features

- ✅ Razorpay payment integration with signature verification
- ✅ Automatic role assignment based on plan limits
- ✅ Subscription renewal with automatic extension
- ✅ Background service for automatic expiry management
- ✅ Multi-language support (English, Hindi, Gujarati)
- ✅ Concurrent subscriptions for multiple plans

---

## 📊 Data Models

### SubscriptionPlan Model

```json
{
  "planId": 1,
  "planName": "Basic Plan",
  "durationDays": 30,
  "price": 999.00,
  "maxEquipment": 5,
  "maxProducts": 10,
  "status": "Active"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `planId` | INT | Primary key, auto-increment |
| `planName` | STRING | Plan name (e.g., "Basic", "Premium") |
| `durationDays` | INT | Subscription duration (1-3650 days) |
| `price` | DECIMAL | Price in Indian Rupees (₹) |
| `maxEquipment` | INT | Maximum equipment listings allowed |
| `maxProducts` | INT | Maximum products allowed |
| `status` | STRING | "Active" or "Inactive" |

### UserSubscription Model

```json
{
  "subscriptionId": 1,
  "userId": 5,
  "planId": 1,
  "startDate": "2026-02-19T10:30:00Z",
  "endDate": "2026-03-21T10:30:00Z",
  "paymentStatus": "Paid",
  "status": "Active",
  "paymentId": "pay_xxxxx"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `subscriptionId` | INT | Primary key, auto-increment |
| `userId` | INT | Foreign key to Users table |
| `planId` | INT | Foreign key to SubscriptionPlans table |
| `startDate` | DATETIME | Subscription start date (UTC) |
| `endDate` | DATETIME | Subscription expiry date (UTC) |
| `paymentStatus` | STRING | "Paid", "Pending", or "Failed" |
| `status` | STRING | "Active", "Expired", or "Cancelled" |
| `paymentId` | STRING | Razorpay payment transaction ID |

### BuySubscriptionDto

```json
{
  "planId": 1,
  "paymentId": "pay_xxxxx",
  "orderId": "order_xxxxx",
  "signature": "xxxxx"
}
```

---

## 🔐 Authentication & Authorization

### Bearer Token Required

All endpoints except `GET /api/subscription-plans` require JWT bearer token in header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Role-Based Access

- **Farmer** - Can view plans and purchase subscriptions
- **Owner** - Can list equipment (granted if plan has `maxEquipment > 0`)
- **Seller** - Can list products (granted if plan has `maxProducts > 0`)
- **Admin** - Can create, update, and manage plans

---

## 🌍 Multi-Language Support

All endpoints support automatic language detection via the `Accept-Language` header.

**Supported Languages:**
- `en-IN` (English) - Default
- `hi-IN` (Hindi)
- `gu-IN` (Gujarati)

**Examples:**
```
Accept-Language: en
Accept-Language: hi
Accept-Language: gu
Accept-Language: hi-IN
```

---

## 📚 API Endpoints

### 1️⃣ Public Endpoints

---

#### 1.1 Get All Active Plans

**Endpoint:** `GET /api/subscription-plans`

**Authentication:** Not required (AllowAnonymous)

**Description:** Retrieve list of all active subscription plans

**Query Parameters:** None

**Response (200 OK):**

```json
{
  "message": "Subscription plans list",
  "total": 3,
  "data": [
    {
      "planId": 1,
      "planName": "Basic Plan",
      "durationDays": 30,
      "price": 999.00,
      "maxEquipment": 5,
      "maxProducts": 10
    },
    {
      "planId": 2,
      "planName": "Premium Plan",
      "durationDays": 90,
      "price": 2499.00,
      "maxEquipment": 20,
      "maxProducts": 50
    },
    {
      "planId": 3,
      "planName": "Elite Plan",
      "durationDays": 180,
      "price": 4999.00,
      "maxEquipment": 100,
      "maxProducts": 200
    }
  ]
}
```

**Error Responses:**
- `500 Internal Server Error` - Database or translation service error

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/subscription-plans" \
  -H "Accept-Language: en"
```

---

### 2️⃣ User Subscription Endpoints

---

#### 2.1 Create Razorpay Order

**Endpoint:** `POST /api/subscription/create-order/{planId}`

**Authentication:** Required (Bearer Token)

**Authorization:** Any authenticated user (Farmer role minimum)

**Description:** Create a Razorpay payment order for a subscription plan

**URL Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `planId` | INT | Yes | ID of the subscription plan |

**Request Headers:**

```
Authorization: Bearer {token}
Accept-Language: en
```

**Request Body:** None

**Response (200 OK):**

```json
{
  "orderId": "order_xxxxx",
  "key": "rzp_live_xxxxx",
  "amount": 999.00,
  "planId": 1,
  "receipt": "rcpt_1_20260219101530"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `orderId` | STRING | Razorpay order ID |
| `key` | STRING | Razorpay API key for checkout |
| `amount` | DECIMAL | Plan price in rupees |
| `planId` | INT | Selected plan ID |
| `receipt` | STRING | Internal receipt reference (≤40 chars) |

**Error Responses:**
- `400 Bad Request` - Invalid plan ID
- `404 Not Found` - Plan not found
- `401 Unauthorized` - Missing or invalid token
- `500 Internal Server Error` - Razorpay configuration error

**Example Request:**

```bash
curl -X POST "http://localhost:5000/api/subscription/create-order/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"
```

**Razorpay Payment Flow:**

```
1. Client calls this endpoint
2. Server creates Razorpay Order
3. Client receives orderId + key
4. Client opens Razorpay checkout modal
5. User completes payment
6. Client receives paymentId + signature
7. Client calls verify-payment endpoint
```

---

#### 2.2 Verify Payment & Activate Subscription

**Endpoint:** `POST /api/subscription/verify-payment`

**Authentication:** Required (Bearer Token)

**Authorization:** Any authenticated user

**Description:** Verify Razorpay payment signature and activate subscription. This endpoint:
- Verifies the payment using HMAC-SHA256 signature
- Creates or renews a subscription
- Automatically assigns Owner/Seller roles based on plan limits
- Returns updated JWT token with new roles

**Request Headers:**

```
Authorization: Bearer {token}
Content-Type: application/json
Accept-Language: en
```

**Request Body:**

```json
{
  "planId": 1,
  "paymentId": "pay_xxxxx",
  "orderId": "order_xxxxx",
  "signature": "xxxxx"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `planId` | INT | Yes | Subscription plan ID (1-1000) |
| `paymentId` | STRING | Yes | Razorpay payment ID (5-200 chars) |
| `orderId` | STRING | Yes | Razorpay order ID (5-200 chars) |
| `signature` | STRING | Yes | HMAC-SHA256 signature verification |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Subscription Activated & Access Updated Successfully!",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "roles": ["Farmer", "Owner", "Seller"],
  "validFrom": "2026-02-19T10:30:00Z",
  "validTill": "2026-03-21T10:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `success` | BOOLEAN | Always true on success |
| `message` | STRING | Success message (multi-language) |
| `token` | STRING | New JWT token with updated roles |
| `roles` | ARRAY | User's current roles after activation |
| `validFrom` | DATETIME | Subscription start date |
| `validTill` | DATETIME | Subscription expiry date |

**Subscription Behavior:**

| Scenario | Action |
|----------|--------|
| **New Subscription** | Creates new UserSubscription record for this plan |
| **Existing Active Subscription** | Extends EndDate by plan duration (renewal) |
| **Expired Subscription** | Creates new record (treated as new subscription) |
| **Multiple Plans** | Allows multiple concurrent subscriptions |

**Role Assignment Logic:**

```
IF plan.maxEquipment > 0:
  └─ Assign "Owner" role

IF plan.maxProducts > 0:
  └─ Assign "Seller" role

ALWAYS:
  └─ Retain "Farmer" role (base role)
```

**Error Responses:**
- `400 Bad Request` - Invalid signature, payment verification failed
- `404 Not Found` - Plan not found
- `401 Unauthorized` - Missing or invalid token
- `500 Internal Server Error` - Database or Razorpay error

**Example Request:**

```bash
curl -X POST "http://localhost:5000/api/subscription/verify-payment" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{
    "planId": 1,
    "paymentId": "pay_IzydBU2Y8bSz3a",
    "orderId": "order_IzydBU2Y8bSz3a",
    "signature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
  }'
```

**Important Notes:**

- ✅ Signature is automatically verified server-side using HMAC-SHA256
- ✅ Payment status is set to "Paid" only after successful verification
- ✅ Expired subscriptions are automatically cleaned up by background service
- ✅ Concurrent subscriptions are supported (multiple plans possible)
- ✅ Extension logic: If EndDate is future-dated, extend from EndDate; otherwise from today

---

#### 2.3 Get My Active Subscriptions

**Endpoint:** `GET /api/subscription/my-subscription`

**Authentication:** Required (Bearer Token)

**Authorization:** Any authenticated user

**Description:** Retrieve all active subscriptions for the current user

**Request Headers:**

```
Authorization: Bearer {token}
Accept-Language: en
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": [
    {
      "subscriptionId": 1,
      "planId": 1,
      "planName": "Basic Plan",
      "startDate": "2026-02-19T10:30:00Z",
      "endDate": "2026-03-21T10:30:00Z",
      "status": "Active"
    },
    {
      "subscriptionId": 2,
      "planId": 2,
      "planName": "Premium Plan",
      "startDate": "2026-02-19T11:00:00Z",
      "endDate": "2026-05-20T11:00:00Z",
      "status": "Active"
    }
  ]
}
```

**Response Details:**
- Returns only subscriptions where:
  - `status = "Active"`
  - `endDate > current UTC time`
- Multiple subscriptions can be active simultaneously

**Error Responses:**
- `401 Unauthorized` - Missing or invalid token

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/subscription/my-subscription" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"
```

---

#### 2.4 Check Expired Subscriptions (Admin)

**Endpoint:** `POST /api/subscription/check-expiry`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Description:** Manually trigger expiration check and role deactivation. Normally runs automatically every hour via background service.

**Request Headers:**

```
Authorization: Bearer {admin_token}
Accept-Language: en
```

**Request Body:** None

**Response (200 OK):**

```json
{
  "status": "3 subscriptions expired and roles removed."
}
```

**What This Does:**

1. Finds all subscriptions with `endDate < current UTC time`
2. Updates status to "Expired"
3. Removes Owner/Seller roles from affected users
4. Ensures users retain Farmer role
5. Returns count of deactivated subscriptions

**Error Responses:**
- `401 Unauthorized` - Missing/invalid token or not an Admin

**Example Request:**

```bash
curl -X POST "http://localhost:5000/api/subscription/check-expiry" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

---

### 3️⃣ Admin Subscription Management Endpoints

---

#### 3.1 Create New Subscription Plan

**Endpoint:** `POST /api/admin/subscription/add-plan`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Description:** Add a new subscription plan

**Request Headers:**

```
Authorization: Bearer {admin_token}
Content-Type: application/json
Accept-Language: en
```

**Request Body:**

```json
{
  "planName": "Enterprise Plan",
  "durationDays": 365,
  "price": 9999.00,
  "maxEquipment": 500,
  "maxProducts": 1000,
  "status": "Active"
}
```

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `planName` | STRING | Yes | 3-100 chars, non-empty | Unique plan name |
| `durationDays` | INT | Yes | 1-3650 | Subscription period |
| `price` | DECIMAL | Yes | ≥ 0 | Plan price in rupees |
| `maxEquipment` | INT | Yes | 0-1000 | Equipment posting limit |
| `maxProducts` | INT | Yes | 0-1000 | Product posting limit |
| `status` | STRING | No | Default: "Active" | "Active" or "Inactive" |

**Response (200 OK):**

```json
{
  "message": "Subscription plan added successfully",
  "planId": 4
}
```

**Error Responses:**
- `400 Bad Request` - Invalid plan data (missing required fields, invalid ranges)
- `401 Unauthorized` - Missing/invalid token or not an Admin
- `500 Internal Server Error` - Database error

**Validation Rules:**
- ❌ PlanName cannot be empty or null
- ❌ Price must be ≥ 0
- ❌ DurationDays must be between 1-3650
- ❌ MaxEquipment must be between 0-1000
- ❌ MaxProducts must be between 0-1000

**Example Request:**

```bash
curl -X POST "http://localhost:5000/api/admin/subscription/add-plan" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{
    "planName": "Enterprise Plan",
    "durationDays": 365,
    "price": 9999.00,
    "maxEquipment": 500,
    "maxProducts": 1000,
    "status": "Active"
  }'
```

---

#### 3.2 Get All Subscription Plans

**Endpoint:** `GET /api/admin/subscription/plans`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Description:** Retrieve all subscription plans (active and inactive)

**Query Parameters:** None

**Request Headers:**

```
Authorization: Bearer {admin_token}
Accept-Language: en
```

**Response (200 OK):**

```json
{
  "message": "Subscription plans list",
  "total": 4,
  "data": [
    {
      "planId": 1,
      "planName": "Basic Plan",
      "durationDays": 30,
      "price": 999.00,
      "maxEquipment": 5,
      "maxProducts": 10,
      "status": "Active"
    },
    {
      "planId": 2,
      "planName": "Premium Plan",
      "durationDays": 90,
      "price": 2499.00,
      "maxEquipment": 20,
      "maxProducts": 50,
      "status": "Active"
    },
    {
      "planId": 3,
      "planName": "Elite Plan",
      "durationDays": 180,
      "price": 4999.00,
      "maxEquipment": 100,
      "maxProducts": 200,
      "status": "Active"
    },
    {
      "planId": 4,
      "planName": "Old Plan",
      "durationDays": 60,
      "price": 1500.00,
      "maxEquipment": 10,
      "maxProducts": 20,
      "status": "Inactive"
    }
  ]
}
```

**Response Details:**
- Includes both "Active" and "Inactive" plans
- All plan data is translated based on `Accept-Language` header
- `total` shows count of all plans

**Error Responses:**
- `401 Unauthorized` - Missing/invalid token or not an Admin

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/admin/subscription/plans" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

---

#### 3.3 Change Plan Status

**Endpoint:** `PUT /api/admin/subscription/status/{id}`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Description:** Toggle subscription plan status between Active and Inactive

**URL Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | INT | Yes | Plan ID to update |

**Request Headers:**

```
Authorization: Bearer {admin_token}
Accept-Language: en
```

**Request Body:** None

**Response (200 OK):**

```json
{
  "planId": 3,
  "status": "Inactive",
  "message": "Plan deactivated successfully"
}
```

**OR (if activating):**

```json
{
  "planId": 3,
  "status": "Active",
  "message": "Plan activated successfully"
}
```

**Behavior:**
- If current status is "Active" → changes to "Inactive"
- If current status is "Inactive" → changes to "Active"
- This toggle prevents users from purchasing inactive plans

**Error Responses:**
- `404 Not Found` - Plan with given ID doesn't exist
- `401 Unauthorized` - Missing/invalid token or not an Admin

**Important Notes:**
- ⚠️ Deactivating a plan doesn't affect existing active subscriptions
- ⚠️ Users cannot purchase deactivated plans
- ⚠️ Existing subscriptions remain valid until expiry

**Example Request (Deactivate):**

```bash
curl -X PUT "http://localhost:5000/api/admin/subscription/status/3" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

**Example Request (Activate):**

```bash
curl -X PUT "http://localhost:5000/api/admin/subscription/status/3" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

---

## 🔄 Complete Subscription Flow

### Step-by-Step Purchase Process

```
┌─────────────────────────────────────────────────────────┐
│ 1. User Views Available Plans                           │
│    GET /api/subscription-plans                          │
│    (No auth required)                                   │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ 2. User Creates Razorpay Order                          │
│    POST /api/subscription/create-order/1               │
│    (Auth required, Returns: orderId + key)             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ 3. Mobile App Opens Razorpay Checkout                   │
│    (Checkout modal handles payment UX)                 │
│    Client receives: paymentId + signature              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ 4. Verify Payment & Activate Subscription              │
│    POST /api/subscription/verify-payment               │
│    (Signature verification + role assignment)          │
│    Returns: New JWT token with roles                   │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│ 5. User Can Now:                                        │
│    ✅ Add Equipment (if Owner role)                     │
│    ✅ Add Products (if Seller role)                     │
│    ✅ View Active Subscriptions                         │
└─────────────────────────────────────────────────────────┘
```

---

## ⏰ Subscription Lifecycle

```
Subscription Created
        ↓
    ACTIVE (startDate to endDate)
        ↓
    EXPIRED (endDate < utcNow)
        ↓
    Background Service Deactivates
        ↓
    Roles Removed (Owner/Seller)
        ↓
    User Reverts to Farmer Role
```

### Background Service

- **Service Name:** `SubscriptionExpiryBackgroundService`
- **Running Interval:** Every 1 hour
- **What It Does:**
  1. Finds subscriptions with `endDate < DateTime.UtcNow`
  2. Updates status to "Expired"
  3. Removes Owner/Seller roles
  4. Guarantees "Farmer" role is retained
  5. Logs expired subscription count

### Role Assignment/Removal Rules

| Plan Has | User Gets | User Loses |
|----------|-----------|-----------|
| MaxEquipment > 0 | Owner role | Owner role (on expiry) |
| MaxProducts > 0 | Seller role | Seller role (on expiry) |
| Any subscription | Farmer role | Never lost |

---

## 🔒 Security Features

### Payment Verification

All payments verified using **HMAC-SHA256** signature:

```csharp
string signature = HMAC-SHA256(orderId + "|" + paymentId, razorpaySecret)
if (signature != clientSignature)
    return Error: "Payment verification failed"
```

**Security Guarantees:**
- ✅ Server-side signature verification (malicious apps can't bypass)
- ✅ HTTPS encryption in production
- ✅ Razorpay secret never sent to client
- ✅ Payment ID uniqueness prevents replay attacks

### JWT Token Updates

When subscription is activated, a new JWT token is issued with:
- Updated roles (Owner/Seller if applicable)
- Renewed expiry time
- Old token becomes invalid

### Database Constraints

- Unique index on (UserId, RoleId) - Prevents duplicate role assignments
- Foreign key restrictions - Prevents orphaned records
- Cascade delete on user deletion

---

## 📊 Common Use Cases

### Use Case 1: New User Purchases First Subscription

```json
Request:
POST /api/subscription/verify-payment
{
  "planId": 1,
  "paymentId": "pay_ABC123",
  "orderId": "order_ABC123",
  "signature": "xxxxx"
}

Response:
{
  "success": true,
  "message": "Subscription Activated & Access Updated Successfully!",
  "token": "NEW_JWT_WITH_ROLES",
  "roles": ["Farmer", "Owner"],
  "validFrom": "2026-02-19T10:30:00Z",
  "validTill": "2026-03-21T10:30:00Z"
}
```

### Use Case 2: User Renews Expiring Subscription

```
Current subscription:
- planId: 1
- endDate: 2026-03-20 (tomorrow)
- status: Active

User purchases same plan again:
POST /api/subscription/verify-payment (planId: 1)

Result:
- endDate extended to: 2026-04-19 (30 days from 2026-03-20)
- status: Active
- roles: Unchanged
```

### Use Case 3: Admin Deactivates Plan

```json
Request:
PUT /api/admin/subscription/status/2

Result:
- Plan 2 status: Active → Inactive
- Existing users: Still have access (subscriptions active)
- New users: Cannot purchase plan 2
```

### Use Case 4: Subscription Expires Automatically

```
Background Service (every hour):
1. Finds subscriptions: endDate < DateTime.UtcNow && status = "Active"
2. Updates status to "Expired"
3. Removes Owner/Seller roles
4. Sends notification (optional)

User tries to add equipment:
- Authorization check fails
- "Owner role required" error
- Prompts to renew subscription
```

---

## ❌ Error Handling

### Common Error Responses

| Status | Error | Cause | Solution |
|--------|-------|-------|----------|
| 400 | Payment verification failed | Invalid signature | Verify Razorpay secret config |
| 400 | Invalid plan details | Wrong plan data on create | Check field constraints |
| 401 | Unauthorized | Missing/expired token | Login again, refresh token |
| 401 | Unauthorized | Not an Admin | Ensure user has Admin role |
| 404 | Plan not found | Invalid planId | Verify plan exists |
| 500 | Payment configuration error | Missing Razorpay keys | Check appsettings.json |
| 500 | Database error | DB connection issue | Check SQL Server connection |

### Error Response Format

```json
{
  "message": "Error description"
}
```

OR for validation errors:

```json
{
  "message": "Invalid plan details"
}
```

---

## 🧪 Testing Guide

### Prerequisites

1. Valid Razorpay test mode credentials in `appsettings.json`:

```json
{
  "Razorpay": {
    "Key": "rzp_test_xxxxx",
    "Secret": "xxxxx"
  }
}
```

2. Valid JWT token from authentication endpoint

### Test Scenarios

#### Test 1: View Plans (No Auth)

```bash
curl -X GET "http://localhost:5000/api/subscription-plans" \
  -H "Accept-Language: en"
```

**Expected:** 200 OK with list of active plans

#### Test 2: Create Order (With Auth)

```bash
curl -X POST "http://localhost:5000/api/subscription/create-order/1" \
  -H "Authorization: Bearer {YOUR_TOKEN}" \
  -H "Accept-Language: en"
```

**Expected:** 200 OK with orderId and Razorpay key

#### Test 3: Invalid Signature (Should Fail)

```bash
curl -X POST "http://localhost:5000/api/subscription/verify-payment" \
  -H "Authorization: Bearer {YOUR_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "planId": 1,
    "paymentId": "pay_INVALID",
    "orderId": "order_INVALID",
    "signature": "INVALID_SIGNATURE"
  }'
```

**Expected:** 400 Bad Request with "Payment verification failed"

#### Test 4: Get My Subscriptions

```bash
curl -X GET "http://localhost:5000/api/subscription/my-subscription" \
  -H "Authorization: Bearer {YOUR_TOKEN}" \
  -H "Accept-Language: en"
```

**Expected:** 200 OK with user's active subscriptions

#### Test 5: Admin View All Plans

```bash
curl -X GET "http://localhost:5000/api/admin/subscription/plans" \
  -H "Authorization: Bearer {ADMIN_TOKEN}" \
  -H "Accept-Language: en"
```

**Expected:** 200 OK with all plans (active + inactive)

---

## 💾 Database Queries

### Find User's Active Subscriptions

```sql
SELECT
    us.SubscriptionId,
    us.UserId,
    us.PlanId,
    sp.PlanName,
    us.StartDate,
    us.EndDate,
    us.Status
FROM UserSubscriptions us
JOIN SubscriptionPlans sp ON us.PlanId = sp.PlanId
WHERE us.UserId = 5
  AND us.Status = 'Active'
  AND us.EndDate > GETUTCDATE();
```

### Find Expired Subscriptions

```sql
SELECT
    us.SubscriptionId,
    u.FullName,
    sp.PlanName,
    us.EndDate
FROM UserSubscriptions us
JOIN Users u ON us.UserId = u.UserId
JOIN SubscriptionPlans sp ON us.PlanId = sp.PlanId
WHERE us.Status = 'Active'
  AND us.EndDate < GETUTCDATE();
```

### Check Plan Limits vs Current Usage

```sql
SELECT
    u.UserId,
    u.FullName,
    sp.PlanName,
    sp.MaxEquipment,
    COUNT(DISTINCT e.EquipmentId) AS CurrentEquipment,
    sp.MaxEquipment - COUNT(DISTINCT e.EquipmentId) AS RemainingSlots
FROM Users u
JOIN UserSubscriptions us ON u.UserId = us.UserId
JOIN SubscriptionPlans sp ON us.PlanId = sp.PlanId
LEFT JOIN Equipments e ON u.UserId = e.OwnerId AND e.Status = 'Approved'
WHERE us.Status = 'Active' AND us.EndDate > GETUTCDATE()
GROUP BY u.UserId, u.FullName, sp.PlanName, sp.MaxEquipment;
```

---

## 🛠️ Configuration

### appsettings.json

```json
{
  "Razorpay": {
    "Key": "rzp_live_xxxxx",
    "Secret": "xxxxx"
  },
  "TranslationService": {
    "ApiKey": "xxxxx",
    "Region": "eastus"
  }
}
```

### Required Services

- SQL Server Database
- Razorpay Account (Live or Test mode)
- Azure Translator (for multi-language support)

---

## 📝 API Summary Table

| Endpoint | Method | Auth | Role | Description |
|----------|--------|------|------|-------------|
| `/api/subscription-plans` | GET | ❌ | Public | Get active plans |
| `/api/subscription/create-order/{id}` | POST | ✅ | Any | Create Razorpay order |
| `/api/subscription/verify-payment` | POST | ✅ | Any | Verify payment & activate |
| `/api/subscription/my-subscription` | GET | ✅ | Any | Get active subscriptions |
| `/api/subscription/check-expiry` | POST | ✅ | Admin | Manually check expiry |
| `/api/admin/subscription/add-plan` | POST | ✅ | Admin | Create new plan |
| `/api/admin/subscription/plans` | GET | ✅ | Admin | View all plans |
| `/api/admin/subscription/status/{id}` | PUT | ✅ | Admin | Toggle plan status |

---

## 📌 Important Notes

1. **Signature Verification:** All payments are verified server-side. The client signature is matched against the server-generated signature using HMAC-SHA256.

2. **Concurrent Subscriptions:** Users can have multiple active subscriptions for different plans simultaneously.

3. **Renewal Logic:** If extending a subscription, if the current EndDate is in the future, extension happens from that date; otherwise from the current date.

4. **Role Guarantees:** The "Farmer" role is never removed. Only "Owner" and "Seller" roles are conditional.

5. **Automatic Expiry:** Background service runs every hour. There may be a slight delay (up to 1 hour) before expired subscriptions are deactivated.

6. **Payment Retention:** Payment IDs are stored for audit trailing and can be used to trace transactions back to Razorpay.

---

## 🚀 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-19 | Initial release with Razorpay integration |

---

## 📞 Support

For issues or questions regarding subscription APIs:

1. Check [DATABASE_DOCUMENTATION.md](DATABASE_DOCUMENTATION.md) for schema details
2. Review [SECURITY_AUDIT_REPORT.md](SECURITY_AUDIT_REPORT.md) for security considerations
3. Consult [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for other API endpoints
