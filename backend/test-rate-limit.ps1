# PowerShell script to test rate limiting and failed login attempts
# Make sure your backend is running before executing this script

$baseUrl = "http://localhost:8080"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Testing Rate Limiting & Failed Login Attempts" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Rate Limiting on Login Endpoint
Write-Host "Test 1: Testing Rate Limiting (10 requests per 5 minutes)" -ForegroundColor Yellow
Write-Host "Sending 12 login requests to exceed the rate limit...`n" -ForegroundColor Yellow

$loginBody = @{
    usernameOrEmail = "testuser"
    password = "wrongpassword"
} | ConvertTo-Json

for ($i = 1; $i -le 12; $i++) {
    Write-Host "Request $i:" -ForegroundColor Green
    
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body $loginBody `
            -UseBasicParsing `
            -ErrorAction Stop
        
        $statusCode = $response.StatusCode
        $headers = $response.Headers
        
        Write-Host "  Status: $statusCode" -ForegroundColor Green
        Write-Host "  X-Rate-Limit-Limit: $($headers['X-Rate-Limit-Limit'])" -ForegroundColor White
        Write-Host "  X-Rate-Limit-Remaining: $($headers['X-Rate-Limit-Remaining'])" -ForegroundColor White
        Write-Host "  X-Rate-Limit-Duration: $($headers['X-Rate-Limit-Duration'])" -ForegroundColor White
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
        
        if ($statusCode -eq 429) {
            Write-Host "  Status: 429 - TOO MANY REQUESTS" -ForegroundColor Red
            Write-Host "  Message: $($errorBody.message)" -ForegroundColor Red
            Write-Host "  ✓ Rate limiting is working!" -ForegroundColor Green
        } else {
            Write-Host "  Status: $statusCode" -ForegroundColor Yellow
            Write-Host "  Message: $($errorBody.message)" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Start-Sleep -Milliseconds 500
}

Write-Host "`n========================================`n" -ForegroundColor Cyan

# Test 2: Failed Login Attempts and Account Locking
Write-Host "Test 2: Testing Failed Login Attempts (5 attempts lock account)" -ForegroundColor Yellow
Write-Host "Creating a test user and testing failed login attempts...`n" -ForegroundColor Yellow

# First, create a test user
$signupBody = @{
    username = "testuser_failedlogin"
    email = "failedlogin@test.com"
    password = "TestPassword123!"
    firstName = "Test"
    lastName = "User"
    gender = "MALE"
} | ConvertTo-Json

Write-Host "Creating test user..." -ForegroundColor Green
try {
    $signupResponse = Invoke-WebRequest -Uri "$baseUrl/api/auth/signup" `
        -Method POST `
        -ContentType "application/json" `
        -Body $signupBody `
        -UseBasicParsing
    Write-Host "✓ User created (email verification required)`n" -ForegroundColor Green
} catch {
    Write-Host "Note: User may already exist or creation failed`n" -ForegroundColor Yellow
}

# Manually verify the email using database or skip this step
Write-Host "NOTE: You need to manually verify the email in the database:" -ForegroundColor Cyan
Write-Host "UPDATE users SET enabled = 1, email_verification_token = NULL WHERE email = 'failedlogin@test.com';`n" -ForegroundColor Cyan

Write-Host "After verifying email, test failed login attempts:" -ForegroundColor Yellow
Write-Host "Run the following commands to test (after email verification):`n" -ForegroundColor Yellow

Write-Host "# Attempt 6 failed logins" -ForegroundColor White
Write-Host "for (`$i = 1; `$i -le 6; `$i++) {" -ForegroundColor White
Write-Host "    Write-Host `"Failed Login Attempt `$i`"" -ForegroundColor White
Write-Host "    try {" -ForegroundColor White
Write-Host "        Invoke-WebRequest -Uri `"$baseUrl/api/auth/login`" ``" -ForegroundColor White
Write-Host "            -Method POST ``" -ForegroundColor White
Write-Host "            -ContentType `"application/json`" ``" -ForegroundColor White
Write-Host "            -Body '{`"usernameOrEmail`":`"testuser_failedlogin`",`"password`":`"WrongPassword123!`"}' ``" -ForegroundColor White
Write-Host "            -UseBasicParsing" -ForegroundColor White
Write-Host "    } catch {" -ForegroundColor White
Write-Host "        `$errorBody = `$_.ErrorDetails.Message | ConvertFrom-Json" -ForegroundColor White
Write-Host "        Write-Host `"  Error: `$(`$errorBody.message)`" -ForegroundColor Red" -ForegroundColor White
Write-Host "    }" -ForegroundColor White
Write-Host "    Start-Sleep -Seconds 1" -ForegroundColor White
Write-Host "}`n" -ForegroundColor White

Write-Host "Expected behavior:" -ForegroundColor Cyan
Write-Host "  - Attempts 1-4: 'Invalid credentials'" -ForegroundColor White
Write-Host "  - Attempt 5: 'Invalid credentials' (account locked)" -ForegroundColor White
Write-Host "  - Attempt 6+: 'Account is locked. Try again later.'" -ForegroundColor White

Write-Host "`n========================================`n" -ForegroundColor Cyan

# Test 3: Rate Limiting on Signup
Write-Host "Test 3: Testing Rate Limiting on Signup (5 requests per hour)" -ForegroundColor Yellow
Write-Host "Sending 6 signup requests...`n" -ForegroundColor Yellow

for ($i = 1; $i -le 6; $i++) {
    Write-Host "Signup Request $i:" -ForegroundColor Green
    
    $signupTestBody = @{
        username = "testuser_$i"
        email = "test$i@example.com"
        password = "TestPassword123!"
        firstName = "Test"
        lastName = "User$i"
        gender = "MALE"
    } | ConvertTo-Json
    
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/signup" `
            -Method POST `
            -ContentType "application/json" `
            -Body $signupTestBody `
            -UseBasicParsing `
            -ErrorAction Stop
        
        $statusCode = $response.StatusCode
        $headers = $response.Headers
        
        Write-Host "  Status: $statusCode" -ForegroundColor Green
        Write-Host "  X-Rate-Limit-Remaining: $($headers['X-Rate-Limit-Remaining'])" -ForegroundColor White
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 429) {
            Write-Host "  Status: 429 - TOO MANY REQUESTS" -ForegroundColor Red
            Write-Host "  ✓ Rate limiting is working on signup!" -ForegroundColor Green
        } else {
            Write-Host "  Status: $statusCode" -ForegroundColor Yellow
            try {
                $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
                Write-Host "  Message: $($errorBody.message)" -ForegroundColor Yellow
            } catch {
                Write-Host "  Error parsing response" -ForegroundColor Yellow
            }
        }
    }
    
    Write-Host ""
    Start-Sleep -Milliseconds 500
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
