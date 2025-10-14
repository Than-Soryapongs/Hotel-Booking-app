# Quick Test - Rate Limiting

# Test 1: Login Rate Limit (should fail after 10 attempts)
Write-Host "`n=== Testing Login Rate Limit (10 requests/5min) ===`n" -ForegroundColor Cyan

for ($i = 1; $i -le 12; $i++) {
    Write-Host "Request $i`: " -NoNewline
    
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
            -Method POST `
            -ContentType "application/json" `
            -Body '{"usernameOrEmail":"test","password":"test"}' `
            -Headers @{} `
            -ResponseHeadersVariable "headers"
        
        Write-Host "OK (Remaining: $($headers['X-Rate-Limit-Remaining'][0]))" -ForegroundColor Green
        
    } catch {
        if ($_.Exception.Response.StatusCode -eq 429) {
            Write-Host "RATE LIMITED ✓" -ForegroundColor Red
        } else {
            Write-Host "Error: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
        }
    }
    
    Start-Sleep -Milliseconds 200
}

Write-Host "`n=== Test Complete ===`n" -ForegroundColor Cyan
Write-Host "Expected: First 10 requests OK, then RATE LIMITED" -ForegroundColor White
Write-Host "If you see 'RATE LIMITED' after request 10, it's working! ✓`n" -ForegroundColor Green
