package com.thunderboarsolution.MVVMretrofiltrequest.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a single study session.
 *
 * Fields:
 * - id: auto-generated primary key
 * - subjectId: ID from Subjects API used to resolve subject name/icon
 * - dateEpochMillis: UTC epoch millis for session date
 * - durationMinutes: study duration in minutes
 * - focusLevel: 1–5 integer rating
 * - note: optional reflection/notes
 */
@Entity(
        tableName = "sessions",
        indices = {
                @Index(value = {"subjectId"}),
                @Index(value = {"dateEpochMillis"})
        }
)
public class SessionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "subjectId")
    public String subjectId;

    @ColumnInfo(name = "dateEpochMillis")
    public long dateEpochMillis;

    @ColumnInfo(name = "durationMinutes")
    public int durationMinutes;

    @ColumnInfo(name = "focusLevel")
    public int focusLevel;

    @ColumnInfo(name = "note")
    public String note;

    public SessionEntity() {
        // Required by Room
    }

    @Ignore
    public SessionEntity(@NonNull String subjectId,
                         long dateEpochMillis,
                         int durationMinutes,
                         int focusLevel,
                         String note) {
        this.subjectId = subjectId;
        this.dateEpochMillis = dateEpochMillis;
        this.durationMinutes = durationMinutes;
        this.focusLevel = focusLevel;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(@NonNull String subjectId) {
        this.subjectId = subjectId;
    }

    public long getDateEpochMillis() {
        return dateEpochMillis;
    }

    public void setDateEpochMillis(long dateEpochMillis) {
        this.dateEpochMillis = dateEpochMillis;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getFocusLevel() {
        return focusLevel;
    }

    public void setFocusLevel(int focusLevel) {
        this.focusLevel = focusLevel;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}