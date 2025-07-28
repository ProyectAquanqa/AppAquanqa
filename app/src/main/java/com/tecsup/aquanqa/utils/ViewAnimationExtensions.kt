package com.tecsup.aquanqa.utils

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children

/**
 * Extensiones para facilitar el uso de animaciones en Views
 */

/**
 * Anima la entrada de una View con el estilo del tema oscuro
 */
fun View.animateEnter(delay: Long = 0, onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateCardEnter(this, delay, onComplete)
}

/**
 * Anima la salida de una View
 */
fun View.animateExit(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateFragmentExit(this, onComplete)
}

/**
 * Anima el press de un botón
 */
fun View.animatePress(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateButtonPress(this, onComplete)
}

/**
 * Anima la aparición de un FAB
 */
fun View.animateFabShow(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateFabShow(this, onComplete)
}

/**
 * Anima la desaparición de un FAB
 */
fun View.animateFabHide(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateFabHide(this, onComplete)
}

/**
 * Anima un slide up desde abajo
 */
fun View.animateSlideUp(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateSlideUp(this, onComplete)
}

/**
 * Anima un slide down hacia abajo
 */
fun View.animateSlideDown(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateSlideDown(this, onComplete)
}

/**
 * Anima un shake para errores
 */
fun View.animateShake(onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animateShake(this, onComplete)
}

/**
 * Anima un pulse para llamar la atención
 */
fun View.animatePulse(repeatCount: Int = 2, onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.animatePulse(this, repeatCount, onComplete)
}

/**
 * Inicia animación de loading
 */
fun View.startLoading(): Animator {
    return DarkThemeAnimations.animateLoading(this).apply { start() }
}

/**
 * Detiene animación de loading
 */
fun View.stopLoading(animator: Animator, onComplete: (() -> Unit)? = null) {
    DarkThemeAnimations.stopLoading(animator, this, onComplete)
}

/**
 * Anima todos los hijos de un ViewGroup con efecto staggered
 */
fun ViewGroup.animateChildrenStaggered(staggerDelay: Long = 100L) {
    val childViews = children.toList()
    DarkThemeAnimations.animateStaggeredEnter(childViews, staggerDelay)
}

/**
 * Fade in suave
 */
fun View.fadeIn(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction { onComplete?.invoke() }
        .start()
}

/**
 * Fade out suave
 */
fun View.fadeOut(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction { 
            visibility = View.GONE
            onComplete?.invoke() 
        }
        .start()
}

/**
 * Scale in con efecto de rebote
 */
fun View.scaleIn(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    scaleX = 0f
    scaleY = 0f
    visibility = View.VISIBLE
    animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(duration)
        .setInterpolator(android.view.animation.OvershootInterpolator())
        .withEndAction { onComplete?.invoke() }
        .start()
}

/**
 * Scale out
 */
fun View.scaleOut(duration: Long = DarkThemeAnimations.DURATION_SHORT, onComplete: (() -> Unit)? = null) {
    animate()
        .scaleX(0f)
        .scaleY(0f)
        .setDuration(duration)
        .withEndAction { 
            visibility = View.GONE
            onComplete?.invoke() 
        }
        .start()
}

/**
 * Slide in desde la izquierda
 */
fun View.slideInFromLeft(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    translationX = -width.toFloat()
    visibility = View.VISIBLE
    animate()
        .translationX(0f)
        .setDuration(duration)
        .withEndAction { onComplete?.invoke() }
        .start()
}

/**
 * Slide out hacia la izquierda
 */
fun View.slideOutToLeft(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    animate()
        .translationX(-width.toFloat())
        .setDuration(duration)
        .withEndAction { 
            visibility = View.GONE
            onComplete?.invoke() 
        }
        .start()
}

/**
 * Slide in desde la derecha
 */
fun View.slideInFromRight(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    translationX = width.toFloat()
    visibility = View.VISIBLE
    animate()
        .translationX(0f)
        .setDuration(duration)
        .withEndAction { onComplete?.invoke() }
        .start()
}

/**
 * Slide out hacia la derecha
 */
fun View.slideOutToRight(duration: Long = DarkThemeAnimations.DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
    animate()
        .translationX(width.toFloat())
        .setDuration(duration)
        .withEndAction { 
            visibility = View.GONE
            onComplete?.invoke() 
        }
        .start()
}

/**
 * Animación combinada de entrada con múltiples efectos
 */
fun View.animateEnterCombined(
    fromAlpha: Float = 0f,
    fromScale: Float = 0.8f,
    fromTranslationY: Float = 100f,
    duration: Long = DarkThemeAnimations.DURATION_LONG,
    delay: Long = 0,
    onComplete: (() -> Unit)? = null
) {
    alpha = fromAlpha
    scaleX = fromScale
    scaleY = fromScale
    translationY = fromTranslationY
    visibility = View.VISIBLE
    
    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .translationY(0f)
        .setDuration(duration)
        .setStartDelay(delay)
        .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
        .withEndAction { onComplete?.invoke() }
        .start()
}

/**
 * Resetea todas las propiedades de animación de una View
 */
fun View.resetAnimationProperties() {
    alpha = 1f
    scaleX = 1f
    scaleY = 1f
    translationX = 0f
    translationY = 0f
    rotation = 0f
    clearAnimation()
}