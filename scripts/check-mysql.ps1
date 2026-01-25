# Script to check MySQL
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

Write-Host "Checking MySQL..." -ForegroundColor Yellow

# Check MySQL service
$mysqlService = Get-Service -Name "*mysql*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    Write-Host "MySQL Service found: $($mysqlService.Name)" -ForegroundColor Green
    Write-Host "Status: $($mysqlService.Status)" -ForegroundColor $(if ($mysqlService.Status -eq 'Running') { 'Green' } else { 'Red' })
} else {
    Write-Host "MySQL Service not found!" -ForegroundColor Red
    Write-Host "Please install and start MySQL" -ForegroundColor Yellow
}

# Check MySQL connection
try {
    $connection = New-Object System.Data.SqlClient.SqlConnection
    $connection.ConnectionString = "Server=localhost;Database=mysql;User Id=root;Password=root;"
    $connection.Open()
    $connection.Close()
    Write-Host "MySQL connection successful!" -ForegroundColor Green
} catch {
    Write-Host "Cannot connect to MySQL: $_" -ForegroundColor Red
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "1. MySQL is installed" -ForegroundColor Yellow
    Write-Host "2. MySQL service is running" -ForegroundColor Yellow
    Write-Host "3. Username: root, Password: root" -ForegroundColor Yellow
}
