# Convert PNG and JPG images to WebP format
# Requires: Windows 10+ with libwebp support or FFmpeg installed

param(
    [string]$ImagePath = "$PSScriptRoot\images",
    [int]$Quality = 80,
    [switch]$DeleteOriginals = $false
)

function Install-WebPTools {
    Write-Host "Checking for image conversion tools..." -ForegroundColor Yellow
    
    # Check if FFmpeg is installed
    $ffmpeg = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($ffmpeg) {
        Write-Host "FFmpeg found: $($ffmpeg.Source)" -ForegroundColor Green
        return "ffmpeg"
    }
    
    # Check if cwebp is installed (WebP library)
    $cwebp = Get-Command cwebp -ErrorAction SilentlyContinue
    if ($cwebp) {
        Write-Host "cwebp found: $($cwebp.Source)" -ForegroundColor Green
        return "cwebp"
    }
    
    Write-Host "No conversion tools found. Installing FFmpeg via Chocolatey..." -ForegroundColor Yellow
    
    # Check if Chocolatey is installed
    $choco = Get-Command choco -ErrorAction SilentlyContinue
    if (-not $choco) {
        Write-Host "Installing Chocolatey..." -ForegroundColor Yellow
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    }
    
    choco install ffmpeg -y
    return "ffmpeg"
}

function Convert-ImageToWebP {
    param(
        [string]$SourceFile,
        [string]$DestFile,
        [string]$Tool,
        [int]$Quality
    )
    
    try {
        if ($Tool -eq "ffmpeg") {
            ffmpeg -i $SourceFile -q:v $Quality $DestFile -y 2>&1 | Out-Null
        }
        elseif ($Tool -eq "cwebp") {
            cwebp -q $Quality $SourceFile -o $DestFile
        }
        
        if (Test-Path $DestFile) {
            $sourceSize = (Get-Item $SourceFile).Length / 1KB
            $destSize = (Get-Item $DestFile).Length / 1KB
            $savings = [math]::Round((1 - ($destSize / $sourceSize)) * 100, 2)
            
            Write-Host "✓ Converted: $(Split-Path $SourceFile -Leaf) → $(Split-Path $DestFile -Leaf) ($savings% smaller)" -ForegroundColor Green
            return $true
        }
    }
    catch {
        Write-Host "✗ Error converting $(Split-Path $SourceFile -Leaf): $_" -ForegroundColor Red
        return $false
    }
}

# Main script
Write-Host "WebP Image Converter" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

if (-not (Test-Path $ImagePath)) {
    Write-Host "Error: Image path '$ImagePath' not found!" -ForegroundColor Red
    exit 1
}

$tool = Install-WebPTools

Write-Host "`nConverting images to WebP (Quality: $Quality)..." -ForegroundColor Cyan
Write-Host "Source: $ImagePath`n" -ForegroundColor Gray

$images = Get-ChildItem -Path $ImagePath -Include *.png, *.jpg, *.jpeg
$convertedCount = 0
$totalSize = 0
$savedSize = 0

foreach ($image in $images) {
    $webpFile = [System.IO.Path]::ChangeExtension($image.FullName, ".webp")
    
    # Skip if already webp or is an SVG
    if ($image.Extension -eq ".webp" -or $image.Extension -eq ".svg") {
        continue
    }
    
    if (Convert-ImageToWebP -SourceFile $image.FullName -DestFile $webpFile -Tool $tool -Quality $Quality) {
        $convertedCount++
        $totalSize += (Get-Item $image.FullName).Length
        $savedSize += (Get-Item $image.FullName).Length - (Get-Item $webpFile).Length
        
        if ($DeleteOriginals) {
            Remove-Item $image.FullName
            Write-Host "  Deleted original: $(Split-Path $image.FullName -Leaf)" -ForegroundColor Gray
        }
    }
}

Write-Host "`n" -ForegroundColor Gray
Write-Host "Conversion Summary" -ForegroundColor Cyan
Write-Host "=================" -ForegroundColor Cyan
Write-Host "Files converted: $convertedCount" -ForegroundColor Green
Write-Host "Total original size: $([math]::Round($totalSize / 1MB, 2)) MB" -ForegroundColor Gray
Write-Host "Total saved: $([math]::Round($savedSize / 1MB, 2)) MB" -ForegroundColor Yellow

if (-not $DeleteOriginals) {
    Write-Host "`nNote: Original files were NOT deleted. Use -DeleteOriginals flag to remove them." -ForegroundColor Yellow
}

Write-Host "`nWebP conversion complete!" -ForegroundColor Cyan
