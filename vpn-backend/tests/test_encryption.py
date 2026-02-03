"""Encryption tests: AES output format compatible with Android."""
from django.test import TestCase, override_settings

from utils.encryption import encrypt_plaintext


class EncryptionTest(TestCase):
    def test_encrypt_returns_base64(self):
        key = b"0" * 32
        out = encrypt_plaintext("vless://test@host:443", key)
        self.assertIsInstance(out, str)
        import base64
        decoded = base64.b64decode(out, validate=True)
        self.assertGreaterEqual(len(decoded), 16)

    def test_encrypt_different_each_time(self):
        key = b"1" * 32
        a = encrypt_plaintext("same", key)
        b = encrypt_plaintext("same", key)
        self.assertNotEqual(a, b)

    def test_key_must_be_32_bytes(self):
        with self.assertRaises(ValueError):
            encrypt_plaintext("x", b"short")
