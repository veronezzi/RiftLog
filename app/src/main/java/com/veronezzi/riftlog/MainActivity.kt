package com.veronezzi.riftlog

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.veronezzi.riftlog.databinding.ActivityMainBinding

/**
 * Single-Activity host: Toolbar + NavHostFragment + BottomNavigationView, wired via Navigation
 * Component.
 *
 * Deliberately NOT using BottomNavigationView.setupWithNavController(): this app pushes detail
 * screens on top of tabs (Home -> Profile, ChampionStats -> ChampionDetail) that aren't part of
 * the bottom-nav menu. NavigationUI's default popUpTo(start){saveState=true}/restoreState=true
 * behavior only pops what's directly above the target and can leave a pushed detail screen
 * stranded in the back stack across tab switches, so after a couple of switches "Home" resolves
 * back into the stranded Profile screen instead of the Home tab. Popping all the way to the
 * graph's start destination on every tab tap keeps each tab's root clean, at the cost of not
 * preserving per-tab scroll/back-stack state across switches - an acceptable trade for this app.
 *
 * The Toolbar's back arrow still goes through the standard NavigationUI machinery
 * (setupActionBarWithNavController + AppBarConfiguration), since that part doesn't conflict with
 * the custom bottom-nav listener above - it only reacts to the current destination.
 *
 * Because a bottom-nav tab is a top-level destination in [AppBarConfiguration] (no back arrow,
 * by design), any screen that pushes one of those tab fragments as a stacked destination instead
 * of switching to it would land on a screen with no visible way back - the toolbar suppresses the
 * arrow for a top-level id regardless of how it was reached. [switchToTab] is the one path
 * allowed to land on a tab fragment, so every caller (this activity's own listener, or a fragment
 * like Profile linking to "view match history") goes through the same pop-to-start-then-navigate
 * behavior instead of a plain nav action push.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var startDestinationId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        navController = navHostFragment.navController
        startDestinationId = navController.graph.startDestinationId

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.matchHistoryFragment, R.id.championStatsFragment, R.id.buildsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.bottomNav.setOnItemSelectedListener { item ->
            switchToTab(item.itemId)
            true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            invalidateOptionsMenu()
        }
    }

    /** Pops to the graph's start destination, then navigates to [tabId] if it isn't the start
     * destination itself. See the class doc for why every landing on a tab fragment must go
     * through this instead of a plain nav action. */
    fun switchToTab(tabId: Int) {
        if (tabId == navController.currentDestination?.id) return
        navController.popBackStack(startDestinationId, false)
        if (tabId != startDestinationId) {
            navController.navigate(
                tabId,
                null,
                navOptions {
                    anim {
                        enter = R.anim.nav_fade_in
                        exit = R.anim.nav_fade_out
                    }
                },
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_settings)?.isVisible = navController.currentDestination?.id != R.id.settingsFragment
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            navController.navigate(
                R.id.settingsFragment,
                null,
                navOptions {
                    anim {
                        enter = R.anim.nav_slide_in_right
                        exit = R.anim.nav_slide_out_left
                        popEnter = R.anim.nav_slide_in_left
                        popExit = R.anim.nav_slide_out_right
                    }
                },
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()
}
