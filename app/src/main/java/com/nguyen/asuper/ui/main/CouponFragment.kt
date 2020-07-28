package com.nguyen.asuper.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nguyen.asuper.R
import com.nguyen.asuper.ui.main.adapter.CouponAdapter
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_coupon.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class CouponFragment : Fragment() {

    private val mainViewModel by viewModel<MainViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_coupon, container, false)
        mainViewModel.getCoupons()

        val recyclerView = view.coupon_recyclerview
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

//        mainViewModel.couponList.observe(viewLifecycleOwner, Observer {
//            view.loading_icon.visibility = View.GONE
//            recyclerView.adapter = CouponAdapter(it, mainViewModel)
//        })

        view.close_button.setOnClickListener {
            findNavController().popBackStack()
        }
        return view
    }

}