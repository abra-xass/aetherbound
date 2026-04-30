package com.aetherbound.game.core.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

/**
 * Keeps Thot's `TorKeepAliveService` process pinned at high priority
 * while Aetherbound is in a multiplayer match.
 *
 * Two mechanisms:
 *
 *   1. **Bind to Thot's ForegroundService** — Android keeps the bound
 *      process alive at importance ≥ FOREGROUND_SERVICE as long as the
 *      binding is active. This survives normal background-app
 *      memory pressure, OEM aggressive savers (most of them), and the
 *      user backgrounding the messenger.
 *   2. **Heartbeat broadcast** — every 15s while the match is live, fire
 *      a no-op broadcast at Thot. This re-asserts process activity to
 *      schedulers that key off recent IPC traffic.
 *
 * Aetherbound never depends on Thot at compile-time — the binding uses
 * an explicit [ComponentName] string. If Thot isn't installed (or the
 * service has been renamed) the bind is a graceful no-op and the
 * connection runs in best-effort mode (Tor stays up via Thot's own
 * ForegroundService notification anyway).
 */
class ThotConnectionKeeper(private val ctx: Context) {

    companion object {
        const val THOT_PACKAGE = "com.thot.messenger"
        const val SERVICE_CLASS = "com.thot.messenger.TorKeepAliveService"
        const val HEARTBEAT_ACTION = "com.thot.messenger.AETHER_HEARTBEAT"
        private const val TAG = "ThotConnectionKeeper"
    }

    private var binding = false
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Bound to Thot's keep-alive service")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Lost binding to Thot's keep-alive service")
        }
    }

    /**
     * Start the bind. Idempotent — calling twice does nothing. Returns
     * true if Thot is installed and the bind request was issued.
     */
    fun start(): Boolean {
        if (binding) return true
        val intent = Intent().apply {
            component = ComponentName(THOT_PACKAGE, SERVICE_CLASS)
        }
        binding = runCatching {
            ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        }.getOrDefault(false)
        if (!binding) Log.w(TAG, "Thot service unreachable — running with reduced persistence")
        return binding
    }

    /** Release the bind. Match-end / scene-leave should call this. */
    fun stop() {
        if (!binding) return
        runCatching { ctx.unbindService(conn) }
        binding = false
    }

    /**
     * Send a heartbeat broadcast at Thot. Doesn't require Thot to react —
     * the IPC itself signals "Aetherbound still active and depending on
     * Thot's network" to Android's scheduler.
     */
    fun heartbeat() {
        runCatching {
            ctx.sendBroadcast(Intent(HEARTBEAT_ACTION).apply {
                setPackage(THOT_PACKAGE)
            })
        }
    }

    /** True when the bind is currently active. */
    val isActive: Boolean get() = binding
}
