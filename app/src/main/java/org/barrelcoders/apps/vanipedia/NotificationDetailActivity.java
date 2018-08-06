package org.barrelcoders.apps.vanipedia;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class NotificationDetailActivity extends AppCompatActivity {

    public static String NotificationTitle = null ;
    public static String NotificationDescription = null ;
    public static String NotificationDate = null ;
    public static String NotificationImage = null ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView txtTitle =(TextView) findViewById(R.id.textViewTitle);
        txtTitle.setText(NotificationTitle);
        TextView txtDate =(TextView) findViewById(R.id.textViewDate);
        txtDate.setText(NotificationDate);
        TextView txtDescription=(TextView) findViewById(R.id.textViewDescription);
        txtDescription.setText(NotificationDescription);
        ImageView imgView =(ImageView) findViewById(R.id.imageView);
        Picasso.with(this).load(NotificationImage).into(imgView);

    }

}
