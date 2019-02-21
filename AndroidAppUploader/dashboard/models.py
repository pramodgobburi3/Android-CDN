from django.db import models
from users.models import *

# Create your models here.
class Upload(models.Model):
    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=100)
    file = models.FileField(upload_to='uploads/')
    file_name = models.CharField(max_length=200, null=True)
    date_uploaded = models.DateTimeField(auto_now_add=True, blank=True)
    user = models.ForeignKey(User, on_delete=models.CASCADE, null=True)

    def __str__(self):
        return self.name

