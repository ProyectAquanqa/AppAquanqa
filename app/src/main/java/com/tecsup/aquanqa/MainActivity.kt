package com.tecsup.aquanqa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.tecsup.aquanqa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fabChatbot.setOnClickListener {
            findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.navigation_chatbot)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.drawerNavView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_anuncios, R.id.navigation_beneficios, R.id.navigation_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup NavController with NavigationView (for Drawer)
        navView.setupWithNavController(navController)

        // Setup NavController with BottomNavigationView
        val bottomNavView = binding.appBarMain.bottomNavView
        bottomNavView.setupWithNavController(navController)

        // Evita que el fragmento se recargue al volver a seleccionar el mismo ítem
        bottomNavView.setOnItemReselectedListener {
            // No hacer nada para prevenir la recarga
        }

        // Mostrar/Ocultar componentes según el destino de navegación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> {
                    binding.appBarMain.fabChatbot.visibility = View.VISIBLE
                    binding.appBarMain.bottomNavView.visibility = View.VISIBLE
                }
                R.id.navigation_chatbot -> {
                    binding.appBarMain.fabChatbot.visibility = View.GONE
                    binding.appBarMain.bottomNavView.visibility = View.GONE
                }
                else -> {
                    binding.appBarMain.fabChatbot.visibility = View.GONE
                    binding.appBarMain.bottomNavView.visibility = View.VISIBLE
                }
            }
        }

        // color icono
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}