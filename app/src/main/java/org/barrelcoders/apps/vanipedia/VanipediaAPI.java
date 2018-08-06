package org.barrelcoders.apps.vanipedia;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public interface VanipediaAPI {
    //for example: http://www.codeshow.in/vanipedia/index.php?r=Likes/APICreate&id=1098&location=XXX
    @POST("vanipedia/index.php")
    Call<String> postLike(@Query("r") String page, @Query("lecture_id") int lecture_id,
                          @Query("location") String location, @Query("id") String id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=AndroidDevices/APICreate&id=1098
    @POST("vanipedia/index.php")
    Call<String> addAndroidDevice(@Query("r") String page, @Query("id") String id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Comments/APICreate&id=1098
    @POST("vanipedia/index.php")
    Call<String> postComment(@Query("r") String page, @Query("lecture_id") int lecture_id,
                             @Query("message") String message, @Query("location") String location, @Query("id") String id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Rating/APICreate&id=1098&rating=4
    @POST("vanipedia/index.php")
    Call<String> postRating(@Query("r") String page, @Query("lecture_id") int lecture_id, @Query("rating") String rating,
                            @Query("location") String location, @Query("id") String id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=User/APICreate&name=Ankit&email=ankit@gmail.com&picture=xxx
    @POST("vanipedia/index.php")
    Call<String> register(@Query("r") String page, @Query("name") String name, @Query("email") String email, @Query("picture") String picture);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Visitors/APICrate&location=xxx&id=0
    @POST("vanipedia/index.php")
    Call<String> postVisitor(@Query("r") String page, @Query("location") String location, @Query("id") String id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Lectures/APIRealTimeCounters&id=52
    @GET("vanipedia/index.php")
    Call<String> getLectureCounters(@Query("r") String page, @Query("id") int id);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Notification/APIAllNotification
    @GET("vanipedia/index.php")
    Call<NotificationResponse> getNotifications(@Query("r") String page);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=AndroidDevices/AndroidAppVersion
    @GET("vanipedia/index.php")
    Call<String> getCurrentAppVersion(@Query("r") String page);

    //for example: http://www.codeshow.in/vanipedia/index.php?r=Site/Data
    @GET("vanipedia/index.php")
    Call<String> downloadData(@Query("r") String page);

}
