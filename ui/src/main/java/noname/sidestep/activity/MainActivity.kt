/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package noname.sidestep.activity

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.service.quicksettings.TileService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.core.content.PermissionChecker
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import noname.sidestep.Application
import noname.sidestep.QuickTileService
import noname.sidestep.R
import noname.sidestep.databinding.AltMainActivityBinding
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import noname.sidestep.model.ObservableTunnel
import noname.sidestep.util.ErrorMessages
import noname.sidestep.util.applicationScope

/**
 * Simplified version of switching interface, allowing for turning on and off a single VPN profile.
 */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    private var actionBar: ActionBar? = null
    private val permissionActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK)
                toggleTunnelWithPermissionsResult()
        }
    private lateinit var binding: AltMainActivityBinding

    private fun toggleTunnelWithPermissionsResult() {
        val tunnel = Application.getTunnelManager().lastUsedTunnel ?: return
        lifecycleScope.launch {
            try {
                tunnel.setStateAsync(Tunnel.State.UP)
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

        binding = AltMainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Dispatchers.Main.immediate.applicationScope.launch {
            binding.tunnel = Application.getTunnelManager().getTunnels().first()
        }

        binding.vMainToggleButton.setOnClickListener {
            lifecycleScope.launch {
                runCatching {
                    binding.tunnel?.setStateAsync(Tunnel.State.TOGGLE)
                    TileService.requestListeningState(this@MainActivity, ComponentName(this@MainActivity, QuickTileService::class.java))
                }.onFailure {
                    Log.w(TAG, "Error while enabling configuration: ${it}")
                    // And if that fails, call permission requesting variant
                    if (Application.getBackend() is GoBackend) {
                        val intent = GoBackend.VpnService.prepare(this@MainActivity)
                        if (intent != null) {
                            permissionActivityResultLauncher.launch(intent)
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
