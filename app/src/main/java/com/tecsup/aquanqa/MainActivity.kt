package com.tecsup.aquanqa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.tecsup.aquanqa.data.LoginDataSource
import com.tecsup.aquanqa.data.LoginRepository
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.repository.UserRepository
import com.tecsup.aquanqa.databinding.ActivityMainBinding
import com.tecsup.aquanqa.ui.login.LoginActivity
import com.tecsup.aquanqa.ui.profile.ProfileViewModel
import com.tecsup.aquanqa.ui.profile.ProfileViewModelFactory
import kotlinx.coroutines.launch
import androidx.navigation.ui.NavigationUI
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var loginRepository: LoginRepository
    private lateinit var userRepository: UserRepository
    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Inicialización de repositorios y preferencias
        val userPreferences = UserPreferences(applicationContext)
        val loginDataSource = LoginDataSource(userPreferences)
        loginRepository = LoginRepository(dataSource = loginDataSource, userPreferences = userPreferences)
        userRepository = UserRepository(applicationContext, userPreferences)

        // Inicializar el ViewModel compartido
        profileViewModel = ViewModelProvider(this, ProfileViewModelFactory(application)).get(ProfileViewModel::class.java)

        binding.appBarMain.fabChatbot.setOnClickListener {
            navController.navigate(R.id.navigation_chatbot)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.drawerNavView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_anuncios, R.id.navigation_beneficios, R.id.navigation_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Configurar navegación para Drawer y BottomNav
        navView.setupWithNavController(navController)
        binding.appBarMain.bottomNavView.setupWithNavController(navController)

        // Configurar la cabecera del Drawer
        setupDrawerHeader()

        // Configurar listener para status bar transparente cuando se abre el drawer
        setupDrawerStatusBar(drawerLayout)

        // Configurar listener de navegación SOLO para el item de logout
        navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.nav_logout) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                    logoutUser()
                true // Evento manejado
            } else {
                // Dejar que el Navigation Component maneje el resto
                // Cierra el drawer después de la navegación
                val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                    handled
            }
        }

        // Configurar la visibilidad de la UI basada en el destino de navegación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isChatbot = destination.id == R.id.navigation_chatbot
            binding.appBarMain.fabChatbot.visibility = if (isChatbot) View.GONE else View.VISIBLE
            binding.appBarMain.bottomNavView.visibility = if (isChatbot) View.GONE else View.VISIBLE
                }

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white)
    }

    private fun setupDrawerHeader() {
        val headerView = binding.drawerNavView.getHeaderView(0)
        val profileImage = headerView.findViewById<ImageView>(R.id.nav_header_profile_image)
        val userName = headerView.findViewById<TextView>(R.id.nav_header_user_name)
        val userEmail = headerView.findViewById<TextView>(R.id.nav_header_user_email)

        // Observar el LiveData del ViewModel para actualizar la cabecera del drawer
        profileViewModel.userProfile.observe(this) { userProfile ->
            userProfile?.let {
                userName.text = "${it.first_name} ${it.last_name}"
                userEmail.text = it.email ?: "Email no disponible"

                // Construir la URL completa para la imagen
                val imageUrl = it.foto_perfil?.let { url ->
                    if (url.startsWith("http")) url else userRepository.getBaseUrl() + url
                }

                    // Cargar imagen de perfil con Glide
                    Glide.with(this@MainActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImage)
                }
        }

        // Cargar el perfil si aún no se ha hecho
        profileViewModel.loadUserProfile()
    }
    
    /**
     * Configura el status bar para que sea blanco cuando se abre el drawer
     */
    private fun setupDrawerStatusBar(drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) {
                // Status bar blanco cuando se abre el drawer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.white)
                    window.navigationBarColor = ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Iconos del status bar oscuros para que se vean sobre fondo blanco
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
            }

            override fun onDrawerClosed(drawerView: View) {
                // Restaurar status bar original cuando se cierra el drawer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.statusBarColor = ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
                    window.navigationBarColor = ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Mantener iconos claros según el tema original
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Opcional: Puedes agregar animaciones durante el deslizamiento
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Opcional: Manejar cambios de estado del drawer
            }
        })
    }

    private fun logoutUser() {
        lifecycleScope.launch {
            loginRepository.logout()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}