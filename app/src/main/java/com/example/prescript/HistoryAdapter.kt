package com.example.prescript

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusDot: View = view.findViewById(R.id.status_dot)
        val missionText: TextView = view.findViewById(R.id.mission_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.missionText.text = item.missionText
        
        if (item.isCompleted) {
            holder.statusDot.setBackgroundResource(R.drawable.circle_green)
        } else {
            holder.statusDot.setBackgroundResource(R.drawable.circle_red)
        }
    }

    override fun getItemCount() = historyList.size
}