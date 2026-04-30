package xyz.chambaz.tilde.data

import android.content.Context
import android.content.Intent

object AppRepository {
    private var cache: List<AppInfo>? = null

    fun invalidate() { cache = null }

    fun getInstalledApps(context: Context): List<AppInfo> {
        cache?.let { return it }
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppInfo(
                    packageName = ri.activityInfo.packageName,
                    label       = ri.loadLabel(pm).toString(),
                )
            }
            .sortedBy { it.label.lowercase() }
        cache = result
        return result
    }
}
