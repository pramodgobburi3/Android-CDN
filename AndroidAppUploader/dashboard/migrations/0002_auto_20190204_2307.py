# Generated by Django 2.1.5 on 2019-02-04 23:07

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('dashboard', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='upload',
            name='id',
            field=models.AutoField(primary_key=True, serialize=False),
        ),
    ]
