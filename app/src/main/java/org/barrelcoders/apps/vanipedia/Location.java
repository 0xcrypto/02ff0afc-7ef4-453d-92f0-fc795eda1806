package org.barrelcoders.apps.vanipedia;

import com.google.gson.annotations.SerializedName;

/**
 * Created by mac on 5/14/18.
 */

public class Location {
    @SerializedName("locations")

    private int id = 0;
    private String name = null;
    private Lecture[] lectures = null;

    public Location(int id,
                   String name,
                    Lecture[] lectures) {
        this.id = id;
        this.name = name;
        this.lectures = lectures;
    }

    public String getName() {return this.name;}
    public int getId()
    {
        return this.id;
    }
    public Lecture[] getLectures() {return this.lectures;}
    public int getTotalLectures() {return this.lectures.length;}
    public Lecture getLectureById(int id)
    {
        Lecture _lecture = null;

        for(int i=0; i <= this.lectures.length - 1; i++){
            if(id == lectures[i].getId()){
                _lecture = lectures[i];
                break;
            }
            else{
                continue;
            }
        }
        return _lecture;
    }

    public Lecture getLectureByIndex(int index)
    {
        Lecture _lecture = null;

        for(int i=0; i <= this.lectures.length - 1; i++){
            if(i == index){
                _lecture = this.lectures[index];
                break;
            }
            else{
                continue;
            }
        }
        return _lecture;
    }

    public int getLectureNumberById(int id)
    {
        int number = 0;

        for(int i=0; i <= this.lectures.length - 1; i++){
            if(this.lectures[i].getId() == id){
                number = (i+1);
                break;
            }
            else{
                number++;
                continue;
            }
        }
        return number;
    }
}
