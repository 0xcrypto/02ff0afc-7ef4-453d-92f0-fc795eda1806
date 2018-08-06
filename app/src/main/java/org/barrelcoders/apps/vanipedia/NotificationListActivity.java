package org.barrelcoders.apps.vanipedia;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class NotificationListActivity extends AppCompatActivity {

    Context context;
    private VanipediaAPI _vanipediaAPI = null;
    private String ROOT_URL = "http://www.codeshow.in/";
    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list=(ListView) findViewById(R.id.notificationListView);
        list.setAdapter(null);
        final Context context = this;

        if(!isNetworkConnected()){
            Toast.makeText(getApplicationContext(), R.string.ConnectToInternet, Toast.LENGTH_LONG).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ROOT_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        _vanipediaAPI = retrofit.create(VanipediaAPI.class);
        Call<NotificationResponse> call = _vanipediaAPI.getNotifications("Notification/APIAllNotification");
        call.enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Response<NotificationResponse> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    List<Notification> notifications = response.body().getResults();
                    showList(notifications);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(context, R.string.ProblemFetchingNotifications, Toast.LENGTH_LONG).show();
            }
        });


    }

    private boolean isNetworkConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    public void showList(List<Notification> notifications){
        list.setAdapter(new NotificationAdapter(this, notifications));
    }

}
