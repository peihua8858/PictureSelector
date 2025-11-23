package com.peihua8858.selector.picker.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.peihua8858.permisstions.fragment.requestPermissions
import com.peihua8858.selector.android.R
import com.peihua8858.selector.picker.PhotoPickerActivity
import com.peihua8858.selector.picker.model.Category
import com.peihua8858.selector.picker.viewmodel.PickerViewModel
import com.peihua8858.selector.utils.LayoutModeUtils
import com.peihua8858.selector.utils.getPermissions
import com.peihua8858.tools.collections.isNonEmpty
import com.peihua8858.selector.picker.fragment.PhotosTabFragment.Companion.show

/**
 * Albums tab fragment for showing the albums
 */
class AlbumsTabFragment : TabFragment() {
    override val mPickerViewModel: PickerViewModel
        get() {
            return ViewModelProvider(requireActivity())[PickerViewModel::class.java]
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set the pane title for A11y.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.accessibilityPaneTitle = getString(com.peihua8858.selector.R.string.picker_albums)
        }
        setEmptyMessage(com.peihua8858.selector.R.string.picker_albums_empty_message)
        val adapter = AlbumsTabAdapter( { v: View -> onItemClick(v) },
            mPickerViewModel.hasMimeTypeFilters()
        )
        mPickerViewModel.categories.observe(this) {
            if (it.isSuccess) {
                val categoryList = it.result
                if (categoryList.isNonEmpty()) {
                    adapter.updateCategoryList(categoryList)
                }
                // Handle emptyView's visibility
                updateVisibilityForEmptyView(categoryList.isNullOrEmpty())
            } else if (it.isError) {
                updateVisibilityForEmptyView(true)
            }
        }
        this.requestPermissions(*getPermissions(mPickerViewModel.mMimeTypeFilters)) {
            onGranted {
                mPickerViewModel.requestCategories()
            }
            onDenied {
                updateVisibilityForEmptyView(true)
            }
        }
        val layoutManager = GridLayoutManager(context,spanCount)
        val itemDecoration = AlbumsTabItemDecoration(view.context)
        adapter.createSpanSizeLookup(layoutManager)
        mRecyclerView?.apply {
           val spacing = resources.getDimensionPixelSize(com.peihua8858.selector.R.dimen.picker_album_item_spacing)
            val albumSize = resources.getDimensionPixelSize(com.peihua8858.selector.R.dimen.picker_album_size)
            setColumnWidth(albumSize + spacing)
            setMinimumSpanCount(spanCount)
            this.layoutManager = layoutManager
            this.adapter = adapter
            addItemDecoration(itemDecoration)
            setReachBottomRow(spanCount)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as PhotoPickerActivity?)?.updateCommonLayouts(LayoutModeUtils.MODE_ALBUMS_TAB,  /* title */ "")
    }

    private fun onItemClick(view: View) {
        val category: Category = view.tag as Category
        show(requireActivity().supportFragmentManager, category)
    }

    companion object {
        /**
         * Create the albums tab fragment and add it into the FragmentManager
         *
         * @param fm The fragment manager
         */
        fun show(fm: FragmentManager) {
            val ft = fm.beginTransaction()
            val fragment = AlbumsTabFragment()
            ft.replace(R.id.fragment_container, fragment)
            ft.commitAllowingStateLoss()
        }
    }
}