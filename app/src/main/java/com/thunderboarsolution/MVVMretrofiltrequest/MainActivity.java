package com.thunderboarsolution.MVVMretrofiltrequest;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import android.graphics.Color;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.thunderboarsolution.MVVMretrofiltrequest.adapter.SessionAdapter;
import com.thunderboarsolution.MVVMretrofiltrequest.network.Subject;
import com.thunderboarsolution.MVVMretrofiltrequest.repository.StudyRepository;
import com.thunderboarsolution.MVVMretrofiltrequest.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // Summary
    private TextView tvTotalMinutes;
    private TextView tvMostSubject;
    private TextView tvAvgFocus;

    // Filters
    private Spinner spinnerSubject;
    private Spinner spMinFocus;
    private Spinner spMaxFocus;
    private EditText etNoteQuery;
    private Button btnFromDate;
    private Button btnToDate;
    private Button btnClearFilters;

    // Chart
    private PieChart pieChart;

    // List
    private RecyclerView rvSessions;
    private SessionAdapter sessionAdapter;

    // FAB
    private FloatingActionButton fabAddSession;
    private FloatingActionButton fabChatbot;

    // ViewModel
    private HomeViewModel viewModel;

    // Local state for filters
    private long fromMillis = 0L;
    private long toMillis = Long.MAX_VALUE;

    // Subject spinner data
    private final List<SubjectOption> subjectOptions = new ArrayList<>();
    private ArrayAdapter<SubjectOption> subjectAdapter;
    private Map<String, Subject> lastSubjectsMap = new HashMap<>();
    private Map<String, Integer> last7DaysData = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupRecycler();
        setupSpinners();
        setupPieChart();
        setupListeners();

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe Subjects to populate spinner and labels
        viewModel.getSubjectsMap().observe(this, subjects -> {
            lastSubjectsMap = subjects != null ? subjects : new HashMap<>();
            rebuildSubjectSpinner(lastSubjectsMap);
            // Update summary 'most subject' label if needed
            StudyRepository.Summary summary = viewModel.getWeeklySummary().getValue();
            if (summary != null) {
                renderSummary(summary, lastSubjectsMap);
            }
            // Update pie chart labels
            if (last7DaysData != null) {
                renderPieChart(last7DaysData, lastSubjectsMap);
            }
        });

        // Observe weekly summary
        viewModel.getWeeklySummary().observe(this, summary -> {
            if (summary != null) {
                renderSummary(summary, lastSubjectsMap);
            } else {
                tvTotalMinutes.setText("0 min this week");
                tvMostSubject.setText("N/A");
                tvAvgFocus.setText("⭐ 0.0");
            }
        });

        // Observe sessions UI for list
        viewModel.getSessionsUi().observe(this, uiList -> {
            if (uiList != null) {
                sessionAdapter.setItems(uiList);
            } else {
                sessionAdapter.setItems(new ArrayList<>());
            }
        });

        // Observe last 7 days aggregation for chart
        viewModel.getLast7DaysBySubject().observe(this, map -> {
            last7DaysData = map != null ? map : new HashMap<>();
            renderPieChart(last7DaysData, lastSubjectsMap);
        });

        // Seed default filters
        applyFilterToViewModel();
        // Initial subjects refresh is triggered by ViewModel constructor
    }

    private void bindViews() {
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes);
        tvMostSubject = findViewById(R.id.tvMostSubject);
        tvAvgFocus = findViewById(R.id.tvAvgFocus);

        spinnerSubject = findViewById(R.id.spinnerSubject);
        spMinFocus = findViewById(R.id.spMinFocus);
        spMaxFocus = findViewById(R.id.spMaxFocus);
        etNoteQuery = findViewById(R.id.etNoteQuery);
        btnFromDate = findViewById(R.id.btnFromDate);
        btnToDate = findViewById(R.id.btnToDate);
        btnClearFilters = findViewById(R.id.btnClearFilters);

        pieChart = findViewById(R.id.pieChart);

        rvSessions = findViewById(R.id.rvSessions);
        fabAddSession = findViewById(R.id.fabAddSession);
        fabChatbot = findViewById(R.id.fabChatbot);
    }

    private void setupRecycler() {
        rvSessions.setHasFixedSize(true);
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        sessionAdapter = new SessionAdapter(this);
        rvSessions.setAdapter(sessionAdapter);
    }

    private void setupSpinners() {
        // Subject spinner
        subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subjectOptions);
        subjectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subjectAdapter);

        // Focus min/max spinners
        List<Integer> focusValues = new ArrayList<>();
        for (int i = 1; i <= 5; i++) focusValues.add(i);

        ArrayAdapter<Integer> minAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, focusValues);
        minAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMinFocus.setAdapter(minAdapter);
        spMinFocus.setSelection(0); // 1

        ArrayAdapter<Integer> maxAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, focusValues);
        maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMaxFocus.setAdapter(maxAdapter);
        spMaxFocus.setSelection(4); // 5
    }

    private void setupPieChart() {
        // We'll compute percent labels ourselves in ValueFormatter
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        // Labels will be drawn via value formatter; disable entry labels
        pieChart.setDrawEntryLabels(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.getLegend().setEnabled(true);
        // No-data state to make it obvious when there are no sessions in last 7 days
        pieChart.setNoDataText("No study time in the last 7 days");
        pieChart.setNoDataTextColor(Color.GRAY);
    }

    private void setupListeners() {
        spinnerSubject.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> {
            applyFilterToViewModel();
        }));

        spMinFocus.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> {
            ensureFocusRange();
            applyFilterToViewModel();
        }));
        spMaxFocus.setOnItemSelectedListener(new SimpleItemSelectedListener(() -> {
            ensureFocusRange();
            applyFilterToViewModel();
        }));

        etNoteQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (viewModel != null) viewModel.setFilterNoteQuery(s != null ? s.toString() : null);
            }
        });

        btnFromDate.setOnClickListener(v -> showDatePicker(true));
        btnToDate.setOnClickListener(v -> showDatePicker(false));

        btnClearFilters.setOnClickListener(v -> {
            if (viewModel != null) {
                viewModel.clearFilters();
            }
            // Reset local and UI
            fromMillis = 0L;
            toMillis = Long.MAX_VALUE;
            spinnerSubject.setSelection(0);
            spMinFocus.setSelection(0);
            spMaxFocus.setSelection(4);
            etNoteQuery.setText("");
        });

        fabAddSession.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AddSessionActivity.class);
            startActivity(i);
        });

        fabChatbot.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(i);
        });
    }

    private void showDatePicker(boolean isFrom) {
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    picked.set(Calendar.HOUR_OF_DAY, isFrom ? 0 : 23);
                    picked.set(Calendar.MINUTE, isFrom ? 0 : 59);
                    picked.set(Calendar.SECOND, isFrom ? 0 : 59);
                    picked.set(Calendar.MILLISECOND, isFrom ? 0 : 999);

                    if (isFrom) {
                        fromMillis = picked.getTimeInMillis();
                    } else {
                        toMillis = picked.getTimeInMillis();
                    }
                    applyFilterToViewModel();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void ensureFocusRange() {
        int min = (int) spMinFocus.getSelectedItem();
        int max = (int) spMaxFocus.getSelectedItem();
        if (min > max) {
            // Swap to keep min <= max
            int minIndex = spMinFocus.getSelectedItemPosition();
            int maxIndex = spMaxFocus.getSelectedItemPosition();
            spMinFocus.setSelection(maxIndex);
            spMaxFocus.setSelection(minIndex);
        }
    }

    private void applyFilterToViewModel() {
        if (viewModel == null) return;

        // Subject
        String subjectId = null;
        int selectedIdx = spinnerSubject.getSelectedItemPosition();
        if (selectedIdx >= 0 && selectedIdx < subjectOptions.size()) {
            SubjectOption opt = subjectOptions.get(selectedIdx);
            subjectId = opt.id.equals("__ALL__") ? null : opt.id;
        }

        // Focus
        int min = (int) spMinFocus.getSelectedItem();
        int max = (int) spMaxFocus.getSelectedItem();

        viewModel.setFilterSubjectId(subjectId);
        viewModel.setFilterFocusRange(min, max);
        viewModel.setFilterDateRange(fromMillis, toMillis);
        viewModel.setFilterNoteQuery(etNoteQuery.getText() != null ? etNoteQuery.getText().toString() : null);
    }

    private void rebuildSubjectSpinner(Map<String, Subject> map) {
        subjectOptions.clear();
        // First option: All subjects
        subjectOptions.add(new SubjectOption("__ALL__", "All subjects"));
        if (map != null) {
            for (Map.Entry<String, Subject> e : map.entrySet()) {
                Subject s = e.getValue();
                String name = (s != null && s.getName() != null && !s.getName().trim().isEmpty())
                        ? s.getName() : e.getKey();
                subjectOptions.add(new SubjectOption(e.getKey(), name));
            }
        }
        subjectAdapter.notifyDataSetChanged();
    }

    private void renderSummary(StudyRepository.Summary summary, Map<String, Subject> subjects) {
        int mins = Math.max(0, summary.totalMinutesThisWeek);
        int hours = mins / 60;
        int rem = mins % 60;
        String totalText;
        if (hours > 0 && rem > 0) {
            totalText = String.format(Locale.getDefault(), "%dh %dm", hours, rem);
        } else if (hours > 0) {
            totalText = String.format(Locale.getDefault(), "%dh", hours);
        } else {
            totalText = String.format(Locale.getDefault(), "%dm", rem);
        }
        tvTotalMinutes.setText(totalText);
    
        String mostName = "N/A";
        if (summary.mostStudiedSubjectId != null) {
            Subject subj = subjects != null ? subjects.get(summary.mostStudiedSubjectId) : null;
            mostName = (subj != null && subj.getName() != null && !subj.getName().trim().isEmpty())
                    ? subj.getName()
                    : (summary.mostStudiedSubjectId != null ? summary.mostStudiedSubjectId : "N/A");
        }
        tvMostSubject.setText(mostName);
    
        tvAvgFocus.setText(String.format(Locale.getDefault(), "⭐ %.1f", summary.averageFocus));
    }

    private void renderPieChart(Map<String, Integer> minutesBySubjectId, Map<String, Subject> subjects) {
        List<PieEntry> entries = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : minutesBySubjectId.entrySet()) {
            int mins = Math.max(0, e.getValue() != null ? e.getValue() : 0);
            total += mins;
        }
        if (total == 0) {
            pieChart.clear();
            pieChart.setNoDataText("No study time in the last 7 days");
            pieChart.invalidate();
            return;
        }
        for (Map.Entry<String, Integer> e : minutesBySubjectId.entrySet()) {
            int mins = Math.max(0, e.getValue() != null ? e.getValue() : 0);
            if (mins == 0) continue;
            String label = e.getKey();
            Subject subj = subjects != null ? subjects.get(e.getKey()) : null;
            if (subj != null && subj.getName() != null && !subj.getName().trim().isEmpty()) {
                label = subj.getName();
            }
            entries.add(new PieEntry(mins, label));
        }
        PieDataSet dataSet = new PieDataSet(entries, "Last 7 days");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        final int totalMinutes = total;
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry entry) {
                float percent = totalMinutes == 0 ? 0f : (value / totalMinutes * 100f);
                return String.format(Locale.getDefault(), "%s %.0f%%", entry.getLabel(), percent);
            }
        });
        pieChart.setUsePercentValues(false);
        pieChart.setData(data);
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

    // Helper listener to reduce boilerplate for spinner selections
    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable onChange;
        SimpleItemSelectedListener(Runnable onChange) { this.onChange = onChange; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) { if (onChange != null) onChange.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { if (onChange != null) onChange.run(); }
    }

    // Subject spinner option
    private static class SubjectOption {
        final String id;
        final String name;
        SubjectOption(String id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
}