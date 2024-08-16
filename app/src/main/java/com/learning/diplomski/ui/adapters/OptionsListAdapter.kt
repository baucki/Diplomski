package com.learning.diplomski.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.learning.diplomski.R

class OptionsListAdapter(
    private val optionsList: ArrayList<String>,
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<OptionsListAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(option: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_option_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = optionsList[position].trim()
        holder.bind(option)
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(option)
        }
    }

    override fun getItemCount(): Int = optionsList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.text_option)

        fun bind(word: String) {
            textView.text = word
        }
    }

}