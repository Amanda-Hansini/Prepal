package com.example.finalyearprojectnew;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class PerformanceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<PerformanceReportActivity.ReportItem> items;
    private OnSemesterActionListener actionListener;

    public interface OnSemesterActionListener {
        void onEdit(String semesterName);
        void onDelete(String semesterName);
    }

    public PerformanceAdapter(List<PerformanceReportActivity.ReportItem> items, OnSemesterActionListener actionListener) {
        this.items = items;
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == PerformanceReportActivity.ReportItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_performance_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_performance_module, parent, false);
            return new ModuleViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PerformanceReportActivity.ReportItem item = items.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder hHolder = (HeaderViewHolder) holder;
            hHolder.tvHeader.setText(item.headerText);
            
            // Link Edit/Delete to the actual semester ID/Name
            // Note: item.semesterDocId would be useful here
            String semesterName = item.semesterDocId; 
            
            hHolder.ivEdit.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onEdit(semesterName);
            });
            
            hHolder.ivDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDelete(semesterName);
            });

        } else if (holder instanceof ModuleViewHolder) {
            ModuleViewHolder mHolder = (ModuleViewHolder) holder;
            Map<String, Object> mod = item.moduleData;
            
            String id = (String) mod.get("module_id");
            String name = (String) mod.get("module_name");
            String grade = (String) mod.get("grade");
            
            double credits = 0;
            Object credObj = mod.get("credits");
            if (credObj instanceof Double) credits = (Double) credObj;
            else if (credObj instanceof Long) credits = ((Long) credObj).doubleValue();

            double gp = 0;
            Object ptObj = mod.get("grade_point");
            if (ptObj instanceof Double) gp = (Double) ptObj;
            else if (ptObj instanceof Long) gp = ((Long) ptObj).doubleValue();

            mHolder.tvModuleId.setText(id != null ? id : "N/A");
            mHolder.tvModuleName.setText(name != null ? name : "Unknown Module");
            mHolder.tvGrade.setText(grade != null ? grade : "-");
            mHolder.tvCredits.setText(String.format("%.1f Credits", credits));

            int statusColor;
            if (grade != null) {
                if (grade.equals("F") || grade.equals("AB") || grade.equals("MC") || grade.equals("NE") || gp < 2.0) {
                    statusColor = Color.parseColor("#D50000"); // Vibrant Red
                } else if (grade.startsWith("A")) {
                    statusColor = Color.parseColor("#00C853"); // Vibrant Green
                } else {
                    statusColor = Color.parseColor("#2962FF"); // Vibrant Blue
                }
            } else {
                statusColor = Color.parseColor("#2962FF");
            }

            mHolder.viewGradeIndicator.setBackgroundColor(statusColor);
            mHolder.tvGrade.setTextColor(statusColor);

            mHolder.tvModuleName.setTextColor(Color.BLACK);
            mHolder.tvModuleId.setTextColor(Color.parseColor("#057BFE"));
            mHolder.tvCredits.setTextColor(Color.parseColor("#757575"));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        ImageView ivEdit, ivDelete;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
            ivEdit = itemView.findViewById(R.id.ivEditSemester);
            ivDelete = itemView.findViewById(R.id.ivDeleteSemester);
        }
    }

    public static class ModuleViewHolder extends RecyclerView.ViewHolder {
        TextView tvModuleId, tvModuleName, tvGrade, tvCredits;
        View viewGradeIndicator;
        CardView cardView;

        public ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            viewGradeIndicator = itemView.findViewById(R.id.viewGradeIndicator);
            tvModuleId = itemView.findViewById(R.id.tvModuleId);
            tvModuleName = itemView.findViewById(R.id.tvModuleName);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvCredits = itemView.findViewById(R.id.tvCredits);
        }
    }
}
