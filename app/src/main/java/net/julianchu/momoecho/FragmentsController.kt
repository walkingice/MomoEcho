package net.julianchu.momoecho

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import net.julianchu.momoecho.editclip.createEditClipFragment
import net.julianchu.momoecho.player.createBrowserFragment
import net.julianchu.momoecho.player.createPlayerFragment

fun openPlayerFragment(mgr: FragmentManager) {
    mgr.beginTransaction()
        .setCustomAnimations(
            R.anim.slide_in_right_to_left,
            R.anim.slide_out_right_to_left,
            R.anim.slide_in_left_to_right,
            R.anim.slide_out_left_to_right
        )
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .replace(android.R.id.content, createPlayerFragment())
        .addToBackStack("browser")
        .commit()
}

fun openEditClipFragment(mgr: FragmentManager) {
    mgr.beginTransaction()
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .replace(android.R.id.content, createEditClipFragment())
        .addToBackStack("editClip")
        .commit()
}

fun initBrowserFragment(mgr: FragmentManager) {
    mgr.beginTransaction()
        .replace(android.R.id.content, createBrowserFragment())
        .commitNow()
}
