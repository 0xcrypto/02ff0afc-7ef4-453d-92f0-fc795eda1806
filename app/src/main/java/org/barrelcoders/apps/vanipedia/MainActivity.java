package org.barrelcoders.apps.vanipedia;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MainActivity extends AppCompatActivity {
    private TextView info;
    private LoginButton loginButton;
    SharedPreferences sharedpreferences;
    private CallbackManager callbackManager;
    private Context context;
    private Retrofit retrofit;
    private VanipediaAPI _vanipediaAPI = null;
    private String ROOT_URL = "http://www.codeshow.in/";
    public static final String VanipediaPreferences = "vanipedia.preferences";
    public static final String IsLoggedInPreferenceKey = "isLoggedIn";
    public static final String UserIdPreferenceKey = "LoggedInUserID";
    public static final String FacebookNamePreferenceKey = "facebook.profile.name";
    public static final String FacebookEmailPreferenceKey = "facebook.profile.email";
    public static final String FacebookPicturePreferenceKey = "facebook.profile.picture";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView(R.layout.activity_main);

        String pref = GetSharedPreference(IsLoggedInPreferenceKey);
        if(pref != null){
            Intent myIntent = new Intent(this, PlayerActivity.class);
            startActivityForResult(myIntent, 0);
        }

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);

        loginButton = (LoginButton)findViewById(R.id.login_button);
        loginButton.setVisibility(View.VISIBLE);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email", "user_birthday", "user_friends"));

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                SetSharedPreference(IsLoggedInPreferenceKey, "YES");

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday,picture.type(large)");
                new GraphRequest(AccessToken.getCurrentAccessToken(),
                        "me", parameters, HttpMethod.GET,
                        new GraphRequest.Callback() {
                            @Override
                            public void onCompleted(GraphResponse response) {
                                if (response != null) {
                                    try {
                                        loginButton.setVisibility(View.GONE);
                                        JSONObject data = response.getJSONObject();
                                        String name = data.getString("name");
                                        String email = data.getString("email");
                                        String picture = data.getJSONObject("picture").getJSONObject("data").getString("url");

                                        SetSharedPreference(FacebookNamePreferenceKey, name);
                                        SetSharedPreference(FacebookEmailPreferenceKey, email);
                                        SetSharedPreference(FacebookPicturePreferenceKey, picture);

                                        Call<String> call = _vanipediaAPI.register("User/APICreate", name, email, picture);
                                        call.enqueue(new Callback<String>() {
                                            @Override
                                            public void onResponse(Response<String> response, Retrofit retrofit) {
                                                if (response.isSuccess()) {
                                                    String userId = response.body().toString();
                                                    SetSharedPreference(UserIdPreferenceKey, userId);
                                                    Intent myIntent = new Intent(context, PlayerActivity.class);
                                                    startActivityForResult(myIntent, 0);
                                                }
                                                else{
                                                    Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                                Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
                                            }
                                        });


                                    } catch (Exception e) {
                                        Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                    }).executeAsync();
            }

            @Override
            public void onCancel() {
                Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
            }
        });


    }
    @Override
    public void onBackPressed() {
        String pref = GetSharedPreference(IsLoggedInPreferenceKey);
        if(pref == null){
            return;
        }
        else{
            super.onBackPressed();
        }
    }

    public void SetSharedPreference(String key, String value){
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, value);
        editor.commit();
        sharedpreferences = null;
    }
    public String GetSharedPreference(String key){
        String value = null;
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        value = sharedpreferences.getString(key, null);
        return value;
    }

}
