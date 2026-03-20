package com.example.prescript

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HistoryAdapter(private var historyList: List<HistoryItem>, private var currentTheme: String) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val historyItemCard: MaterialCardView = view.findViewById(R.id.history_item_card)
        val missionText: TextView = view.findViewById(R.id.mission_text)
        val dateText: TextView = view.findViewById(R.id.date_text)
        val statusIcon: ImageView = view.findViewById(R.id.status_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.missionText.text = item.missionText
        holder.dateText.text = item.date

        val context = holder.itemView.context

        if (currentTheme == "Light") {
            holder.historyItemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.history_card_background_light))
            holder.missionText.setTextColor(ContextCompat.getColor(context, R.color.history_item_text_light))
            holder.dateText.setTextColor(ContextCompat.getColor(context, R.color.hint_color))
        } else { // Dark Theme
            holder.historyItemCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.history_card_background_dark))
            holder.missionText.setTextColor(ContextCompat.getColor(context, R.color.history_item_text_dark))
            holder.dateText.setTextColor(ContextCompat.getColor(context, R.color.hint_color))
        }

        if (item.isCompleted) {
            holder.statusIcon.setImageResource(R.drawable.ic_check_circle)
            holder.statusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.good_count))
        } else {
            holder.statusIcon.setImageResource(R.drawable.ic_cancel_circle)
            holder.statusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bad_count))
        }
    }

    override fun getItemCount() = historyList.size

    fun updateTheme(newTheme: String) {
        currentTheme = newTheme
        notifyDataSetChanged()
    }

    fun updateData(newData: List<HistoryItem>) {
        historyList = newData
        notifyDataSetChanged()
    }
}