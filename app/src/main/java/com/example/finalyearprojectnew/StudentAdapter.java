package com.example.finalyearprojectnew;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onDeleteClick(Student student);
        void onStatusToggle(Student student);
    }

    public StudentAdapter(List<Student> studentList, OnStudentClickListener listener) {
        this.studentList = studentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_list, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.tvStudentName.setText(student.getFullName());
        holder.tvStudentId.setText(student.getStudentId());
        holder.tvStudentEmail.setText(student.getEmail());
        
        holder.tvStudentStatus.setText(student.getStatus());
        if ("active".equalsIgnoreCase(student.getStatus())) {
            holder.tvStudentStatus.setTextColor(Color.parseColor("#006400")); // Dark Green
            holder.tvStudentStatus.setBackgroundColor(Color.parseColor("#E8F8F5"));
        } else {
            holder.tvStudentStatus.setTextColor(Color.parseColor("#8B0000")); // Dark Red
            holder.tvStudentStatus.setBackgroundColor(Color.parseColor("#FDEDEC"));
        }

        holder.ivDeleteStudent.setOnClickListener(v -> listener.onDeleteClick(student));
        holder.tvStudentStatus.setOnClickListener(v -> listener.onStatusToggle(student));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentId, tvStudentEmail, tvStudentStatus;
        ImageView ivDeleteStudent;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvStudentId = itemView.findViewById(R.id.tvStudentId);
            tvStudentEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvStudentStatus = itemView.findViewById(R.id.tvStudentStatus);
            ivDeleteStudent = itemView.findViewById(R.id.ivDeleteStudent);
        }
    }
}
