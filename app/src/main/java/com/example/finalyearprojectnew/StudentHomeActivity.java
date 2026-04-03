package com.example.finalyearprojectnew;

import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;

public class StudentHomeActivity extends AppCompatActivity {

    private LineChart lineChartGpa;
    private BarChart barChartPerformance;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        lineChartGpa = findViewById(R.id.lineChartGpa);
        barChartPerformance = findViewById(R.id.barChartPerformance);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Set Home as active item
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        setupBottomNavigation();
        setupGpaChart();
        setupPerformanceChart();
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(StudentHomeActivity.this, StudentCalendarActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_chats) {
                Toast.makeText(this, "Chats coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Profile coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupGpaChart() {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 3.2f)); // Sem 1
        entries.add(new Entry(1, 3.4f)); // Sem 2
        entries.add(new Entry(2, 3.6f)); // Sem 3
        entries.add(new Entry(3, 3.5f)); // Sem 4
        entries.add(new Entry(4, 3.8f)); // Future Prediction

        LineDataSet dataSet = new LineDataSet(entries, "GPA Trend");
        dataSet.setColor(Color.parseColor("#057BFE"));
        dataSet.setCircleColor(Color.parseColor("#0161D7"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E6EEFF"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#0D2F5A"));

        LineData lineData = new LineData(dataSet);
        lineChartGpa.setData(lineData);

        // Customize X-Axis
        String[] semesters = new String[] { "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Proj (Fut)" };
        XAxis xAxis = lineChartGpa.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(semesters));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // Styling
        lineChartGpa.getDescription().setEnabled(false);
        lineChartGpa.getAxisRight().setEnabled(false);
        lineChartGpa.animateX(1000);
        lineChartGpa.invalidate();
    }

    private void setupPerformanceChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 85f)); // Module 1
        entries.add(new BarEntry(1, 78f)); // Module 2
        entries.add(new BarEntry(2, 45f)); // Module 3 (Weak)
        entries.add(new BarEntry(3, 92f)); // Module 4
        entries.add(new BarEntry(4, 88f)); // Module 5

        BarDataSet dataSet = new BarDataSet(entries, "Module Results");

        // Highlight weak module in red (index 2)
        int[] colors = new int[] {
                Color.parseColor("#057BFE"), // Normal
                Color.parseColor("#057BFE"), // Normal
                Color.parseColor("#E74C3C"), // Weak (Red)
                Color.parseColor("#057BFE"), // Normal
                Color.parseColor("#057BFE") // Normal
        };
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#0D2F5A"));

        BarData barData = new BarData(dataSet);
        barChartPerformance.setData(barData);

        // Customize X-Axis
        String[] modules = new String[] { "Mod A", "Mod B", "Mod C", "Mod D", "Mod E" };
        XAxis xAxis = barChartPerformance.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(modules));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        // Styling
        barChartPerformance.getDescription().setEnabled(false);
        barChartPerformance.getAxisRight().setEnabled(false);
        barChartPerformance.animateY(1000);
        barChartPerformance.invalidate();
    }
}
