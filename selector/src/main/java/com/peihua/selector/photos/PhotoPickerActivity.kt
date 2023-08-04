package com.peihua.selector.photos

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsetsController
import android.view.accessibility.AccessibilityManager
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.fz.common.utils.getColorCompat
import com.fz.common.utils.getDrawableCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.peihua.photopicker.R
import com.peihua.photopicker.databinding.ActivityPhotoPickerBinding
import com.peihua.selector.data.PickerResult
import com.peihua.selector.data.Selection
import com.peihua.selector.photos.ui.TabContainerFragment
import com.peihua.selector.util.LayoutModeUtils
import com.peihua.selector.util.LayoutModeUtils.MODE_PHOTOS_TAB
import com.peihua.selector.util.isAtLeastPie
import com.peihua.selector.util.isAtLeastT
import com.peihua.selector.viewmodel.PickerViewModel
import kotlin.math.roundToInt

/**
 * Photo Picker allows users to choose one or more photos and/or videos to share with an app. The
 * app does not get access to all photos/videos.
 */
class PhotoPickerActivity : AppCompatActivity() {
    private val mPickerViewModel by viewModels<PickerViewModel>()
    private val binding by lazy { ActivityPhotoPickerBinding.bind(findViewById(R.id.cl_root_view)) }
    private val mSelection: Selection by lazy { mPickerViewModel.selection }
    private val mBottomSheetBehavior: BottomSheetBehavior<*> by lazy { BottomSheetBehavior.from(mBottomSheetView) }
    private val mBottomBar: View by lazy { binding.pickerBottomBar }
    private val mBottomSheetView: View by lazy { binding.bottomSheet }
    private val mFragmentContainerView: View by lazy { binding.fragmentContainer }
    private val mDragBar: View by lazy { binding.dragBar }
    private val mPrivacyText: View by lazy { binding.privacyText }
    private val mTabLayout: TabLayout by lazy { binding.tabLayout }
    private val mToolbar: Toolbar by lazy { binding.toolbar }

    @ColorInt
    private var mDefaultBackgroundColor = 0

    @ColorInt
    private var mToolBarIconColor = 0
    private var mToolbarHeight = 0
    private var mIsAccessibilityEnabled = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        // We use the device default theme as the base theme. Apply the material them for the
        // material components. We use force "false" here, only values that are not already defined
        // in the base theme will be copied.
        theme.applyStyle(R.style.PickerMaterialTheme,  /* force */false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_picker)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val attrs = intArrayOf(R.attr.actionBarSize, R.attr.pickerTextColor)
        val ta = obtainStyledAttributes(attrs)
        // Save toolbar height so that we can use it as padding for FragmentContainerView
        mToolbarHeight = ta.getDimensionPixelSize( /* index */0,  /* defValue */-1)
        mToolBarIconColor = ta.getColor( /* index */1,  /* defValue */-1)
        ta.recycle()
        mDefaultBackgroundColor = getColorCompat(R.color.picker_background_color)
        try {
            mPickerViewModel.parseValuesFromIntent(intent)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Finished activity due to an exception while parsing extras", e)
            setCancelledResultAndFinishSelf()
        }
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        mIsAccessibilityEnabled = accessibilityManager.isEnabled
        accessibilityManager.addAccessibilityStateChangeListener { enabled: Boolean ->
            mIsAccessibilityEnabled = enabled
        }
        initBottomSheetBehavior()
        restoreState(savedInstanceState)
    }

    public override fun onDestroy() {
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                val outRect = Rect()
                mBottomSheetView.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun setTitle(title: CharSequence) {
        super.setTitle(title)
        supportActionBar?.title = title
    }

    /**
     * Called when owning activity is saving state to be used to restore state during creation.
     *
     * @param state Bundle to save state
     */
    public override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        saveBottomSheetState()
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            restoreBottomSheetState()
        } else {
            setupInitialLaunchState()
        }
    }

    /**
     * Sets up states for the initial launch. This includes updating common layouts, selecting
     * Photos tab item and saving the current bottom sheet state for later.
     */
    private fun setupInitialLaunchState() {
        updateCommonLayouts(MODE_PHOTOS_TAB,  /* title */"")
        TabContainerFragment.show(supportFragmentManager)
        saveBottomSheetState()
    }

    private fun initBottomSheetBehavior() {
        initStateForBottomSheet()
        mBottomSheetBehavior.addBottomSheetCallback(createBottomSheetCallBack())
        setRoundedCornersForBottomSheet()
    }

    private fun createBottomSheetCallBack(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            private var mIsHiddenDueToBottomSheetClosing = false
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    finish()
                }
                saveBottomSheetState()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // We need to handle this state if the user is swiping till the bottom of the
                // screen but then swipes up bottom sheet suddenly
                if (slideOffset > HIDE_PROFILE_BUTTON_THRESHOLD &&
                    mIsHiddenDueToBottomSheetClosing
                ) {
                    mIsHiddenDueToBottomSheetClosing = false
                }
            }
        }
    }

    private fun setRoundedCornersForBottomSheet() {
        val cornerRadius = resources.getDimensionPixelSize(R.dimen.picker_top_corner_radius).toFloat()
        val viewOutlineProvider: ViewOutlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, (view.height + cornerRadius).toInt(), cornerRadius)
            }
        }
        mBottomSheetView.outlineProvider = viewOutlineProvider
    }

    private fun initStateForBottomSheet() {
        if (!mIsAccessibilityEnabled && !mSelection.canSelectMultiple()
            && !isOrientationLandscape
        ) {
            val peekHeight = bottomSheetPeekHeight.roundToInt()
            mBottomSheetBehavior.peekHeight = peekHeight
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            mBottomSheetBehavior.setSkipCollapsed(true)
        }
    }

    private fun restoreBottomSheetState() {
        // BottomSheet is always EXPANDED for landscape
        if (isOrientationLandscape) {
            return
        }
        val savedState: Int = mPickerViewModel.bottomSheetState
        if (isValidBottomSheetState(savedState)) {
            mBottomSheetBehavior.state = when (savedState) {
                4 -> BottomSheetBehavior.STATE_COLLAPSED
                5 -> BottomSheetBehavior.STATE_HIDDEN
                6 -> BottomSheetBehavior.STATE_HALF_EXPANDED
                3 -> BottomSheetBehavior.STATE_EXPANDED
                else -> BottomSheetBehavior.STATE_EXPANDED
            }

        }
    }

    private fun saveBottomSheetState() {
        // Do not save state for landscape or preview mode. This is because they are always in
        // STATE_EXPANDED state.
        if (isOrientationLandscape || !mBottomSheetView.clipToOutline) {
            return
        }
        mPickerViewModel.bottomSheetState = mBottomSheetBehavior.state
    }

    private fun isValidBottomSheetState(state: Int): Boolean {
        return state == BottomSheetBehavior.STATE_COLLAPSED ||
                state == BottomSheetBehavior.STATE_EXPANDED
    }

    private val isOrientationLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun setResultAndFinishSelf() {
        setResult(
            RESULT_OK, PickerResult.getPickerResponseIntent(
                mSelection.canSelectMultiple(),
                mSelection.selectedItems, mPickerViewModel.configModel.mimeType
            )
        )
        finish()
    }

    private fun setCancelledResultAndFinishSelf() {
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * Updates the common views such as Title, Toolbar, Navigation bar, status bar and bottom sheet
     * behavior
     *
     * @param mode [LayoutModeUtils.Mode] which describes the layout mode to update.
     * @param title the title to set for the Activity
     */
    fun updateCommonLayouts(mode: LayoutModeUtils.Mode, title: String) {
        updateTitle(title)
        updateToolbar(mode)
        updateStatusBarAndNavigationBar(mode)
        updateBottomSheetBehavior(mode)
        updateFragmentContainerViewPadding(mode)
        updateDragBarVisibility(mode)
        updatePrivacyTextVisibility(mode)
        // The bottom bar and profile button are not shown on preview, hide them in preview. We
        // handle the visibility of them in TabFragment. We don't need to make them shown in
        // non-preview page here.
        if (mode.isPreview) {
            mBottomBar.visibility = View.GONE
        }
    }

    private fun updateTitle(title: String) {
        setTitle(title)
    }

    /**
     * Updates the icons and show/hide the tab layout with `mode`.
     *
     * @param mode [LayoutModeUtils.Mode] which describes the layout mode to update.
     */
    private fun updateToolbar(mode: LayoutModeUtils.Mode) {
        val isPreview: Boolean = mode.isPreview
        val shouldShowTabLayout: Boolean = mode.isPhotosTabOrAlbumsTab
        // 1. Set the tabLayout visibility
        mTabLayout.isVisible = shouldShowTabLayout

        // 2. Set the toolbar color
        val toolbarColor = if (isPreview && !shouldShowTabLayout) {
            if (isOrientationLandscape) {
                // Toolbar in Preview will have transparent color in Landscape mode.
                ColorDrawable(getColorCompat(android.R.color.transparent))
            } else {
                // Toolbar in Preview will have a solid color with 90% opacity in Portrait mode.
                ColorDrawable(getColorCompat(R.color.preview_scrim_solid_color))
            }
        } else {
            ColorDrawable(mDefaultBackgroundColor)
        }

        // 3. Set the toolbar icon.
        val icon: Drawable?
        if (shouldShowTabLayout) {
            icon = getDrawableCompat(R.drawable.ic_close)
        } else {
            icon = getDrawableCompat(R.drawable.ic_arrow_back)
            // Preview mode has dark background, hence icons will be WHITE in color
            icon?.setTint(if (isPreview) Color.WHITE else mToolBarIconColor)
        }
        supportActionBar?.apply {
            setBackgroundDrawable(toolbarColor)
            setHomeAsUpIndicator(icon)
            setHomeActionContentDescription(if (shouldShowTabLayout) android.R.string.cancel else R.string.abc_action_bar_up_description)
        }
    }

    /**
     * Updates status bar and navigation bar
     *
     * @param mode [LayoutModeUtils.Mode] which describes the layout mode to update.
     */
    private fun updateStatusBarAndNavigationBar(mode: LayoutModeUtils.Mode) {
        val isPreview: Boolean = mode.isPreview
        val navigationBarColor =
            if (isPreview) getColorCompat(R.color.preview_background_color) else if (isAtLeastT) mDefaultBackgroundColor else Color.BLACK
        window.navigationBarColor = navigationBarColor
        val statusBarColor =
            getColorCompat(if (isPreview) R.color.preview_background_color else android.R.color.transparent)
        window.statusBarColor = statusBarColor

        // Update the system bar appearance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            var appearance = 0
            if (!isPreview) {
                val uiModeNight = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (uiModeNight == Configuration.UI_MODE_NIGHT_NO) {
                    // If the system is not in Dark theme, set the system bars to light mode.
                    appearance = mask
                }
            }
            window.insetsController?.setSystemBarsAppearance(appearance, mask)
        }
    }

    /**
     * Updates the bottom sheet behavior
     *
     * @param mode [LayoutModeUtils.Mode] which describes the layout mode to update.
     */
    private fun updateBottomSheetBehavior(mode: LayoutModeUtils.Mode) {
        val isPreview: Boolean = mode.isPreview
        mBottomSheetView.clipToOutline = !isPreview
        // the photo in photos grid
        mBottomSheetBehavior.isDraggable = !isPreview
        if (isPreview) {
            if (mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                // Sets bottom sheet behavior state to STATE_EXPANDED if it's not already expanded.
                // This is useful when user goes to Preview mode which is always Full screen.
                // partial screen. This is similar to long press animation.
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            restoreBottomSheetState()
        }
    }

    /**
     * Updates the FragmentContainerView padding.
     *
     *
     * For Preview mode, toolbar overlaps the Fragment content, hence the padding will be set to 0.
     * For Non-Preview mode, toolbar doesn't overlap the contents of the fragment, hence we set the
     * padding as the height of the toolbar.
     */
    private fun updateFragmentContainerViewPadding(mode: LayoutModeUtils.Mode) {
        val topPadding: Int = if (mode.isPreview) {
            0
        } else {
            mToolbarHeight
        }
        mFragmentContainerView.setPadding(
            mFragmentContainerView.paddingLeft,
            topPadding, mFragmentContainerView.paddingRight,
            mFragmentContainerView.paddingBottom
        )
    }

    private fun updateDragBarVisibility(mode: LayoutModeUtils.Mode) {
        mDragBar.isVisible = !mode.isPreview
    }

    private fun updatePrivacyTextVisibility(mode: LayoutModeUtils.Mode) {
        // The privacy text is only shown on the Photos tab and Albums tab
        mPrivacyText.isVisible = mode.isPhotosTabOrAlbumsTab
    }

    companion object {
        private const val TAG = "PhotoPickerActivity"
        private const val BOTTOM_SHEET_PEEK_HEIGHT_PERCENTAGE = 0.75f
        private const val HIDE_PROFILE_BUTTON_THRESHOLD = -0.5f
    }

    val bottomSheetPeekHeight: Float
        get() {
            val screenHeight = resources.displayMetrics.heightPixels
            return screenHeight * BOTTOM_SHEET_PEEK_HEIGHT_PERCENTAGE
        }
}