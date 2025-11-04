package com.thunderboarsolution.MVVMretrofiltrequest.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.thunderboarsolution.MVVMretrofiltrequest.db.SessionEntity;
import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.repository.StudyRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HomeViewModel orchestrates:
 * - Subjects metadata (name, icon)
 * - Sessions from Room
 * - Multi-criteria filters
 * - Weekly summary and last-7-days aggregation for charts
 */
public class HomeViewModel extends AndroidViewModel {

    private final StudyRepository repository;

    // Upstream sources
    private final LiveData<Map<String, Subject>> subjectsMap;
    private final LiveData<List<SessionEntity>> allSessions;

    // Filters
    private final MutableLiveData<String> filterSubjectId = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> filterMinFocus = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> filterMaxFocus = new MutableLiveData<>(5);
    private final MutableLiveData<Long> filterFromMillis = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> filterToMillis = new MutableLiveData<>(Long.MAX_VALUE);
    private final MutableLiveData<String> filterNoteQuery = new MutableLiveData<>(null);

    // Filtered sessions from DAO (swapped source)
    private final MediatorLiveData<List<SessionEntity>> filteredSessions = new MediatorLiveData<>();
    private LiveData<List<SessionEntity>> currentFilteredSource;

    // UI-ready sessions joined with subject metadata
    private final MediatorLiveData<List<SessionUI>> sessionsUi = new MediatorLiveData<>();

    // Weekly summary (total minutes, avg focus, most studied subjectId)
    private final MediatorLiveData<StudyRepository.Summary> weeklySummary = new MediatorLiveData<>();

    // Aggregation for last 7 days: subjectId -> total minutes
    private final MediatorLiveData<Map<String, Integer>> last7DaysBySubject = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = StudyRepository.getInstance(application);
        subjectsMap = repository.getSubjectsMap();
        allSessions = repository.getAllSessions();

        // Initial subjects load
        repository.refreshSubjects();

        // Recompute Room filtered source whenever filters change
        filteredSessions.addSource(filterSubjectId, v -> requeryFiltered());
        filteredSessions.addSource(filterMinFocus, v -> requeryFiltered());
        filteredSessions.addSource(filterMaxFocus, v -> requeryFiltered());
        filteredSessions.addSource(filterFromMillis, v -> requeryFiltered());
        filteredSessions.addSource(filterToMillis, v -> requeryFiltered());
        filteredSessions.addSource(filterNoteQuery, v -> requeryFiltered());
        // Seed initial query
        requeryFiltered();

        // Build UI list when filtered sessions or subjects change
        sessionsUi.addSource(filteredSessions, v -> rebuildUi());
        sessionsUi.addSource(subjectsMap, v -> rebuildUi());

        // Compute weekly summary and last-7-days aggregation based on all sessions
        weeklySummary.addSource(allSessions, sessions -> {
            List<SessionEntity> safe = sessions != null ? sessions : new ArrayList<>();
            weeklySummary.setValue(repository.computeWeeklySummary(safe, System.currentTimeMillis()));
        });
        last7DaysBySubject.addSource(allSessions, sessions -> {
            List<SessionEntity> safe = sessions != null ? sessions : new ArrayList<>();
            last7DaysBySubject.setValue(repository.aggregateLast7DaysBySubject(safe, System.currentTimeMillis()));
        });
    }

    private void requeryFiltered() {
        String subjectId = filterSubjectId.getValue();
        long from = safeLong(filterFromMillis.getValue(), 0L);
        long to = safeLong(filterToMillis.getValue(), Long.MAX_VALUE);
        int minFocus = safeInt(filterMinFocus.getValue(), 1);
        int maxFocus = safeInt(filterMaxFocus.getValue(), 5);
        String noteQuery = filterNoteQuery.getValue();

        LiveData<List<SessionEntity>> newSource =
                repository.getFilteredSessions(subjectId, from, to, minFocus, maxFocus, noteQuery);

        if (currentFilteredSource != null) {
            filteredSessions.removeSource(currentFilteredSource);
        }
        currentFilteredSource = newSource;
        filteredSessions.addSource(currentFilteredSource, filteredSessions::setValue);
    }

    private void rebuildUi() {
        List<SessionEntity> sessions = filteredSessions.getValue();
        Map<String, Subject> subjects = subjectsMap.getValue();
        if (sessions == null) {
            sessionsUi.setValue(new ArrayList<>());
            return;
        }
        Map<String, Subject> safeMap = subjects != null ? subjects : new HashMap<>();
        List<SessionUI> uiList = new ArrayList<>(sessions.size());
        for (SessionEntity s : sessions) {
            if (s == null) continue;
            Subject subj = safeMap.get(s.getSubjectId());
            String subjectName = subj != null ? safeString(subj.getName(), s.getSubjectId()) : s.getSubjectId();
            String iconUrl = subj != null ? safeString(subj.getIconUrl(), null) : null;
            uiList.add(new SessionUI(
                    s.getId(),
                    s.getSubjectId(),
                    subjectName,
                    iconUrl,
                    s.getDateEpochMillis(),
                    s.getDurationMinutes(),
                    s.getFocusLevel(),
                    safeString(s.getNote(), null)
            ));
        }
        sessionsUi.setValue(uiList);
    }

    private static int safeInt(Integer v, int def) {
        return v != null ? v : def;
    }

    private static long safeLong(Long v, long def) {
        return v != null ? v : def;
    }

    private static String safeString(String v, String def) {
        return v != null ? v : def;
    }

    // Exposed getters
    public LiveData<Map<String, Subject>> getSubjectsMap() {
        return subjectsMap;
    }

    public LiveData<List<SessionUI>> getSessionsUi() {
        return sessionsUi;
    }

    public LiveData<StudyRepository.Summary> getWeeklySummary() {
        return weeklySummary;
    }

    public LiveData<Map<String, Integer>> getLast7DaysBySubject() {
        return last7DaysBySubject;
    }

    // Filter setters
    public void setFilterSubjectId(String subjectId) {
        filterSubjectId.setValue(subjectId);
    }

    public void setFilterFocusRange(int min, int max) {
        filterMinFocus.setValue(min);
        filterMaxFocus.setValue(max);
    }

    public void setFilterDateRange(long fromMillis, long toMillis) {
        filterFromMillis.setValue(fromMillis);
        filterToMillis.setValue(toMillis);
    }

    public void setFilterNoteQuery(String query) {
        filterNoteQuery.setValue(query);
    }

    public void clearFilters() {
        filterSubjectId.setValue(null);
        filterMinFocus.setValue(1);
        filterMaxFocus.setValue(5);
        filterFromMillis.setValue(0L);
        filterToMillis.setValue(Long.MAX_VALUE);
        filterNoteQuery.setValue(null);
    }

    // Commands
    public void refreshSubjects() {
        repository.refreshSubjects();
    }

    public void insertSession(@NonNull SessionEntity entity) {
        repository.insertSession(entity);
    }

    // UI model for RecyclerView rows
    public static class SessionUI {
        public final long id;
        public final String subjectId;
        public final String subjectName;
        public final String iconUrl;
        public final long dateEpochMillis;
        public final int durationMinutes;
        public final int focusLevel;
        public final String note;

        public SessionUI(long id,
                         String subjectId,
                         String subjectName,
                         String iconUrl,
                         long dateEpochMillis,
                         int durationMinutes,
                         int focusLevel,
                         String note) {
            this.id = id;
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.iconUrl = iconUrl;
            this.dateEpochMillis = dateEpochMillis;
            this.durationMinutes = durationMinutes;
            this.focusLevel = focusLevel;
            this.note = note;
        }
    }
}