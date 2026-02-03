# راهنمای استقرار بک‌اند VPN روی سرور اوبونتو

این راهنما برای سرور اوبونتو ۲۴.۰۴ (مثلاً با IP مثل `192.168.100.100`) است. بک‌اند با Docker اجرا می‌شود.

---

## پیش‌نیاز

- دسترسی root یا sudo به سرور
- اتصال اینترنت از سرور

---

## گام ۱: اتصال به سرور

```bash
ssh root@192.168.100.100
```

(آدرس را با IP یا دامنه واقعی سرور خود عوض کنید.)

---

## گام ۲: کپی کردن پروژه بک‌اند به سرور

از **کامپیوتر خود** (محلی)، پوشه `vpn-backend` را به سرور بفرستید:

```bash
# از پوشه AndroidVpn (همان جایی که vpn-backend قرار دارد)
scp -r vpn-backend root@192.168.100.100:/root/
```

یا اگر روی سرور git دارید:

```bash
# روی سرور
cd /root
git clone <آدرس-ریپو-شما> AndroidVpn
cd AndroidVpn/vpn-backend
```

بعد از این مرحله روی سرور مسیر پروژه باید `/root/vpn-backend` باشد.

---

## گام ۳: نصب Docker و Docker Compose روی سرور

به سرور وصل شوید و از داخل پروژه اسکریپت نصب را اجرا کنید:

```bash
ssh root@192.168.100.100
cd /root/vpn-backend
chmod +x deploy/install-docker.sh
sudo ./deploy/install-docker.sh
```

---

## گام ۴: تنظیم فایل .env

روی سرور:

```bash
cd /root/vpn-backend
cp deploy/.env.server.example .env
nano .env
```

**حتماً این موارد را تغییر دهید:**

| متغیر | توضیح |
|--------|--------|
| `SECRET_KEY` | یک رشته تصادفی طولانی (حداقل ۵۰ کاراکتر) |
| `ALLOWED_HOSTS` | IP سرور خود، مثلاً `192.168.100.100` |
| `CONFIG_ENCRYPTION_KEY` | دقیقاً ۳۲ کاراکتر — **همان کلیدی که در اپ اندروید استفاده شده** |
| `USE_HTTPS` | برای دسترسی با IP و HTTP برابر `false` بگذارید |

مثال برای دسترسی با IP:

```
ALLOWED_HOSTS=192.168.100.100,localhost,127.0.0.1
USE_HTTPS=false
CONFIG_ENCRYPTION_KEY=your-32-char-key-here!!!!!!!!!
```

ذخیره: در `nano` با `Ctrl+O` و خروج با `Ctrl+X`.

---

## گام ۵: اجرای سرویس‌ها با Docker Compose

روی سرور:

```bash
cd /root/vpn-backend
docker compose -f deploy/docker-compose.server.yml build --no-cache
docker compose -f deploy/docker-compose.server.yml up -d
```

منتظر بمانید تا همه کانتینرها بالا بیایند (چند دقیقه). سپس یک بار مایگریشن و ساخت کاربر ادمین:

```bash
bash deploy/run-first-time.sh
```

نام کاربری و رمز را برای ورود به پنل ادمین انتخاب کنید.

---

## گام ۶: تست

- **API لیست سرورها:**  
  `http://192.168.100.100:8000/api/servers/`
- **پنل ادمین:**  
  `http://192.168.100.100:8000/admin/`

در اپ اندروید آدرس API را روی `http://192.168.100.100:8000/` (با پورت ۸۰۰۰) تنظیم کنید.

---

## دستورات مفید

| کار | دستور |
|-----|--------|
| مشاهده لاگ‌ها | `docker compose -f deploy/docker-compose.server.yml logs -f` |
| توقف سرویس‌ها | `docker compose -f deploy/docker-compose.server.yml down` |
| راه‌اندازی مجدد | `docker compose -f deploy/docker-compose.server.yml up -d` |
| ورود به شل وب | `docker compose -f deploy/docker-compose.server.yml exec web bash` |

---

## باز کردن پورت ۸۰۰۰ در فایروال (در صورت نیاز)

اگر از `ufw` استفاده می‌کنید:

```bash
sudo ufw allow 8000/tcp
sudo ufw reload
```

---

## بعداً: استفاده از دامنه و HTTPS

وقتی دامنه (مثلاً `api.example.com`) و گواهی SSL داشتید:

1. در `.env`:  
   `ALLOWED_HOSTS=api.example.com,192.168.100.100`  
   و `USE_HTTPS=true`
2. یک reverse proxy (مثلاً Nginx) با SSL جلوی پورت ۸۰۰۰ قرار دهید و در اپ اندروید آدرس را به `https://api.example.com/` تغییر دهید.

با انجام این گام‌ها، بک‌اند روی سرور شما در دسترس است و اپ می‌تواند به آن متصل شود.
