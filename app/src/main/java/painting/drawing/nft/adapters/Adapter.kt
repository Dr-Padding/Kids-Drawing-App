package painting.drawing.nft.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import painting.drawing.nft.Tools
import com.drawing.paint.databinding.ItemToolBinding


class Adapter(val tools: List<Tools>, val listener: MyOnClickListener) :
    RecyclerView.Adapter<Adapter.ToolsViewHolder>() {

    inner class ToolsViewHolder(val binding: ItemToolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position: Int = bindingAdapterPosition
                listener.onClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolsViewHolder {
        val binding = ItemToolBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolsViewHolder, position: Int) {
        holder.apply {
            binding.ibTool.setImageResource(tools[position].image)
        }
    }

    override fun getItemCount(): Int {
        return tools.size
    }

    interface MyOnClickListener {
        fun onClick(position: Int)
    }
}