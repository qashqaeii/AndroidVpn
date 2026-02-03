"""
AES-256-CBC encryption for VLESS config.
Output: Base64(IV_16_bytes + ciphertext). Compatible with Android ConfigDecryptor.
"""
import base64
import os
from typing import Optional

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

IV_LENGTH = 16
KEY_LENGTH = 32
ALGORITHM = "AES-256-CBC"


def _ensure_key(key: Optional[bytes]) -> bytes:
    """Ensure key is 32 bytes for AES-256."""
    if not key:
        raise ValueError("Encryption key must be set (e.g. from env).")
    key_bytes = key if isinstance(key, bytes) else key.encode("utf-8")
    if len(key_bytes) < KEY_LENGTH:
        raise ValueError(f"Key must be at least {KEY_LENGTH} bytes.")
    return key_bytes[:KEY_LENGTH]


def encrypt_plaintext(plaintext: str, key: Optional[bytes] = None) -> str:
    """
    Encrypt plaintext with AES-256-CBC. IV is prepended and both are Base64-encoded.
    Compatible with Android ConfigDecryptor (IV first 16 bytes, then ciphertext).
    """
    key_bytes = _ensure_key(key)
    iv = os.urandom(IV_LENGTH)
    backend = default_backend()
    cipher = Cipher(
        algorithms.AES(key_bytes),
        modes.CBC(iv),
        backend=backend,
    )
    encryptor = cipher.encryptor()
    # PKCS7 padding (pad to 16 bytes)
    pad_len = 16 - (len(plaintext.encode("utf-8")) % 16)
    padded = plaintext.encode("utf-8") + bytes([pad_len] * pad_len)
    ciphertext = encryptor.update(padded) + encryptor.finalize()
    return base64.b64encode(iv + ciphertext).decode("ascii")
