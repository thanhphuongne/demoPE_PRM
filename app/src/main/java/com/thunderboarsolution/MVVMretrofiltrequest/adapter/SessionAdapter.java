package com.thunderboarsolution.MVVMretrofiltrequest.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.thunderboarsolution.MVVMretrofiltrequest.R;
import com.thunderboarsolution.MVVMretrofiltrequest.viewmodel.HomeViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying study sessions with subject icon, name, date, duration, and focus stars.
 *
 * Binds HomeViewModel.SessionUI to session_item.xml views.
 */
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private final Context context;
    private final List<HomeViewModel.SessionUI> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final SimpleDateFormat dayTimeFormat = new SimpleDateFormat("EEE, h:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

    public SessionAdapter(@NonNull Context context) {
        this.context = context;
    }

    public void setItems(@NonNull List<HomeViewModel.SessionUI> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.session_item, parent, false);
        return new SessionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        HomeViewModel.SessionUI item = items.get(position);

        holder.tvSubject.setText(item.subjectName != null ? item.subjectName : item.subjectId);
        holder.tvDuration.setText(formatDuration(item.durationMinutes));
        holder.tvDate.setText(formatDate(item.dateEpochMillis));

        holder.ratingFocus.setNumStars(5);
        holder.ratingFocus.setStepSize(1f);
        holder.ratingFocus.setRating(Math.max(0, Math.min(5, item.focusLevel)));

        if (item.iconUrl != null && !item.iconUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(item.iconUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_launcher_foreground);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDuration(int minutes) {
        int safe = Math.max(0, minutes);
        return safe + " minutes";
    }

    private String formatDate(long epochMillis) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMillis);

        if (isSameDay(c, now)) {
            return "Today, " + timeFormat.format(new Date(epochMillis));
        }

        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(c, yesterday)) {
            return "Yesterday";
        }

        // Same week: show day-of-week and time
        if (now.get(Calendar.WEEK_OF_YEAR) == c.get(Calendar.WEEK_OF_YEAR)
                && now.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
            return dayTimeFormat.format(new Date(epochMillis));
        }

        // Fallback: date only
        return dateFormat.format(new Date(epochMillis));
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.ERA) == b.get(Calendar.ERA)
                && a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvSubject;
        TextView tvDate;
        TextView tvDuration;
        RatingBar ratingFocus;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ratingFocus = itemView.findViewById(R.id.ratingFocus);
        }
    }
}