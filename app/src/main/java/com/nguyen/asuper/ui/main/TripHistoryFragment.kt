package com.nguyen.asuper.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyen.asuper.R
import com.nguyen.asuper.ui.main.adapter.TripHistoryAdapter
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_trip_history.*
import kotlinx.android.synthetic.main.fragment_trip_history.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class TripHistoryFragment : Fragment() {

    private val mainViewModel by viewModel<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_trip_history, container, false)

        view.loading_icon_trip_history.visibility = View.VISIBLE

        mainViewModel.getTripHistory()

        val recyclerView = view.trip_history_recyclerview
        recyclerView.layoutManager = LinearLayoutManager(context)


        mainViewModel.tripsList.observe(viewLifecycleOwner, Observer {
            view.loading_icon_trip_history.visibility = View.GONE
            if(it.isEmpty()){
               view.empty_trip.visibility = View.VISIBLE
            } else {
                view.empty_trip.visibility = View.GONE
                recyclerView.adapter = TripHistoryAdapter(it)
            }

        })
        return view
    }


}