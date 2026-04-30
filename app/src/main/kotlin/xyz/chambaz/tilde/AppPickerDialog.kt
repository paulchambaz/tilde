package xyz.chambaz.tilde

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.chambaz.tilde.data.AppInfo
import xyz.chambaz.tilde.data.AppRepository

class AppPickerDialog : DialogFragment() {

    var onPicked: ((String) -> Unit)? = null
    private var allApps: List<AppInfo> = emptyList()
    private lateinit var adapter: AppsAdapter
    private lateinit var etSearch: EditText
    private lateinit var rvApps: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        etSearch = EditText(requireContext()).apply {
            hint = "Search…"
            textSize = 32f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text))
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
            background = null
            setPadding(48, 32, 48, 16)
        }
        rvApps = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(etSearch)
        root.addView(rvApps)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AppsAdapter(emptyList()) { pkg -> onPicked?.invoke(pkg); dismiss() }
        rvApps.layoutManager = LinearLayoutManager(requireContext())
        rvApps.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                adapter.update(filterApps(s?.toString() ?: "", allApps))
            }
        })

        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)

        lifecycleScope.launch(Dispatchers.IO) {
            allApps = AppRepository.getInstalledApps(requireContext())
            withContext(Dispatchers.Main) { adapter.update(allApps) }
        }
    }

    private inner class AppsAdapter(
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
            holder.tv.text = items[position].label
            holder.tv.setOnClickListener { onTap(items[position].packageName) }
        }

        fun update(newItems: List<AppInfo>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    companion object {
        fun show(manager: FragmentManager, onPicked: (String) -> Unit): AppPickerDialog {
            val d = AppPickerDialog()
            d.onPicked = onPicked
            d.show(manager, "app_picker")
            return d
        }
    }
}
