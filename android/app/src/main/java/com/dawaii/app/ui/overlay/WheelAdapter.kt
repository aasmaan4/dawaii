package com.dawaii.app.ui.overlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dawaii.app.R
import com.dawaii.app.data.preferences.AppTheme

class WheelAdapter(
    private val numbers: List<Int>,
    private val theme: AppTheme,
    private val onItemClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<WheelAdapter.WheelViewHolder>() {

    class WheelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvWheelNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WheelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wheel_number, parent, false)
        return WheelViewHolder(view)
    }

    override fun onBindViewHolder(holder: WheelViewHolder, position: Int) {
        val number = numbers[position]
        holder.tvNumber.text = number.toString()

        val colorRes = if (theme == AppTheme.SAKURA) {
            R.color.sakura_pink_primary
        } else {
            R.color.normal_text_primary
        }
        holder.tvNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(number)
        }
    }

    override fun getItemCount(): Int = numbers.size

    fun getItemAt(position: Int): Int? = numbers.getOrNull(position)
}
