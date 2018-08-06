package org.barrelcoders.apps.vanipedia;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class NotificationAdapter extends BaseAdapter{
    List<Notification> result;
    Context context;
    int [] imageId;
    private static LayoutInflater inflater=null;
    public NotificationAdapter(NotificationListActivity mainActivity, List<Notification> notifications) {
        // TODO Auto-generated constructor stub
        result = notifications;
        context = mainActivity;
        inflater = ( LayoutInflater )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return result.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        TextView title;
        TextView description;
        TextView date;
        ImageView img;

    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Notification notification = result.get(position);
        Holder holder=new Holder();
        View rowView;
        rowView = inflater.inflate(R.layout.notification, null);
        holder.title=(TextView) rowView.findViewById(R.id.textViewTitle);
        holder.date=(TextView) rowView.findViewById(R.id.textViewDate);
        holder.description=(TextView) rowView.findViewById(R.id.textViewDescription);
        holder.img=(ImageView) rowView.findViewById(R.id.imageView);
        holder.title.setText(notification.getTitle());
        holder.description.setText(notification.getDescription());
        holder.date.setText(notification.getDate());
        Picasso.with(context).load(notification.getImage()).into(holder.img);
        rowView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationDetailActivity.NotificationTitle = notification.getTitle();
                NotificationDetailActivity.NotificationDescription = notification.getDescription();
                NotificationDetailActivity.NotificationDate = notification.getDate();
                NotificationDetailActivity.NotificationImage = notification.getImage();
                Intent myIntent = new Intent(v.getContext(), NotificationDetailActivity.class);
                ((Activity) v.getContext()).startActivityForResult(myIntent, 0);
            }
        });
        return rowView;
    }

}
