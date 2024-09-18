package com.example.todolist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.kharchaji.R


class TodoAdapter(private val items: MutableList<TodoItem>, private val onItemRemove: (Int) -> Unit) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val buttonRemove: ImageButton = itemView.findViewById(R.id.buttonRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.text
        holder.checkBox.isChecked = item.isDone

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isDone = isChecked
        }

        holder.buttonRemove.setOnClickListener {
            onItemRemove(position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

