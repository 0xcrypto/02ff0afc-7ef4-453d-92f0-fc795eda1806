package org.barrelcoders.apps.vanipedia;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class SignUpActivity extends AppCompatActivity {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sign_up);

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);

        TextView btnSignIn = findViewById (R.id.btnSignIn);
        btnSignIn.setPaintFlags(btnSignIn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, SignInActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        Button btnSignUp = findViewById (R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignUp();
            }
        });

        loginButton = (LoginButton)findViewById(R.id.login_button);
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
                                                        SetSharedPreference(NamePreferenceKey, user.getName());
                                                        SetSharedPreference(EmailPreferenceKey, user.getEmail());
                                                        SetSharedPreference(PicturePreferenceKey, user.getPicture());
                                                        SetSharedPreference(UserIdPreferenceKey, user.getId());
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

    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }
    public void SignUp(){
        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        EditText txtName = findViewById (R.id.txtName);
        EditText txtEmail = findViewById (R.id.txtEmail);
        EditText txtPassword = findViewById (R.id.txtPassword);

        String name = txtName.getText().toString();
        String email = txtEmail.getText().toString();
        String password = txtPassword.getText().toString();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$";

        if(TextUtils.isEmpty(name)){
            Toast.makeText(context, R.string.PleaseEnterValidName, Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(email) || !email.matches(emailPattern)){
            Toast.makeText(context, R.string.PleaseEnterValidEmail, Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(password) || !password.matches(passwordPattern)){
            Toast.makeText(context, R.string.PleaseEnterValidPassword, Toast.LENGTH_LONG).show();
            return;
        }

        _utility = new JsonUtil();

        final ProgressDialog signUpDialog = ProgressDialog.show(context ,"Signing up on Vanipedia", "Please wait...");
        Call<User> call = _vanipediaAPI.tinySignUp("User/APITinySignUp", name, email, password);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Response<User> response, Retrofit retrofit) {
                signUpDialog.dismiss();
                if (response.isSuccess()) {
                    User user = response.body();
                    String status = user.getStatus();
                    if(status.equals("ERROR")){
                        Toast.makeText(context, R.string.ProblemSignUp, Toast.LENGTH_LONG).show();
                    }
                    else if(status.equals("ALREADY_REGISTERED")){
                        Toast.makeText(context, R.string.AlreadySignUpPleaseSignIn, Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context, R.string.SignUpSuccessPleaseSignIn, Toast.LENGTH_LONG).show();
                        Intent myIntent = new Intent(context, SignInActivity.class);
                        startActivityForResult(myIntent, 0);
                    }
                }
                else{
                    Toast.makeText(context, R.string.ProblemSignIn, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                signUpDialog.dismiss();
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
