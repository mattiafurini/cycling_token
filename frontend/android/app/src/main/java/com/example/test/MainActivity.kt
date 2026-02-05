package com.example.test

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.test.databinding.ActivityMainBinding
import com.example.test.utils.LanguageManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        // Hide bottom nav on wallet connect screen if needed, or handle login flow before this activity.
        // For now, we will handle wallet connection as a start destination or a separate activity.
        // Let's check if user is "connected". If not, maybe show a different UI or navigate to WalletFragment.
        
        navView.setupWithNavController(navController)
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_wallet_connect) {
                navView.visibility = View.GONE
            } else {
                navView.visibility = View.VISIBLE
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before activity is created
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}