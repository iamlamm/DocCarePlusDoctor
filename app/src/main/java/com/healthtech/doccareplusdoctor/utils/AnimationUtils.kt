package com.healthtech.doccareplusdoctor.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

object AnimationUtils {

    /**
     * Xử lý animation mượt mà khi show một view
     */
    fun View.showWithAnimation(duration: Long = 400) {
        alpha = 0f
        scaleX = 0.92f
        scaleY = 0.92f
        visibility = View.VISIBLE

        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    /**
     * Xử lý animation mượt mà khi hide một view
     */
    fun View.hideWithAnimation(duration: Long = 400, onEnd: () -> Unit = {}) {
        animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onEnd()
            }
            .start()
    }

    /**
     * Xử lý animation fade in cho view
     */
    fun View.fadeIn(
        duration: Long = 800,
        delay: Long = 0,
        onEnd: () -> Unit = {}
    ) {
        alpha = 0f
        ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
            this.duration = duration
            startDelay = delay
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    /**
     * Xử lý animation fade out cho view
     */
    fun View.fadeOut(
        duration: Long = 400,
        onEnd: () -> Unit = {}
    ) {
        ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    /**
     * Xử lý animation fade in cho nhiều view theo thứ tự
     */
    fun fadeInSequentially(vararg views: View, delayBetween: Long = 300) {
        views.forEachIndexed { index, view ->
            view.fadeIn(delay = index * delayBetween)
        }
    }

//    /**
//     * Xử lý animation fade out cho nhiều view theo thứ tự
//     */
//    fun fadeOutSequentially(vararg views: View, delayBetween: Long = 300) {
//        views.forEachIndexed { index, view ->
//            view.fadeOut(delay = index * delayBetween)
//        }
//    }
}