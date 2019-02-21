package com.example.pramodgobburi.appuploader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.pramodgobburi.appuploader.Models.Constants;
import com.example.pramodgobburi.appuploader.Models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import uk.me.hardill.volley.multipart.MultipartRequest;

public class LoginActivity extends AppCompatActivity {

    private EditText mUsername;
    private EditText mPassword;
    private Button mSigninBtn;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mUsername = (EditText) findViewById(R.id.signin_username);
        mPassword = (EditText) findViewById(R.id.signin_password);
        mSigninBtn = (Button) findViewById(R.id.signin_button);

        requestQueue = Volley.newRequestQueue(this);

        if(User.currentUser != null) {
            toMainActivity();
        }

        mSigninBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mUsername.getText().toString().isEmpty() || !mPassword.getText().toString().isEmpty()) {
                    signinRequest();
                }
            }
        });


    }

    private void signinRequest() {
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();

        MultipartRequest signinRequest = new MultipartRequest(
                Constants.Urls.SIGNIN,
                null,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        JSONObject jsonObject;
                        String json = "";
                        try {
                            json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            jsonObject = new JSONObject(json);

                            User.getInstance().setAccessToken(jsonObject.getString("access_token"));
                            User.getInstance().setRefreshToken(jsonObject.getString("refresh_token"));
                            Log.e("TOKEN", User.getAccessToken());

                            SharedPreferences sp = getSharedPreferences("Login", 0);
                            SharedPreferences.Editor Ed = sp.edit();
                            byte[] accessTokenData = User.getAccessToken().getBytes("UTF-8");
                            byte[] refreshTokenData = User.getRefreshToken().getBytes("UTF-8");
                            Ed.putString("id_accessToken", Base64.encodeToString(accessTokenData, Base64.DEFAULT));
                            Ed.putString("id_refreshToken", Base64.encodeToString(refreshTokenData, Base64.DEFAULT));
                            String loginType = "userPass";
                            Ed.putString("id_username", Base64.encodeToString(username.getBytes(), Base64.DEFAULT));
                            Ed.putString("id_password", Base64.encodeToString(password.getBytes(), Base64.DEFAULT));
                            Ed.commit();

                            getUserInfo();


                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        );

        signinRequest.addPart(new MultipartRequest.FormPart("username", username));
        signinRequest.addPart(new MultipartRequest.FormPart("password", password));
        signinRequest.addPart(new MultipartRequest.FormPart("client_id", Constants.APIKeys.CLIENTID));
        signinRequest.addPart(new MultipartRequest.FormPart("client_secret", Constants.APIKeys.CLIENTSECRET));
        signinRequest.addPart(new MultipartRequest.FormPart("grant_type", "password"));

        requestQueue.add(signinRequest);
    }

    private void getUserInfo() {
        MultipartRequest getUserRequest = new MultipartRequest(
                Constants.Urls.USERINFO,
                null,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        JSONObject jsonObject;
                        String json = "";
                        try {
                            json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                            jsonObject = new JSONObject(json);

                            if(jsonObject.getString("status").equals("Successful")) {
                                String username = jsonObject.getString("user_name");
                                JSONObject profile = jsonObject.getJSONObject("user_profile");
                                String firstname = profile.getString("firstname");
                                String lastname = profile.getString("lastname");
                                String email = profile.getString("email");
                                int id = profile.getInt("id");

                                User.currentUser.setUserData(id, username, firstname, lastname, email);

                                Log.e("ACCESS", User.currentUser.getAccessToken());
                                Log.e("REFRESH", User.currentUser.getRefreshToken());
                                Log.e("USERNAME", User.currentUser.getUsername());
                                Log.e("FIRSTNAME", User.currentUser.getFirstname());
                                Log.e("LASTNAME", User.currentUser.getLastname());
                                Log.e("EMAIL", User.currentUser.getEmail());
                                toMainActivity();
                            }


                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        );

        getUserRequest.addPart(new MultipartRequest.FormPart("access_token", User.currentUser.getAccessToken()));
        requestQueue.add(getUserRequest);
    }

    private void toMainActivity() {
        Intent toMain = new Intent(this, MainActivity.class);
        startActivity(toMain);
        finish();
    }
}
