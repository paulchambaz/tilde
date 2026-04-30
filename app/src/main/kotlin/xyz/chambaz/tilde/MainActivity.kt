package xyz.chambaz.tilde

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import xyz.chambaz.tilde.data.PrefsRepository
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    internal lateinit var pager: ViewPager2

    private val velocityTracker = VelocityTracker.obtain()
    private var isDragging = false
    private var isHorizontalDrag = false
    private var pointerDownY = 0f
    private var pointerDownX = 0f
    private var lastEventY = 0f
    private var settleAnimator: ValueAnimator? = null
    private var currentScrollPosition = 0f
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsRepository.init(this)

        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_VERTICAL
            overScrollMode = View.OVER_SCROLL_NEVER
            isUserInputEnabled = false
        }
        setContentView(pager)
        pager.adapter = LauncherPagerAdapter(this)
        pager.post {
            (pager.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                currentScrollPosition = position + positionOffset
            }
        })
    }

    override fun onResume() {
        super.onResume()
        promptDefaultLauncher()
    }

    override fun onDestroy() {
        super.onDestroy()
        velocityTracker.recycle()
    }

    internal fun scrollToPage(page: Int) {
        settleAnimator?.cancel()
        if (pager.isFakeDragging) pager.endFakeDrag()
        animateToPage(page)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                settleAnimator?.cancel()
                velocityTracker.clear()
                velocityTracker.addMovement(ev)
                pointerDownY = ev.y
                pointerDownX = ev.x
                lastEventY = ev.y
                isDragging = false
                isHorizontalDrag = false
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker.addMovement(ev)
                val dy = ev.y - lastEventY
                lastEventY = ev.y

                if (!isDragging && !isHorizontalDrag) {
                    val totalDy = abs(ev.y - pointerDownY)
                    val totalDx = abs(ev.x - pointerDownX)
                    if (maxOf(totalDy, totalDx) > touchSlop) {
                        if (totalDy > totalDx * 1.5f && shouldInterceptVertical(ev.y - pointerDownY)) {
                            isDragging = true
                            if (!pager.isFakeDragging) pager.beginFakeDrag()
                            hideKeyboard()
                            val cancel = MotionEvent.obtain(ev).also { it.action = MotionEvent.ACTION_CANCEL }
                            super.dispatchTouchEvent(cancel)
                            cancel.recycle()
                        } else {
                            isHorizontalDrag = true
                        }
                    }
                }

                if (isDragging) {
                    pager.fakeDragBy(dy)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker.addMovement(ev)
                if (isDragging) {
                    isDragging = false
                    velocityTracker.computeCurrentVelocity(1000)
                    val vy = velocityTracker.yVelocity
                    settleAfterDrag(vy)
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun settleAfterDrag(velocityY: Float) {
        val pageCount = pager.adapter?.itemCount ?: 1
        val flingThreshold = 800f
        val scrollPos = currentScrollPosition
        val currentPage = scrollPos.toInt().coerceIn(0, pageCount - 1)
        val offset = scrollPos - currentPage
        val snappedPage = if (offset >= 0.5f) (currentPage + 1).coerceAtMost(pageCount - 1) else currentPage

        val targetPage = when {
            velocityY < -flingThreshold -> (currentPage + 1).coerceAtMost(pageCount - 1)
            velocityY > flingThreshold -> {
                val going = (snappedPage - 1).coerceAtLeast(0)
                if (going == 0 && scrollPos < 0.1f) {
                    if (pager.isFakeDragging) pager.endFakeDrag()
                    expandNotifications()
                    return
                }
                going
            }
            else -> snappedPage
        }

        animateToPage(targetPage)
    }

    private fun animateToPage(targetPage: Int) {
        val h = pager.height.toFloat().takeIf { it > 0 } ?: return
        val delta = (targetPage - currentScrollPosition) * h
        if (abs(delta) < 1f) {
            if (pager.isFakeDragging) pager.endFakeDrag()
            return
        }
        if (!pager.isFakeDragging) pager.beginFakeDrag()
        var last = 0f
        settleAnimator = ValueAnimator.ofFloat(0f, delta).apply {
            duration = 350
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                pager.fakeDragBy(-(value - last))
                last = value
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (pager.isFakeDragging) pager.endFakeDrag()
                }
                override fun onAnimationCancel(animation: Animator) {
                    if (pager.isFakeDragging) pager.endFakeDrag()
                }
            })
            start()
        }
    }

    private fun shouldInterceptVertical(dy: Float): Boolean {
        if (pager.currentItem == 0) return true
        val drawer = supportFragmentManager.fragments.filterIsInstance<AppDrawerFragment>().firstOrNull()
            ?: return true
        return if (dy > 0) !drawer.rvApps.canScrollVertically(-1)  // drag down: only if list is at top
        else !drawer.rvApps.canScrollVertically(1)                  // drag up: only if list is at bottom
    }

    internal fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    @Suppress("DiscouragedPrivateApi")
    fun expandNotifications() {
        val sbm = getSystemService("statusbar") ?: return
        sbm.javaClass.getMethod("expandNotificationsPanel").invoke(sbm)
    }

    private fun promptDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (!rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
            }
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val res = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (res?.activityInfo?.packageName != packageName) {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }
    }
}

private class LauncherPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> HomeFragment()
        else -> AppDrawerFragment()
    }
}
