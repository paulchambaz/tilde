package xyz.chambaz.tilde

import xyz.chambaz.tilde.data.AppInfo

fun filterApps(query: String, apps: List<AppInfo>): List<AppInfo> {
    if (query.isEmpty()) return apps
    val q = query.lowercase()
    return apps.mapNotNull { app ->
        val label = app.label.lowercase()
        var li = 0
        var matchStart = -1
        for (c in q) {
            val found = label.indexOf(c, li)
            if (found == -1) return@mapNotNull null
            if (matchStart == -1) matchStart = found
            li = found + 1
        }
        app to matchStart
    }.sortedBy { it.second }.map { it.first }
}
