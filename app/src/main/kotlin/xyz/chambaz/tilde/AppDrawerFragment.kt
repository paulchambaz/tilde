package xyz.chambaz.tilde

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import kotlin.math.abs
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.chambaz.tilde.data.AppInfo
import xyz.chambaz.tilde.data.AppRepository

class AppDrawerFragment : Fragment() {

    internal lateinit var etSearch: EditText
    internal lateinit var rvApps: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: AppsAdapter
    private var allApps: List<AppInfo> = emptyList()
    private var visibleApps: List<AppInfo> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_app_drawer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etSearch = view.findViewById(R.id.etSearch)
        rvApps = view.findViewById(R.id.rvApps)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        val btnSettings = view.findViewById<ImageButton>(R.id.btnSettings)

        adapter = AppsAdapter(emptyList()) { pkg -> launch(pkg) }
        rvApps.layoutManager = LinearLayoutManager(requireContext())
        rvApps.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                visibleApps = filterApps(s?.toString() ?: "", allApps)
                adapter.update(visibleApps)
            }
        })

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                visibleApps.firstOrNull()?.let { launch(it.packageName) }
                true
            } else false
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        val openSettings = { startActivity(Intent(requireContext(), SettingsActivity::class.java)) }
        val swipeGesture = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) <= abs(dy) || dx >= 0) return false
                openSettings()
                return true
            }
        })
        view.setOnTouchListener { _, event -> swipeGesture.onTouchEvent(event); false }

        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        var interceptStartX = 0f
        var interceptStartY = 0f
        var intercepting = false
        rvApps.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (!intercepting) swipeGesture.onTouchEvent(e)
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
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { swipeGesture.onTouchEvent(e) }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as? MainActivity)?.scrollToPage(0)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val apps = AppRepository.getInstalledApps(requireContext())
            withContext(Dispatchers.Main) {
                allApps = apps
                visibleApps = apps
                adapter.update(apps)
                tvEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        etSearch.text.clear()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun launch(packageName: String) {
        if (packageName.isEmpty()) return
        val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName) ?: return
        (requireActivity() as? MainActivity)?.hideKeyboard()
        etSearch.text.clear()
        (requireActivity() as? MainActivity)?.pager?.setCurrentItem(0, false)
        startActivity(intent)
    }
}

private class AppsAdapter(
    private var items: List<AppInfo>,
    private val onTap: (String) -> Unit,
) : RecyclerView.Adapter<AppsAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(TextView(parent.context).apply {
            textSize = 32f
            setTextColor(ContextCompat.getColor(parent.context, R.color.text))
            setPadding(48, 8, 48, 8)
        })

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.tv.text = app.label
        holder.tv.setOnClickListener { onTap(app.packageName) }
    }

    fun update(newItems: List<AppInfo>) {
        items = newItems
        notifyDataSetChanged()
    }
}
