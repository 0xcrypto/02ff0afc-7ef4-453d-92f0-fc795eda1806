package org.barrelcoders.apps.vanipedia;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
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
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class SignInActivity extends AppCompatActivity {
    private TextView info;
    private LoginButton loginButton;
    SharedPreferences sharedpreferences;
    private CallbackManager callbackManager;
    private Context context;
    private Retrofit retrofit;
    private VanipediaAPI _vanipediaAPI = null;
    private JsonUtil _utility = null;
    private String ROOT_URL = "http://www.codeshow.in/";
    public static final String VanipediaPreferences = "vanipedia.preferences";
    public static final String IsLoggedInPreferenceKey = "isLoggedIn";
    public static final String UserIdPreferenceKey = "LoggedInUserID";
    public static final String NamePreferenceKey = "profile.name";
    public static final String EmailPreferenceKey = "profile.email";
    public static final String PicturePreferenceKey = "profile.picture";

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
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sign_in);

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(client)
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);

        TextView btnSignUp = findViewById (R.id.btnSignUp);
        btnSignUp.setPaintFlags(btnSignUp.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, SignUpActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        TextView btnForgotPassword = findViewById (R.id.btnForgotPassword);
        btnForgotPassword.setPaintFlags(btnForgotPassword.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, ForgotPasswordActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        Button btnSignIn = findViewById (R.id.btnSignIn);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignIn();
            }
        });

        String pref = GetSharedPreference(IsLoggedInPreferenceKey);
        if(pref != null){
            Intent myIntent = new Intent(this, PlayerActivity.class);
            startActivityForResult(myIntent, 0);
        }

        loginButton = findViewById(R.id.login_button);
        loginButton.setVisibility(View.VISIBLE);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

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

                                        final ProgressDialog signInFBDialog = ProgressDialog.show(context ,"Signing in using facebook", "Please wait...");

                                        Call<User> call = _vanipediaAPI.tinyFBSignIn("User/APITinyFBSignIn", name, email, picture);
                                        call.enqueue(new Callback<User>() {
                                            @Override
                                            public void onResponse(Response<User> response, Retrofit retrofit) {
                                                signInFBDialog.dismiss();
                                                if (response.isSuccess()) {
                                                    User user = response.body();
                                                    String status = user.getStatus();
                                                    if(status.equals("SUCCESS")){
                                                        SetSharedPreference(IsLoggedInPreferenceKey, "YES");
                                                        SetSharedPreference(UserIdPreferenceKey, user.getId());
                                                        SetSharedPreference(NamePreferenceKey, user.getName());
                                                        SetSharedPreference(EmailPreferenceKey, user.getEmail());
                                                        SetSharedPreference(PicturePreferenceKey, user.getPicture());
                                                        SetSharedPreference(IsLoggedInPreferenceKey, "YES");

                                                        Intent myIntent = new Intent(context, PlayerActivity.class);
                                                        startActivityForResult(myIntent, 0);
                                                    }
                                                    else{
                                                        Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                                else{
                                                    Toast.makeText(context, R.string.ProblemFacebookLogin, Toast.LENGTH_LONG).show();
                                                }
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                                signInFBDialog.dismiss();
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

    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }
    public void SignIn(){
        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        EditText txtEmail = findViewById (R.id.txtEmail);
        EditText txtPassword = findViewById (R.id.txtPassword);

        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if(TextUtils.isEmpty(email) || !email.matches(emailPattern)){
            Toast.makeText(context, R.string.PleaseEnterValidEmail, Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(context, R.string.PleaseEnterPassword, Toast.LENGTH_LONG).show();
            return;
        }

        _utility = new JsonUtil();


        final ProgressDialog signInDialog = ProgressDialog.show(context ,"Signing in", "Please wait...");
        Call<User> call = _vanipediaAPI.tinySignIn("User/APITinySignIn", email, password);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Response<User> response, Retrofit retrofit) {
                signInDialog.dismiss();
                if (response.isSuccess()) {
                    User user = response.body();
                    String status = user.getStatus();
                    if(status.equals("NOT_EXISTS")){
                        Toast.makeText(context, R.string.EmailNotExist, Toast.LENGTH_LONG).show();
                    }
                    else if(status.equals("PASSWORD_NOT_MATCHED")){
                        Toast.makeText(context, R.string.PasswordNotMatched, Toast.LENGTH_LONG).show();
                    }
                    else{
                        SetSharedPreference(UserIdPreferenceKey, user.getId());
                        SetSharedPreference(NamePreferenceKey, user.getName());
                        SetSharedPreference(EmailPreferenceKey, user.getEmail());
                        SetSharedPreference(PicturePreferenceKey, user.getPicture());
                        SetSharedPreference(IsLoggedInPreferenceKey, "YES");

                        Intent myIntent = new Intent(context, PlayerActivity.class);
                        startActivityForResult(myIntent, 0);
                    }
                }
                else{
                    Toast.makeText(context, R.string.ProblemSignIn, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                signInDialog.dismiss();
                Toast.makeText(context, R.string.ProblemSignIn, Toast.LENGTH_LONG).show();
            }
        });
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
