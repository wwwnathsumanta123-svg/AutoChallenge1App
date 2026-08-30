package com.example.autochallenge;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AutoChallengePrefs";
    private TextView tvStatus, tvProgress, tvBalance;
    private ProgressBar progressBar;
    private EditText etAmount, etNote;
    private RadioGroup rgType;
    private RadioButton rbIncome, rbExpense;
    private Button btnAddTransaction, btnCompleteDay;
    private ListView lvTransactions;

    private int completedDays = 0;
    private double totalIncome = 0.0;
    private double totalExpense = 0.0;
    private ArrayList<String> transactionList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();
        scheduleDailyReminder();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        completedDays = prefs.getInt("completedDays", 0);
        totalIncome = Double.longBitsToDouble(prefs.getLong("totalIncome", Double.doubleToLongBits(0.0)));
        totalExpense = Double.longBitsToDouble(prefs.getLong("totalExpense", Double.doubleToLongBits(0.0)));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 32, 32, 32);

        tvStatus = new TextView(this);
        tvStatus.setTextSize(20);
        tvStatus.setPadding(0, 0, 0, 16);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(30);

        tvProgress = new TextView(this);
        tvProgress.setPadding(0, 8, 0, 16);

        tvBalance = new TextView(this);
        tvBalance.setTextSize(16);
        tvBalance.setPadding(0, 8, 0, 24);

        etAmount = new EditText(this);
        etAmount.setHint("Amount (₹)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        etNote = new EditText(this);
        etNote.setHint("Note (e.g. Fuel, Ride)");

        rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);
        rbIncome = new RadioButton(this);
        rbIncome.setText("Income");
        rbIncome.setChecked(true);
        rbExpense = new RadioButton(this);
        rbExpense.setText("Expense");
        rgType.addView(rbIncome);
        rgType.addView(rbExpense);

        btnAddTransaction = new Button(this);
        btnAddTransaction.setText("Add Entry");

        btnCompleteDay = new Button(this);
        btnCompleteDay.setText("Complete Today's Challenge");

        lvTransactions = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, transactionList);
        lvTransactions.setAdapter(adapter);

        rootLayout.addView(tvStatus);
        rootLayout.addView(progressBar);
        rootLayout.addView(tvProgress);
        rootLayout.addView(tvBalance);
        rootLayout.addView(etAmount);
        rootLayout.addView(etNote);
        rootLayout.addView(rgType);
        rootLayout.addView(btnAddTransaction);
        rootLayout.addView(btnCompleteDay);
        rootLayout.addView(lvTransactions);

        setContentView(rootLayout);

        updateUI();

        btnAddTransaction.setOnClickListener(v -> addTransaction());
        btnCompleteDay.setOnClickListener(v -> completeDay());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void addTransaction() {
        String amtStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (amtStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        boolean isIncome = rbIncome.isChecked();

        if (isIncome) {
            totalIncome += amount;
            transactionList.add(0, "+ ₹" + amount + " (" + (note.isEmpty() ? "Income" : note) + ")");
        } else {
            totalExpense += amount;
            transactionList.add(0, "- ₹" + amount + " (" + (note.isEmpty() ? "Expense" : note) + ")");
        }

        saveData();
        updateUI();
        etAmount.setText("");
        etNote.setText("");
    }

    private void completeDay() {
        if (completedDays < 30) {
            completedDays++;
            saveData();
            updateUI();
            Toast.makeText(this, "Day " + completedDays + " Completed!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Challenge Already Finished!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        tvStatus.setText("Day " + (completedDays < 30 ? completedDays + 1 : 30) + " of 30");
        progressBar.setProgress(completedDays);
        tvProgress.setText(completedDays + " / 30 Days Completed");

        double netProfit = totalIncome - totalExpense;
        tvBalance.setText("Income: ₹" + totalIncome + " | Expense: ₹" + totalExpense + "\nNet Balance: ₹" + netProfit);

        adapter.notifyDataSetChanged();
    }

    private void saveData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("completedDays", completedDays);
        editor.putLong("totalIncome", Double.doubleToLongBits(totalIncome));
        editor.putLong("totalExpense", Double.doubleToLongBits(totalExpense));
        editor.apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("DAILY_REMINDER", "Daily Reminder", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Reminds you to update your daily income/expense");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void scheduleDailyReminder() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 20);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (Calendar.getInstance().after(calendar)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }
}
