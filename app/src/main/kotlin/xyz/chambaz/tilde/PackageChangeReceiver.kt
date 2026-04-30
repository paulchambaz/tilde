package xyz.chambaz.tilde

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import xyz.chambaz.tilde.data.AppRepository

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppRepository.invalidate()
    }
}
