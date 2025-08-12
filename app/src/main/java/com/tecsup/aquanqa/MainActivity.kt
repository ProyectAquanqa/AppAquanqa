package com.tecsup.aquanqa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.navigation.NavigationView
import com.tecsup.aquanqa.data.FirebaseManager
import com.tecsup.aquanqa.data.LoginDataSource
import com.tecsup.aquanqa.data.LoginRepository
import com.tecsup.aquanqa.data.Result
import com.tecsup.aquanqa.data.SessionManager
import com.tecsup.aquanqa.data.manager.TokenRefreshManager
import com.tecsup.aquanqa.data.preferences.UserPreferences
import com.tecsup.aquanqa.data.repository.UserRepository
import com.tecsup.aquanqa.databinding.ActivityMainBinding
import com.tecsup.aquanqa.ui.login.LoginActivity
import com.tecsup.aquanqa.ui.profile.ProfileViewModel
import com.tecsup.aquanqa.ui.profile.ProfileViewModelFactory
import com.tecsup.aquanqa.utils.NotificationPermissionHelper
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
    private lateinit var firebaseManager: FirebaseManager
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper
    private lateinit var tokenRefreshManager: TokenRefreshManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Inicialización de repositorios y preferencias
        val userPreferences = UserPreferences(applicationContext)
        val loginDataSource = LoginDataSource(userPreferences)
        loginRepository = LoginRepository(
            dataSource = loginDataSource, 
            userPreferences = userPreferences,
            context = applicationContext
        )
        userRepository = UserRepository(applicationContext, userPreferences)

        // Obtener managers desde AquanqaApplication (ya inicializados)
        val app = application as AquanqaApplication
        tokenRefreshManager = app.getTokenRefreshManager()
        
        // Inicializar el ViewModel compartido
        profileViewModel = ViewModelProvider(this, ProfileViewModelFactory(application)).get(ProfileViewModel::class.java)

        // Inicializar helper de permisos de notificaciones
        notificationPermissionHelper = NotificationPermissionHelper(this)
        notificationPermissionHelper.initialize()

        // Obtener Firebase Manager (ya inicializado en Application)
        firebaseManager = FirebaseManager(applicationContext, userPreferences, app.getSessionManager())
        
        // Solicitar permisos de notificaciones y luego inicializar Firebase
        requestNotificationPermissionsAndInitializeFirebase()
        
        // El monitoreo de tokens ya se inicia automaticamente en Application

        binding.appBarMain.fabChatbot.setOnClickListener {
            navController.navigate(R.id.navigation_chatbot)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.drawerNavView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_anuncios, R.id.navigation_lunch, R.id.navigation_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Configurar navegación para Drawer y BottomNav
        navView.setupWithNavController(navController)
        
        // Configurar Bottom Navigation con gestión correcta del back stack
        setupBottomNavigationWithBackStackManagement()

        // Optimizar rendimiento del drawer durante el deslizamiento
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Forzar capa de hardware mientras se desliza para evitar jank
                if (binding.appBarMain.root.layerType != View.LAYER_TYPE_HARDWARE) {
                    binding.appBarMain.root.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                // Restaurar capa
                binding.appBarMain.root.setLayerType(View.LAYER_TYPE_NONE, null)
            }

            override fun onDrawerClosed(drawerView: View) {
                // Restaurar capa
                binding.appBarMain.root.setLayerType(View.LAYER_TYPE_NONE, null)
            }
        })

        // Configurar la cabecera del Drawer
        setupDrawerHeader()



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
            val isProfile = destination.id == R.id.navigation_profile
            val isEditProfile = destination.id == R.id.editProfileFragment
            val isNotifications = destination.id == R.id.navigation_notifications
            val isEventDetail = destination.id == R.id.eventDetailFragment
            
            // Ocultar FAB en chatbot, perfil, editar perfil y notificaciones
            val shouldHideFab = isChatbot || isProfile || isEditProfile || isNotifications
            binding.appBarMain.fabChatbot.visibility = if (shouldHideFab) View.GONE else View.VISIBLE
            
            // Solo ocultar bottom nav en chatbot
            binding.appBarMain.bottomNavView.visibility = if (isChatbot) View.GONE else View.VISIBLE

            // Actualizar selección del bottom navigation solo si estamos en un destino principal
            val topLevelDestinations = setOf(
                R.id.navigation_home,
                R.id.navigation_anuncios,
                R.id.navigation_lunch,
                R.id.navigation_profile
            )
            
            if (topLevelDestinations.contains(destination.id)) {
                // Actualizar la selección del bottom nav sin disparar el listener
                val bottomNav = binding.appBarMain.bottomNavView
                if (bottomNav.selectedItemId != destination.id) {
                    bottomNav.selectedItemId = destination.id
                    Log.d("MainActivity", "Bottom nav actualizado a: ${destination.id}")
                }
            }

            // Pre-cargar liviano el header del drawer cuando se entra al drawer destination
            // para que el drawer se sienta más fluido (evita cargas pesadas al abrir)
            if (::profileViewModel.isInitialized) {
                // No hace trabajo pesado, solo asegura datos listos
                profileViewModel.userProfile.value ?: profileViewModel.loadUserProfile()
            }
        }

        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    /**
     * Solicita permisos de notificaciones y luego inicializa Firebase
     */
    private fun requestNotificationPermissionsAndInitializeFirebase() {
        notificationPermissionHelper.checkAndRequestPermission { granted ->
            // Inicializar Firebase independientemente del permiso
            firebaseManager.initializeFirebase()
            
            if (!granted) {
                // Opcional: mostrar diálogo explicando la importancia de las notificaciones
            }
        }
    }

    /**
     * Configura el Bottom Navigation con gestión correcta del back stack.
     * Asegura que al cambiar entre tabs, siempre se muestre el fragment raíz correspondiente.
     */
    private fun setupBottomNavigationWithBackStackManagement() {
        // IDs de los fragments del bottom navigation
        val topLevelDestinations = setOf(
            R.id.navigation_home,
            R.id.navigation_anuncios, 
            R.id.navigation_lunch,
            R.id.navigation_profile
        )

        binding.appBarMain.bottomNavView.setOnItemSelectedListener { item ->
            val selectedId = item.itemId
            
            // Solo proceder si el item seleccionado está en los top level destinations
            if (topLevelDestinations.contains(selectedId)) {
                // Si ya estamos en el destino seleccionado, no hacer nada
                if (navController.currentDestination?.id == selectedId) {
                    return@setOnItemSelectedListener true
                }
                
                // Limpiar todo el back stack hasta el grafo de navegación raíz
                navController.popBackStack(R.id.mobile_navigation, false)
                
                // Navegar al destino seleccionado
                try {
                    navController.navigate(selectedId)
                    Log.d("MainActivity", "Navegado correctamente a: $selectedId")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error navegando a $selectedId: ${e.message}")
                    return@setOnItemSelectedListener false
                }
                
                return@setOnItemSelectedListener true
            }
            
            false
        }
        
        // Configurar el estado inicial del bottom navigation
        binding.appBarMain.bottomNavView.selectedItemId = R.id.navigation_home
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

                    // Cargar imagen de perfil con Glide optimizado para scroll suave
                    Glide.with(this@MainActivity)
                        .load(imageUrl)
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .thumbnail(0.25f)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .override(256, 256)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImage)
                }
        }

        // Cargar el perfil si aún no se ha hecho
        profileViewModel.loadUserProfile()
    }
    

    private fun logoutUser() {
        binding.appBarMain.logoutProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Iniciando logout completo")
                
                // Obtener AquanqaApplication para limpieza completa
                val app = application as AquanqaApplication
                
                // Remover token FCM del servidor
                firebaseManager.unregisterTokenFromServer()
                
                // Logout del repositorio
                loginRepository.logout()
                
                // Limpieza completa de sesion a nivel de aplicacion
                app.clearCompleteSession()
                
                Log.d("MainActivity", "Logout completo exitoso")
                
            } catch (e: Exception) {
                Log.e("MainActivity", "Error durante logout", e)
            } finally {
                // Ocultar ProgressBar y navegar a LoginActivity
                binding.appBarMain.logoutProgressBar.visibility = View.GONE
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        android.util.Log.d("MainActivity", "Inflando menú de toolbar")
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        android.util.Log.d("MainActivity", "Menú inflado con ${menu.size()} items")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        android.util.Log.d("MainActivity", "Item de menú seleccionado: ${item.itemId}")
        return when (item.itemId) {
            R.id.action_notifications -> {
                android.util.Log.d("MainActivity", "Navegando a notificaciones")
                navController.navigate(R.id.navigation_notifications)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Manejar navegación hacia atrás con gestión especial para fragments anidados
        return handleBackNavigation() || navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Maneja la navegación hacia atrás con lógica especial para fragments anidados
     */
    private fun handleBackNavigation(): Boolean {
        val currentDestination = navController.currentDestination?.id
        
        return when (currentDestination) {
            R.id.navigation_notifications -> {
                // Desde notificaciones, volver al último tab activo del bottom navigation
                navigateToLastActiveBottomNavTab()
                true
            }
            R.id.eventDetailFragment -> {
                // Desde detalle de evento, volver a notificaciones
                navController.popBackStack()
                true
            }
            else -> false
        }
    }

    /**
     * Navega al último tab activo del bottom navigation
     */
    private fun navigateToLastActiveBottomNavTab() {
        // Obtener el item actualmente seleccionado en el bottom navigation
        val selectedItemId = binding.appBarMain.bottomNavView.selectedItemId
        
        // Limpiar el back stack hasta el nivel raíz
        navController.popBackStack(R.id.mobile_navigation, false)
        
        // Navegar al tab seleccionado
        try {
            navController.navigate(selectedItemId)
            Log.d("MainActivity", "Navegando de vuelta al tab: $selectedItemId")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error navegando de vuelta: ${e.message}")
            // Fallback al home
            navController.navigate(R.id.navigation_home)
        }
    }
    
    override fun onBackPressed() {
        // Manejar el botón de atrás del sistema
        if (!handleSystemBackPress()) {
            super.onBackPressed()
        }
    }

    /**
     * Maneja el botón de atrás del sistema con lógica especial para fragments anidados
     */
    private fun handleSystemBackPress(): Boolean {
        val currentDestination = navController.currentDestination?.id
        
        return when (currentDestination) {
            R.id.navigation_notifications -> {
                // Desde notificaciones, volver al último tab activo del bottom navigation
                navigateToLastActiveBottomNavTab()
                true
            }
            R.id.eventDetailFragment -> {
                // Desde detalle de evento, volver a notificaciones
                navController.popBackStack()
                true
            }
            R.id.navigation_home, R.id.navigation_anuncios, 
            R.id.navigation_lunch, R.id.navigation_profile -> {
                // Si estamos en un tab principal, salir de la app
                false
            }
            else -> {
                // Para otros casos, usar la navegación estándar
                if (!navController.popBackStack()) {
                    // Si no hay más fragments en el stack, salir de la app
                    false
                } else {
                    true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener monitoreo de tokens y limpiar recursos
        if (::tokenRefreshManager.isInitialized) {
            tokenRefreshManager.stopTokenMonitoring()
        }
    }
}