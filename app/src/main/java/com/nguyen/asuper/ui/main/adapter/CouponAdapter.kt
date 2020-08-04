package com.nguyen.asuper.ui.main.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nguyen.asuper.data.Coupon
import com.nguyen.asuper.databinding.ItemCouponBinding
import com.nguyen.asuper.viewmodels.MainViewModel

class CouponAdapter(private val coupons: List<Coupon>,
                    private val mainViewModel: MainViewModel,
                    private val closeCouponDialog: () -> Unit): RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    inner class CouponViewHolder(private val binding: ItemCouponBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(coupon: Coupon){
            binding.coupon = coupon
            binding.discount = coupon.discount
            binding.executePendingBindings()
            binding.couponContainer.setOnClickListener {
                coupon.code?.let {
                    code -> mainViewModel.pickCoupon(coupon)
                    closeCouponDialog.invoke()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        return CouponViewHolder(ItemCouponBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return coupons.size
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.bind(coupons[position])
    }
}