package com.nguyen.asuper.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar.LayoutParams
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.nguyen.asuper.R
import com.nguyen.asuper.databinding.NavHeaderBinding
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.ui.auth.RegisterFragment
import com.nguyen.asuper.ui.auth.RegisterFragment.Companion.PICK_IMAGE_REQUEST
import com.nguyen.asuper.ui.main.DriverNotifyDialogFragment
import com.nguyen.asuper.ui.main.adapter.EditUserDialogFragment
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUserId
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var navBinding: NavHeaderBinding


    private val mainViewModel by viewModel<MainViewModel>()

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(currentUser.home == null || currentUser.work == null){
            startActivity(Intent(this, ProvideAddressActivity::class.java))
        }

        val mainToolBar = toolbar
        navController = findNavController(R.id.main_fragment)
        val drawerLayout = drawer_layout
        val navigationView = navigation_view
        val view = navigationView.getHeaderView(0)
        navBinding = NavHeaderBinding.bind(view)

        navBinding.user = currentUser

        navBinding.avatarImageView.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*";
            intent.action = Intent.ACTION_GET_CONTENT;
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        navBinding.editUserButton.setOnClickListener {
            val newFragment = EditUserDialogFragment(mainViewModel, fun(newName: String){
                navBinding.nameHeader.text = newName
            })
            newFragment.show(supportFragmentManager, "EditDialog")
        }

        navigationView.setupWithNavController(navController)

        setSupportActionBar(mainToolBar)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navigationView.setNavigationItemSelectedListener {
            if(it.itemId == R.id.log_out_item){
                startActivity(Intent(this, AuthenticationActivity::class.java))
                finish()
                currentLoggedUserId = null
            } else if(it.itemId == R.id.trip_history_fragment) {
                findNavController(R.id.main_fragment).navigate(R.id.trip_history_fragment)
                drawerLayout.closeDrawer(Gravity.START)
            } else if(it.itemId == R.id.home_and_work_item) {
                val intent = Intent(this, ProvideAddressActivity::class.java)
                intent.putExtra("home", currentUser.home?.address)
                intent.putExtra("work", currentUser.work?.address)
                startActivity(intent)
            }
            false
        }

//        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
//        supportActionBar?.setCustomView(R.layout.abs_layout)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.customView?.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun hideKeyboard(){
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun hideActionBar(){
        supportActionBar?.hide()
    }

    fun showActionBar(){
        supportActionBar?.show()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(!this::navBinding.isInitialized) return
        if(resultCode != Activity.RESULT_OK) return

        if(requestCode == PICK_IMAGE_REQUEST){
            val uri = data?.data
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(navBinding.avatarImageView)
            mainViewModel.saveAvatar(currentUser.id, uri)
        }
    }
}