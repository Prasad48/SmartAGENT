package com.bhavaniprasad.smartagent;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface Api {

    String BASE_URL = "https://demo6977317.mockable.io/";

    @GET("fetch_config")
    Call<Dbhandler> getjson();
}
