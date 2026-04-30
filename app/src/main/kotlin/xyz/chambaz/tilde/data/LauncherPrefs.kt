package xyz.chambaz.tilde.data

data class LauncherPrefs(
    val favoriteCount: Int = 4,
    val favorites: List<Favorite> = emptyList(),
    val timeTapPackage: String = "",
    val dateTapPackage: String = "",
    val swipeLeftPackage: String = "",
    val swipeRightPackage: String = "",
)
