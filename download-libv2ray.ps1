# دانلود libv2ray.aar از ریلیز ازپیش‌ساخته و قرار دادن در app/libs
# اجرا: .\download-libv2ray.ps1

$libsDir = Join-Path $PSScriptRoot "app\libs"
$dest = Join-Path $libsDir "libv2ray.aar"

if (Test-Path $dest) {
    Write-Host "libv2ray.aar از قبل وجود دارد: $dest"
    exit 0
}

New-Item -ItemType Directory -Force -Path $libsDir | Out-Null

$urls = @(
    "https://github.com/Mronezc/V2rayNG_Android.aar/releases/download/v1.8.1/libv2ray.aar",
    "https://github.com/Mronezc/V2rayNG_Android.aar/releases/download/v1.8.4/libv2ray.aar"
)

foreach ($url in $urls) {
    try {
        Write-Host "در حال دانلود از $url ..."
        Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing
        Write-Host "دانلود انجام شد: $dest"
        exit 0
    } catch {
        Write-Host "خطا: $_"
    }
}

Write-Host "دانلود از هیچ منبعی انجام نشد. لطفاً به صورت دستی از یکی از آدرس‌های زیر فایل را بگیرید و در app\libs با نام libv2ray.aar ذخیره کنید:"
Write-Host "  https://github.com/Mronezc/V2rayNG_Android.aar/releases"
exit 1
