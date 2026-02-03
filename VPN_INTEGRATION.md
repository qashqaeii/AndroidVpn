#VPN_INTEGRATION.md
# ادغام هستهٔ VPN واقعی (مثل v2rayNG)

در این پروژه لایهٔ ادغام آماده است؛ برای **اتصال واقعی** باید فایل **libv2ray.aar** (و در صورت تمایل **tun2socks**) را خودتان اضافه کنید.

## وضعیت فعلی کد

- **LibV2RayBridge**: با reflection به libv2ray متصل می‌شود. دو API پشتیبانی می‌شود: (۱) `go.libv2ray` با CoreController و (۲) **`libv2ray`** با V2RayPoint (AARهای Mronezc/2dust). اگر AAR در `app/libs/` باشد، هسته راه می‌افتد. برای اینکه سوکت‌های هسته از TUN خارج شوند، **Protect(fd)** در handler با `VpnService.protect(fd)` پیاده شده و از طریق `V2RayCoreManagerImpl` به پل داده می‌شود.
- **V2RayCoreManagerImpl**: در `start()` مقداردهی env، تنظیم **protectCallback** (برای VpnService.protect)، و سپس `LibV2RayBridge.start(configJson, protectCallback)` را صدا می‌زند. در صورت نبود یا عدم تطابق AAR، خطای «هستهٔ V2Ray راه نیفتاد» نمایش داده می‌شود؛ یک بار دیگر resolve در `start()` امتحان می‌شود.
- **Tun2SocksRunner** و **Tun2SocksStub**: رابط و stub برای tun2socks تعریف شده؛ بعد از بالا آمدن هسته، `MyVpnService` با `tun2socksRunner.start(tunFd, "127.0.0.1:10808")` TUN را به SOCKS وصل می‌کند. با stub فعلی وضعیت «Connected» نمایش داده می‌شود ولی ترافیک سیستم از TUN هدایت نمی‌شود؛ برای هدایت واقعی ترافیک باید یک پیاده‌سازی واقعی tun2socks اضافه شود (بخش ۲ زیر).
- کانفیگ کامل V2Ray (inbound SOCKS روی ۱۰۸۰۸ + outbound VLESS) در `VlessParser.parseToV2RayJson()` ساخته می‌شود.

## ۱) هستهٔ V2Ray (libv2ray)

### منبع

- [AndroidLibV2rayLite](https://github.com/2dust/AndroidLibV2rayLite) (همان هستهٔ v2rayNG).
- **AAR از قبل بیلدشده:** [Mronezc/V2rayNG_Android.aar](https://github.com/Mronezc/V2rayNG_Android.aar) (ریلیزهای v1.8.1 و v1.8.4).

### نحوهٔ استفاده در این اپ (بدون نصب Go)

1. **دانلود خودکار (ترجیحی)**
   - با اولین بیلد، Gradle در صورت نبودن فایل، تلاش می‌کند `libv2ray.aar` را از ریلیزهای Mronezc دانلود کند و در `app/libs/` قرار دهد.
   - یا از ریشهٔ پروژه اسکریپت را اجرا کنید:  
     **Windows:** `.\download-libv2ray.ps1`  
     پس از آن پروژه را بیلد کنید.

2. **دانلود دستی**
   - برو به [ریلیزهای Mronezc/V2rayNG_Android.aar](https://github.com/Mronezc/V2rayNG_Android.aar/releases)، یکی از ریلیزها (مثلاً v1.8.1 یا v1.8.4) را باز کن، فایل AAR را دانلود کن و با نام **`libv2ray.aar`** در پوشه **`app/libs/`** قرار بده.

3. **قرار دادن در پروژه**
   - فایل AAR باید با نام `libv2ray.aar` در **`app/libs/`** باشد.
   - در `app/build.gradle.kts` وابستگی `implementation(fileTree("libs") { include("*.aar") })` از قبل اضافه شده است؛ با قرار دادن AAR در `app/libs/` به‌طور خودکار در بیلد لحاظ می‌شود.

3. **نام کلاس/پکیج در AAR**
   - پل با reflection پکیج‌های `go.libv2ray` و `libv2ray` را امتحان می‌کند و متدهای `InitCoreEnv` / `initCoreEnv`، `NewCoreController` / `newCoreController` و `StartLoop` / `startLoop` و `StopLoop` / `stopLoop` را پیدا می‌کند. اگر AAR شما پکیج یا نام متد متفاوتی دارد، در `LibV2RayBridge.kt` می‌توانید پکیج/نام متدها را اضافه یا اصلاح کنید.

بعد از اضافه کردن AAR و اجرای اپ، با زدن Connect باید هسته واقعاً بالا بیاید و در صورت پیکربندی درست سرور، اتصال برقرار شود (بدون tun2socks فقط ترافیکی که مستقیم از پروکسی SOCKS استفاده کند از هسته عبور می‌کند).

## ۲) اتصال TUN به هسته (tun2socks)

- هسته با کانفیگ فعلی یک **inbound SOCKS روی پورت ۱۰۸۰۸** (`127.0.0.1:10808`) باز می‌کند.
- برای اینکه **همهٔ ترافیک TUN** از VPN عبور کند، باید ترافیک TUN از طریق **tun2socks** به `127.0.0.1:10808` فرستاده شود.
- در این پروژه:
  - رابط `Tun2SocksRunner` با متدهای `start(tunFd, proxyAddress)` و `stop()` تعریف شده است.
  - بعد از بالا آمدن هسته، `MyVpnService` با آدرس `127.0.0.1:10808` (ثابت `SOCKS_PROXY_ADDRESS`) فراخوانی می‌کند.
  - پیاده‌سازی فعلی `Tun2SocksStub` هیچ کاری انجام نمی‌دهد؛ برای VPN واقعی باید یک پیاده‌سازی واقعی (مثلاً با [go-tun2socks](https://github.com/eycorsican/go-tun2socks) یا [xxf098/go-tun2socks-build](https://github.com/xxf098/go-tun2socks-build)) بنویسید و TUN fd و آدرس پروکسی را به آن بدهید و در سرویس به‌جای stub از همان استفاده کنید.

## خلاصه

- با قرار دادن **libv2ray.aar** در **`app/libs/`** هستهٔ V2Ray در اپ فعال می‌شود و Connect با هسته واقعی کار می‌کند.
- با اضافه کردن پیاده‌سازی واقعی **tun2socks** و اتصال TUN به `127.0.0.1:10808`، ترافیک سیستم از طریق VPN به سرور هدایت می‌شود و اپ مثل v2rayNG به‌طور کامل VPN می‌کند.
