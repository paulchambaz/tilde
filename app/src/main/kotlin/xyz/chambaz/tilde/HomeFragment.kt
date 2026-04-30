package xyz.chambaz.tilde

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlinx.coroutines.launch
import xyz.chambaz.tilde.data.Favorite
import xyz.chambaz.tilde.data.LauncherPrefs
import xyz.chambaz.tilde.data.PrefsRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var rvFavorites: RecyclerView
    private lateinit var adapter: FavoritesAdapter
    private lateinit var gesture: GestureDetector
    private var currentPrefs = LauncherPrefs()

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM")

    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            tvTime.text = LocalTime.now().format(timeFmt)
            tvDate.text = LocalDate.now().format(dateFmt)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvTime = view.findViewById(R.id.tvTime)
        tvDate = view.findViewById(R.id.tvDate)
        rvFavorites = view.findViewById(R.id.rvFavorites)

        adapter = FavoritesAdapter(emptyList()) { pkg -> launch(pkg) }
        rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        rvFavorites.adapter = adapter

        tvTime.setOnClickListener { launch(currentPrefs.timeTapPackage) }
        tvDate.setOnClickListener { launch(currentPrefs.dateTapPackage) }

        gesture = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) <= abs(dy)) return false
                if (dx < 0) launch(currentPrefs.swipeLeftPackage)
                else launch(currentPrefs.swipeRightPackage)
                return true
            }
        })
        view.setOnTouchListener { v, event ->
            gesture.onTouchEvent(event)
            v.performClick()
            true
        }

        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        var interceptStartX = 0f
        var interceptStartY = 0f
        var intercepting = false
        rvFavorites.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (!intercepting) gesture.onTouchEvent(e)
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        interceptStartX = e.x; interceptStartY = e.y; intercepting = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(e.x - interceptStartX)
                        val dy = abs(e.y - interceptStartY)
                        if (!intercepting && dx > touchSlop && dx > dy) intercepting = true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> intercepting = false
                }
                return intercepting
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { gesture.onTouchEvent(e) }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* launcher stays on home */ }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            PrefsRepository.prefs.collect { prefs ->
                currentPrefs = prefs
                adapter.update(prefs.favorites)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
    }

    private fun launch(packageName: String) {
        if (packageName.isEmpty()) return
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }
}

private class FavoritesAdapter(
    private var items: List<Favorite>,
    private val onTap: (String) -> Unit,
) : RecyclerView.Adapter<FavoritesAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(TextView(parent.context).apply {
            textSize = 32f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(parent.context, R.color.text))
            setPadding(48, 8, 48, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        })

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val installed = item.packageName.isEmpty() ||
            holder.tv.context.packageManager.getLaunchIntentForPackage(item.packageName) != null
        holder.tv.text = if (installed) item.displayLabel else ""
        holder.tv.setOnClickListener { onTap(item.packageName) }
    }

    fun update(newItems: List<Favorite>) {
        items = newItems
        notifyDataSetChanged()
    }
}
