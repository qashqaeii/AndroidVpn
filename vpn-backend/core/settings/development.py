"""Development settings. Load after base."""
from .base import *  # noqa: F401, F403

DEBUG = True
ALLOWED_HOSTS = ["*"]

if env("DATABASE_URL", default=""):
    DATABASES = {"default": env.db()}

# Optional: disable throttling in dev
REST_FRAMEWORK["DEFAULT_THROTTLE_RATES"] = {"anon": "1000/minute"}

if not CONFIG_ENCRYPTION_KEY:
    CONFIG_ENCRYPTION_KEY = "a" * 32  # 32-byte key for AES-256 (dev only)
