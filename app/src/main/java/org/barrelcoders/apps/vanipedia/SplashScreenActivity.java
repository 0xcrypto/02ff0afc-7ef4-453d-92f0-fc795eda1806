package org.barrelcoders.apps.vanipedia;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.VideoView;
import android.net.Uri;
import android.media.MediaPlayer;

public class SplashScreenActivity extends AppCompatActivity {
    VideoView videoView;
    SharedPreferences sharedpreferences;
    public static final String VanipediaPreferences = "vanipedia.preferences";
    public static final String IsLoggedInPreferenceKey = "isLoggedIn";
    private static int SPLASH_TIME_OUT = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash_screen);

        WebView webView = findViewById(R.id.SpalshView);
        webView.loadUrl("file:///android_asset/index.html");

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startNextActivity();
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    public void startNextActivity(){
        String pref = GetSharedPreference(IsLoggedInPreferenceKey);
        if(pref != null){
            Intent myIntent = new Intent(this, PlayerActivity.class);
            startActivityForResult(myIntent, 0);
        }
        else{
            Intent myIntent = new Intent(this, SignInActivity.class);
            startActivityForResult(myIntent, 0);
        }
    }
    public String GetSharedPreference(String key){
        String value = null;
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        value = sharedpreferences.getString(key, null);
        return value;
    }
}
