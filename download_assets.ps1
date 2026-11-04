$dest = "C:\Users\Shadow\OneDrive\Documents\TestPractice\Game Prodject\assets\rooms\"
$ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

Write-Host "=== Game Asset Downloader ===" -ForegroundColor Cyan

# 1. cat.png - CC0 pixel art cat sprite
Write-Host "`nDownloading cat.png (CC0 pixel art cat)..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "https://opengameart.org/sites/default/files/pixel_cat_0.png" -OutFile ($dest + "cat.png") -UserAgent $ua -TimeoutSec 30
    $sz = (Get-Item ($dest + "cat.png")).Length
    if ($sz -gt 5000) { Write-Host "  OK: cat.png ($sz bytes)" -ForegroundColor Green }
    else { Write-Host "  WARN: cat.png too small ($sz bytes), may be an error page" -ForegroundColor Red }
} catch { Write-Host "  FAILED: $_" -ForegroundColor Red }

# 2. roach.png - CC-BY 4.0 cockroach pixel art sprite (already exists but verify)
Write-Host "`nChecking/downloading roach.png (CC-BY 4.0 cockroach sprite)..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "https://opengameart.org/sites/default/files/roach_0.png" -OutFile ($dest + "roach.png") -UserAgent $ua -TimeoutSec 30
    $sz = (Get-Item ($dest + "roach.png")).Length
    if ($sz -gt 1000) { Write-Host "  OK: roach.png ($sz bytes)" -ForegroundColor Green }
    else { Write-Host "  WARN: roach.png too small ($sz bytes)" -ForegroundColor Red }
} catch { Write-Host "  FAILED: $_" -ForegroundColor Red }

# 3. bathroom.jpg - Wikimedia Commons bathroom (CC-BY-SA 3.0)
Write-Host "`nDownloading bathroom.jpg (Wikimedia Commons)..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri "https://upload.wikimedia.org/wikipedia/commons/thumb/0/05/Interior_Jacobsen_House_Bathroom_Earthship_2009.JPG/1280px-Interior_Jacobsen_House_Bathroom_Earthship_2009.JPG" -OutFile ($dest + "bathroom.jpg") -UserAgent $ua -TimeoutSec 60
    $sz = (Get-Item ($dest + "bathroom.jpg")).Length
    if ($sz -gt 50000) { Write-Host "  OK: bathroom.jpg ($sz bytes)" -ForegroundColor Green }
    else { Write-Host "  WARN: bathroom.jpg too small ($sz bytes)" -ForegroundColor Red }
} catch { Write-Host "  FAILED: $_" -ForegroundColor Red }

# 4. garage.jpg - Check existing or download from pxhere (CC0)
Write-Host "`nChecking existing garage.jpg..." -ForegroundColor Yellow
$garageFile = $dest + "garage.jpg"
if (Test-Path $garageFile) {
    $sz = (Get-Item $garageFile).Length
    Write-Host "  EXISTS: garage.jpg ($sz bytes)" -ForegroundColor Green
    Write-Host "  NOTE: Existing file may already be suitable. Check manually if it shows empty residential garage." -ForegroundColor Cyan
} else {
    Write-Host "  Not found, attempting download from pxhere..." -ForegroundColor Yellow
    try {
        # Try pxhere garage image
        $page = Invoke-WebRequest -Uri "https://pxhere.com/en/photo/961676" -UserAgent $ua -TimeoutSec 30
        $html = $page.Content
        $match = [regex]::Match($html, 'https://i\.pxhere\.com/photos/[^\s"]+\.jpg[^\s"]*')
        if ($match.Success) {
            $imgUrl = $match.Value -replace '".*',''
            Write-Host "  Found URL: $imgUrl" -ForegroundColor Cyan
            Invoke-WebRequest -Uri $imgUrl -OutFile $garageFile -UserAgent $ua -TimeoutSec 60
            $sz = (Get-Item $garageFile).Length
            Write-Host "  OK: garage.jpg ($sz bytes)" -ForegroundColor Green
        } else {
            Write-Host "  Could not parse CDN URL from pxhere page" -ForegroundColor Red
        }
    } catch { Write-Host "  FAILED: $_" -ForegroundColor Red }
}

# 5. moms_room.jpg - Download bedroom photo from pxhere (CC0)
Write-Host "`nDownloading moms_room.jpg (CC0 bedroom photo from pxhere)..." -ForegroundColor Yellow
try {
    $page2 = Invoke-WebRequest -Uri "https://pxhere.com/en/photo/1037310" -UserAgent $ua -TimeoutSec 30
    $html2 = $page2.Content
    $match2 = [regex]::Match($html2, 'https://i\.pxhere\.com/photos/[^\s"]+\.jpg[^\s"]*')
    if ($match2.Success) {
        $imgUrl2 = $match2.Value -replace '".*',''
        Write-Host "  Found URL: $imgUrl2" -ForegroundColor Cyan
        Invoke-WebRequest -Uri $imgUrl2 -OutFile ($dest + "moms_room.jpg") -UserAgent $ua -TimeoutSec 60
        $sz = (Get-Item ($dest + "moms_room.jpg")).Length
        if ($sz -gt 50000) { Write-Host "  OK: moms_room.jpg ($sz bytes)" -ForegroundColor Green }
        else { Write-Host "  WARN: moms_room.jpg too small ($sz bytes)" -ForegroundColor Red }
    } else {
        Write-Host "  Could not parse CDN URL. Trying fallback Wikimedia bedroom..." -ForegroundColor Yellow
        # Fallback: Wikimedia Commons bedroom
        Invoke-WebRequest -Uri "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/I_love_lucy_bedroom_set_1953.JPG/1280px-I_love_lucy_bedroom_set_1953.JPG" -OutFile ($dest + "moms_room.jpg") -UserAgent $ua -TimeoutSec 60
        $sz = (Get-Item ($dest + "moms_room.jpg")).Length
        Write-Host "  Fallback: moms_room.jpg ($sz bytes)" -ForegroundColor Cyan
    }
} catch { Write-Host "  FAILED: $_" -ForegroundColor Red }

# Final summary
Write-Host "`n=== FINAL FILE LISTING ===" -ForegroundColor Cyan
Get-ChildItem $dest | Where-Object { $_.Name -in @("garage.jpg","bathroom.jpg","moms_room.jpg","cat.png","roach.png") } |
    Select-Object Name, @{N="Size_KB";E={[math]::Round($_.Length/1KB,1)}} | Format-Table -AutoSize

Write-Host "`nDone! Please verify images visually." -ForegroundColor Green
