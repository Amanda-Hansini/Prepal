package com.example.finalyearprojectnew.network;

import com.example.finalyearprojectnew.models.PredictionRequest;
import com.example.finalyearprojectnew.models.PredictionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PredictionApi {
    @POST("api/predict")
    Call<PredictionResponse> predictGpa(@Body PredictionRequest request);

    @retrofit2.http.Multipart
    @POST("api/predict")
    Call<PredictionResponse> uploadTranscript(
        @retrofit2.http.Part("student_id") okhttp3.RequestBody studentId,
        @retrofit2.http.Part okhttp3.MultipartBody.Part file
    );
}
