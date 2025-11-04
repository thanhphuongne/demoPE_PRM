package com.thunderboarsolution.MVVMretrofiltrequest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.viewmodel.AddSessionViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddSessionActivity extends AppCompatActivity {

    private Spinner spSubject;
    private Button btnPickDate;
    private TextView tvPickedDate;
    private EditText etDuration;
    private RatingBar rbFocus;
    private EditText etNotes;
    private Button btnSaveSession;

    private AddSessionViewModel viewModel;

    private final List<SubjectOption> subjectOptions = new ArrayList<>();
    private ArrayAdapter<SubjectOption> subjectAdapter;

    private long pickedDateMillis = System.currentTimeMillis();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_session);

        bindViews();
        setupSubjectSpinner();
        updatePickedDateLabel();

        viewModel = new ViewModelProvider(this).get(AddSessionViewModel.class);

        // Observe subjects map to populate spinner
        viewModel.getSubjectsMap().observe(this, map -> rebuildSubjectSpinner(map));

        // Observe save result
        viewModel.getSaveResult().observe(this, result -> {
            if (result == null) return;
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
            if (result.ok) {
                finish();
            }
        });

        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnSaveSession.setOnClickListener(v -> onSaveClicked());
    }

    private void bindViews() {
        spSubject = findViewById(R.id.spSubject);
        btnPickDate = findViewById(R.id.btnPickDate);
        tvPickedDate = findViewById(R.id.tvPickedDate);
        etDuration = findViewById(R.id.etDuration);
        rbFocus = findViewById(R.id.rbFocus);
        etNotes = findViewById(R.id.etNotes);
        btnSaveSession = findViewById(R.id.btnSaveSession);
    }

    private void setupSubjectSpinner() {
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectOptions);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(subjectAdapter);
    }

    private void rebuildSubjectSpinner(Map<String, Subject> map) {
        subjectOptions.clear();
        // Require explicit selection; no placeholder "All"
        if (map != null) {
            for (Map.Entry<String, Subject> e : map.entrySet()) {
                Subject s = e.getValue();
                String name = (s != null && s.getName() != null && !s.getName().trim().isEmpty())
                        ? s.getName() : e.getKey();
                subjectOptions.add(new SubjectOption(e.getKey(), name));
            }
        }
        subjectAdapter.notifyDataSetChanged();
        // Default select first if available
        if (!subjectOptions.isEmpty()) {
            spSubject.setSelection(0);
        }
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(pickedDateMillis);
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    picked.set(Calendar.HOUR_OF_DAY, 12);
                    picked.set(Calendar.MINUTE, 0);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    pickedDateMillis = picked.getTimeInMillis();
                    updatePickedDateLabel();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void updatePickedDateLabel() {
        tvPickedDate.setText(dateFormat.format(pickedDateMillis));
    }

    private void onSaveClicked() {
        String subjectId = null;
        int idx = spSubject.getSelectedItemPosition();
        if (idx >= 0 && idx < subjectOptions.size()) {
            subjectId = subjectOptions.get(idx).id;
        }

        int duration = parseIntSafe(etDuration.getText() != null ? etDuration.getText().toString() : null);
        int focus = Math.round(rbFocus.getRating());
        String note = etNotes.getText() != null ? etNotes.getText().toString() : null;

        viewModel.saveSession(subjectId, pickedDateMillis, duration, focus, note);
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static class SubjectOption {
        final String id;
        final String name;
        SubjectOption(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}