package com.nguyen.asuper.ui

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
import com.nguyen.asuper.R
import com.nguyen.asuper.databinding.NavHeaderBinding
import com.nguyen.asuper.repository.AuthRepository.Companion.currentUser
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUserId
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController


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
        val navBinding = NavHeaderBinding.bind(view)

        navBinding.user = currentUser


        navigationView.setupWithNavController(navController)

        setSupportActionBar(mainToolBar)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        navigationView.setNavigationItemSelectedListener {
            if(it.itemId == R.id.log_out_item){
                startActivity(Intent(this, AuthenticationActivity::class.java))
                finish()
                currentLoggedUserId = null
            } else {
                findNavController(R.id.main_fragment).navigate(R.id.trip_history_fragment)
                drawerLayout.closeDrawer(Gravity.START)
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


}