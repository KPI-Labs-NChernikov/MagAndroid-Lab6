package dev.nikita_chernikov.lab6

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(
    context: Context,
    private val reminders: List<Reminder>,
    private val onDeleteClickListener: (Reminder) -> Unit
) : ArrayAdapter<Reminder>(context, 0, reminders) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_reminder, parent, false)

        val reminder = getItem(position)

        val themeTextView = itemView.findViewById<TextView>(R.id.textViewItemTheme)
        val messageTextView = itemView.findViewById<TextView>(R.id.textViewItemMessage)
        val dateTextView = itemView.findViewById<TextView>(R.id.textViewItemDate)
        val deleteButton = itemView.findViewById<Button>(R.id.buttonDelete)

        themeTextView.text = reminder?.theme
        messageTextView.text = reminder?.message
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        dateTextView.text = sdf.format(Date(reminder?.date ?: 0))

        deleteButton.setOnClickListener {
            if (reminder != null) {
                onDeleteClickListener(reminder)
            }
        }

        return itemView
    }
}
