#!/bin/bash
# نصب Docker و Docker Compose روی Ubuntu 24.04
# اجرا: sudo bash install-docker.sh

set -e
export DEBIAN_FRONTEND=noninteractive

echo "[1/5] به‌روزرسانی پکیج‌ها..."
apt-get update -qq

echo "[2/5] نصب پیش‌نیازها..."
apt-get install -y -qq ca-certificates curl gnupg lsb-release

echo "[3/5] اضافه کردن مخزن Docker..."
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "[4/5] نصب Docker Engine و Docker Compose..."
apt-get update -qq
apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "[5/5] فعال‌سازی و اجرای سرویس Docker..."
systemctl enable docker
systemctl start docker

echo ""
echo "=== نصب با موفقیت انجام شد ==="
docker --version
docker compose version
