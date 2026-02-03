"""Production settings. Secure, Redis cache. HTTPS optional (USE_HTTPS env)."""
from .base import *  # noqa: F401, F403

DEBUG = False
USE_HTTPS = env.bool("USE_HTTPS", default=True)  # noqa: F405
SECURE_SSL_REDIRECT = USE_HTTPS
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https") if USE_HTTPS else None
SESSION_COOKIE_SECURE = USE_HTTPS
CSRF_COOKIE_SECURE = USE_HTTPS
SECURE_BROWSER_XSS_FILTER = True
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = "DENY"

# Database: PostgreSQL from env
DATABASES = {"default": env.db()}

# Redis cache
CACHES = {
    "default": {
        "BACKEND": "django_redis.cache.RedisCache",
        "LOCATION": env("REDIS_URL"),
        "OPTIONS": {"CLIENT_CLASS": "django_redis.client.DefaultClient"},
    }
}

# CORS: restrict to your app / domains
CORS_ALLOWED_ORIGINS = env.list("CORS_ALLOWED_ORIGINS", default=[])
if not CORS_ALLOWED_ORIGINS:
    CORS_ALLOWED_ORIGINS = ["https://your-app-domain.com"]
CSRF_TRUSTED_ORIGINS = env.list("CSRF_TRUSTED_ORIGINS", default=[])

# Gzip responses
try:
    mid_idx = MIDDLEWARE.index("django.middleware.common.CommonMiddleware")  # noqa: F405
    MIDDLEWARE.insert(mid_idx + 1, "django.middleware.gzip.GZipMiddleware")  # noqa: F405
except (ValueError, IndexError):
    pass

# Must set in production
if not CONFIG_ENCRYPTION_KEY:
    raise ValueError("CONFIG_ENCRYPTION_KEY must be set in production.")
