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

import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class ForgotPasswordActivity extends AppCompatActivity {
    private Context context;
    private Retrofit retrofit;
    private VanipediaAPI _vanipediaAPI = null;
    private JsonUtil _utility = null;
    private String ROOT_URL = "http://www.codeshow.in/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_forgot_password);
        context = this;

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);

        Button btnSendPassword = findViewById (R.id.btnSendPassword);
        btnSendPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendPassword();
            }
        });

        TextView btnSignIn = findViewById (R.id.btnSignIn);
        btnSignIn.setPaintFlags(btnSignIn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(context, SignInActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }

    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }
    public void SendPassword(){
        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        EditText txtEmail = findViewById (R.id.txtEmail);

        String email = txtEmail.getText().toString();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if(TextUtils.isEmpty(email) || !email.matches(emailPattern)){
            Toast.makeText(context, R.string.PleaseEnterValidEmail, Toast.LENGTH_LONG).show();
            return;
        }

        _utility = new JsonUtil();
        final ProgressDialog mailDialog = ProgressDialog.show(context ,"Sending email", "Please wait...");
        Call<String> call = _vanipediaAPI.tinyForgotPassword("User/APITinyForgotPassword", email);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                mailDialog.dismiss();
                if (response.isSuccess()) {
                    String status = response.body().toString();
                    if(status.equals("NOT_EXISTS")){
                        Toast.makeText(context, R.string.EmailNotExist, Toast.LENGTH_LONG).show();
                    }
                    else if(status.equals("MAIL_SENT_ERROR")){
                        Toast.makeText(context, R.string.ProblemSendingPasswordToEmail, Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(context, R.string.PasswordSentOnEmail, Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(context, R.string.ProblemSendingPasswordToEmail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                mailDialog.dismiss();
                Toast.makeText(context, R.string.ProblemSendingPasswordToEmail, Toast.LENGTH_LONG).show();
            }
        });
    }
}
