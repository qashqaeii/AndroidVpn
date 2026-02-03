"""
API: list active servers, sorted by priority, cached.
"""
from django.utils.decorators import method_decorator
from django.views.decorators.cache import cache_page
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from servers.models import Server
from servers.serializers import ServerListSerializer

CACHE_TTL = 60  # seconds


class ServerListView(APIView):
    """GET /api/servers/ — active servers only, encrypted config, sorted by priority."""

    throttle_scope = "anon"

    @method_decorator(cache_page(CACHE_TTL))
    def get(self, request):
        qs = Server.objects.filter(is_active=True).order_by("-priority", "id")
        serializer = ServerListSerializer(qs, many=True)
        return Response(serializer.data, status=status.HTTP_200_OK)
