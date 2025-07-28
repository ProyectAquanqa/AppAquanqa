package com.tecsup.aquanqa.utils

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.tecsup.aquanqa.R

/**
 * Helper para aplicar animaciones consistentes en fragments
 */
abstract class AnimatedFragment : Fragment() {
    
    private var hasAnimatedIn = false
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (!hasAnimatedIn) {
            setupInitialState(view)
            animateIn(view)
            hasAnimatedIn = true
        }
    }
    
    /**
     * Configura el estado inicial antes de la animación
     */
    protected open fun setupInitialState(view: View) {
        view.alpha = 0f
        view.translationY = 50f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
    }
    
    /**
     * Anima la entrada del fragment
     */
    protected open fun animateIn(view: View) {
        DarkThemeAnimations.animateFragmentEnter(view) {
            onAnimationComplete()
        }
    }
    
    /**
     * Llamado cuando la animación de entrada se completa
     */
    protected open fun onAnimationComplete() {
        // Override en subclases si es necesario
    }
    
    /**
     * Anima la salida del fragment
     */
    fun animateOut(onComplete: (() -> Unit)? = null) {
        view?.let { view ->
            DarkThemeAnimations.animateFragmentExit(view) {
                onComplete?.invoke()
            }
        }
    }
}

/**
 * Helper para animaciones de listas y cards
 */
object FragmentAnimationHelper {
    
    /**
     * Anima la entrada de elementos en una lista con efecto staggered
     */
    fun animateListItems(views: List<View>, staggerDelay: Long = 100L) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 80f
            view.scaleX = 0.9f
            view.scaleY = 0.9f
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400L)
                .setStartDelay(index * staggerDelay)
                .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start()
        }
    }
    
    /**
     * Anima la entrada de cards con delay personalizado
     */
    fun animateCards(vararg views: View, baseDelay: Long = 0L, staggerDelay: Long = 150L) {
        views.forEachIndexed { index, view ->
            DarkThemeAnimations.animateCardEnter(
                view, 
                delay = baseDelay + (index * staggerDelay)
            )
        }
    }
    
    /**
     * Anima elementos de UI con diferentes tipos de entrada
     */
    fun animateUIElements(
        fadeInViews: List<View> = emptyList(),
        slideUpViews: List<View> = emptyList(),
        scaleInViews: List<View> = emptyList(),
        baseDelay: Long = 0L
    ) {
        var currentDelay = baseDelay
        
        // Fade in views
        fadeInViews.forEach { view ->
            view.fadeIn(onComplete = null)
            currentDelay += 100L
        }
        
        // Slide up views
        slideUpViews.forEach { view ->
            view.postDelayed({
                view.animateSlideUp()
            }, currentDelay)
            currentDelay += 100L
        }
        
        // Scale in views
        scaleInViews.forEach { view ->
            view.postDelayed({
                view.scaleIn()
            }, currentDelay)
            currentDelay += 100L
        }
    }
    
    /**
     * Anima la transición entre estados de loading
     */
    fun animateLoadingTransition(
        loadingView: View,
        contentView: View,
        isLoading: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        if (isLoading) {
            // Mostrar loading, ocultar contenido
            contentView.fadeOut {
                loadingView.fadeIn {
                    onComplete?.invoke()
                }
            }
        } else {
            // Mostrar contenido, ocultar loading
            loadingView.fadeOut {
                contentView.fadeIn {
                    onComplete?.invoke()
                }
            }
        }
    }
    
    /**
     * Anima la transición entre estados de error
     */
    fun animateErrorTransition(
        errorView: View,
        contentView: View,
        showError: Boolean,
        onComplete: (() -> Unit)? = null
    ) {
        if (showError) {
            contentView.fadeOut {
                errorView.animateEnterCombined(
                    fromTranslationY = 50f,
                    onComplete = {
                        errorView.animateShake()
                        onComplete?.invoke()
                    }
                )
            }
        } else {
            errorView.fadeOut {
                contentView.animateEnterCombined(onComplete = onComplete)
            }
        }
    }
}

/**
 * Extensión para Fragment que facilita el uso de animaciones
 */
fun Fragment.animateViewsOnCreate(
    view: View,
    animateChildren: Boolean = true,
    staggerDelay: Long = 100L
) {
    lifecycleScope.launch {
        delay(50) // Pequeño delay para asegurar que el layout esté listo
        
        if (animateChildren && view is android.view.ViewGroup) {
            view.animateChildrenStaggered(staggerDelay)
        } else {
            view.animateEnter()
        }
    }
}

/**
 * Extensión para animar transiciones de navegación
 */
fun Fragment.setCustomAnimations(
    enter: Int = R.anim.nav_enter_dark,
    exit: Int = R.anim.nav_exit_dark,
    popEnter: Int = R.anim.nav_pop_enter_dark,
    popExit: Int = R.anim.nav_pop_exit_dark
) {
    parentFragmentManager.beginTransaction()
        .setCustomAnimations(enter, exit, popEnter, popExit)
        .commit()
}