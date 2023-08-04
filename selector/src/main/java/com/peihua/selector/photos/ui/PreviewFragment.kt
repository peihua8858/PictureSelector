package com.peihua.selector.photos.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.fz.common.utils.getColorCompat
import com.peihua.photopicker.R
import com.peihua.photopicker.databinding.PickerFragmentPreviewBinding
import com.peihua.selector.photos.PhotoPickerActivity
import com.peihua.selector.data.MuteStatus
import com.peihua.selector.data.Selection
import com.peihua.selector.data.model.Item
import com.peihua.selector.util.LayoutModeUtils
import com.peihua.selector.viewmodel.PickerViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Displays a selected items in one up view. Supports deselecting items.
 */
class PreviewFragment : Fragment() {
    private val mViewModel by lazy { ViewModelProvider(requireActivity())[PickerViewModel::class.java] }
    private val binding by lazy { PickerFragmentPreviewBinding.bind(requireView()) }
    private val mSelection: Selection by lazy { mViewModel.selection }
    private var mViewPager2Wrapper: ViewPager2Wrapper? = null
    private var mShouldShowGifBadge = false
    private var mShouldShowMotionPhotoBadge = false
    private val mMuteStatus: MuteStatus by lazy { mViewModel.muteStatus }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Register with the activity to inform the system that the app bar fragment is
        // participating in the population of the options menu
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.picker_preview_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // All logic to hide/show an item in the menu must be in this method
        val gifItem = menu.findItem(R.id.preview_gif)
        val motionPhotoItem = menu.findItem(R.id.preview_motion_photo)
        gifItem.isVisible = mShouldShowGifBadge
        motionPhotoItem.isVisible = mShouldShowMotionPhotoBadge
    }

    override fun onCreateView(
        inflater: LayoutInflater, parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.picker_fragment_preview, parent,  /* attachToRoot */false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Set the pane title for A11y.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.accessibilityPaneTitle = getString(R.string.picker_preview)
        }
        val selectedItemsList = mSelection.selectedItemsForPreview
        val selectedItemsListSize = selectedItemsList.size
        if (selectedItemsListSize <= 0) {
            // This can happen if we lost PickerViewModel to optimize memory.
            Log.e(TAG, "No items to preview. Returning back to photo grid")
            requireActivity().supportFragmentManager.popBackStack()
        } else check(!(selectedItemsListSize > 1 && !mSelection.canSelectMultiple())) {
            // This should never happen
            ("Found more than one preview items in single select"
                    + " mode. Selected items count: " + selectedItemsListSize)
        }

        // Initialize ViewPager2 to swipe between multiple pictures/videos in preview
        val viewPager = view.findViewById<ViewPager2>(R.id.preview_viewPager)
            ?: throw IllegalStateException(
                ("Expected to find ViewPager2 in " + view
                        + ", but found null")
            )
        mViewPager2Wrapper = ViewPager2Wrapper(viewPager, selectedItemsList, mMuteStatus)
        setUpPreviewLayout(view, arguments)
        setupScrimLayerAndBottomBar(view)
    }

    private fun setupScrimLayerAndBottomBar(fragmentView: View) {
        val isLandscape = (resources.configuration.orientation
                == Configuration.ORIENTATION_LANDSCAPE)

        // Show the scrim layers in Landscape mode. The default visibility is GONE.
        if (isLandscape) {
            val topScrim = fragmentView.findViewById<View>(R.id.preview_top_scrim)
            topScrim.visibility = View.VISIBLE
            val bottomScrim = fragmentView.findViewById<View>(R.id.preview_bottom_scrim)
            bottomScrim.visibility = View.VISIBLE
        }

        // Set appropriate background color for the bottom bar
        val bottomBarColor: Int
        bottomBarColor = if (isLandscape) {
            Color.TRANSPARENT
        } else {
            getColorCompat(R.color.preview_scrim_solid_color)
        }
        val bottomBar = fragmentView.findViewById<View>(R.id.preview_bottom_bar)
        bottomBar.setBackgroundColor(bottomBarColor)
    }

    private fun setUpPreviewLayout(view: View, args: Bundle?) {
        if (args == null) {
            // We are willing to crash PhotoPickerActivity because this error might only happen
            // during development.
            throw IllegalArgumentException(
                "Can't determine the type of the Preview, arguments"
                        + " is not set"
            )
        }
        val previewType = args.getInt(PREVIEW_TYPE, -1)
        if (previewType == PREVIEW_ON_LONG_PRESS) {
            setUpPreviewLayoutForLongPress(view)
        } else if (previewType == PREVIEW_ON_VIEW_SELECTED) {
            setUpPreviewLayoutForViewSelected(view)
        } else {
            // We are willing to crash PhotoPickerActivity because this error might only happen
            // during development.
            throw IllegalArgumentException("No preview type specified")
        }
    }

    /**
     * Adjusts the select/add button layout for preview on LongPress
     */
    private fun setUpPreviewLayoutForLongPress(view: View) {
        val addOrSelectButton = view.findViewById<Button>(R.id.preview_add_or_select_button)

        // Preview on Long Press will reuse AddOrSelect button as
        // * Add button - Button with text "Add" - for single select mode
        // * Select button - Button with text "Select"/"Deselect" based on the selection state of
        //                   the item - for multi select mode
        if (!mSelection.canSelectMultiple()) {
            // On clicking add button we return the picker result to calling app.
            // This destroys PickerActivity and all fragments.
            addOrSelectButton.setOnClickListener { (activity as PhotoPickerActivity?)?.setResultAndFinishSelf() }
        } else {
            // For preview on long press, we always preview only one item.
            // Selection#getSelectedItemsForPreview is guaranteed to return only one item. Hence,
            // we can always use position=0 as current position.
            updateSelectButtonText(
                addOrSelectButton,
                mSelection.isItemSelected(mViewPager2Wrapper!!.getItemAt( /* position */0))
            )
            addOrSelectButton.setOnClickListener { onClickSelectButton(addOrSelectButton) }
        }

        // Set the appropriate special format icon based on the item in the preview
        updateSpecialFormatIcon(mViewPager2Wrapper!!.getItemAt( /* position */0))
    }

    /**
     * Adjusts the layout based on Multi select and adds appropriate onClick listeners
     */
    private fun setUpPreviewLayoutForViewSelected(view: View) {
        // Hide addOrSelect button of long press, we have a separate add button for view selected
        val addOrSelectButton = view.findViewById<Button>(R.id.preview_add_or_select_button)
        addOrSelectButton.visibility = View.GONE
        val viewSelectedAddButton = view.findViewById<Button>(R.id.preview_add_button)
        viewSelectedAddButton.visibility = View.VISIBLE
        // On clicking add button we return the picker result to calling app.
        // This destroys PickerActivity and all fragments.
        viewSelectedAddButton.setOnClickListener { (activity as PhotoPickerActivity?)?.setResultAndFinishSelf() }
        val selectedCheckButton = view.findViewById<Button>(R.id.preview_selected_check_button)
        selectedCheckButton.visibility = View.VISIBLE
        // Update the select icon and text according to the state of selection while swiping
        // between photos
        mViewPager2Wrapper!!.addOnPageChangeCallback(OnPageChangeCallback(selectedCheckButton))

        // Update add button text to include number of items selected.
        mSelection.selectedItemCount.observe(this) { selectedItemCount: Int ->
            viewSelectedAddButton.text = generateAddButtonString(
                requireContext(), selectedItemCount
            )
        }
        selectedCheckButton.setOnClickListener { onClickSelectedCheckButton(selectedCheckButton) }
    }

    override fun onResume() {
        super.onResume()
        (activity as PhotoPickerActivity?)!!.updateCommonLayouts(
            LayoutModeUtils.MODE_PREVIEW,  /* title */
            ""
        )
    }

    override fun onStop() {
        super.onStop()
        if (mViewPager2Wrapper != null) {
            mViewPager2Wrapper!!.onStop()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mViewPager2Wrapper != null) {
            mViewPager2Wrapper!!.onStart()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mViewPager2Wrapper != null) {
            mViewPager2Wrapper!!.onDestroy()
        }
    }

    private fun onClickSelectButton(selectButton: Button) {
        val isSelectedNow = updateSelectionAndGetState()
        updateSelectButtonText(selectButton, isSelectedNow)
    }

    private fun onClickSelectedCheckButton(selectedCheckButton: Button) {
        val isSelectedNow = updateSelectionAndGetState()
        updateSelectedCheckButtonStateAndText(selectedCheckButton, isSelectedNow)
    }

    private fun updateSelectionAndGetState(): Boolean {
        val currentItem = mViewPager2Wrapper!!.currentItem
        val wasSelectedBefore = mSelection!!.isItemSelected(currentItem)
        if (wasSelectedBefore) {
            // If the item is previously selected, current user action is to deselect the item
            mSelection!!.removeSelectedItem(currentItem)
        } else {
            // If the item is not previously selected, current user action is to select the item
            mSelection!!.addSelectedItem(currentItem)
        }

        // After the user has clicked the button, current state of the button should be opposite of
        // the previous state.
        // If the previous state was to "Select" the item, and user clicks "Select" button,
        // wasSelectedBefore = false. And item will be added to selected items. Now, user can only
        // deselect the item. Hence, isSelectedNow is opposite of previous state,
        // i.e., isSelectedNow = true.
        return !wasSelectedBefore
    }

    private inner class OnPageChangeCallback(private val mSelectedCheckButton: Button) :
        ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            // No action to take as we don't have deselect view here.
            if (!mSelection!!.canSelectMultiple()) return
            val item = mViewPager2Wrapper!!.getItemAt(position)
            // Set the appropriate select/deselect state for each item in each page based on the
            // selection list.
            updateSelectedCheckButtonStateAndText(
                mSelectedCheckButton,
                mSelection!!.isItemSelected(item)
            )

            // Set the appropriate special format icon based on the item in the preview
            updateSpecialFormatIcon(item)
        }
    }

    private fun updateSpecialFormatIcon(item: Item) {
        mShouldShowGifBadge = item.isGifOrAnimatedWebp
        mShouldShowMotionPhotoBadge = item.isMotionPhoto
        // Invalidating options menu calls onPrepareOptionsMenu() where the logic for
        // hiding/showing menu items is placed.
        requireActivity().invalidateOptionsMenu()
    }

    companion object {
        private const val TAG = "PreviewFragment"
        private const val PREVIEW_TYPE = "preview_type"
        private const val PREVIEW_ON_LONG_PRESS = 1
        private const val PREVIEW_ON_VIEW_SELECTED = 2
        val argsForPreviewOnLongPress = Bundle()

        init {
            argsForPreviewOnLongPress.putInt(PREVIEW_TYPE, PREVIEW_ON_LONG_PRESS)
        }

        val argsForPreviewOnViewSelected = Bundle()

        init {
            argsForPreviewOnViewSelected.putInt(PREVIEW_TYPE, PREVIEW_ON_VIEW_SELECTED)
        }

        private fun updateSelectButtonText(
            selectButton: Button,
            isSelected: Boolean
        ) {
            selectButton.setText(if (isSelected) R.string.picker_deselect else R.string.picker_select)
        }

        private fun updateSelectedCheckButtonStateAndText(
            selectedCheckButton: Button,
            isSelected: Boolean
        ) {
            selectedCheckButton.setText(if (isSelected) R.string.picker_selected else R.string.picker_deselected)
            selectedCheckButton.isSelected = isSelected
        }

        fun show(fm: FragmentManager, args: Bundle) {
            if (fm.isStateSaved) {
                Log.d(TAG, "Skip show preview fragment because state saved")
                return
            }
            val fragment = PreviewFragment()
            fragment.arguments = args
            fm.beginTransaction()
                .replace(R.id.fragment_container, fragment, TAG)
                .addToBackStack(TAG)
                .commitAllowingStateLoss()
        }

        /**
         * Get the fragment in the FragmentManager
         * @param fm the fragment manager
         */
        operator fun get(fm: FragmentManager): Fragment? {
            return fm.findFragmentByTag(TAG)
        }

        // TODO: There is a same method in TabFragment. To find a way to reuse it.
        private fun generateAddButtonString(context: Context, size: Int): String {
            val sizeString = NumberFormat.getInstance(Locale.getDefault()).format(size.toLong())
            val template = context.getString(R.string.picker_add_button_multi_select)
            return TextUtils.expandTemplate(template, sizeString).toString()
        }
    }
}