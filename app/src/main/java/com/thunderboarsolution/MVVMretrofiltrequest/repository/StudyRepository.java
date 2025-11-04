package com.thunderboarsolution.MVVMretrofiltrequest.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.thunderboarsolution.MVVMretrofiltrequest.db.AppDatabase;
import com.thunderboarsolution.MVVMretrofiltrequest.db.SessionDao;
import com.thunderboarsolution.MVVMretrofiltrequest.db.SessionEntity;
import com.thunderboarsolution.MVVMretrofiltrequest.network.RetroInstance;
import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.network.SubjectService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository coordinating Room and Subjects Retrofit API.
 * - Caches subjects map in memory
 * - Exposes sessions LiveData from Room
 * - Provides filtered queries
 * - Handles inserts off main thread
 */
public class StudyRepository {

    private static volatile StudyRepository INSTANCE;

    private final Context appContext;
    private final AppDatabase db;
    private final SessionDao sessionDao;
    private final Executor ioExecutor = Executors.newSingleThreadExecutor();

    // Cache subjects in memory and expose as LiveData
    private final MutableLiveData<Map<String, Subject>> subjectsMapLiveData = new MutableLiveData<>(new HashMap<>());

    private StudyRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(appContext);
        this.sessionDao = db.sessionDao();
    }

    public static StudyRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (StudyRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StudyRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    // Subjects API
    public LiveData<Map<String, Subject>> getSubjectsMap() {
        return subjectsMapLiveData;
    }

    public void refreshSubjects() {
        SubjectService service = RetroInstance.getRetrofitClient().create(SubjectService.class);
        service.getSubjects().enqueue(new Callback<List<Subject>>() {
            @Override
            public void onResponse(Call<List<Subject>> call, Response<List<Subject>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Subject> map = new HashMap<>();
                    for (Subject s : response.body()) {
                        if (s != null && s.getId() != null) {
                            map.put(s.getId(), s);
                        }
                    }
                    subjectsMapLiveData.postValue(map);
                }
            }

            @Override
            public void onFailure(Call<List<Subject>> call, Throwable t) {
                // Keep prior cache; optionally post empty map or error state
            }
        });
    }

    // Sessions (Room)
    public LiveData<List<SessionEntity>> getAllSessions() {
        return sessionDao.getAllSessions();
    }

    public LiveData<List<SessionEntity>> getFilteredSessions(String subjectId,
                                                             long fromMillis,
                                                             long toMillis,
                                                             int minFocus,
                                                             int maxFocus,
                                                             String noteQuery) {
        return sessionDao.getFilteredSessions(subjectId, fromMillis, toMillis, minFocus, maxFocus, noteQuery);
    }

    public void insertSession(@NonNull SessionEntity entity) {
        ioExecutor.execute(() -> sessionDao.insert(entity));
    }

    // Helpers

    /**
     * Aggregate minutes per subject for last 7 days (nowMillis exclusive of older).
     * Returns a map of subjectId -> totalMinutes.
     */
    public Map<String, Integer> aggregateLast7DaysBySubject(@NonNull List<SessionEntity> sessions, long nowMillis) {
        long sevenDaysAgo = nowMillis - 7L * 24L * 60L * 60L * 1000L;
        Map<String, Integer> sums = new HashMap<>();
        for (SessionEntity s : sessions) {
            if (s == null) continue;
            if (s.getDateEpochMillis() >= sevenDaysAgo && s.getDateEpochMillis() <= nowMillis) {
                String key = s.getSubjectId();
                int prev = sums.containsKey(key) ? sums.get(key) : 0;
                sums.put(key, prev + Math.max(0, s.getDurationMinutes()));
            }
        }
        return sums;
    }

    /**
     * Compute basic summary for this week from a provided list:
     * - totalMinutes
     * - averageFocus (1..5)
     * - mostStudiedSubjectId
     */
    public Summary computeWeeklySummary(@NonNull List<SessionEntity> sessions, long nowMillis) {
        long sevenDaysAgo = nowMillis - 7L * 24L * 60L * 60L * 1000L;
        int total = 0;
        int focusSum = 0;
        int focusCount = 0;
        Map<String, Integer> subjectMinutes = new HashMap<>();

        for (SessionEntity s : sessions) {
            if (s == null) continue;
            if (s.getDateEpochMillis() >= sevenDaysAgo && s.getDateEpochMillis() <= nowMillis) {
                total += Math.max(0, s.getDurationMinutes());
                focusSum += s.getFocusLevel();
                focusCount += 1;
                String k = s.getSubjectId();
                int prev = subjectMinutes.containsKey(k) ? subjectMinutes.get(k) : 0;
                int newVal = prev + Math.max(0, s.getDurationMinutes());
                subjectMinutes.put(k, newVal);
            }
        }
        String topSubjectId = null;
        int topVal = -1;
        for (Map.Entry<String, Integer> e : subjectMinutes.entrySet()) {
            if (e.getValue() > topVal) {
                topVal = e.getValue();
                topSubjectId = e.getKey();
            }
        }
        float avgFocus = (focusCount == 0) ? 0f : (focusSum * 1f / focusCount);
        return new Summary(total, avgFocus, topSubjectId);
    }

    // Simple summary DTO
    public static class Summary {
        public final int totalMinutesThisWeek;
        public final float averageFocus; // 0..5
        public final String mostStudiedSubjectId;

        public Summary(int totalMinutesThisWeek, float averageFocus, String mostStudiedSubjectId) {
            this.totalMinutesThisWeek = totalMinutesThisWeek;
            this.averageFocus = averageFocus;
            this.mostStudiedSubjectId = mostStudiedSubjectId;
        }
    }
}