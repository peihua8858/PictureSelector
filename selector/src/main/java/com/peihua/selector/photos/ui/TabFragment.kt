package com.peihua.selector.photos.ui

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.fz.common.utils.getScreenWidth
import com.fz.common.view.utils.dp
import com.fz.common.view.utils.pxToDp
import com.peihua.photopicker.R
import com.peihua.selector.data.Selection
import com.peihua.selector.photos.PhotoPickerActivity
import com.peihua.selector.viewmodel.PickerViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * The base abstract Tab fragment
 */
abstract class TabFragment : Fragment() {
    protected abstract val mPickerViewModel: PickerViewModel

    protected val mSelection: Selection by lazy { mPickerViewModel.selection }

    protected var mRecyclerView: RecyclerPreloadView? = null
    private var mEmptyView: View? = null
    private var mEmptyTextView: TextView? = null
    private var mIsAccessibilityEnabled = false
    private var mAddButton: Button? = null
    private var mBottomBar: View? = null
    private val mSlideUpAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            context,
            R.anim.picker_slide_up
        )
    }
    private val mSlideDownAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            context,
            R.anim.picker_slide_down
        )
    }

    private var mRecyclerViewBottomPadding = 0
    private val mIsBottomBarVisible = MutableLiveData(false)
    private val mIsProfileButtonVisible = MutableLiveData(false)
    open val isEnabledLoadMore: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.picker_fragment_picker_tab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = context
        mRecyclerView = view.findViewById(R.id.picker_tab_recyclerview)
        mEmptyView = view.findViewById(R.id.empty)
        mEmptyTextView = view.findViewById(R.id.empty_text_view)
        mAddButton = requireActivity().findViewById(R.id.button_add)
        mBottomBar = requireActivity().findViewById(R.id.picker_bottom_bar)
        mRecyclerView?.setHasFixedSize(true)
        mRecyclerView?.isEnabledLoadMore = isEnabledLoadMore
        mRecyclerView?.setOnRecyclerViewPreloadListener { onLoadMore() }
        mRecyclerViewBottomPadding = resources.getDimensionPixelSize(
            R.dimen.picker_recycler_view_bottom_padding
        )
        mIsBottomBarVisible.observe(this) { updateRecyclerViewBottomPadding() }
        mIsProfileButtonVisible.observe(this) { updateRecyclerViewBottomPadding() }
        val canSelectMultiple = mSelection.canSelectMultiple()
        if (canSelectMultiple) {
            mAddButton?.setOnClickListener { (activity as? PhotoPickerActivity)?.setResultAndFinishSelf() }
            val viewSelectedButton =
                requireActivity().findViewById<Button>(R.id.button_view_selected)
            // Transition to PreviewFragment on clicking "View Selected".
            viewSelectedButton.setOnClickListener {
                mSelection.prepareSelectedItemsForPreviewAll()
                PreviewFragment.show(
                    requireActivity().supportFragmentManager,
                    PreviewFragment.argsForPreviewOnViewSelected
                )
            }
            mSelection.selectedItemCount.observe(this) { selectedItemListSize: Int ->
                updateVisibilityAndAnimateBottomBar(selectedItemListSize)
            }
        }

        val accessibilityManager =
            context?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        mIsAccessibilityEnabled = accessibilityManager.isEnabled
        accessibilityManager.addAccessibilityStateChangeListener { enabled: Boolean ->
            mIsAccessibilityEnabled = enabled
        }
    }

    open fun onLoadMore() {

    }

    private fun updateRecyclerViewBottomPadding() {
        val recyclerViewBottomPadding: Int =
            if (mIsProfileButtonVisible.value!! || mIsBottomBarVisible.value!!) {
                mRecyclerViewBottomPadding
            } else {
                0
            }
        mRecyclerView?.setPadding(0, 0, 0, recyclerViewBottomPadding)
    }

    private fun updateVisibilityAndAnimateBottomBar(selectedItemListSize: Int) {
        if (!mSelection.canSelectMultiple()) {
            return
        }
        mBottomBar?.apply {
            if (selectedItemListSize == 0) {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                    startAnimation(mSlideDownAnimation)
                }
            } else {
                if (visibility == View.GONE) {
                    visibility = View.VISIBLE
                    startAnimation(mSlideUpAnimation)
                }
                mAddButton?.text = generateAddButtonString(context, selectedItemListSize)
            }

        }
        mIsBottomBarVisible.value = selectedItemListSize > 0
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecyclerView?.clearOnScrollListeners()
    }


    protected fun setEmptyMessage(resId: Int) {
        mEmptyTextView?.setText(resId)
    }

    /**
     * If we show the [.mEmptyView], hide the [.mRecyclerView]. If we don't hide the
     * [.mEmptyView], show the [.mRecyclerView]
     */
    protected fun updateVisibilityForEmptyView(shouldShowEmptyView: Boolean) {
        mEmptyView?.isVisible = shouldShowEmptyView
        mRecyclerView?.isGone = shouldShowEmptyView
    }

    companion object {
        private fun generateAddButtonString(context: Context?, size: Int): String {
            val sizeString = NumberFormat.getInstance(Locale.getDefault()).format(size.toLong())
            val template = context?.getString(R.string.picker_add_button_multi_select)
            return TextUtils.expandTemplate(template, sizeString).toString()
        }
    }

    val spanCount: Int
        get() {
            val screenWidth = requireActivity().getScreenWidth().pxToDp()
            when {
                screenWidth > 840 -> {
                    return 12
                }

                screenWidth > 600 -> {
                    return 6
                }
            }
            return PhotosTabAdapter.COLUMN_COUNT
        }
}