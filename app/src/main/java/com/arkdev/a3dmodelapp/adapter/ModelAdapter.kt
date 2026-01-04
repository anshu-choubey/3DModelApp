package com.arkdev.a3dmodelapp.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arkdev.a3dmodelapp.data.model.ModelData
import com.arkdev.a3dmodelapp.databinding.ItemModelBinding
import java.text.SimpleDateFormat
import java.util.*

class ModelAdapter(
    private val isAdmin: Boolean,
    private val onViewClick: (ModelData) -> Unit,
    private val onDeleteClick: ((ModelData) -> Unit)?
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    private var models = listOf<ModelData>()

    fun submitList(newList: List<ModelData>) {
        models = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount() = models.size

    inner class ViewHolder(private val binding: ItemModelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ModelData) {
            binding.tvModelName.text = model.name
            binding.tvModelDescription.text = model.description
            binding.tvModelSize.text = "${model.fileSize / 1024} KB"

            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            binding.tvUploadDate.text = sdf.format(Date(model.uploadedAt))

            binding.btnView.setOnClickListener { onViewClick(model) }

            if (isAdmin) {
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnDelete.setOnClickListener { onDeleteClick?.invoke(model) }
            } else {
                binding.btnDelete.visibility = View.GONE
            }
        }
    }
}
