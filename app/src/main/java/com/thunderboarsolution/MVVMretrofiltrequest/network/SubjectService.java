package com.thunderboarsolution.MVVMretrofiltrequest.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit service for subjects metadata.
 * Base URL is configured in RetroInstance:
 *   https://687319aac75558e273535336.mockapi.io/
 *
 * Endpoint:
 *   GET /api/subjects
 */
public interface SubjectService {

    @GET("api/subjects")
    Call<List<Subject>> getSubjects();
}