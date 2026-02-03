from django.urls import path, include

urlpatterns = [
    path("servers/", include("servers.urls")),
]
