package com.example.finalyearprojectnew;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private List<CalendarEvent> eventList = new ArrayList<>();

    public void setEvents(List<CalendarEvent> events) {
        this.eventList = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = eventList.get(position);
        holder.tvTitle.setText(event.getTitle());
        holder.tvDescription.setText(event.getDescription());
        holder.tvTime.setText(event.getTime());

        if (event.getDescription() == null || event.getDescription().trim().isEmpty()) {
            holder.tvDescription.setVisibility(View.GONE);
        } else {
            holder.tvDescription.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvTime;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventItemTitle);
            tvDescription = itemView.findViewById(R.id.tvEventItemDescription);
            tvTime = itemView.findViewById(R.id.tvEventItemTime);
        }
    }
}
