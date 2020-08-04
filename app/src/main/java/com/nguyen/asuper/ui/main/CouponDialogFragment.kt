package com.nguyen.asuper.ui.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nguyen.asuper.R
import com.nguyen.asuper.data.Coupon
import com.nguyen.asuper.ui.main.adapter.CouponAdapter
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_coupon.view.*

class CouponDialogFragment(private val mainViewModel: MainViewModel): DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIcon: ProgressBar
    private lateinit var pickCouponListener: View.OnClickListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_coupon, container, false)


        mainViewModel.couponList.observe(viewLifecycleOwner, Observer {
            Log.d("Coupon", "Coupons: ${it.size}")
            loadingIcon.visibility = View.GONE
            recyclerView.adapter = CouponAdapter(it, mainViewModel, fun(){
                (it as ArrayList<Coupon>).clear()
                dismiss()
            })
        })


        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.fragment_coupon, null)

            recyclerView = view.coupon_recyclerview
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            loadingIcon = view.loading_icon

            view.close_button.setOnClickListener {
                dismiss()
            }

            mainViewModel.getCoupons()

            builder.setView(view)
                .setCancelable(false)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}