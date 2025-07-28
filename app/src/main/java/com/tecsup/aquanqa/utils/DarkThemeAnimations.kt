package com.tecsup.aquanqa.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Utilidades para animaciones del tema oscuro
 * Proporciona animaciones consistentes y suaves para todos los componentes
 */
object DarkThemeAnimations {
    
    // Duraciones estándar
    const val DURATION_SHORT = 150L
    const val DURATION_MEDIUM = 300L
    const val DURATION_LONG = 400L
    
    // Interpoladores
    private val fastOutSlowIn = FastOutSlowInInterpolator()
    private val decelerate = DecelerateInterpolator()
    private val overshoot = OvershootInterpolator(1.2f)
    private val accelerateDecelerate = AccelerateDecelerateInterpolator()
    
    /**
     * Animación de entrada para fragments
     */
    fun animateFragmentEnter(view: View, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.translationY = 50f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 50f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = DURATION_LONG
            interpolator = fastOutSlowIn
        }
        
        animatorSet.doOnEnd { onComplete?.invoke() }
        animatorSet.start()
    }
    
    /**
     * Animación de salida para fragments
     */
    fun animateFragmentExit(view: View, onComplete: (() -> Unit)? = null) {
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(view, "translationY", 0f, -30f),
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = DURATION_MEDIUM
            interpolator = fastOutSlowIn
        }
        
        animatorSet.doOnEnd { onComplete?.invoke() }
        animatorSet.start()
    }
    
    /**
     * Animación de entrada para cards con efecto staggered
     */
    fun animateCardEnter(view: View, delay: Long = 0, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.translationY = 80f
        view.scaleX = 0.9f
        view.scaleY = 0.9f
        
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "translationY", 80f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f)
            )
            duration = DURATION_LONG
            interpolator = decelerate
            startDelay = delay
        }
        
        animatorSet.doOnEnd { onComplete?.invoke() }
        animatorSet.start()
    }
    
    /**
     * Animación de entrada staggered para múltiples views
     */
    fun animateStaggeredEnter(views: List<View>, staggerDelay: Long = 100L) {
        views.forEachIndexed { index, view ->
            animateCardEnter(view, delay = index * staggerDelay)
        }
    }
    
    /**
     * Animación de press para botones
     */
    fun animateButtonPress(view: View, onComplete: (() -> Unit)? = null) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
            )
            duration = DURATION_SHORT
            interpolator = fastOutSlowIn
        }
        
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            )
            duration = DURATION_SHORT
            interpolator = overshoot
        }
        
        val sequence = AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
        }
        
        sequence.doOnEnd { onComplete?.invoke() }
        sequence.start()
    }
    
    /**
     * Animación de FAB con efecto de rebote
     */
    fun animateFabShow(view: View, onComplete: (() -> Unit)? = null) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.rotation = -45f
        view.visibility = View.VISIBLE
        
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(view, "rotation", -45f, 0f)
            )
            duration = DURATION_MEDIUM
            interpolator = overshoot
        }
        
        animatorSet.doOnEnd { onComplete?.invoke() }
        animatorSet.start()
    }
    
    /**
     * Animación de FAB hide
     */
    fun animateFabHide(view: View, onComplete: (() -> Unit)? = null) {
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f),
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(view, "rotation", 0f, 45f)
            )
            duration = DURATION_SHORT
            interpolator = fastOutSlowIn
        }
        
        animatorSet.doOnEnd { 
            view.visibility = View.GONE
            onComplete?.invoke() 
        }
        animatorSet.start()
    }
    
    /**
     * Animación de reveal circular para elementos
     */
    fun animateCircularReveal(view: View, centerX: Int, centerY: Int, onComplete: (() -> Unit)? = null) {
        val finalRadius = Math.hypot(view.width.toDouble(), view.height.toDouble()).toFloat()
        
        val animator = android.animation.ValueAnimator.ofFloat(0f, finalRadius).apply {
            duration = DURATION_LONG
            interpolator = fastOutSlowIn
            
            addUpdateListener { animation ->
                val radius = animation.animatedValue as Float
                view.clipBounds = android.graphics.Rect(
                    (centerX - radius).toInt(),
                    (centerY - radius).toInt(),
                    (centerX + radius).toInt(),
                    (centerY + radius).toInt()
                )
            }
        }
        
        animator.doOnStart { view.visibility = View.VISIBLE }
        animator.doOnEnd { 
            view.clipBounds = null
            onComplete?.invoke() 
        }
        animator.start()
    }
    
    /**
     * Animación de slide up para bottom sheets o diálogos
     */
    fun animateSlideUp(view: View, onComplete: (() -> Unit)? = null) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            duration = DURATION_MEDIUM
            interpolator = decelerate
        }
        
        animatorSet.doOnEnd { onComplete?.invoke() }
        animatorSet.start()
    }
    
    /**
     * Animación de slide down para ocultar elementos
     */
    fun animateSlideDown(view: View, onComplete: (() -> Unit)? = null) {
        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat()),
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
            )
            duration = DURATION_MEDIUM
            interpolator = accelerateDecelerate
        }
        
        animatorSet.doOnEnd { 
            view.visibility = View.GONE
            onComplete?.invoke() 
        }
        animatorSet.start()
    }
    
    /**
     * Animación de shake para errores
     */
    fun animateShake(view: View, onComplete: (() -> Unit)? = null) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
            duration = 600L
            interpolator = accelerateDecelerate
        }
        
        shake.doOnEnd { onComplete?.invoke() }
        shake.start()
    }
    
    /**
     * Animación de pulse para llamar la atención
     */
    fun animatePulse(view: View, repeatCount: Int = 2, onComplete: (() -> Unit)? = null) {
        val pulse = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f),
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f)
            )
            duration = 800L
            interpolator = accelerateDecelerate
        }
        
        pulse.doOnEnd { 
            if (repeatCount > 0) {
                animatePulse(view, repeatCount - 1, onComplete)
            } else {
                onComplete?.invoke()
            }
        }
        pulse.start()
    }
    
    /**
     * Animación de loading con rotación
     */
    fun animateLoading(view: View): Animator {
        return ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = accelerateDecelerate
        }
    }
    
    /**
     * Detener animación de loading
     */
    fun stopLoading(animator: Animator, view: View, onComplete: (() -> Unit)? = null) {
        animator.cancel()
        
        // Animar de vuelta a rotación 0
        ObjectAnimator.ofFloat(view, "rotation", view.rotation, 0f).apply {
            duration = DURATION_SHORT
            interpolator = decelerate
            doOnEnd { onComplete?.invoke() }
            start()
        }
    }
}