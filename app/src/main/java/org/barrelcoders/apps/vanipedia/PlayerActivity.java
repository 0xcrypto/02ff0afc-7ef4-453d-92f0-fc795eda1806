package org.barrelcoders.apps.vanipedia;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class PlayerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public Location CurrentlyPlayingLocation = null;
    public Lecture CurrentlyPlayingLecture = null;
    public int CurrentLectureIndex = 0,
            TotalLectureInCurrentLocation = 0;
    public String LoggedInUserId = null;
    private Dialog progressDialog = null;
    private ArrayList<Location> _locationsOnDial;
    public static final String VanipediaPreferences = "vanipedia.preferences";
    public static final String IsLoggedInPreferenceKey = "isLoggedIn";
    public static final String UserIdPreferenceKey = "LoggedInUserID";
    public static final String FacebookNamePreferenceKey = "facebook.profile.name";
    public static final String FacebookEmailPreferenceKey = "facebook.profile.email";
    public static final String FacebookPicturePreferenceKey = "facebook.profile.picture";
    public static final String PlayerLocationsPreferenceKey = "player.locations";
    public static ArrayList<Location> PLAYER_LOCATIONS;
    public static int TOTAL_LOCATIONS = 0;
    SharedPreferences sharedpreferences;
    private ArrayList<Location> _locations;
    NavigationView navigationView;
    private static Matrix matrix;
    private ImageView innerDial;
    private ViewGroup _dialBookLinearLayout;
    private double[] angleRanges;
    private ProgressDialog lectureLoadingDialog;
    public static final int progress_bar_type = 0;
    private MediaPlayer lecturePlayer;
    private Context context;
    private Retrofit retrofit;
    private LinearLayout realtimeCounterContainer;
    private boolean onGoingChapterCounterRequest = false;
    private boolean onGoingChapterDownloadingRequest = false;
    private String ROOT_URL = "http://www.codeshow.in/";
    private VanipediaAPI _vanipediaAPI = null;
    private boolean isAnyLecturePlaying = false;
    private TextView currentChapterTextView = null;
    private boolean isLargeScreen = false;
    private AppEventsLogger logger;
    private GPSTracker gps;
    private String locationString;

    @Override
    protected void onResume() {
        super.onResume();
        //AppEventsLogger.activateApp(getApplication());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_player);
        context = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        currentChapterTextView = (TextView) findViewById(R.id.currentChapterTextView);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        isLargeScreen = metrics.densityDpi > 320 ? true :false;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play, this.getTheme()));
        } else {
            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CurrentlyPlayingLecture == null && !isAnyLecturePlaying){
                    Toast.makeText(context, R.string.SelectAnyLecture, Toast.LENGTH_LONG).show();
                    return;
                }

                FloatingActionButton fabtn = (FloatingActionButton)view;
                if(fabtn.getDrawable().getConstantState() == getResources().getDrawable(android.R.drawable.ic_media_play).getConstantState()){
                    if(lecturePlayer != null){
                        lecturePlayer.start();
                        isAnyLecturePlaying = true;
                        fabtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                        currentChapterTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        currentChapterTextView.setMarqueeRepeatLimit(-1);
                        currentChapterTextView.setSingleLine(true);
                        currentChapterTextView.setSelected(true);
                    }
                }
                else{
                    if(lecturePlayer != null && lecturePlayer.isPlaying()){
                        lecturePlayer.pause();
                        fabtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                        isAnyLecturePlaying = false;
                        currentChapterTextView.setSingleLine(false);
                    }
                }
            }
        });

        FloatingActionButton prevBtn = (FloatingActionButton) findViewById(R.id.prev);
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CurrentlyPlayingLocation == null){
                    Toast.makeText(context, R.string.SelectAnyLocation, Toast.LENGTH_LONG).show();
                    return;
                }

                if(lecturePlayer != null && lecturePlayer.isPlaying()) {
                    isAnyLecturePlaying = false;
                    lecturePlayer.stop();
                }

                TotalLectureInCurrentLocation = CurrentlyPlayingLocation.getTotalLectures();

                if(CurrentLectureIndex == 0 && !isAnyLecturePlaying){
                    CurrentLectureIndex = TotalLectureInCurrentLocation - 1;
                }
                else{
                    CurrentLectureIndex--;
                }

                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(CurrentLectureIndex);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });


        FloatingActionButton nextBtn = (FloatingActionButton) findViewById(R.id.next);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CurrentlyPlayingLocation == null){
                    Toast.makeText(context, R.string.SelectAnyLocation, Toast.LENGTH_LONG).show();
                    return;
                }

                if(lecturePlayer != null && lecturePlayer.isPlaying()) {
                    isAnyLecturePlaying = false;
                    lecturePlayer.stop();
                }

                TotalLectureInCurrentLocation = CurrentlyPlayingLocation.getTotalLectures();

                if(CurrentLectureIndex == (TotalLectureInCurrentLocation - 1) && !isAnyLecturePlaying){
                    CurrentLectureIndex = 0;
                }
                else{
                    CurrentLectureIndex++;
                }

                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(CurrentLectureIndex);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.setReadTimeout(60, TimeUnit.SECONDS);
        retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);

        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        _locations = new App().getLocations();

        loadUserPreferences();
        renderNavigationView();
        updateLocationsData();
        showLocationsOnDial();
        initializeLocationColors();
        initializeLocationsOnClick();
        loadFacebookProfile();
        initializeButtonsClickHandlers();

        if (matrix == null) {
            matrix = new Matrix();
        } else {
            matrix.reset();
        }

        innerDial = (ImageView) findViewById(R.id.innerDialView);
        //innerDial.setOnTouchListener(new OnTouchListener());

        ImageView backgroundImage = (ImageView) findViewById(R.id.backgroundImageView);
        backgroundImage.setScaleType(ImageView.ScaleType.FIT_XY);
        Picasso.with(context).load(R.drawable.screen_dot_1).into(backgroundImage);

        ImageView innerDialImage = (ImageView) findViewById(R.id.innerDialView);
        Picasso.with(context).load(R.drawable.screen_1).into(innerDialImage);

        realtimeCounterContainer = (LinearLayout) findViewById(R.id.realtimeCounterContainer);
        realtimeCounterContainer.setVisibility(View.INVISIBLE);



        try{
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getPackageName(),0);
            final float installedAppVersion = Float.parseFloat(pInfo.versionName);
            Call<String> call = _vanipediaAPI.getCurrentAppVersion("AndroidDevices/AndroidAppVersion");
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Response<String> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        float marketAppVersion = Float.parseFloat(response.body().toString());
                        if(installedAppVersion < marketAppVersion){
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("Warning")
                                    .setMessage("Your version of app is out of date please upgrade to enjoy latest features")
                                    .setCancelable(false)
                                    .setNegativeButton("Update",new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
                                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(i);
                                            //dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    }
                    else{
                        Toast.makeText(context, R.string.AppUpdateWarning, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(context, R.string.AppUpdateWarning, Toast.LENGTH_LONG).show();
                }
            });
        }
        catch (Exception e){
            Toast.makeText(context, R.string.AppUpdateWarning, Toast.LENGTH_LONG).show();
        }

        gps = new GPSTracker(PlayerActivity.this);

        if(gps.canGetLocation()){
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            locationString = latitude+","+longitude;
        }else{
            gps.showSettingsAlert();
        }

        Call<String> call = _vanipediaAPI.postVisitor("Visitors/APICreate", locationString, LoggedInUserId);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                if (response.isSuccess()) {}
                else{
                    Toast.makeText(context, R.string.ProblemFetchingFacebookInformation, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(context, R.string.ProblemFetchingFacebookInformation, Toast.LENGTH_LONG).show();
            }
        });

        //String deviceInformation = FirebaseInstanceId.getInstance().getToken();
    }

    private void updateLayout(){
        //LinearLayout layout = (LinearLayout)findViewById(R.id.outerDialViewLinearLayout);
        //if(isLargeScreen)
        //    layout.setLayoutParams(new LinearLayout.LayoutParams(400, 400));
    }
    @Override
    public void onStop() {
        super.onStop();
        dismissDialog();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissDialog();
    }

    private void dismissDialog(){
        if(progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
    }

    private void loadUserPreferences(){

        String userId = GetSharedPreference(UserIdPreferenceKey);
        if(userId != null){
            LoggedInUserId = userId;
        }

        String pref = GetSharedPreference(PlayerLocationsPreferenceKey);
        if(pref != null){
            PLAYER_LOCATIONS = new Gson().fromJson(pref, new TypeToken<ArrayList<Location>>() {}.getType());
            if(PLAYER_LOCATIONS.size() > 0){
                TOTAL_LOCATIONS = PLAYER_LOCATIONS.size();
            }
        }
        else {
            ArrayList<Location> _defaultPlayerLocations = new ArrayList<Location>();
            _defaultPlayerLocations.add(_locations.get(0));
            _defaultPlayerLocations.add(_locations.get(1));
            _defaultPlayerLocations.add(_locations.get(2));
            _defaultPlayerLocations.add(_locations.get(3));
            _defaultPlayerLocations.add(_locations.get(4));
            _defaultPlayerLocations.add(_locations.get(5));
            _defaultPlayerLocations.add(_locations.get(6));
            _defaultPlayerLocations.add(_locations.get(7));
            SetSharedPreference(PlayerLocationsPreferenceKey, new Gson().toJson(_defaultPlayerLocations, new TypeToken<ArrayList<Location>>(){}.getType()));
            PLAYER_LOCATIONS = _defaultPlayerLocations;
            TOTAL_LOCATIONS = PLAYER_LOCATIONS.size();
        }
    }
    public void setBackgroundImage(String name){
        try{
            ImageView backgroundImage = (ImageView) findViewById(R.id.backgroundImageView);
            backgroundImage.setScaleType(ImageView.ScaleType.FIT_XY);
            ImageView innerDialImage = (ImageView) findViewById(R.id.innerDialView);

            int index = -1;
            for(int i = 0; i <= _locationsOnDial.size()-1; i++) {
                if (_locationsOnDial.get(i) != null) {
                    if (_locationsOnDial.get(i).getName().equals(name)) {
                        index = i;
                        break;
                    }
                    else{
                        continue;
                    }
                }
                else{
                    continue;
                }
            }

            switch (index){
                case 0:
                    Picasso.with(context).load(R.drawable.screen_dot_1).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_1).into(innerDialImage);
                    break;
                case 1:
                    Picasso.with(context).load(R.drawable.screen_dot_2).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_2).into(innerDialImage);
                    break;
                case 2:
                    Picasso.with(context).load(R.drawable.screen_dot_3).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_3).into(innerDialImage);
                    break;
                case 3:
                    Picasso.with(context).load(R.drawable.screen_dot_4).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_4).into(innerDialImage);
                    break;
                case 5:
                    Picasso.with(context).load(R.drawable.screen_dot_5).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_5).into(innerDialImage);
                    break;
                case 6:
                    Picasso.with(context).load(R.drawable.screen_dot_6).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_6).into(innerDialImage);
                    break;
                case 7:
                    Picasso.with(context).load(R.drawable.screen_dot_7).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_7).into(innerDialImage);
                    break;
                case 8:
                    Picasso.with(context).load(R.drawable.screen_dot_8).into(backgroundImage);
                    Picasso.with(context).load(R.drawable.screen_8).into(innerDialImage);
                    break;
            }
        }
        catch (Exception e){
            Toast.makeText(context, R.string.ErrorOccurred, Toast.LENGTH_LONG).show();
        }
    }
    private class OnTouchListener implements View.OnTouchListener {
        private double startAngle;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    startAngle = getAngle(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_MOVE:
                    double currentAngle = getAngle(event.getX(), event.getY());
                    rotateDialer((float) (startAngle - currentAngle));
                    startAngle = currentAngle;
                    double absoluteAngle = Math.ceil(Math.abs(getCurrentAngle(event.getX(), event.getY())));
                    updateLectures(absoluteAngle);
                    //TextView currentAngleTextView = (TextView)findViewById(R.id.currentAngle);
                    //currentAngleTextView.setText(new Double(absoluteAngle).toString());
                    break;

                case MotionEvent.ACTION_UP:
                    break;
            }

            return true;
        }
    }
    private void loadFacebookProfile(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);

        String name = GetSharedPreference(FacebookNamePreferenceKey);
        if(name != null){
            TextView txtName = (TextView) header.findViewById(R.id.facebookProfileName);
            txtName.setTypeface(txtName.getTypeface(), Typeface.BOLD_ITALIC);
            txtName.setText("Welcome "+name);
        }
        String profilePictureUrl = GetSharedPreference(FacebookPicturePreferenceKey);
        if(profilePictureUrl != null){
            try {
                URL url = new URL(profilePictureUrl);
                ImageView imageView = (ImageView) header.findViewById(R.id.facebookProfileImage);
                new ImageDownloadTask(imageView).execute(url);
            } catch(IOException e) {
                Toast.makeText(context, R.string.ProblemFetchingFacebookInformation, Toast.LENGTH_LONG).show();
            }
        }
    }
    private void updateLectures(double angle){
        if(CurrentlyPlayingLocation != null){
            for(int i=0; i<=angleRanges.length-1; i++){
                if(angleRanges[i] == angle){
                    CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(i);

                    if(!isNetworkConnected()){
                        Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if(isAnyLecturePlaying){
                        //Toast.makeText(context, R.string.PausePlayCurrentChapter, Toast.LENGTH_LONG).show();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        final AlertDialog alert = builder.create();
                        builder.setTitle("Warning")
                                .setMessage(R.string.PausePlayCurrentLecture)
                                .setCancelable(false)
                                .setNegativeButton("OK",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alert.dismiss();
                                    }
                                });
                        alert.show();
                        return;
                    }

                    if(onGoingChapterCounterRequest || onGoingChapterDownloadingRequest){
                        //Toast.makeText(context, , Toast.LENGTH_LONG).show();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        final AlertDialog alert = builder.create();
                        builder.setTitle("Warning")
                                .setMessage(R.string.RequestAlreadyInQueue)
                                .setCancelable(false)
                                .setNegativeButton("OK",new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alert.dismiss();
                                    }
                                });
                        alert.show();
                        return;
                    }

                    CurrentLectureIndex = i+1;
                    playLectureMP3(CurrentlyPlayingLocation.getLectureByIndex(CurrentLectureIndex));
                }
            }
        }
    }
    private void updateRealTimeCounters(int chapterId){
        onGoingChapterCounterRequest = true;
        Call<String> call = _vanipediaAPI.getLectureCounters("Lectures/APIRealTimeCounters", chapterId);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                onGoingChapterCounterRequest = false;

                if (response.isSuccess()) {
                    String result = response.body().toString();
                    String[] counters = result.split("\\|");
                    Button btnLike = (Button) findViewById(R.id.btnLikes);
                    btnLike.setText(counters[0]);
                    Button btnComment = (Button) findViewById(R.id.btnComment);
                    btnComment.setText(counters[1]);
                    Button btnRating = (Button) findViewById(R.id.btnRating);
                    btnRating.setText(counters[2]);
                    realtimeCounterContainer.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(context, R.string.ErrorFetchingLectureCounter, Toast.LENGTH_LONG).show();
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        if(progressDialog != null && progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.ErrorFetchingLectureCounter, Toast.LENGTH_LONG).show();
                        if(progressDialog != null && progressDialog.isShowing()){
                            progressDialog.dismiss();
                        }
                    }
                });
                onGoingChapterCounterRequest = false;
            }
        });
    }
    private void playLectureMP3(Lecture lecture){
        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            String chapterUrl = lecture.getMP3URL();
            context = this;

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            if(lecturePlayer != null && lecturePlayer.isPlaying()) {
                fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                lecturePlayer.stop();
                lecturePlayer = null;
            }

            currentChapterTextView.setSingleLine(false);
            new DownloadChapterFromURL().execute(chapterUrl, Integer.toString(lecture.getId()));

        } catch (Exception e) {
            Toast.makeText(this, R.string.ProblemPlayingLecture, Toast.LENGTH_LONG).show();
        }
    }
    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }
    private double[] getAngles(int n){
        int x=0, y=90;
        float diff = (y-x)/n;
        float d = 0;
        double[] angles = new double[n];
        for(int i=1;i<=n;i++){
            d = d + diff;
            angles[i-1] = Math.round(d * 100.0) / 100.0;
        }
        return angles;
    }
    private void rotateDialer(float degrees) {
        matrix.postRotate(degrees);
    }
    private double getCurrentAngle(double xTouch, double yTouch){
        ImageView imageView = (ImageView)findViewById(R.id.innerDialView);
        double centreX=imageView.getX() + imageView.getWidth()  / 2;
        double centreY=imageView.getY() + imageView.getHeight() / 2;
        //double currentAngle = (Math.toDegrees( Math.atan2(xTouch - 360.0, 360.0 - yTouch) ) + 360.0) % 360.0;
        double currentAngle = Math.toDegrees( Math.atan2(-(yTouch - 360), (xTouch-360) ));
        return Math.round((((currentAngle + 360) % 360)-180) * 100.0) / 100.0 ;
    }
    public void setAngleRange(){
        realtimeCounterContainer.setVisibility(View.INVISIBLE);
        angleRanges = getAngles(CurrentlyPlayingLocation.getTotalLectures());
    }
    public void stopOngoingRequests(){
        onGoingChapterDownloadingRequest = false;
        onGoingChapterCounterRequest = false;
    }
    private double getAngle(double xTouch, double yTouch) {
        int dialerHeight = 200;
        int dialerWidth = 200;

        double x = xTouch - (dialerWidth / 2d);
        double y = dialerHeight - yTouch - (dialerHeight / 2d);

        switch (getQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 2:
                return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            case 3:
                return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
            default:
                return 0;
        }
    }
    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }
    public void renderNavigationView(){
        Menu menu = navigationView.getMenu();
        for (int i = 0; i < _locations.size(); i++) {
            Location location = _locations.get(i);
            if (isItemBelongsToPlayerLocations(location.getName())){
                menu.add(R.id.location, location.getId(), Menu.NONE, location.getName()).setIcon(R.drawable.ic_check).setCheckable(true).setChecked(true);
            }
            else{
                menu.add(R.id.location, location.getId(), Menu.NONE, location.getName()).setIcon(R.drawable.ic_uncheck).setCheckable(true).setChecked(false);
            }
        }
        menu.setGroupCheckable(R.id.location, false, true);
        menu.setGroupVisible(R.id.location, true);
    }
    public boolean isItemBelongsToPlayerLocations(String name){
        boolean doesBelong = false;
        for(int i = 0; i <= PLAYER_LOCATIONS.size()-1; i++){
            if(PLAYER_LOCATIONS.get(i).getName().equals(name)){
                doesBelong = true;
                break;
            }
            else{
                continue;
            }
        }
        return doesBelong;
    }
    public void updateLocationsData(){
        _locationsOnDial = new ArrayList<Location>(8);
        switch(PLAYER_LOCATIONS.size()){
            case 1:
                _locationsOnDial.add(0, null);
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(2, null);
                _locationsOnDial.add(3, null);
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, null);
                _locationsOnDial.add(7, null);
                _locationsOnDial.add(8, null);
                break;
            case 2:
                _locationsOnDial.add(0, null);
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(2, null);
                _locationsOnDial.add(3, null);
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, null);
                _locationsOnDial.add(7, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(8, null);
                break;
            case 3:
                _locationsOnDial.add(0, null);
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(2, null);
                _locationsOnDial.add(3, null);
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(7, null);
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(2));
                break;
            case 4:
                _locationsOnDial.add(0, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(1, null);
                _locationsOnDial.add(2, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(3, null);
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(2));
                _locationsOnDial.add(7, null);
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(3));
                break;
            case 5:
                _locationsOnDial.add(0, null);
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(2, null);
                _locationsOnDial.add(3, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, PLAYER_LOCATIONS.get(2));
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(3));
                _locationsOnDial.add(7, null);
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(4));
                break;
            case 6:
                _locationsOnDial.add(0, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(2, PLAYER_LOCATIONS.get(2));
                _locationsOnDial.add(3, null);
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(3));
                _locationsOnDial.add(7, PLAYER_LOCATIONS.get(4));
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(5));
                break;
            case 7:
                _locationsOnDial.add(0, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(2, PLAYER_LOCATIONS.get(2));
                _locationsOnDial.add(3, PLAYER_LOCATIONS.get(3));
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, null);
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(4));
                _locationsOnDial.add(7, PLAYER_LOCATIONS.get(5));
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(6));
                break;
            case 8:
                _locationsOnDial.add(0, PLAYER_LOCATIONS.get(0));
                _locationsOnDial.add(1, PLAYER_LOCATIONS.get(1));
                _locationsOnDial.add(2, PLAYER_LOCATIONS.get(2));
                _locationsOnDial.add(3, PLAYER_LOCATIONS.get(3));
                _locationsOnDial.add(4, null);
                _locationsOnDial.add(5, PLAYER_LOCATIONS.get(4));
                _locationsOnDial.add(6, PLAYER_LOCATIONS.get(5));
                _locationsOnDial.add(7, PLAYER_LOCATIONS.get(6));
                _locationsOnDial.add(8, PLAYER_LOCATIONS.get(7));
                break;
        }
    }
    public void showLocationsOnDial(){
        TextView txtBook0 = (TextView) findViewById (R.id.txtbook0);
        TextView txtBook1 = (TextView) findViewById (R.id.txtbook1);
        TextView txtBook2 = (TextView) findViewById (R.id.txtbook2);
        TextView txtBook3 = (TextView) findViewById (R.id.txtbook3);
        TextView txtBook5 = (TextView) findViewById (R.id.txtbook5);
        TextView txtBook6 = (TextView) findViewById (R.id.txtbook6);
        TextView txtBook7 = (TextView) findViewById (R.id.txtbook7);
        TextView txtBook8 = (TextView) findViewById (R.id.txtbook8);
        txtBook0.setTextColor(Color.WHITE);
        txtBook1.setTextColor(Color.WHITE);
        txtBook2.setTextColor(Color.WHITE);
        txtBook3.setTextColor(Color.WHITE);
        txtBook5.setTextColor(Color.WHITE);
        txtBook6.setTextColor(Color.WHITE);
        txtBook7.setTextColor(Color.WHITE);
        txtBook8.setTextColor(Color.WHITE);

        if(PLAYER_LOCATIONS.size() == 1){
            txtBook0.setText(null);
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(null);
            txtBook3.setText(null);
            txtBook5.setText(null);
            txtBook6.setText(null);
            txtBook7.setText(null);
            txtBook8.setText(null);

        }

        if(PLAYER_LOCATIONS.size() == 2){
            txtBook0.setText(null);
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(null);
            txtBook3.setText(null);
            txtBook5.setText(null);
            txtBook6.setText(null);
            txtBook7.setText(_locationsOnDial.get(7).getName());
            txtBook8.setText(null);

        }

        if(PLAYER_LOCATIONS.size() == 3){
            txtBook0.setText(null);
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(null);
            txtBook3.setText(null);
            txtBook5.setText(null);
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(null);
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }

        if(PLAYER_LOCATIONS.size() == 4){
            txtBook0.setText(_locationsOnDial.get(0).getName());
            txtBook1.setText(null);
            txtBook2.setText(_locationsOnDial.get(2).getName());
            txtBook3.setText(null);
            txtBook5.setText(null);
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(null);
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }

        if(PLAYER_LOCATIONS.size() == 5){
            txtBook0.setText(null);
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(null);
            txtBook3.setText(_locationsOnDial.get(3).getName());
            txtBook5.setText(_locationsOnDial.get(5).getName());
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(null);
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }

        if(PLAYER_LOCATIONS.size() == 6){
            txtBook0.setText(_locationsOnDial.get(0).getName());
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(_locationsOnDial.get(2).getName());
            txtBook3.setText(null);
            txtBook5.setText(null);
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(_locationsOnDial.get(7).getName());
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }

        if(PLAYER_LOCATIONS.size() == 7){
            txtBook0.setText(_locationsOnDial.get(0).getName());
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(_locationsOnDial.get(2).getName());
            txtBook3.setText(_locationsOnDial.get(3).getName());
            txtBook5.setText(null);
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(_locationsOnDial.get(7).getName());
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }

        if(PLAYER_LOCATIONS.size() == 8){
            txtBook0.setText(_locationsOnDial.get(0).getName());
            txtBook1.setText(_locationsOnDial.get(1).getName());
            txtBook2.setText(_locationsOnDial.get(2).getName());
            txtBook3.setText(_locationsOnDial.get(3).getName());
            txtBook5.setText(_locationsOnDial.get(5).getName());
            txtBook6.setText(_locationsOnDial.get(6).getName());
            txtBook7.setText(_locationsOnDial.get(7).getName());
            txtBook8.setText(_locationsOnDial.get(8).getName());
        }
    }
    private void selectLocation(String name){
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, R.string.WaitBeforePlaying, Toast.LENGTH_LONG).show();
            }
        });

        loadChapters(name);
        currentChapterTextView.setText(CurrentlyPlayingLocation.getName());
        setAngleRange();
        setBackgroundImage(name);
        stopOngoingRequests();
    }
    public void initializeLocationColors(){
        TextView txtBook0 = (TextView) findViewById (R.id.txtbook0);
        TextView txtBook1 = (TextView) findViewById (R.id.txtbook1);
        TextView txtBook2 = (TextView) findViewById (R.id.txtbook2);
        TextView txtBook3 = (TextView) findViewById (R.id.txtbook3);
        TextView txtBook5 = (TextView) findViewById (R.id.txtbook5);
        TextView txtBook6 = (TextView) findViewById (R.id.txtbook6);
        TextView txtBook7 = (TextView) findViewById (R.id.txtbook7);
        TextView txtBook8 = (TextView) findViewById (R.id.txtbook8);
        txtBook0.setTextColor(Color.WHITE);
        txtBook1.setTextColor(Color.WHITE);
        txtBook2.setTextColor(Color.WHITE);
        txtBook3.setTextColor(Color.WHITE);
        txtBook5.setTextColor(Color.WHITE);
        txtBook6.setTextColor(Color.WHITE);
        txtBook7.setTextColor(Color.WHITE);
        txtBook8.setTextColor(Color.WHITE);
    }
    public void initializeLocationsOnClick(){
        LinearLayout book_1_layout = (LinearLayout) findViewById (R.id.place0);
        book_1_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(0) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(0).getTotalLectures();
                selectLocation(_locationsOnDial.get(0).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook0 = (TextView) findViewById (R.id.txtbook0);
                txtBook0.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);

            }
        });

        LinearLayout book_2_layout = (LinearLayout) findViewById (R.id.place1);
        book_2_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(1) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(1).getTotalLectures();
                selectLocation(_locationsOnDial.get(1).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook1 = (TextView) findViewById (R.id.txtbook1);
                txtBook1.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });

        LinearLayout book_3_layout = (LinearLayout) findViewById (R.id.place2);
        book_3_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(2) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(2).getTotalLectures();
                selectLocation(_locationsOnDial.get(2).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook2 = (TextView) findViewById (R.id.txtbook2);
                txtBook2.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });

        LinearLayout book_4_layout = (LinearLayout) findViewById (R.id.place3);
        book_4_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(3) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(3).getTotalLectures();
                selectLocation(_locationsOnDial.get(3).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook3 = (TextView) findViewById (R.id.txtbook3);
                txtBook3.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);

            }
        });
        LinearLayout book_5_layout = (LinearLayout) findViewById (R.id.place5);
        book_5_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(5) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(5).getTotalLectures();
                selectLocation(_locationsOnDial.get(5).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook5 = (TextView) findViewById (R.id.txtbook5);
                txtBook5.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });


        LinearLayout book_6_layout = (LinearLayout) findViewById (R.id.place6);
        book_6_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(6) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(6).getTotalLectures();
                selectLocation(_locationsOnDial.get(6).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook6 = (TextView) findViewById (R.id.txtbook6);
                txtBook6.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);

            }
        });

        LinearLayout book_7_layout = (LinearLayout) findViewById (R.id.place7);
        book_7_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(7) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(7).getTotalLectures();
                selectLocation(_locationsOnDial.get(7).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook7 = (TextView) findViewById (R.id.txtbook7);
                txtBook7.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });

        LinearLayout book_8_layout = (LinearLayout) findViewById (R.id.place8);
        book_8_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(_locationsOnDial.get(8) == null)
                    return;

                TotalLectureInCurrentLocation = _locationsOnDial.get(8).getTotalLectures();
                selectLocation(_locationsOnDial.get(8).getName());
                CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(0);
                initializeLocationColors();
                TextView txtBook8 = (TextView) findViewById (R.id.txtbook8);
                txtBook8.setTextColor(Color.YELLOW);
                playLectureMP3(CurrentlyPlayingLecture);
            }
        });
    }
    public void loadChapters(String locationName){
        CurrentlyPlayingLocation = searchLocationByName(locationName);
    }
    public void SetSharedPreference(String key, String value){
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, value);
        editor.commit();
        sharedpreferences = null;
    }
    public void RemoveSharedPreference(String key){
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.remove(key);
        editor.commit();
        sharedpreferences = null;
    }
    public String GetSharedPreference(String key){
        String value = null;
        sharedpreferences = getSharedPreferences(VanipediaPreferences, Context.MODE_PRIVATE);
        value = sharedpreferences.getString(key, null);
        return value;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            String pref = GetSharedPreference(IsLoggedInPreferenceKey);
            if(pref != null){
                return;
            }
            else{
                super.onBackPressed();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(lecturePlayer != null)
            lecturePlayer.stop();

        if(!isNetworkConnected()){
            Toast.makeText(this, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return true;
        }

        if(id == R.id.action_notification){
            Intent myIntent = new Intent(this, NotificationListActivity.class);
            startActivityForResult(myIntent, 0);
        }

        if (id == R.id.action_logout) {
            LoginManager.getInstance().logOut();
            RemoveSharedPreference(IsLoggedInPreferenceKey);
            RemoveSharedPreference(FacebookNamePreferenceKey);
            RemoveSharedPreference(FacebookEmailPreferenceKey);
            RemoveSharedPreference(FacebookPicturePreferenceKey);
            Intent myIntent = new Intent(this, MainActivity.class);
            startActivityForResult(myIntent, 0);
        }

        return super.onOptionsItemSelected(item);
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        if (item.getIcon().getConstantState().equals(ContextCompat.getDrawable(this, R.drawable.ic_check).getConstantState())) {
            if(PLAYER_LOCATIONS.size() == 1){
                Toast.makeText(this, R.string.AtleastOnLocationOnDial, Toast.LENGTH_LONG).show();
            }
            else {
                Iterator<Location> iterator = PLAYER_LOCATIONS.iterator();
                while(iterator.hasNext())
                {
                    if (iterator.next().getName().equals(item.getTitle()))
                    {
                        iterator.remove();
                        break;
                    }
                }

                TOTAL_LOCATIONS = PLAYER_LOCATIONS.size();
                Type listOfTestObject = new TypeToken<ArrayList<Location>>(){}.getType();
                SetSharedPreference(PlayerLocationsPreferenceKey, new Gson().toJson(PLAYER_LOCATIONS, listOfTestObject));
                item.setIcon(R.drawable.ic_uncheck);
            }
        }
        else{
            if(PLAYER_LOCATIONS.size() >= 8){
                Toast.makeText(this, R.string.MaxLocationOnDial, Toast.LENGTH_LONG).show();
            }
            else{
                PLAYER_LOCATIONS.add(searchLocationByName(item.getTitle().toString()));
                TOTAL_LOCATIONS = PLAYER_LOCATIONS.size();
                Type listOfTestObject = new TypeToken<ArrayList<Location>>(){}.getType();
                SetSharedPreference(PlayerLocationsPreferenceKey, new Gson().toJson(PLAYER_LOCATIONS, listOfTestObject));
                item.setIcon(R.drawable.ic_check);
            }
        }

        updateLocationsData();
        showLocationsOnDial();
        initializeLocationColors();

        return true;
    }
    public Location searchLocationByName(String name){
        Location _location = null;

        for(int i=0; i<=_locations.size()-1; i++){

            if(_locations.get(i).getName().equals(name)){
                _location = _locations.get(i);
                break;
            }
        }

        return _location;
    }
    protected Dialog createDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                lectureLoadingDialog = new ProgressDialog(this);
                lectureLoadingDialog.setTitle("Downloading Lecture");
                lectureLoadingDialog.setMessage("Please wait while lecture is downloading...");
                lectureLoadingDialog.setCancelable(false);
                lectureLoadingDialog.show();
                return lectureLoadingDialog;
            default:
                return null;
        }
    }
    public void initializeButtonsClickHandlers(){
        Button btnLike = (Button) findViewById(R.id.btnLikes);
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isNetworkConnected()){
                    Toast.makeText(context, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
                    return;
                }

                if(CurrentlyPlayingLecture == null){
                    Toast.makeText(context, R.string.PlayLectureFirst, Toast.LENGTH_LONG).show();
                    return;
                }

                final ProgressDialog likeDialog = ProgressDialog.show(context ,"Like this Lecture", "Please wait...");
                Call<String> call = _vanipediaAPI.postLike("Likes/APICreate", CurrentlyPlayingLecture.getId(), locationString, LoggedInUserId);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Response<String> response, Retrofit retrofit) {
                        if (likeDialog != null && likeDialog.isShowing()) {
                            likeDialog.dismiss();
                        }

                        if (response.isSuccess()) {
                            int likes_count = new Integer(response.body().toString());
                            Button btnLike = (Button) findViewById(R.id.btnLikes);
                            btnLike.setText(String.valueOf(likes_count));
                        } else {
                            Toast.makeText(context, R.string.ProblemLikingLecture, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (likeDialog != null && likeDialog.isShowing()) {
                            likeDialog.dismiss();
                        }

                        Toast.makeText(context, R.string.ProblemLikingLecture, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        Button btnComment = (Button) findViewById(R.id.btnComment);
        btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isNetworkConnected()){
                    Toast.makeText(context, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
                    return;
                }

                if(CurrentlyPlayingLecture == null){
                    Toast.makeText(context, R.string.PlayLectureFirst, Toast.LENGTH_LONG).show();
                    return;
                }

                final Dialog commentDialog = new Dialog(context);
                commentDialog.setContentView(R.layout.comment_dialog);
                commentDialog.setTitle("Your comment");

                Button dialogButton = (Button) commentDialog.findViewById(R.id.commentSubmit);
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText textView = (EditText) commentDialog.findViewById(R.id.message);
                        String message = textView.getText().toString();

                        final ProgressDialog commentProgressDialog = ProgressDialog.show(context, "Comment this Lecture", "Please wait...");
                        Call<String> call = _vanipediaAPI.postComment("Comments/APICreate", CurrentlyPlayingLecture.getId(), message , locationString, LoggedInUserId);
                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Response<String> response, Retrofit retrofit) {
                                if (commentProgressDialog != null && commentProgressDialog.isShowing()) {
                                    commentProgressDialog.dismiss();
                                }

                                if (response.isSuccess()) {
                                    Toast.makeText(context, R.string.CommentPostedSuccessfully, Toast.LENGTH_LONG).show();
                                    int comment_count = new Integer(response.body().toString());
                                    Button btnComment = (Button) findViewById(R.id.btnComment);
                                    btnComment.setText(String.valueOf(comment_count));
                                } else {
                                    Toast.makeText(context, R.string.ProblemCommentingLecture, Toast.LENGTH_LONG).show();
                                }

                                commentDialog.dismiss();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (commentProgressDialog != null && commentProgressDialog.isShowing()) {
                                    commentProgressDialog.dismiss();
                                }

                                Toast.makeText(context, R.string.ProblemCommentingLecture, Toast.LENGTH_LONG).show();
                                commentDialog.dismiss();
                            }
                        });
                    }
                });

                commentDialog.show();
                return;
            }
        });

        Button btnRating = (Button) findViewById(R.id.btnRating);
        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isNetworkConnected()){
                    Toast.makeText(context, R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
                    return;
                }

                if(CurrentlyPlayingLecture == null){
                    Toast.makeText(context, R.string.PlayLectureFirst, Toast.LENGTH_LONG).show();
                    return;
                }

                final Dialog ratingDialog = new Dialog(context);
                ratingDialog.setContentView(R.layout.rating_dialog);
                ratingDialog.setTitle("Rate this Lecture");
                RatingBar ratingBar = (RatingBar) ratingDialog.findViewById(R.id.ratingBar);
                ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                        String ratingValue = String.valueOf(rating);

                        final ProgressDialog ratingProgressDialog = ProgressDialog.show(context, "Rate this Lecture", "Please wait...");
                        Call<String> call = _vanipediaAPI.postRating("Rating/APICreate", CurrentlyPlayingLecture.getId(),
                                ratingValue, locationString, LoggedInUserId);
                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Response<String> response, Retrofit retrofit) {
                                if (ratingProgressDialog != null && ratingProgressDialog.isShowing()) {
                                    ratingProgressDialog.dismiss();
                                }

                                if (response.isSuccess()) {
                                    float floatValue = Float.parseFloat(response.body().toString());
                                    Button btnRating = (Button) findViewById(R.id.btnRating);
                                    btnRating.setText(String.format("%.1f", floatValue));
                                } else {
                                    Toast.makeText(context, R.string.ProblemRatingLecture, Toast.LENGTH_LONG).show();
                                }
                                ratingDialog.dismiss();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (ratingProgressDialog != null && ratingProgressDialog.isShowing()) {
                                    ratingProgressDialog.dismiss();
                                }

                                Toast.makeText(context, R.string.ProblemRatingLecture, Toast.LENGTH_LONG).show();
                                ratingDialog.dismiss();
                            }
                        });
                    }
                });

                ratingDialog.show();
                return;
            }
        });
    }
    class DownloadChapterFromURL extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = createDialog(progress_bar_type);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            playDownloadedChapter(params[1], params[0]);

            return null;
        }

        protected void onProgressUpdate(String... progress) {
            //lectureLoadingDialog.setProgress(Integer.parseInt(progress[0]));
        }

        @Override
        protected void onPostExecute(String file_url) {
            onGoingChapterDownloadingRequest = false;
            runOnUiThread(new Runnable() {
                public void run() {
                    if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                }
            });

        }
    }
    class ImageDownloadTask extends AsyncTask<URL, Void, Bitmap>{

        ImageView _imageView;

        public ImageDownloadTask(ImageView imgView){
            _imageView = imgView;
        }

        @Override
        protected Bitmap doInBackground(URL... urls) {

            Bitmap networkBitmap = null;

            URL networkUrl = urls[0]; //Load the first element
            try {
                networkBitmap = BitmapFactory.decodeStream(
                        networkUrl.openConnection().getInputStream());
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.ProblemFetchingFacebookInformation, Toast.LENGTH_LONG).show();
                    }
                });
            }

            return networkBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            _imageView.setImageBitmap(result);
        }

    }
    private void playDownloadedChapter(final String chapterId, final String chapterUrl){
        try {
            onGoingChapterDownloadingRequest = true;
            lecturePlayer = new MediaPlayer();
            lecturePlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            lecturePlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            lecturePlayer.setDataSource(chapterUrl);
            lecturePlayer.prepareAsync();
            lecturePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    if(lecturePlayer != null){
                        onGoingChapterDownloadingRequest = false;
                        isAnyLecturePlaying = true;

                        Lecture lecture = CurrentlyPlayingLocation.getLectureById(Integer.parseInt(chapterId));
                        //CurrentlyPlayingLocation.getName() + " - " +
                        currentChapterTextView.setText(CurrentlyPlayingLocation.getName() +
                                " - ("+CurrentlyPlayingLocation.getLectureNumberById(CurrentlyPlayingLecture.getId())
                                +"/"+CurrentlyPlayingLocation.getTotalLectures()+") " +lecture.getVanipediaTitle());
                        currentChapterTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        currentChapterTextView.setMarqueeRepeatLimit(-1);
                        currentChapterTextView.setSingleLine(true);
                        currentChapterTextView.setSelected(true);

                        lecturePlayer.start();

                        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause, context.getTheme()));
                        } else {
                            fab.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                        }

                        updateRealTimeCounters(CurrentlyPlayingLecture.getId());

                    }

                }
            });
            lecturePlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    onGoingChapterDownloadingRequest = false;

                    if (progressDialog != null && progressDialog.isShowing())
                        progressDialog.dismiss();

                    Toast.makeText(context, R.string.ProblemPlayingLecture, Toast.LENGTH_LONG).show();
                    return false;
                }
            });
            lecturePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if(lecturePlayer != null)
                        lecturePlayer.stop();

                    lecturePlayer = null;

                    if (CurrentLectureIndex == CurrentlyPlayingLocation.getTotalLectures()){
                        CurrentLectureIndex = 1;
                    }
                    else{
                        CurrentLectureIndex++;
                    }

                    CurrentlyPlayingLecture = CurrentlyPlayingLocation.getLectureByIndex(CurrentLectureIndex - 1);
                    playLectureMP3(CurrentlyPlayingLecture);
                    currentChapterTextView.setSingleLine(false);
                }
            });
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                public void run() {

                    Toast.makeText(context, R.string.ProblemDownloaingLecture, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}


