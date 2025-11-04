package com.thunderboarsolution.MVVMretrofiltrequest.viewmodel;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.thunderboarsolution.MVVMretrofiltrequest.db.SessionEntity;
import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.repository.StudyRepository;

import java.util.Map;

/**
 * ViewModel for Add New Study Session screen.
 * Exposes subjects map and provides a validated saveSession() API.
 */
public class AddSessionViewModel extends AndroidViewModel {

    private final StudyRepository repository;
    private final LiveData<Map<String, Subject>> subjectsMap;

    private final MutableLiveData<Result> saveResult = new MutableLiveData<>();

    public AddSessionViewModel(@NonNull Application application) {
        super(application);
        repository = StudyRepository.getInstance(application);
        subjectsMap = repository.getSubjectsMap();
        // Ensure subjects are loaded at least once
        repository.refreshSubjects();
    }

    public LiveData<Map<String, Subject>> getSubjectsMap() {
        return subjectsMap;
    }

    public LiveData<Result> getSaveResult() {
        return saveResult;
    }

    /**
     * Validate and save a new session to Room.
     * Requirements: subjectId not empty, durationMinutes > 0, focusLevel in [1,5]
     */
    public void saveSession(String subjectId,
                            long dateEpochMillis,
                            int durationMinutes,
                            int focusLevel,
                            String note) {
        if (TextUtils.isEmpty(subjectId)) {
            saveResult.postValue(Result.error("Subject is required"));
            return;
        }
        if (durationMinutes <= 0) {
            saveResult.postValue(Result.error("Duration must be greater than 0"));
            return;
        }
        if (focusLevel < 1 || focusLevel > 5) {
            saveResult.postValue(Result.error("Focus must be in range 1..5"));
            return;
        }

        SessionEntity entity = new SessionEntity(subjectId, dateEpochMillis, durationMinutes, focusLevel, note);
        repository.insertSession(entity);
        saveResult.postValue(Result.success());
    }

    // Result DTO for UI
    public static class Result {
        public final boolean ok;
        public final String message;

        private Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public static Result success() {
            return new Result(true, "Saved");
        }

        public static Result error(String message) {
            return new Result(false, message);
        }
    }
}