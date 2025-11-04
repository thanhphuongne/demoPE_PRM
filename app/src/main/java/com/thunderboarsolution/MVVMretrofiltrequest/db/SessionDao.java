package com.thunderboarsolution.MVVMretrofiltrequest.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SessionEntity session);

    @Query("SELECT * FROM sessions ORDER BY dateEpochMillis DESC")
    LiveData<List<SessionEntity>> getAllSessions();

    /**
     * Multi-criteria filtered query:
     * - subjectId: exact match when non-null
     * - date range: inclusive BETWEEN
     * - focus range: inclusive BETWEEN
     * - noteQuery: substring match when non-null
     */
    @Query("SELECT * FROM sessions WHERE " +
            "(:subjectId IS NULL OR subjectId = :subjectId) AND " +
            "dateEpochMillis BETWEEN :fromMillis AND :toMillis AND " +
            "focusLevel BETWEEN :minFocus AND :maxFocus AND " +
            "(:noteQuery IS NULL OR note LIKE '%' || :noteQuery || '%') " +
            "ORDER BY dateEpochMillis DESC")
    LiveData<List<SessionEntity>> getFilteredSessions(
            String subjectId,
            long fromMillis,
            long toMillis,
            int minFocus,
            int maxFocus,
            String noteQuery
    );
}