package xyz.chambaz.tilde

import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.chambaz.tilde.data.AppInfo
import xyz.chambaz.tilde.data.AppRepository
import xyz.chambaz.tilde.data.LauncherPrefs
import xyz.chambaz.tilde.data.PrefsRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var llContent: LinearLayout
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var swipeGesture: GestureDetector

    override fun onStop() {
        super.onStop()
        if (!isFinishing) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        llContent = findViewById(R.id.llContent)
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        swipeGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) <= abs(dy) || dx <= 0) return false
                finish()
                return true
            }
        })

        lifecycleScope.launch(Dispatchers.IO) {
            val apps = AppRepository.getInstalledApps(this@SettingsActivity)
            withContext(Dispatchers.Main) {
                allApps = apps
                launch {
                    PrefsRepository.prefs.collect { prefs -> rebuildRows(prefs, allApps) }
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        swipeGesture.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun rebuildRows(prefs: LauncherPrefs, apps: List<AppInfo>) {
        llContent.removeAllViews()
        llContent.addView(buildFavoriteCountRow(prefs.favoriteCount))
        repeat(prefs.favoriteCount) { slot ->
            llContent.addView(buildFavoriteSlotRow(slot, prefs, apps))
        }
        llContent.addView(spacer())
        llContent.addView(buildGestureRow("Tap time", prefs.timeTapPackage, apps) { PrefsRepository.updateTimeTap(it) })
        llContent.addView(buildGestureRow("Tap date", prefs.dateTapPackage, apps) { PrefsRepository.updateDateTap(it) })
        llContent.addView(spacer())
        llContent.addView(buildGestureRow("Swipe left", prefs.swipeLeftPackage, apps) { PrefsRepository.updateSwipeLeft(it) })
        llContent.addView(buildGestureRow("Swipe right", prefs.swipeRightPackage, apps) { PrefsRepository.updateSwipeRight(it) })
    }

    private fun buildFavoriteCountRow(count: Int): LinearLayout {
        val row = hRow()
        row.addView(TextView(this).apply {
            text = "Favorites"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
            setPadding(48, 16, 48, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(flatBtn("−").apply {
            setOnClickListener { if (count > 1) lifecycleScope.launch { PrefsRepository.updateFavoriteCount(count - 1) } }
        })
        row.addView(TextView(this).apply {
            text = count.toString()
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
            setPadding(16, 16, 16, 16)
        })
        row.addView(flatBtn("+").apply {
            setOnClickListener { lifecycleScope.launch { PrefsRepository.updateFavoriteCount(count + 1) } }
        })
        return row
    }

    private fun buildFavoriteSlotRow(slot: Int, prefs: LauncherPrefs, apps: List<AppInfo>): LinearLayout {
        val fav = prefs.favorites.find { it.slot == slot }
        var rowPkg = fav?.packageName ?: ""

        val row = hRow()
        row.addView(TextView(this).apply {
            text = "${slot + 1}."
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
            setPadding(48, 16, 16, 16)
        })

        val et = EditText(this).apply {
            setText(fav?.displayLabel ?: "")
            hint = "label"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
            setHintTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text_hint))
            background = null
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) lifecycleScope.launch {
                    PrefsRepository.updateFavorite(slot, rowPkg, text.toString())
                }
            }
        }
        row.addView(et)

        val btn = flatBtn(apps.firstOrNull { it.packageName == rowPkg }?.label ?: "none")
        btn.setOnClickListener {
            AppPickerDialog.show(supportFragmentManager) { pkg ->
                rowPkg = pkg
                btn.text = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                lifecycleScope.launch { PrefsRepository.updateFavorite(slot, pkg, et.text.toString()) }
            }
        }
        row.addView(btn)

        return row
    }

    private fun buildGestureRow(label: String, pkg: String, apps: List<AppInfo>, onPick: suspend (String) -> Unit): LinearLayout {
        val row = hRow()
        row.addView(TextView(this).apply {
            text = label
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
            setPadding(48, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val btn = flatBtn(apps.firstOrNull { it.packageName == pkg }?.label ?: "none")
        btn.setOnClickListener {
            AppPickerDialog.show(supportFragmentManager) { picked ->
                btn.text = apps.firstOrNull { it.packageName == picked }?.label ?: picked
                lifecycleScope.launch { onPick(picked) }
            }
        }
        row.addView(btn)
        return row
    }

    private fun hRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun spacer() = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 48)
    }

    private fun flatBtn(label: String) = Button(this).apply {
        text = label
        textSize = 16f
        setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.text))
        background = null
        setPadding(16, 16, 48, 16)
    }
}
