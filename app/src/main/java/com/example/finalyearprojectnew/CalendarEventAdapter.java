package com.example.finalyearprojectnew;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import android.widget.ImageView;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.EventViewHolder> {

    private List<CalendarEvent> eventList = new ArrayList<>();
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEditClick(CalendarEvent event);
        void onDeleteClick(CalendarEvent event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

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

        holder.ivEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(event);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvTime;
        ImageView ivEdit, ivDelete;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventItemTitle);
            tvDescription = itemView.findViewById(R.id.tvEventItemDescription);
            tvTime = itemView.findViewById(R.id.tvEventItemTime);
            ivEdit = itemView.findViewById(R.id.ivEditEvent);
            ivDelete = itemView.findViewById(R.id.ivDeleteEvent);
        }
    }
}
