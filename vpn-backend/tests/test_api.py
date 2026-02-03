"""API tests: response shape, active filter, encryption."""
import base64
from unittest.mock import patch

from django.test import TestCase, override_settings
from rest_framework.test import APIClient

from servers.models import Server


@override_settings(CONFIG_ENCRYPTION_KEY="a" * 32)
class ServerListAPITest(TestCase):
    def setUp(self):
        self.client = APIClient()

    def test_empty_list(self):
        r = self.client.get("/api/servers/")
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json(), [])

    def test_only_active_returned(self):
        Server.objects.create(
            name="Active", country="FI", flag_emoji="🇫🇮",
            config_vless="vless://...", is_active=True, priority=1
        )
        Server.objects.create(
            name="Inactive", country="DE", flag_emoji="🇩🇪",
            config_vless="vless://...", is_active=False, priority=2
        )
        r = self.client.get("/api/servers/")
        self.assertEqual(r.status_code, 200)
        data = r.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["name"], "Active")
        self.assertEqual(data[0]["country"], "FI")
        self.assertEqual(data[0]["status"], "online")

    def test_sorted_by_priority(self):
        Server.objects.create(name="Low", country="X", flag_emoji="x", config_vless="vless://x", priority=0)
        Server.objects.create(name="High", country="Y", flag_emoji="y", config_vless="vless://y", priority=10)
        r = self.client.get("/api/servers/")
        data = r.json()
        self.assertEqual(data[0]["name"], "High")
        self.assertEqual(data[1]["name"], "Low")

    def test_response_has_encrypted_config(self):
        Server.objects.create(
            name="S1", country="FI", flag_emoji="🇫🇮",
            config_vless="vless://secret@host:443", is_active=True
        )
        r = self.client.get("/api/servers/")
        data = r.json()
        self.assertEqual(len(data), 1)
        self.assertIn("config", data[0])
        # Config must not be plain VLESS
        self.assertNotIn("vless://secret", data[0]["config"])
        # Must be base64-like
        raw = base64.b64decode(data[0]["config"], validate=True)
        self.assertGreaterEqual(len(raw), 16)
