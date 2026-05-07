package com.example.finalyearprojectnew;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SemesterChipAdapter extends RecyclerView.Adapter<SemesterChipAdapter.ViewHolder> {

    private final List<String> labels;
    private int selectedPosition = -1;
    private final OnSemesterSelectedListener listener;

    public interface OnSemesterSelectedListener {
        void onSelected(int position);
    }

    public SemesterChipAdapter(List<String> labels, OnSemesterSelectedListener listener) {
        this.labels = labels;
        this.listener = listener;
        if (!labels.isEmpty()) {
            this.selectedPosition = labels.size() - 1; // Default to latest
        }
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPos);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_semester_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvChip.setText(labels.get(position));
        boolean isSelected = (position == selectedPosition);
        holder.tvChip.setSelected(isSelected);
        
        if (isSelected) {
            holder.tvChip.setTextColor(Color.WHITE);
        } else {
            holder.tvChip.setTextColor(Color.parseColor("#057BFE"));
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onSelected(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChip;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tvChip);
        }
    }
}
