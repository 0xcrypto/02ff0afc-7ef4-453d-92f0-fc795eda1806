package org.barrelcoders.apps.vanipedia;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("image")
    private String image;
    @SerializedName("date")
    private String date;

    public Notification(String id, String title, String description, String image, String date) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.date = date;
    }

    public String getTitle(){
        return title;
    }

    public String getDescription(){
        return description;
    }

    public String getImage(){
        return image;
    }

    public String getDate(){
        return date;
    }
}
