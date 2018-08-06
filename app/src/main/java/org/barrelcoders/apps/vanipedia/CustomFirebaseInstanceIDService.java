package org.barrelcoders.apps.vanipedia;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.*;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class CustomFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = CustomFirebaseInstanceIDService.class.getSimpleName();
    public static final String VanipediaPreferences = "vanipedia.preferences" ;
    private String ROOT_URL = "http://www.codeshow.in/";
    SharedPreferences sharedpreferences;
    private VanipediaAPI _vanipediaAPI = null;
    public static final String DeviceInformationSavedPreferenceKey = "vanipedia.device.info.saved";
    private Retrofit retrofit;
    @Override
    public void onTokenRefresh() {
        String deviceInformation = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Token Value: " + deviceInformation);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);
        setDeviceInformationFetched(deviceInformation);
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
    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    private void setDeviceInformationFetched(final String deviceID){
        String preference = GetSharedPreference(DeviceInformationSavedPreferenceKey);

        if(!isNetworkConnected()){
            Toast.makeText(getApplicationContext(), R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        if(preference == null){
            /*Call<String> call = _vanipediaAPI.addAndroidDevice("AndroidDevices/APICreate", deviceID);
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(retrofit.Response<String> response, Retrofit retrofit) {
                    if (response.isSuccess()) {
                        SetSharedPreference(DeviceInformationSavedPreferenceKey,  "YES");
                    }
                }
                @Override
                public void onFailure(Throwable t) {
                    Toast.makeText(getApplicationContext(), "Problem fetching device information, Please try again later", Toast.LENGTH_LONG).show();
                }
            });*/
            FirebaseMessaging.getInstance().subscribeToTopic("general");
            SetSharedPreference(DeviceInformationSavedPreferenceKey,  "YES");
            Log.d(TAG, "Topic subscription successed");

        }
    }
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            //Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            //if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
            //    scheduleJob();
            //} else {
                // Handle message within 10 seconds
            //    handleNow();
            //}

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
}