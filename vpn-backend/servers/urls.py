from django.urls import path

from servers.views import ServerListView

urlpatterns = [
    path("", ServerListView.as_view(), name="server-list"),
]
