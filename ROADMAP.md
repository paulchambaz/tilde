# Tilde Launcher — Roadmap

Minimalistic Android launcher. Three-page vertical swipe: notifications (up),
home (center), app drawer (down). No bloat.

---

## Phase 1 — Scaffold

Set up project bones: launcher registration, ViewPager2, empty fragments,
navigation wired.

### 1.1 Dependencies

- [x] Add to `app/build.gradle.kts`:

```kotlin
implementation("androidx.viewpager2:viewpager2:1.1.0")
implementation("androidx.fragment:fragment-ktx:1.8.0")
implementation("androidx.datastore:datastore-preferences:1.1.1")
```

### 1.2 Manifest

- [x] Add `HOME` + `DEFAULT` intent-filter to `MainActivity`
- [x] Add permission `android.permission.EXPAND_STATUS_BAR`
- [x] Add permission `android.permission.QUERY_ALL_PACKAGES` (needed to list
  installed apps on API 30+)
- [x] Register `SettingsActivity`

### 1.3 Theme

- [x] `res/values/themes.xml` — full-screen, no status bar, no action bar, black
  background, white text. Pure AMOLED.

### 1.4 Fragment skeleton

Create two empty fragments:

- [x] `HomeFragment` (page 0)
- [x] `AppDrawerFragment` (page 1)

Note: `NotificationsFragment` removed — swiping up stays on the home screen
and opens the system notification shade as an overlay, not a launcher page.

### 1.5 MainActivity wiring

- [x] `ViewPager2` (vertical orientation) as sole content view
- [x] `LauncherPagerAdapter : FragmentStateAdapter` returning the two fragments
- [x] `currentItem = 1` on start (home); drawer at page 0, home at page 1
- [x] Upward fling on home (page 1, vy < 0) detected via `GestureDetector` in
  `dispatchTouchEvent` → expand system notification shade as overlay
- [x] Swipe down from home → app drawer (page 0)

**Deliverable**: app installs as launcher, two swipeable pages, swipe up from
home opens notification shade as system overlay without leaving home screen.

---

## Phase 2 — Data Layer

All persisted state lives here. No UI yet.

### 2.1 Data classes

- [x] `data/AppInfo.kt`
```kotlin
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)
```

- [x] `data/Favorite.kt`
```kotlin
data class Favorite(
    val slot: Int,           // 0-indexed position
    val packageName: String,
    val displayLabel: String, // user-renamed label; defaults to app label
)
```

- [x] `data/LauncherPrefs.kt`
```kotlin
data class LauncherPrefs(
    val favoriteCount: Int = 4,
    val favorites: List<Favorite> = emptyList(),
    val timeTapPackage: String = "",
    val dateTapPackage: String = "",
    val swipeLeftPackage: String = "",
    val swipeRightPackage: String = "",
)
```

### 2.2 PrefsRepository

- [x] `data/PrefsRepository.kt` — singleton wrapping `DataStore<Preferences>`

Keys:
- `FAVORITE_COUNT` (Int)
- `FAVORITE_{n}_PACKAGE` (String) for n in 0..MAX_FAVORITES
- `FAVORITE_{n}_LABEL` (String)
- `TIME_TAP_PACKAGE` (String)
- `DATE_TAP_PACKAGE` (String)
- `SWIPE_LEFT_PACKAGE` (String)
- `SWIPE_RIGHT_PACKAGE` (String)

Expose:
```kotlin
val prefs: Flow<LauncherPrefs>
suspend fun updateFavoriteCount(n: Int)
suspend fun updateFavorite(slot: Int, packageName: String, label: String)
suspend fun updateTimeTap(packageName: String)
suspend fun updateDateTap(packageName: String)
suspend fun updateSwipeLeft(packageName: String)
suspend fun updateSwipeRight(packageName: String)
```

### 2.3 AppRepository

- [x] `data/AppRepository.kt` — queries `PackageManager` for installed launchable
  apps, sorted alphabetically by label
- [x] Cache result in memory; invalidate on `ACTION_PACKAGE_ADDED` /
  `ACTION_PACKAGE_REMOVED` broadcast

```kotlin
fun getInstalledApps(context: Context): List<AppInfo>
```

**Deliverable**: data layer complete, unit-testable in isolation.

---

## Phase 3 — Home Screen

### 3.1 Layout

- [x] `fragment_home.xml` — `ConstraintLayout` root, black background
- [x] `TextView` id `tvTime` — large, centered, top half
- [x] `TextView` id `tvDate` — medium, centered, below time
- [x] `RecyclerView` id `rvFavorites` — bottom third, vertical list

### 3.2 Clock

- [x] `Handler` posting every second, formats with `DateTimeFormatter`
- [x] Tap on `tvTime` launches `prefs.timeTapPackage`

### 3.3 Date

- [x] Tap on `tvDate` launches `prefs.dateTapPackage`

### 3.4 Favorites

- [x] `FavoritesAdapter : RecyclerView.Adapter` — one `TextView` per slot showing
  `Favorite.displayLabel`
- [x] Tap launches the app
- [x] Count driven by `prefs.favoriteCount`

### 3.5 Left / right swipe

- [x] `GestureDetectorCompat` on `HomeFragment` root view — `onFling` left/right →
  launch configured package
- [x] Intercept horizontal flings before `ViewPager2` gets them (no conflict with
  vertical paging)

**Deliverable**: working home screen — live clock, date, tappable favorites,
swipe gestures.

---

## Phase 4 — App Drawer

### 4.1 Layout

- [x] `fragment_app_drawer.xml` — `ConstraintLayout` root
- [x] `EditText` id `etSearch` — top, full width, hint "Search…"
- [x] `RecyclerView` id `rvApps` — fills remaining space
- [x] `ImageButton` id `btnSettings` — top-right corner, gear icon

### 4.2 App list

- [x] `AppsAdapter : RecyclerView.Adapter` — each item: label only (no icon)
- [x] Tap launches app
- [x] Load from `AppRepository`, show full list by default

### 4.3 Fuzzy search

- [x] `etSearch` `TextWatcher` → filter list on each keystroke
- [x] `AppFilter.kt` — subsequence match (keep apps whose label contains all
  characters of the query in order), ranked by how early the match starts
- [x] Blank query → show all

### 4.4 Keyboard behavior

- [x] Drawer opens → `etSearch` does **not** auto-focus
- [x] Back from drawer → return to home (page 1)

### 4.5 Settings button

- [x] `btnSettings` tap → `startActivity(Intent(context, SettingsActivity::class.java))`

**Deliverable**: fully functional app drawer with fuzzy search.

---

## Phase 5 — Settings

### 5.1 SettingsActivity

- [x] Plain `AppCompatActivity` with scrollable layout, no `PreferenceFragment`

### 5.2 Favorites count

- [x] `+` / `−` buttons, minimum 1, no upper cap
- [x] Changing count adds/removes slots immediately

### 5.3 Favorite slots

- [x] One row per slot: slot number | display label (`EditText`) | app picker button
- [x] App picker opens `AppPickerDialog`

### 5.4 Gesture & tap targets

- [x] Row for time tap → `timeTapPackage`
- [x] Row for date tap → `dateTapPackage`
- [x] Row for swipe left → `swipeLeftPackage`
- [x] Row for swipe right → `swipeRightPackage`

### 5.5 AppPickerDialog

- [x] Full-screen `DialogFragment` reusing `AppsAdapter` + fuzzy filter from Phase 4
- [x] Returns `packageName` to caller via callback interface

### 5.6 Persistence

- [x] Every change writes immediately to `PrefsRepository` (no save button)
- [x] `Flow` collectors in `HomeFragment` and `AppDrawerFragment` react automatically

**Deliverable**: complete settings screen, all prefs persisted and live.

---

## Phase 6 — Polish

### 6.1 Visual

- [x] Single font, two sizes only (time: 72sp, everything else: 16sp)
- [x] No icons on favorites — text only
- [x] No dividers in app drawer list
- [x] Settings uses same text style as rest of app

### 6.2 Animations

- [x] `ViewPager2` default swipe animation — no custom transitions
- [x] App drawer search filter — instant list update, no animation

### 6.3 Edge cases

- [x] App not installed: show slot as empty, tap does nothing, no crash
- [x] Zero installed apps: empty state label in drawer
- [x] Lock to portrait (`screenOrientation="portrait"` in manifest)
- [x] First launch defaults: `favoriteCount = 4`, all slots empty, all gesture
  targets empty

### 6.4 Back button

- [x] From app drawer → home (ViewPager2 page change)
- [x] From home → do nothing (launcher stays, system handles)
- [x] From settings → back to drawer

### 6.5 Default launcher dialog

- [x] On first launch, if not default launcher, open system "Set as default
  launcher" dialog automatically

---

## Phase 7 — Hardening

### 7.1 Broadcast receiver

- [x] `PackageChangeReceiver` in manifest for `ACTION_PACKAGE_ADDED`,
  `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_CHANGED`
- [x] Invalidates `AppRepository` cache on receive
- [x] Favorites pointing to removed packages show as empty

### 7.2 Memory

- [x] `AppRepository` loads icons lazily
- [x] `WeakReference` cache or `Coil` for icon loading — no all-icons-in-memory

### 7.3 Default launcher enforcement

- [x] Move `promptDefaultLauncher()` to `onResume` so every return to the
  launcher re-checks and re-prompts until Tilde is set as default

### 7.4 Proguard

- [x] `isMinifyEnabled = true` for release build
- [x] Add rules for DataStore

---

## Milestone summary

| Phase | Deliverable |
|---|---|
| 1 | Installs as launcher, three swipeable pages |
| 2 | Data layer: prefs + app list |
| 3 | Live clock, date, favorites, swipe gestures |
| 4 | App drawer with fuzzy search |
| 5 | Settings: favorites, labels, gesture targets |
| 6 | Visual polish, edge cases, back nav |
| 7 | Broadcast receiver, memory, release build |
