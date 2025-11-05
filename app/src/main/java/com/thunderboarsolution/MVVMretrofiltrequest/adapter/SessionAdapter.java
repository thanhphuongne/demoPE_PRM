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

// Fallback avatar rendering + Glide listener
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Color;
import android.util.TypedValue;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

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
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            // Fallback to generated letter avatar if remote icon fails
                            holder.ivIcon.setImageBitmap(createLetterAvatar(item.subjectName != null ? item.subjectName : item.subjectId, dpToPx(40)));
                            return true; // we handled setting the drawable
                        }
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.ivIcon);
        } else {
            // No URL provided -> generate a letter avatar bubble (colored circle + initial)
            holder.ivIcon.setImageBitmap(createLetterAvatar(item.subjectName != null ? item.subjectName : item.subjectId, dpToPx(40)));
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
 
    // Helpers for fallback avatar
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
 
    private Bitmap createLetterAvatar(String name, int sizePx) {
        String letter = "?" ;
        if (name != null && !name.trim().isEmpty()) {
            letter = name.trim().substring(0, 1).toUpperCase(Locale.getDefault());
        }
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
 
        // Background colored circle
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(pickColor(name));
        float radius = sizePx / 2f;
        canvas.drawCircle(radius, radius, radius, bg);
 
        // White letter
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(Color.WHITE);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setTextSize(sizePx * 0.5f);
        Rect bounds = new Rect();
        text.getTextBounds(letter, 0, 1, bounds);
        float x = radius - bounds.width() / 2f - bounds.left;
        float y = radius + bounds.height() / 2f - bounds.bottom;
        canvas.drawText(letter, x, y, text);
        return bmp;
    }
 
    private int pickColor(String key) {
        int[] colors = new int[]{
                0xFF90CAF9, // blue
                0xFFA5D6A7, // green
                0xFFFFF59D, // yellow
                0xFFCE93D8, // purple
                0xFFFFCC80, // orange
                0xFFEF9A9A  // red
        };
        int idx = 0;
        if (key != null) {
            idx = Math.abs(key.toLowerCase(Locale.getDefault()).hashCode()) % colors.length;
        }
        return colors[idx];
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