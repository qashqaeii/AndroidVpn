# Generated migration for Server model

from django.db import migrations, models


class Migration(migrations.Migration):

    initial = True

    dependencies = []

    operations = [
        migrations.CreateModel(
            name="Server",
            fields=[
                ("id", models.BigAutoField(auto_created=True, primary_key=True, serialize=False, verbose_name="ID")),
                ("name", models.CharField(max_length=100)),
                ("country", models.CharField(max_length=5)),
                ("flag_emoji", models.CharField(max_length=10)),
                ("config_vless", models.TextField(help_text="Plain VLESS URL; encrypted when sent to app.")),
                ("is_active", models.BooleanField(default=True)),
                ("priority", models.IntegerField(default=0, help_text="Higher = shown first.")),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "ordering": ["-priority", "id"],
            },
        ),
    ]
