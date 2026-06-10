# 💳 Payment Tracking System Documentation

## Overview

The Payment Tracking System is a new feature that records and retrieves detailed information about all payments made by users in AgriRent. Instead of just storing a payment ID, we now maintain comprehensive payment records for auditing, reporting, and user history.

**Problem Solved:** ✅ Users can now view all their payment history and transaction details

---

## 🏗️ Architecture

### New Components

1. **Payment Model** - Stores complete payment information
2. **PaymentController** - 6 new endpoints for retrieving payment data
3. **Payment Database Table** - With indexes for performance
4. **DTOs** - `PaymentHistoryDto` and `PaymentDetailsDto`

---

## 📊 Payment Model

### Payment Table Structure

```sql
CREATE TABLE [Payments] (
    [PaymentId] INT PRIMARY KEY IDENTITY(1,1),
    [UserId] INT NOT NULL,
    [SubscriptionId] INT NOT NULL,
    [PlanId] INT NOT NULL,
    [RazorpayPaymentId] NVARCHAR(100) NOT NULL,
    [RazorpayOrderId] NVARCHAR(100) NOT NULL,
    [RazorpaySignature] NVARCHAR(255) NOT NULL,
    [Amount] DECIMAL(18,2) NOT NULL,
    [Currency] NVARCHAR(20) NOT NULL,
    [Status] NVARCHAR(50) NOT NULL,
    [PaymentDate] DATETIME2 NOT NULL,
    [ReceiptNumber] NVARCHAR(MAX),
    [ErrorMessage] NVARCHAR(MAX),
    [PaymentMethod] NVARCHAR(MAX),
    [VerificationDate] DATETIME2,
    [Notes] NVARCHAR(MAX),
    
    FOREIGN KEY [UserId] → Users(UserId) ON DELETE CASCADE,
    FOREIGN KEY [SubscriptionId] → UserSubscriptions(SubscriptionId) ON DELETE CASCADE,
    FOREIGN KEY [PlanId] → SubscriptionPlans(PlanId) ON DELETE CASCADE
)
```

### Payment Model (C#)

```csharp
public class Payment
{
    public int PaymentId { get; set; }
    public int UserId { get; set; }
    public int SubscriptionId { get; set; }
    public int PlanId { get; set; }
    
    public string RazorpayPaymentId { get; set; }      // "pay_xxxxx"
    public string RazorpayOrderId { get; set; }        // "order_xxxxx"
    public string RazorpaySignature { get; set; }      // Signature hash
    
    public decimal Amount { get; set; }                // ₹999.00
    public string Currency { get; set; }               // "INR"
    public string Status { get; set; }                 // "Successful", "Failed", "Pending"
    
    public DateTime PaymentDate { get; set; }          // When paid
    public string ReceiptNumber { get; set; }          // "rcpt_1_20260219"
    public string ErrorMessage { get; set; }           // If failed
    public string PaymentMethod { get; set; }          // "Card", "UPI", "Wallet"
    public DateTime? VerificationDate { get; set; }    // When signature verified
    public string Notes { get; set; }                  // Additional info
    
    // Navigation
    public User User { get; set; }
    public UserSubscription Subscription { get; set; }
    public SubscriptionPlan Plan { get; set; }
}
```

### Payment Data Flow

```
User Makes Payment
        ↓
Razorpay generates: PaymentId + OrderId + Signature
        ↓
Client calls: POST /api/subscription/verify-payment
        ↓
Server verifies signature ✓
        ↓
Server creates: UserSubscription record
        ↓
Server creates: Payment record (NEW!)
        ↓
Server returns: Success + JWT token
        ↓
Payment available in history: GET /api/payment/history
```

---

## 📚 Payment API Endpoints

### 1. Get Payment History (Paginated)

**Endpoint:** `GET /api/payment/history`

**Authentication:** Required (Bearer Token)

**Parameters:**

```
?pageSize=10&pageNumber=1&status=Successful
```

| Parameter | Type | Default | Max | Description |
|-----------|------|---------|-----|-------------|
| `pageSize` | INT | 10 | 100 | Results per page |
| `pageNumber` | INT | 1 | - | Page number (1-indexed) |
| `status` | STRING | - | - | Filter: "Successful", "Failed", "Pending" |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Payment history retrieved",
  "totalCount": 5,
  "totalPages": 1,
  "currentPage": 1,
  "pageSize": 10,
  "data": [
    {
      "paymentId": 1,
      "subscriptionId": 1,
      "razorpayPaymentId": "pay_IzydBU2Y8bSz3a",
      "razorpayOrderId": "order_IzydBU2Y8bSz3a",
      "amount": 999.00,
      "currency": "INR",
      "status": "Successful",
      "paymentDate": "2026-02-19T10:30:00Z",
      "receiptNumber": "rcpt_1_20260219",
      "paymentMethod": "card",
      "verificationDate": "2026-02-19T10:30:05Z",
      "planName": "Basic Plan",
      "durationDays": 30,
      "errorMessage": null
    }
  ]
}
```

**Example Request:**

```bash
# Get first 10 successful payments
curl -X GET "http://localhost:5000/api/payment/history?pageSize=10&pageNumber=1&status=Successful" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"

# Get failed payments
curl -X GET "http://localhost:5000/api/payment/history?status=Failed" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get page 2, 20 per page
curl -X GET "http://localhost:5000/api/payment/history?pageSize=20&pageNumber=2" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 2. Get Single Payment Details

**Endpoint:** `GET /api/payment/{paymentId}`

**Authentication:** Required (Bearer Token)

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `paymentId` | INT | Payment ID to retrieve |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Payment details retrieved",
  "data": {
    "paymentId": 1,
    "userId": 5,
    "subscriptionId": 1,
    "planId": 1,
    "razorpayPaymentId": "pay_IzydBU2Y8bSz3a",
    "razorpayOrderId": "order_IzydBU2Y8bSz3a",
    "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d",
    "amount": 999.00,
    "currency": "INR",
    "status": "Successful",
    "paymentDate": "2026-02-19T10:30:00Z",
    "receiptNumber": "rcpt_1_20260219",
    "errorMessage": null,
    "paymentMethod": "card",
    "verificationDate": "2026-02-19T10:30:05Z",
    "notes": "Subscription payment for plan: Basic Plan",
    "planName": "Basic Plan",
    "durationDays": 30,
    "maxEquipment": 5,
    "maxProducts": 10
  }
}
```

**Error Responses:**
- `404 Not Found` - Payment doesn't exist or doesn't belong to user
- `401 Unauthorized` - Missing/invalid token

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/payment/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"
```

---

### 3. Get Payment Summary/Statistics

**Endpoint:** `GET /api/payment/summary/stats`

**Authentication:** Required (Bearer Token)

**Description:** Get aggregate payment statistics for current user

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Payment summary retrieved",
  "data": {
    "totalSpent": 2498.00,
    "totalPayments": 2,
    "successfulPayments": 2,
    "failedPayments": 1,
    "pendingPayments": 0,
    "lastPaymentDate": "2026-02-19T10:30:00Z",
    "averagePaymentValue": 1249.00
  }
}
```

**Fields:**

| Field | Description |
|-------|-------------|
| `totalSpent` | Sum of all successful payments |
| `totalPayments` | Count of successful payments |
| `successfulPayments` | Successful transaction count |
| `failedPayments` | Failed transaction count |
| `pendingPayments` | Pending transaction count |
| `lastPaymentDate` | Most recent payment date |
| `averagePaymentValue` | Average payment amount |

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/payment/summary/stats" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"
```

---

### 4. Get Payments by Date Range

**Endpoint:** `GET /api/payment/range`

**Authentication:** Required (Bearer Token)

**Parameters:**

```
?startDate=2026-02-01&endDate=2026-02-28
```

| Parameter | Type | Format | Description |
|-----------|------|--------|-------------|
| `startDate` | DATE | ISO 8601 | Range start (inclusive) |
| `endDate` | DATE | ISO 8601 | Range end (inclusive) |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Payments retrieved for date range",
  "totalCount": 2,
  "startDate": "2026-02-01T00:00:00Z",
  "endDate": "2026-02-28T00:00:00Z",
  "data": [
    {
      "paymentId": 2,
      "subscriptionId": 2,
      "razorpayPaymentId": "pay_IzydBU2Y8bSz3b",
      "razorpayOrderId": "order_IzydBU2Y8bSz3b",
      "amount": 2499.00,
      "currency": "INR",
      "status": "Successful",
      "paymentDate": "2026-02-18T14:20:00Z",
      "receiptNumber": "rcpt_2_20260218",
      "paymentMethod": "upi",
      "verificationDate": "2026-02-18T14:20:05Z",
      "planName": "Premium Plan",
      "durationDays": 90,
      "errorMessage": null
    }
  ]
}
```

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/payment/range?startDate=2026-02-01&endDate=2026-02-28" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Accept-Language: en"
```

---

### 5. Admin: Get All Payments

**Endpoint:** `GET /api/payment/admin/all`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Parameters:**

```
?pageSize=20&pageNumber=1&status=Successful
```

| Parameter | Type | Default | Max | Description |
|-----------|------|---------|-----|-------------|
| `pageSize` | INT | 20 | 100 | Results per page |
| `pageNumber` | INT | 1 | - | Page number |
| `status` | STRING | - | - | Filter by status |

**Response (200 OK):**

```json
{
  "success": true,
  "message": "All payments retrieved",
  "totalCount": 25,
  "totalPages": 2,
  "currentPage": 1,
  "pageSize": 20,
  "data": [
    {
      "paymentId": 1,
      "userId": 5,
      "userName": "Raj Patel",
      "userMobile": "9876543210",
      "razorpayPaymentId": "pay_IzydBU2Y8bSz3a",
      "razorpayOrderId": "order_IzydBU2Y8bSz3a",
      "amount": 999.00,
      "currency": "INR",
      "status": "Successful",
      "paymentDate": "2026-02-19T10:30:00Z",
      "receiptNumber": "rcpt_1_20260219",
      "paymentMethod": "card",
      "verificationDate": "2026-02-19T10:30:05Z",
      "planName": "Basic Plan",
      "errorMessage": null
    }
  ]
}
```

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/payment/admin/all?pageSize=20&pageNumber=1" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

---

### 6. Admin: Get Revenue Statistics

**Endpoint:** `GET /api/payment/admin/stats`

**Authentication:** Required (Bearer Token)

**Authorization:** Admin role only

**Description:** Get platform-wide payment and revenue analytics

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Revenue statistics retrieved",
  "data": {
    "totalRevenue": 12497.00,
    "totalSuccessful": 5,
    "totalFailed": 1,
    "totalPending": 0,
    "successRate": 83.33,
    "revenueByPlan": [
      {
        "planName": "Basic Plan",
        "revenue": 1998.00,
        "count": 2,
        "avgAmount": 999.00
      },
      {
        "planName": "Premium Plan",
        "revenue": 7497.00,
        "count": 3,
        "avgAmount": 2499.00
      },
      {
        "planName": "Elite Plan",
        "revenue": 4999.00,
        "count": 1,
        "avgAmount": 4999.00
      }
    ]
  }
}
```

**Analytics Fields:**

| Field | Description |
|-------|-------------|
| `totalRevenue` | Sum of all successful payments (₹) |
| `totalSuccessful` | Number of successful transactions |
| `totalFailed` | Number of failed transactions |
| `totalPending` | Number of pending transactions |
| `successRate` | Success rate percentage (0-100%) |
| `revenueByPlan` | Revenue breakdown by subscription plan |

**Example Request:**

```bash
curl -X GET "http://localhost:5000/api/payment/admin/stats" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Accept-Language: en"
```

---

## 🔄 Payment Lifecycle

### Payment Statuses

| Status | Description | When Set |
|--------|-------------|----------|
| `Pending` | Payment initiated but not verified | When order created |
| `Successful` | Payment verified via signature | After successful verification |
| `Failed` | Payment verification failed | If signature doesn't match |

### Payment Status Flow

```
Payment Initiated (status: Pending)
        ↓
User completes Razorpay checkout
        ↓
Client calls verify-payment with signature
        ↓
Server verifies signature
        ├─ ✅ Valid → Status: "Successful"
        └─ ❌ Invalid → Status: "Failed" + ErrorMessage
        ↓
Payment record saved to database
        ↓
User can view in history
```

---

## 🗄️ Database Queries

### Find User's Payment History

```sql
SELECT
    p.PaymentId,
    p.RazorpayPaymentId,
    p.Amount,
    p.Status,
    p.PaymentDate,
    sp.PlanName,
    p.ReceiptNumber
FROM Payments p
JOIN SubscriptionPlans sp ON p.PlanId = sp.PlanId
WHERE p.UserId = 5
ORDER BY p.PaymentDate DESC;
```

### Get Revenue Report by Plan

```sql
SELECT
    sp.PlanName,
    COUNT(*) AS TransactionCount,
    SUM(p.Amount) AS TotalRevenue,
    AVG(p.Amount) AS AvgTransactionValue,
    COUNT(CASE WHEN p.Status = 'Successful' THEN 1 END) AS SuccessfulCount,
    COUNT(CASE WHEN p.Status = 'Failed' THEN 1 END) AS FailedCount
FROM Payments p
JOIN SubscriptionPlans sp ON p.PlanId = sp.PlanId
WHERE p.Status = 'Successful'
GROUP BY sp.PlanName
ORDER BY TotalRevenue DESC;
```

### Find Failed Payments

```sql
SELECT
    p.PaymentId,
    u.FullName,
    u.MobileNumber,
    p.Amount,
    p.PaymentDate,
    p.ErrorMessage,
    sp.PlanName
FROM Payments p
JOIN Users u ON p.UserId = u.UserId
JOIN SubscriptionPlans sp ON p.PlanId = sp.PlanId
WHERE p.Status = 'Failed'
ORDER BY p.PaymentDate DESC;
```

### Calculate Monthly Revenue

```sql
SELECT
    YEAR(p.PaymentDate) AS [Year],
    MONTH(p.PaymentDate) AS [Month],
    COUNT(*) AS TransactionCount,
    SUM(p.Amount) AS MonthlyRevenue
FROM Payments p
WHERE p.Status = 'Successful'
GROUP BY YEAR(p.PaymentDate), MONTH(p.PaymentDate)
ORDER BY [Year] DESC, [Month] DESC;
```

---

## 🔐 Security Features

### Payment Data Protection

1. **Signature Verification:** All payments verified with HMAC-SHA256
2. **Signature Storage:** Signature hash stored for audit trail
3. **User Isolation:** Users can only see their own payments
4. **Admin Authorization:** Admin endpoints restricted to Admin role only
5. **Database Cascade:** Payment records linked to subscriptions via foreign keys

### Audit Trail

Every payment includes:
- ✅ Razorpay Payment ID (unique transaction identifier)
- ✅ Razorpay Order ID (order reference)
- ✅ Signature hash (proof of verification)
- ✅ Payment Date (when transaction occurred)
- ✅ Verification Date (when signature verified)
- ✅ Receipt Number (internal reference)

---

## 💾 Data Types & Constraints

### Payment Fields

| Field | Type | Constraints | Example |
|-------|------|-------------|---------|
| PaymentId | INT | PK, IDENTITY | 1, 2, 3... |
| UserId | INT | FK, NOT NULL | 5 |
| SubscriptionId | INT | FK, NOT NULL | 1 |
| PlanId | INT | FK, NOT NULL | 1 |
| RazorpayPaymentId | VARCHAR(100) | NOT NULL, UNIQUE | "pay_IzydBU2Y8bSz3a" |
| RazorpayOrderId | VARCHAR(100) | NOT NULL | "order_IzydBU2Y8bSz3a" |
| RazorpaySignature | VARCHAR(255) | NOT NULL | "9ef4dffbf..." |
| Amount | DECIMAL(18,2) | NOT NULL, ≥0 | 999.00 |
| Currency | VARCHAR(20) | NOT NULL | "INR" |
| Status | VARCHAR(50) | NOT NULL | "Successful" |
| PaymentDate | DATETIME2 | NOT NULL | 2026-02-19 10:30:00 |
| ReceiptNumber | TEXT | NULLABLE | "rcpt_1_20260219" |
| ErrorMessage | TEXT | NULLABLE | "Declined" |
| PaymentMethod | TEXT | NULLABLE | "card", "upi" |
| VerificationDate | DATETIME2 | NULLABLE | 2026-02-19 10:30:05 |
| Notes | TEXT | NULLABLE | "Renewal of..." |

---

## 🚀 Migration & Setup

### Step 1: Add Migration

```bash
dotnet ef migrations add AddPaymentTable
```

### Step 2: Update Database

```bash
dotnet ef database update
```

### Step 3: Verify Table Created

```sql
SELECT * FROM Payments;
-- Should return empty table with 15 columns
```

---

## 📈 Use Cases

### Use Case 1: User Views Payment History

```bash
curl -X GET "http://localhost:5000/api/payment/history?pageSize=10&pageNumber=1" \
  -H "Authorization: Bearer USER_TOKEN"

Response:
[
  {
    paymentId: 1,
    planName: "Basic Plan",
    amount: 999.00,
    status: "Successful",
    paymentDate: "2026-02-19T10:30:00Z"
  },
  {
    paymentId: 2,
    planName: "Premium Plan",
    amount: 2499.00,
    status: "Successful",
    paymentDate: "2026-02-18T14:20:00Z"
  }
]
```

### Use Case 2: User Views Payment Receipt

```bash
curl -X GET "http://localhost:5000/api/payment/1" \
  -H "Authorization: Bearer USER_TOKEN"

Response includes:
- Full payment details
- Plan information
- Receipt number
- Verification date
- Signature (for audit)
```

### Use Case 3: Admin Views Revenue Stats

```bash
curl -X GET "http://localhost:5000/api/payment/admin/stats" \
  -H "Authorization: Bearer ADMIN_TOKEN"

Response:
{
  totalRevenue: ₹12,497.00,
  totalSuccessful: 5,
  successRate: 83.33%,
  revenueByPlan: [
    { planName: "Basic", revenue: ₹1,998.00, count: 2 },
    { planName: "Premium", revenue: ₹7,497.00, count: 3 },
    { planName: "Elite", revenue: ₹4,999.00, count: 1 }
  ]
}
```

### Use Case 4: User Gets Payment Summary

```bash
curl -X GET "http://localhost:5000/api/payment/summary/stats" \
  -H "Authorization: Bearer USER_TOKEN"

Response:
{
  totalSpent: ₹2,498.00,
  totalPayments: 2,
  lastPaymentDate: "2026-02-19T10:30:00Z",
  averagePaymentValue: ₹1,249.00
}
```

---

## 📋 API Summary

| Endpoint | Method | Auth | Role | Purpose |
|----------|--------|------|------|---------|
| `/api/payment/history` | GET | ✅ | User | View payment history (paginated) |
| `/api/payment/{id}` | GET | ✅ | User | View single payment details |
| `/api/payment/summary/stats` | GET | ✅ | User | View payment statistics |
| `/api/payment/range` | GET | ✅ | User | View payments in date range |
| `/api/payment/admin/all` | GET | ✅ | Admin | View all platform payments |
| `/api/payment/admin/stats` | GET | ✅ | Admin | View revenue statistics |

---

## 📝 Changes Made

### Files Created

1. ✅ `Models/Payment.cs` - Payment model with 15 fields
2. ✅ `DTOs/PaymentHistoryDto.cs` - List view DTO
3. ✅ `DTOs/PaymentDetailsDto.cs` - Detailed view DTO
4. ✅ `Controllers/PaymentController.cs` - 6 endpoints
5. ✅ `Migrations/20260219000000_AddPaymentTable.cs` - Database migration

### Files Modified

1. ✅ `Data/AppDbContext.cs` - Added `DbSet<Payment>`
2. ✅ `Controllers/SubscriptionController.cs` - Updated to save Payment records

---

## ❓ FAQ

**Q: Why create a separate Payment table?**
A: To maintain an audit trail of all payments, enable reporting, and prevent data loss if subscriptions are deleted.

**Q: Can payments be deleted?**
A: No. Payment records are permanent for audit purposes. Only status can be tracked.

**Q: How is payment history secured?**
A: Users can only view their own payments. Admins can view all. All endpoints require authentication.

**Q: What if a payment fails?**
A: Failed payment records are still created with Status="Failed" and ErrorMessage populated for debugging.

**Q: Can I download payment receipts?**
A: Yes, the receipt number and details can be used to generate invoices (future feature).

---

## 🔧 Troubleshooting

### No Payment Records Show Up

**Solution:** Apply the migration first:
```bash
dotnet ef database update
```

### Payment Endpoint Returns 404

**Solution:** Check that PaymentController.cs exists in Controllers folder

### Admin Stats Shows 0 Revenue

**Solution:** Ensure payment records have Status = "Successful"

---

## 🎯 Next Steps (Optional Enhancements)

1. **Invoice Generation** - Create PDF invoices from payment data
2. **Payment Refunds** - Track refund status in payment records
3. **Payment Notifications** - Send email receipts
4. **CSV Export** - Export payment history to CSV
5. **Payment Reminders** - Send upcoming expiry notifications
6. **Failed Payment Retry** - Automatic retry logic

---

Version: 1.0
Date: 2026-02-19
Status: ✅ Complete
