/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.amnezia.awg.activity

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import org.amnezia.awg.Application
import org.amnezia.awg.QuickTileService
import org.amnezia.awg.R
import org.amnezia.awg.backend.BackendException
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.fragment.TunnelDetailFragment
import org.amnezia.awg.fragment.TunnelEditorFragment
import org.amnezia.awg.model.ObservableTunnel
import org.amnezia.awg.model.TunnelManager
import org.amnezia.awg.util.ErrorMessages
import org.amnezia.awg.util.applicationScope

/**
 * CRUD interface for AmneziaWG tunnels. This activity serves as the main entry point to the
 * AmneziaWG application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of AmneziaWG tunnels.
 */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { toggleTunnelWithPermissionsResult() }
    private var startAfterPermissionCheck: Boolean = false

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (startAfterPermissionCheck && grantResults.all { it == PermissionChecker.PERMISSION_GRANTED }) {
            // apparently, we need to start the tunnel now, as we've returned from permission check
            startAfterPermissionCheck = false;
            // I guess that's now okay.
            lifecycleScope.launch {
                Application.getTunnelManager().getTunnels().first()
                    .setStateAsync(Tunnel.State.TOGGLE)
            }
        }
    }

    private fun toggleTunnelWithPermissionsResult() {
        val tunnel = Application.getTunnelManager().lastUsedTunnel ?: return
        lifecycleScope.launch {
            try {
                tunnel.setStateAsync(Tunnel.State.TOGGLE)
            } catch (e: Throwable) {
                TileService.requestListeningState(this@MainActivity, ComponentName(this@MainActivity, QuickTileService::class.java))
                val error = ErrorMessages[e]
                val message = getString(R.string.toggle_error, error)
            }
            TileService.requestListeningState(this@MainActivity, ComponentName(this@MainActivity, QuickTileService::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSelectedTunnelChanged(
        oldTunnel: ObservableTunnel?,
        newTunnel: ObservableTunnel?
    ): Boolean {
        return false
    }

    override fun onBackStackChanged() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.alt_main_activity)
        val tunnelManager = Application.getTunnelManager()
        Dispatchers.Main.applicationScope.launch {
        }
        findViewById<AppCompatButton>(R.id.vMainToggleButton).setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Call that just to get the lastEdited tunnel into place
                    Application.getTunnelManager().getTunnels().first().setStateAsync(Tunnel.State.TOGGLE)
                } catch (e: Throwable) {
                    // And if that fails, call permission requesting variant
                    if (Application.getBackend() is GoBackend) {
                        val intent = GoBackend.VpnService.prepare(this@MainActivity)
                        if (intent != null) {
                            permissionActivityResultLauncher.launch(intent)
                            return@launch
                        }
                    }
                }
            }
        }
        actionBar = supportActionBar
        supportFragmentManager.addOnBackStackChangedListener(this)

        onBackStackChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_activity, menu)
        return true
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
