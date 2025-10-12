#!/usr/bin/env pwsh
# Run tests for medication-api using Maven Docker container
# This script provides a convenient way to run tests without installing Maven locally

Write-Host "🧪 Running NLL-Light Test Suite..." -ForegroundColor Cyan
Write-Host ""

# Ensure we're in the project root
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

# Run tests in Docker container with Maven
Write-Host "📦 Running tests in Maven Docker container..." -ForegroundColor Yellow
docker run --rm `
    -v "${PWD}:/src" `
    -w /src `
    maven:3.9.9-eclipse-temurin-21 `
    mvn -pl medication-api test -Dspring.profiles.active=test

$testResult = $LASTEXITCODE

Write-Host ""
if ($testResult -eq 0) {
    Write-Host "✅ All tests passed!" -ForegroundColor Green
} else {
    Write-Host "❌ Some tests failed. Check output above." -ForegroundColor Red
    exit $testResult
}

# Optional: Generate coverage report
$generateCoverage = Read-Host "Generate coverage report? (y/N)"
if ($generateCoverage -eq "y" -or $generateCoverage -eq "Y") {
    Write-Host ""
    Write-Host "📊 Generating coverage report..." -ForegroundColor Yellow
    docker run --rm `
        -v "${PWD}:/src" `
        -w /src `
        maven:3.9.9-eclipse-temurin-21 `
        mvn -pl medication-api jacoco:report
    
    Write-Host "Coverage report generated at: medication-api/target/site/jacoco/index.html" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done! 🎉" -ForegroundColor Cyan
