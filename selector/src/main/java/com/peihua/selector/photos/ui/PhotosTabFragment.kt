package com.peihua.selector.photos.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.fz.common.collections.isNonEmpty
import com.fz.common.model.ViewModelState
import com.google.android.material.snackbar.Snackbar
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.Item
import com.peihua.selector.photos.PhotoPickerActivity
import com.peihua.selector.util.LayoutModeUtils
import com.peihua.selector.util.isAtLeastPie
import com.peihua.selector.util.isAtLeastR
import com.peihua.selector.util.requestPermissionsDsl
import com.peihua.selector.viewmodel.PickerViewModel

/**
 * Photos tab fragment for showing the photos
 */
class PhotosTabFragment : TabFragment() {
    private var mCategory = Category.DEFAULT
    private var mAdapter: PhotosTabAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After the configuration is changed, if the fragment is now shown, onViewCreated will not
        // be triggered. We need to restore the savedInstanceState in onCreate.
        // E.g. Click the albums -> preview one item -> rotate the device
        if (savedInstanceState != null) {
            mCategory = Category.fromBundle(savedInstanceState)
        }
    }

    override val mPickerViewModel: PickerViewModel
        get() = ViewModelProvider(requireActivity())[PickerViewModel::class.java]


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = PhotosTabAdapter(
            mSelection, resources.displayMetrics.widthPixels,
            { v: View -> onItemClick(v) }) { v: View -> onItemLongClick(v) }
        setEmptyMessage(R.string.picker_photos_empty_message)
        if (mCategory.isDefault) {
            mPickerViewModel.items.observe(this) {
                result(it)
            }
        } else {
            mPickerViewModel.categoryItems.observe(this) {
                result(it)
            }
        }
        if (isAtLeastPie) {
            // Set the pane title for A11y
            view.accessibilityPaneTitle = mCategory.getDisplayName(context)
        }

        this.requestPermissionsDsl(mPickerViewModel.configModel) {
            onGranted {
                mPickerViewModel.requestMediasAsync(1, mCategory)
            }
            onDenied {
                updateVisibilityForEmptyView( /* shouldShowEmptyView */true)
            }
        }

        val layoutManager = GridLayoutManager(context, PhotosTabAdapter.COLUMN_COUNT)
        val lookup = mAdapter!!.createSpanSizeLookup(layoutManager)
        layoutManager.spanSizeLookup = lookup
        val itemDecoration = PhotosTabItemDecoration(view.context)
        mRecyclerView?.apply {
            this.layoutManager = layoutManager
            this.adapter = mAdapter
            addItemDecoration(itemDecoration)
        }
    }

    private fun result(it: ViewModelState<List<Item>>) {
        if (it.isSuccess()) {
            val items = it.data
            if (items.isNonEmpty()) {
                mAdapter?.updateItemList(items)
                // Handle emptyView's visibility
            }
            updateVisibilityForEmptyView(items.isNullOrEmpty())
        } else if (it.isError()) {
            updateVisibilityForEmptyView(true)
        }
    }

    /**
     * Called when owning activity is saving state to be used to restore state during creation.
     *
     * @param state Bundle to save state
     */
    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        mCategory.toBundle(state)
    }

    override fun onResume() {
        super.onResume()
        (activity as PhotoPickerActivity?)?.apply {
            if (mCategory.isDefault) {
                updateCommonLayouts(LayoutModeUtils.MODE_PHOTOS_TAB,  /* title */"")
            } else {
                updateCommonLayouts(LayoutModeUtils.MODE_ALBUM_PHOTOS_TAB, mCategory.getDisplayName(context))
            }
        }
    }

    private fun onItemClick(view: View) {
        if (mSelection.canSelectMultiple()) {
            val isSelectedBefore = view.isSelected
            if (isSelectedBefore) {
                mSelection.removeSelectedItem(view.tag as Item)
            } else {
                if (!mSelection.isSelectionAllowed) {
                    val maxCount = mSelection.maxSelectionLimit
                    val message = getString(
                        if (maxCount > 1) R.string.picker_select_up_to else R.string.picker_select_up_to_single,
                        maxCount
                    )
                    Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                    return
                } else {
                    mSelection.addSelectedItem(view.tag as Item)
                }
            }
            view.isSelected = !isSelectedBefore
            // There is an issue b/223695510 about not selected in Accessibility mode. It only says
            // selected state, but it doesn't say not selected state. Add the not selected only to
            // avoid that it says selected twice.
            if (isAtLeastR) {
                view.stateDescription = if (isSelectedBefore) getString(R.string.picker_not_selected) else null
            }
        } else {
            val item = view.tag as Item
            mSelection.setSelectedItem(item)
            (activity as PhotoPickerActivity?)?.setResultAndFinishSelf()
        }
    }

    private fun onItemLongClick(view: View): Boolean {
        val item = view.tag as Item
        if (!mSelection.canSelectMultiple()) {
            // In single select mode, if the item is previewed, we set it as selected item. This is
            // will assist in "Add" button click to return all selected items.
            // For multi select, long click only previews the item, and until user selects the item,
            // it doesn't get added to selected items. Also, there is no "Add" button in the preview
            // layout that can return selected items.
            mSelection.setSelectedItem(item)
        }
        mSelection.prepareItemForPreviewOnLongPress(item)
        // Transition to PreviewFragment.
        PreviewFragment.show(
            requireActivity().supportFragmentManager,
            PreviewFragment.argsForPreviewOnLongPress
        )
        return true
    }

    companion object {
        private const val FRAGMENT_TAG = "PhotosTabFragment"

        /**
         * Create the fragment with the category and add it into the FragmentManager
         *
         * @param fm the fragment manager
         * @param category the category
         */
        @JvmStatic
        fun show(fm: FragmentManager, category: Category) {
            val ft = fm.beginTransaction()
            val fragment = PhotosTabFragment()
            fragment.mCategory = category
            ft.replace(R.id.fragment_container, fragment, FRAGMENT_TAG)
            if (!fragment.mCategory.isDefault) {
                ft.addToBackStack(FRAGMENT_TAG)
            }
            ft.commitAllowingStateLoss()
        }

        /**
         * Get the fragment in the FragmentManager
         *
         * @param fm The fragment manager
         */
        operator fun get(fm: FragmentManager): Fragment? {
            return fm.findFragmentByTag(FRAGMENT_TAG)
        }
    }
}