"""
Server model: VLESS config, country, flag, active, priority.
Plain config is never exposed via API; only encrypted.
"""
from django.db import models


class Server(models.Model):
    """VPN server entry. config_vless is stored plain in DB, encrypted in API response."""

    name = models.CharField(max_length=100)
    country = models.CharField(max_length=5)
    flag_emoji = models.CharField(max_length=10)
    config_vless = models.TextField(help_text="Plain VLESS URL; encrypted when sent to app.")
    is_active = models.BooleanField(default=True)
    priority = models.IntegerField(default=0, help_text="Higher = shown first.")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-priority", "id"]

    def __str__(self):
        return f"{self.name} ({self.country})"
