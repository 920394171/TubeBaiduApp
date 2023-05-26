package com.android.buaa.tubebaiduapp.utils;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Query;

public interface RetrofitService {
    @POST("upload/node")
    @Multipart
    Call<ResponseBody> uploadNode(
            @PartMap Map<String, RequestBody> paramsMap
    );

//    @POST("label")
//    @Multipart
//    fun uploadLabel(
//        @Part labelFile: MultipartBody.Part,
//        @PartMap paramsMap: MutableMap<String, @JvmSuppressWildcards RequestBody>
//    ): Call<ResponseBody>

    @POST("quest/node")
    @Multipart
    Call<ResponseBody> getNodeByDistance(
            @PartMap Map<String, RequestBody> paramsMap
    );

    @POST("upload/tube")
    @Multipart
    Call<ResponseBody> uploadTube(
            @PartMap Map<String, RequestBody> paramsMap
    );



    @GET("get")
    Call<ResponseBody> getNum(@Query("kw") String kw);


}
