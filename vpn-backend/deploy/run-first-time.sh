#!/bin/bash
# بعد از اولین بار up کردن، این اسکریپت را از پوشه vpn-backend اجرا کنید:
#   cd /root/vpn-backend && bash deploy/run-first-time.sh

set -e
COMPOSE="docker compose -f deploy/docker-compose.server.yml"

echo "اجرای migrate..."
$COMPOSE exec web python manage.py migrate

echo ""
echo "ساخت کاربر ادمین (نام کاربری و رمز را وارد کنید):"
$COMPOSE exec web python manage.py createsuperuser

echo ""
echo "پایان. ادمین: http://IP:8000/admin/"
