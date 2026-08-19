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
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        navController = navHostFragment.navController
        val startDestinationId = navController.graph.startDestinationId

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.matchHistoryFragment, R.id.championStatsFragment, R.id.buildsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != navController.currentDestination?.id) {
                navController.popBackStack(startDestinationId, false)
                if (item.itemId != startDestinationId) {
                    navController.navigate(
                        item.itemId,
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
            true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            invalidateOptionsMenu()
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
