# AgriRent API Testing Script
# This script tests all major endpoints to verify the application is working

$baseUrl = "http://localhost:5288"
$results = @()

Write-Host "🔍 Testing AgriRent API Endpoints..." -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Yellow
Write-Host ""

# Function to test endpoint
function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{"Content-Type" = "application/json"},
        [string]$Body = $null,
        [bool]$ExpectAuth = $false
    )
    
    try {
        $params = @{
            Uri = "$baseUrl$Url"
            Method = $Method
            Headers = $Headers
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params.Body = $Body
        }
        
        $response = Invoke-WebRequest @params
        
        $status = "PASS"
        $statusCode = $response.StatusCode
        $color = "Green"
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        # If we expect 401 Unauthorized, that's OK (endpoint exists but needs auth)
        if ($ExpectAuth -and ($statusCode -eq 401)) {
            $status = "AUTH REQUIRED"
            $color = "Yellow"
        }
        # 404 means endpoint does not exist - that is a problem
        elseif ($statusCode -eq 404) {
            $status = "NOT FOUND"
            $color = "Red"
        }
        else {
            $status = "ERROR"
            $color = "Yellow"
        }
    }
    
    Write-Host "$status - $Method $Url - HTTP $statusCode" -ForegroundColor $color
    
    return @{
        Name = $Name
        Status = $status
        StatusCode = $statusCode
        Url = $Url
    }
}

# Test Home Page
Write-Host "`n📱 Testing Web Pages:" -ForegroundColor Magenta
Test-Endpoint -Name "Home Page" -Method "GET" -Url "/"
Test-Endpoint -Name "Admin Login Page" -Method "GET" -Url "/Admin/Login"

# Test Public Endpoints (No Auth Required)
Write-Host "`n🌐 Testing Public API Endpoints:" -ForegroundColor Magenta
Test-Endpoint -Name "Test Users" -Method "GET" -Url "/api/test/users"
Test-Endpoint -Name "Public Equipment List" -Method "GET" -Url "/api/equipment-public/list"
Test-Endpoint -Name "Public Product List" -Method "GET" -Url "/api/product/list"

# Test Authentication Endpoints
Write-Host "`n🔐 Testing Authentication Endpoints:" -ForegroundColor Magenta
Test-Endpoint -Name "Send OTP" -Method "POST" -Url "/api/auth/send-otp" -Body '{"mobileNumber": "1234567890"}'
Test-Endpoint -Name "Login" -Method "POST" -Url "/api/auth/login"
Test-Endpoint -Name "OTP Register" -Method "POST" -Url "/api/auth/otp-register"

# Test Protected Endpoints (Expect 401)
Write-Host "`n🔒 Testing Protected Endpoints (Should require auth):" -ForegroundColor Magenta
Test-Endpoint -Name "My Profile" -Method "GET" -Url "/api/profile" -ExpectAuth $true
Test-Endpoint -Name "Subscription Plans" -Method "GET" -Url "/api/subscription-plans" -ExpectAuth $true
Test-Endpoint -Name "Add Equipment" -Method "POST" -Url "/api/equipment/add" -ExpectAuth $true
Test-Endpoint -Name "My Products (Seller)" -Method "GET" -Url "/api/seller/my-products" -ExpectAuth $true
Test-Endpoint -Name "Create Booking" -Method "POST" -Url "/api/booking/create" -ExpectAuth $true

# Test Admin Endpoints (Expect 401)
Write-Host "`n👑 Testing Admin Endpoints (Should require admin auth):" -ForegroundColor Magenta
Test-Endpoint -Name "Admin Equipment List" -Method "GET" -Url "/api/admin/equipment/pending" -ExpectAuth $true
Test-Endpoint -Name "Admin Product List" -Method "GET" -Url "/api/admin/products/pending" -ExpectAuth $true
Test-Endpoint -Name "Admin Create Category" -Method "POST" -Url "/api/admin/categories" -ExpectAuth $true

Write-Host "`n✨ Test Summary:" -ForegroundColor Cyan
Write-Host "Application is running on: $baseUrl" -ForegroundColor Green
Write-Host ""
Write-Host "Legend:" -ForegroundColor Yellow
Write-Host "  PASS         - Endpoint working correctly" -ForegroundColor Green
Write-Host "  AUTH REQUIRED - Endpoint exists but needs authentication (expected)" -ForegroundColor Yellow
Write-Host "  NOT FOUND    - Endpoint does not exist (problem)" -ForegroundColor Red
Write-Host "  ERROR       - Other error occurred" -ForegroundColor Yellow
Write-Host ""
