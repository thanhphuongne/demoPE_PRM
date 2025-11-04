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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

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
        return dateFormat.format(new Date(epochMillis));
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