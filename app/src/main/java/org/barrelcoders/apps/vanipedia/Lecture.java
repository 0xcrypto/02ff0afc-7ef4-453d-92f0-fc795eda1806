package org.barrelcoders.apps.vanipedia;

import com.google.gson.annotations.SerializedName;

/**
 * Created by mac on 5/14/18.
 */

public class Lecture {
    @SerializedName("lectures")

    private int id = 0;
    private String title = null;
    private String vanipedia_title = null;
    private String vanipedia_url = null;
    private String mp3_duration = null;
    private String mp3_url = null;
    private String youtube_url = null;
    private String date = null;


    public Lecture(int id,
                   String title,
                   String vanipedia_title,
                   String vanipedia_url,
                   String mp3_duration,
                   String mp3_url,
                   String youtube_url,
                   String date) {
        this.id = id;
        this.title = title;
        this.vanipedia_title = vanipedia_title;
        this.vanipedia_url = vanipedia_url;
        this.mp3_duration = mp3_duration;
        this.mp3_url = mp3_url;
        this.youtube_url = youtube_url;
        this.date = date;
    }

    public int getId() {return this.id;}
    public String getTitle() {return this.title;}
    public String getVanipediaTitle() {return this.vanipedia_title;}
    public String getVanipediaUrl() {return this.vanipedia_url;}
    public String getMP3Duraion() {return this.mp3_duration;}
    public String getMP3URL() {return this.mp3_url;}
    public String getYoutubeURL() {return this.youtube_url;}
    public String getDate() {return this.date;}

}
