package com.veronezzi.riftlog.ui.common

import android.view.View
import android.view.animation.AnimationUtils
import com.rifttracker.designsystem.R as DesignSystemR

/**
 * Shows/hides a skeleton-loading container, starting the design system's alpha-pulse animation
 * while it's up. The animation resource existed but nothing ever called startAnimation() on it,
 * so skeletons rendered as static gray blocks instead of pulsing.
 */
fun View.setSkeletonVisible(visible: Boolean) {
    if (visible) {
        visibility = View.VISIBLE
        if (animation == null) {
            startAnimation(AnimationUtils.loadAnimation(context, DesignSystemR.anim.skeleton_pulse))
        }
    } else {
        clearAnimation()
        visibility = View.GONE
    }
}
