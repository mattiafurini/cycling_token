package com.example.test.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.test.data.RideSession
import com.example.test.databinding.ItemRideHistoryBinding
import java.util.Locale

class HistoryAdapter(
    private val rides: List<RideSession>,
    private val onItemClick: (RideSession) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.RideViewHolder>() {

    inner class RideViewHolder(private val binding: ItemRideHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ride: RideSession) {
            binding.textDate.text = ride.getFormattedDate()
            binding.textTokens.text = "+${ride.tokensEarned} CT"
            binding.textDistance.text = String.format(Locale.getDefault(), "%.2f km", ride.distanceKm)
            binding.textAvgSpeed.text = String.format(Locale.getDefault(), "%.1f km/h", ride.averageSpeed)
            
            val minutes = ride.durationSeconds / 60
            val seconds = ride.durationSeconds % 60
            binding.textDuration.text = String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
            
            binding.root.setOnClickListener {
                onItemClick(ride)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val binding = ItemRideHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        holder.bind(rides[position])
    }

    override fun getItemCount() = rides.size
}