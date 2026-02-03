"""
Serializers for Server API. Config is encrypted before response.
"""
from django.conf import settings
from rest_framework import serializers

from servers.models import Server
from utils.encryption import encrypt_plaintext


class ServerListSerializer(serializers.ModelSerializer):
    """Public list: id, name, country, flag, encrypted config, status."""

    flag = serializers.CharField(source="flag_emoji", read_only=True)
    config = serializers.SerializerMethodField()
    status = serializers.SerializerMethodField()

    class Meta:
        model = Server
        fields = ["id", "name", "country", "flag", "config", "status"]

    def get_config(self, obj: Server) -> str:
        """Encrypt config_vless before sending. Never expose plain."""
        key = getattr(settings, "CONFIG_ENCRYPTION_KEY", None) or ""
        if isinstance(key, str):
            key = key.encode("utf-8")[:32].ljust(32, b"\0")
        return encrypt_plaintext(obj.config_vless, key)

    def get_status(self, obj: Server) -> str:
        return "online" if obj.is_active else "offline"
