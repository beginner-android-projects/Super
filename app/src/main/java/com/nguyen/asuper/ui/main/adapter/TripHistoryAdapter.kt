package com.nguyen.asuper.ui.main.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nguyen.asuper.data.Trip
import com.nguyen.asuper.databinding.ItemTripHistoryBinding

class TripHistoryAdapter(private val tripsList: List<Trip>) : RecyclerView.Adapter<TripHistoryAdapter.TripHistoryViewHolder>() {

    class TripHistoryViewHolder(private val binding: ItemTripHistoryBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(trip: Trip){
            //binding.previewItem.setImageBitmap(trip.preview)
            binding.trip = trip
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripHistoryViewHolder {
        return TripHistoryViewHolder(ItemTripHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount() = tripsList.size

    override fun onBindViewHolder(holder: TripHistoryViewHolder, position: Int) {
        holder.bind(tripsList[position])
    }
}