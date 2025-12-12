package dev.nikita_chernikov.lab6

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.nikita_chernikov.lab6.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var editTextTheme: EditText
    private lateinit var editTextMessage: EditText
    private lateinit var buttonPickDateTime: Button
    private lateinit var textViewSelectedDateTime: TextView
    private lateinit var buttonSaveReminder: Button
    private lateinit var listViewReminders: ListView

    private lateinit var dbManager: SQLiteManager
    private var selectedDateTime: Calendar = Calendar.getInstance()
    private lateinit var reminderAdapter: ReminderAdapter
    private var reminders = mutableListOf<Reminder>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val root = binding.root
        setContentView(root)
        val initialLeft = root.paddingLeft
        val initialTop = root.paddingTop
        val initialRight = root.paddingRight
        val initialBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }

        editTextTheme = findViewById(R.id.editTextTheme)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonPickDateTime = findViewById(R.id.buttonPickDateTime)
        textViewSelectedDateTime = findViewById(R.id.textViewSelectedDateTime)
        buttonSaveReminder = findViewById(R.id.buttonSaveReminder)
        listViewReminders = findViewById(R.id.listViewReminders)

        dbManager = SQLiteManager(this)

        buttonPickDateTime.setOnClickListener {
            pickDateTime()
        }

        buttonSaveReminder.setOnClickListener {
            saveReminder()
        }

        listViewReminders.setOnItemClickListener { _, _, position, _ ->
            val reminder = reminders[position]
            showReminderDetails(reminder)
        }

        val reminderId = intent.getLongExtra("REMINDER_ID", -1)
        if (reminderId != -1L) {
            val reminder = dbManager.getReminder(reminderId)
            if (reminder != null) {
                showReminderDetails(reminder)
            }
        }

        askNotificationPermission()
        loadReminders()
    }

    private fun pickDateTime() {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                selectedDateTime = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute)
                }
                updateDateTimeLabel()
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateDateTimeLabel() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        textViewSelectedDateTime.text = sdf.format(selectedDateTime.time)
    }

    private fun saveReminder() {
        val theme = editTextTheme.text.toString()
        val message = editTextMessage.text.toString()
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (theme.isBlank() || message.isBlank() || textViewSelectedDateTime.text.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionDialog()
            return // Stop the save process until permission is granted
        }

        val id = dbManager.addReminder(theme, message, selectedDateTime.timeInMillis)
        scheduleNotification(id, theme, message, selectedDateTime.timeInMillis)
        loadReminders()
        clearInputFields()
    }

    private fun scheduleNotification(id: Long, theme: String, message: String, time: Long) {
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_ID", id)
            putExtra("THEME", theme)
            putExtra("MESSAGE", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("To ensure reminders are delivered on time, this app needs permission to schedule exact alarms. Please grant this permission in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Intent to open the app's specific settings page for this permission
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadReminders() {
        reminders.clear()
        reminders.addAll(dbManager.getAllReminders())
        reminderAdapter = ReminderAdapter(this, reminders) { reminder ->
            confirmDelete(reminder)
        }
        listViewReminders.adapter = reminderAdapter
    }

    private fun clearInputFields() {
        editTextTheme.text.clear()
        editTextMessage.text.clear()
        textViewSelectedDateTime.text = ""
    }

    private fun showReminderDetails(reminder: Reminder) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(reminder.date))
        AlertDialog.Builder(this)
            .setTitle(reminder.theme)
            .setMessage("Message: ${reminder.message}\nDate: $dateString")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDelete(reminder: Reminder) {
        AlertDialog.Builder(this)
            .setTitle("Delete reminder?")
            .setMessage("Are you sure you want to delete the reminder '${reminder.theme}'?")
            .setPositiveButton("Yes") { _, _ ->
                dbManager.deleteReminder(reminder.id)
                cancelNotification(reminder.id)
                loadReminders()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelNotification(id: Long) {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
