package com.nguyen.asuper.ui.main.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.PlacesClient
import com.nguyen.asuper.R
import com.nguyen.asuper.data.SearchHistory
import com.nguyen.asuper.databinding.ItemAutoCompleteBinding
import com.nguyen.asuper.databinding.ItemCachedAutoCompleteBinding
import com.nguyen.asuper.ui.main.MapFragment.Companion.CURRENT_EDIT_TEXT
import com.nguyen.asuper.util.SavedSharedPreferences.searchHistories
import com.nguyen.asuper.viewmodels.MainViewModel

class AutoCompleteAdapter(private val autoCompleteList: ArrayList<AutocompletePrediction>,
                            private val placesClient: PlacesClient,
                            private val mainViewModel: MainViewModel,
                            private val updateOriginEditText: (String) -> Unit,
                            private val updateDestinationEditText: (String) -> Unit):
    RecyclerView.Adapter<AutoCompleteAdapter.BaseViewHolder>() {

    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        abstract fun bind(autoComplete: AutocompletePrediction?, position: Int)
    }

    inner class AutoCompleteViewHolder(private val binding: ItemAutoCompleteBinding) : BaseViewHolder(binding.root){
        override fun bind(autoComplete: AutocompletePrediction?, position: Int) {
            if(autoComplete == null) Log.d("Map", "Null position: $position")
            binding.autoComplete = autoComplete

            binding.itemContainer.setOnClickListener {
                val searchHistory = createSearchHistory(autoComplete?.placeId!!, autoComplete.getPrimaryText(null)
                    .toString(), autoComplete.getFullText(null).toString())
                val list = ArrayList<SearchHistory>()
                list.addAll(searchHistories)
                if(list.size == 3){
                    list.removeAt(2)
                }
                list.add(0, searchHistory)
                searchHistories = list


                when(CURRENT_EDIT_TEXT){
                    "Origin" -> {
                        updateOriginEditText.invoke(autoComplete.getPrimaryText(null).toString())
                        mainViewModel.getOriginLatLng(autoComplete.placeId, placesClient)
                    }
                    "Destination" -> {
                        updateDestinationEditText.invoke(autoComplete.getPrimaryText(null).toString())
                        mainViewModel.getDestinationLatLng(autoComplete.placeId, placesClient)
                    }
                }
            }

//            val listener = View.OnClickListener {
//
//            }
//
//            binding.locationType.setOnClickListener(listener)
//            binding.locationName.setOnClickListener(listener)
//            binding.locationAddress.setOnClickListener(listener)
        }

        private fun createSearchHistory(id: String, name: String, address: String): SearchHistory{
            val searchHistory = SearchHistory()
            searchHistory.id = id
            searchHistory.name = name
            searchHistory.address = address
            return searchHistory
        }

    }

    inner class CachedAutoComplete(private val binding: ItemCachedAutoCompleteBinding) : BaseViewHolder(binding.root){
        override fun bind(autoComplete: AutocompletePrediction?, position: Int) {
            if(position < searchHistories.size) binding.searchHistory = searchHistories[position]

            binding.itemContainer.setOnClickListener {
                Log.d("Map", "Search history: ${searchHistories[position].id}")
                when(CURRENT_EDIT_TEXT){
                    "Origin" -> {
                        updateOriginEditText.invoke(searchHistories[position].name)
                        mainViewModel.getOriginLatLng(searchHistories[position].id, placesClient)
                    }
                    "Destination" -> {
                        updateDestinationEditText.invoke(searchHistories[position].name)
                        mainViewModel.getDestinationLatLng(searchHistories[position].id, placesClient)
                    }
                }
            }

        }

    }

    override fun getItemViewType(position: Int): Int {
        return if(position < searchHistories.size){
            R.layout.item_cached_auto_complete
        } else {
            R.layout.item_auto_complete
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when(viewType){
            R.layout.item_auto_complete
            -> AutoCompleteViewHolder(ItemAutoCompleteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_cached_auto_complete
            -> CachedAutoComplete(ItemCachedAutoCompleteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> AutoCompleteViewHolder(ItemAutoCompleteBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun getItemCount(): Int {
        return autoCompleteList.size + searchHistories.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if(position >= searchHistories.size) holder.bind(autoCompleteList[position - searchHistories.size], position)
        else holder.bind(null, position)
    }


    fun switchList(newList: List<AutocompletePrediction>){
        autoCompleteList.clear()
        autoCompleteList.addAll(newList)
        notifyDataSetChanged()
    }
}