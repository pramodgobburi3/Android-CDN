from django.shortcuts import render
from django.http import HttpResponse, JsonResponse, HttpResponseRedirect
from django.shortcuts import render_to_response
from dashboard.models import Upload
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth import authenticate
from django.utils.encoding import smart_str
import json
import os
import string
import random
from websocket import create_connection
import socket
from django.contrib.auth import logout
from oauth2_provider.models import AccessToken, Application, RefreshToken
from django.utils import timezone
from users.models import UserProfile
from users.serializers import UsersProfileSerializer

# Create your views here.

client_id = 'sydM1StEQ5669FrQPowx0uDuXAluy2Kq38gWna9j'
client_secret = 'nNw5TKyABGEvqmuXOwQA5sEjIXB1GRJXBbAgFp34q52PR7JQzrT8vIGRg0rWBjrPEZEfIkEvnuzfNqKOamVxHpmMvoP3lgiIXsQpddVwauzxNuGZBaL4IeswiZSGGhYr'

@csrf_exempt
def login(request):
    if request.method == 'GET':
        if request.user is not None:
            if not request.user.is_anonymous:
                print(request.user.is_anonymous)
                uploads = Upload.objects.filter(user_id=request.user.id)
                context = {"uploads": uploads, "user": request.user}
                return render(template_name='dashboard/html/index.html', context=context, request=request)
            else:
                return render_to_response('dashboard/html/login.html')
    if request.method == 'POST':
        username = request.POST['username']
        password = request.POST['password']

        user = authenticate(username= username, password= password)

        if user is not None:
            uploads = Upload.objects.filter(user_id=user.id)
            context = {"uploads": uploads, "user": user}
            return render(template_name='dashboard/html/index.html', context=context, request=request)
        else:
            context = {"error": True}
            return render_to_response('dashboard/html/login.html', context=context)

@csrf_exempt
def logout_req(request):
    logout(request)
    return HttpResponseRedirect("/")

@csrf_exempt
def get_user_info(request):
    if request.method == 'POST':
        if request.POST.get("access_token") is None:
            return JsonResponse({"status": "No access token provided"}, safe=False)
        else:
            try:
                access_token = AccessToken.objects.get(token=request.POST.get("access_token"),
                                                       expires__gt=timezone.now())
            except:
                try:
                    access_token = AccessToken.objects.get(token=request.POST.get("access_token"))
                except:
                    return JsonResponse({"status": "Invalid token"})
                else:
                    return JsonResponse({"status": "Access token has expired. Request a new one."})
            else:
                user = access_token.user
                user_profile = UserProfile.objects.get(id=user.id)
                return JsonResponse({"status": "Successful", "user_name": user.username, "user_profile": UsersProfileSerializer(user_profile).data}, safe=False)

@csrf_exempt
def upload(request):
    if request.method == 'POST':
        name = id_generator()
        file = request.FILES['file']
        user_id = request.POST['user_id']
        if name != '':
            upload = Upload.objects.create(name=name, file=file, file_name=file.name, user_id=user_id)
            host = socket.gethostname()
            print(host)
            ws = create_connection("ws://localhost:2500/ws/uploads/")
            ws.send(json.dumps({'message': upload.name, "file_name": file.name}))

        return HttpResponseRedirect("/")


@csrf_exempt
def download(request, file_name):
    print(file_name)
    if request.method == 'GET':
        upload = Upload.objects.filter(name=file_name).first()
        filename = upload.file.name.split('/')[-1]
        response = HttpResponse(upload.file, content_type='text/plain')
        response['Content-Disposition'] = 'attachment; filename=%s' % filename
        return response


def id_generator(size=30, chars=string.ascii_lowercase + string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))

