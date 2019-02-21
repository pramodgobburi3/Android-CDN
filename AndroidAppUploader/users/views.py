from django.shortcuts import render
from django.http import JsonResponse
from django.contrib.auth.models import User
from django.views.decorators.csrf import csrf_exempt
from oauthlib.common import generate_token
from oauth2_provider.models import AccessToken, Application, RefreshToken
from .models import *
import datetime
from dashboard.models import Upload
from dashboard.views import *
from django.utils import timezone

# Create your views here.
@csrf_exempt
def user_register(request):
    if request.method == "POST":
        body = request.POST
        username = body['username']
        firstname = body['firstname']
        lastname = body['lastname']
        password = body['password']
        email = body['email']

        new_user = User.objects.create_user(username=username, email=email, password=password,
                                            first_name=firstname, last_name=lastname)
        profile = UserProfile.objects.get(user=new_user)
        profile.fistname = firstname
        profile.lastname = lastname
        profile.email = email
        profile.save()
        tokens = manually_create_access_token(new_user)
        return JsonResponse({"status": "success", "Access_Token": tokens["Access_Token"],
                             "Refresh_Token": tokens["Refresh_Token"]})
    else:
        return JsonResponse({"status": "failed"})


@csrf_exempt
def web_register(request):
    if request.method == "GET":
        if request.user is not None:
            if not request.user.is_anonymous:
                uploads = Upload.objects.filter(user_id=request.user.id)
                context = {"uploads": uploads, "user": request.user}
                return render(template_name='dashboard/html/index.html', context=context, request=request)
        context = {"client_id": client_id, "client_secret": client_secret}
        return render(template_name='dashboard/html/register.html', context=context, request=request)
    elif request.method == "POST":
        body = request.POST
        username = body['username']
        firstname = body['firstname']
        lastname = body['lastname']
        password = body['password']
        email = body['email']

        new_user = User.objects.create_user(username=username, email=email, password=password,
                                            first_name=firstname, last_name=lastname)
        profile = UserProfile.objects.get(user=new_user)
        profile.fistname = firstname
        profile.lastname = lastname
        profile.email = email
        profile.save()
        tokens = manually_create_access_token(new_user)
        return HttpResponseRedirect("/")



def manually_create_access_token(user):
    apps = Application.objects.all()

    app = apps[0]
    token = generate_token()
    refresh_toekn = generate_token()
    expire_time = datetime.datetime.now() + datetime.timedelta(days=1)
    ac = AccessToken.objects.create(user=user, token=token, application=app, created=datetime.datetime.now(), expires=expire_time,
                                    scope="read write")
    RefreshToken.objects.create(user=user, token=refresh_toekn, application=app, created=datetime.datetime.now(),
                                access_token=ac)

    return {"Access_Token": token, "Refresh_Token": refresh_toekn}