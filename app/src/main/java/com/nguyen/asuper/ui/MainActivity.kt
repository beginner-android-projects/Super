package com.nguyen.asuper.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.nguyen.asuper.R
import com.nguyen.asuper.ui.main.PaymentDialogFragment
import com.nguyen.asuper.util.SavedSharedPreferences.currentLoggedUser
import com.nguyen.asuper.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val mainViewModel by viewModel<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(currentLoggedUser?.home == null || currentLoggedUser?.work == null){
            startActivity(Intent(this, ProvideAddressActivity::class.java))
        }

        val navController = findNavController(R.id.main_fragment)
        val drawerLayout = drawer_layout

        navigation_view.setupWithNavController(navController)

        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)



        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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


}