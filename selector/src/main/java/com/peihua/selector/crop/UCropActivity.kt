package com.peihua.selector.crop

import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.fz.common.text.isNonEmpty
import com.fz.common.utils.getDrawableCompat
import com.fz.common.utils.getParcelableArrayListExtraCompat
import com.fz.common.utils.getParcelableExtraCompat
import com.peihua.photopicker.R
import com.peihua.photopicker.databinding.PickerCropActivityPhotoboxBinding
import com.peihua.photopicker.databinding.PickerCropControlsBinding
import com.peihua.selector.crop.callback.BitmapCropCallback
import com.peihua.selector.crop.model.AspectRatio
import com.peihua.selector.crop.util.FileUtils
import com.peihua.selector.crop.util.SelectedStateListDrawable
import com.peihua.selector.crop.widget.AspectRatioTextView
import com.peihua.selector.crop.widget.CropImageView
import com.peihua.selector.crop.widget.GestureCropImageView
import com.peihua.selector.crop.widget.HorizontalProgressWheelView.ScrollingListener
import com.peihua.selector.crop.widget.OverlayView
import com.peihua.selector.crop.widget.TransformImageView.TransformImageListener
import com.peihua.selector.crop.widget.UCropView
import java.util.Locale


/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
class UCropActivity : AppCompatActivity() {
    @IntDef(NONE, SCALE, ROTATE, ALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GestureTypes

    private var mToolbarTitle: String? = null
    private var isUseCustomBitmap = false
    private var mActiveControlsWidgetColor = 0

    @ColorInt
    private var mRootViewBackgroundColor = 0

    private var mShowBottomControls = false
    private var mShowLoader = true
    private var isForbidCropGifWebp = false


    private val mControlsTransition: Transition by lazy { AutoTransition() }
    private var mCompressFormat = DEFAULT_COMPRESS_FORMAT
    private var mCompressQuality = DEFAULT_COMPRESS_QUALITY
    private var mAllowedGestures = intArrayOf(SCALE, ROTATE, ALL)
    private val mCropAspectRatioViews: MutableList<ViewGroup> = ArrayList()

    @ColorInt
    private var mToolBarIconColor = Color.WHITE

    @DrawableRes
    private var mToolbarCropDrawable = 0
    private val binding by lazy { PickerCropActivityPhotoboxBinding.inflate(layoutInflater) }
    private val mUCropView: UCropView by lazy { binding.ucrop }
    private val mGestureCropImageView: GestureCropImageView by lazy { binding.ucrop.cropImageView }
    private val mOverlayView: OverlayView by lazy { binding.ucrop.overlayView }
    private val ucorpFrame by lazy { binding.ucropFrame }
    private val ucropPhotoBox: RelativeLayout by lazy { binding.ucropPhotobox }
    private val controlsWrapper by lazy { binding.controlsWrapper }

    private val controlsBinding by lazy { PickerCropControlsBinding.inflate(layoutInflater) }
    private val mWrapperStateAspectRatio by lazy { controlsBinding.stateAspectRatio }
    private val mWrapperStateRotate by lazy { controlsBinding.stateRotate }
    private val mWrapperStateScale by lazy { controlsBinding.stateScale }
    private val mTextViewRotateAngle: TextView by lazy { controlsBinding.textViewRotate }
    private val mTextViewScalePercent: TextView by lazy { controlsBinding.textViewScale }
    private val mBlockingView by lazy {
        val view = View(this)
        val lp = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.addRule(RelativeLayout.BELOW, R.id.toolbar)
        view.layoutParams = lp
        view.isClickable = true
        view
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(R.style.PickerCropMaterialTheme,  /* force */false)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val intent = intent
        setupViews(intent)
        setImageData(intent)
        setInitialState()
        addBlockingView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picker_menu_activity, menu)

        // Change crop & loader menu icons color to match the rest of the UI colors
        val menuItemLoader = menu.findItem(R.id.menu_loader)
        val menuItemLoaderIcon = menuItemLoader.icon
        if (menuItemLoaderIcon != null) {
            try {
                val drawable = menuItemLoaderIcon.mutate()
                drawable.setTint(mToolBarIconColor)
                menuItemLoader.icon = drawable
            } catch (e: Exception) {
                Log.i(
                    TAG,
                    String.format(
                        "%s - %s",
                        e.message,
                        getString(R.string.picker_crop_mutate_exception_hint)
                    )
                )
            }
            (menuItemLoader.icon as Animatable?)?.start()
        }
        val menuItemCrop = menu.findItem(R.id.menu_crop)
        val menuItemCropIcon = getDrawableCompat(mToolbarCropDrawable)
        menuItemCropIcon?.setTint(mToolBarIconColor)
        menuItemCrop.icon = menuItemCropIcon
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_crop)?.isVisible = !mShowLoader
        menu.findItem(R.id.menu_loader)?.isVisible = mShowLoader
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_crop) {
            cropAndSaveImage()
            return true
        } else if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        mGestureCropImageView.cancelAllAnimations()
    }

    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private fun setImageData(intent: Intent) {
        val inputUri = intent.getParcelableExtraCompat(UCrop.EXTRA_INPUT_URI, Uri::class.java)
        var outputUri = intent.getParcelableExtraCompat(UCrop.EXTRA_OUTPUT_URI, Uri::class.java)
        processOptions(intent)
        if (inputUri != null && outputUri != null) {
            try {
                outputUri = FileUtils.replaceOutputUri(
                    this@UCropActivity,
                    isForbidCropGifWebp,
                    inputUri,
                    outputUri
                )
                mGestureCropImageView.setImageUri(inputUri, outputUri, isUseCustomBitmap)
            } catch (e: Exception) {
                setResultError(e)
                finish()
            }
        } else {
            setResultError(NullPointerException(getString(R.string.picker_crop_error_input_data_is_absent)))
            finish()
        }
    }

    /**
     * This method extracts [#optionsBundle][UCrop.Options] from incoming intent
     * and setups Activity, [OverlayView] and [CropImageView] properly.
     */
    @Suppress("deprecation")
    private fun processOptions(intent: Intent) {
        // Bitmap compression options
        val compressionFormatName =
            intent.getStringExtra(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME)
        var compressFormat: CompressFormat? = null
        if (compressionFormatName.isNonEmpty()) {
            compressFormat = CompressFormat.valueOf(compressionFormatName)
        }
        mCompressFormat = compressFormat ?: DEFAULT_COMPRESS_FORMAT
        mCompressQuality =
            intent.getIntExtra(UCrop.Options.EXTRA_COMPRESSION_QUALITY, DEFAULT_COMPRESS_QUALITY)

        // Gestures options
        val allowedGestures = intent.getIntArrayExtra(UCrop.Options.EXTRA_ALLOWED_GESTURES)
        if (allowedGestures != null && allowedGestures.size == TABS_COUNT) {
            mAllowedGestures = allowedGestures
        }
        isUseCustomBitmap =
            intent.getBooleanExtra(UCrop.Options.EXTRA_CROP_CUSTOM_LOADER_BITMAP, false)

        // Crop image view options
        mGestureCropImageView.maxBitmapSize =
            intent.getIntExtra(
                UCrop.Options.EXTRA_MAX_BITMAP_SIZE,
                CropImageView.DEFAULT_MAX_BITMAP_SIZE
            )
        mGestureCropImageView.setMaxScaleMultiplier(
            intent.getFloatExtra(
                UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER,
                CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER
            )
        )
        mGestureCropImageView.setImageToWrapCropBoundsAnimDuration(
            intent.getIntExtra(
                UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
                CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION
            ).toLong()
        )

        // Overlay view options
        mOverlayView.isFreestyleCropEnabled = intent.getBooleanExtra(
            UCrop.Options.EXTRA_FREE_STYLE_CROP,
            OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE
        )
        mOverlayView.setDragSmoothToCenter(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_CROP_DRAG_CENTER,
                false
            )
        )
        mOverlayView.setDimmedColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_DIMMED_LAYER_COLOR,
                resources.getColor(R.color.picker_color_default_dimmed)
            )
        )
        mOverlayView.setCircleStrokeColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CIRCLE_STROKE_COLOR,
                resources.getColor(R.color.picker_color_default_dimmed)
            )
        )
        mOverlayView.setCircleDimmedLayer(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER,
                OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER
            )
        )
        mOverlayView.setShowCropFrame(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_FRAME,
                OverlayView.DEFAULT_SHOW_CROP_FRAME
            )
        )
        mOverlayView.setCropFrameColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_COLOR,
                resources.getColor(R.color.picker_color_default_crop_frame)
            )
        )
        mOverlayView.setCropFrameStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.picker_default_crop_frame_stoke_width)
            )
        )
        mOverlayView.setShowCropGrid(
            intent.getBooleanExtra(
                UCrop.Options.EXTRA_SHOW_CROP_GRID,
                OverlayView.DEFAULT_SHOW_CROP_GRID
            )
        )
        mOverlayView.setCropGridRowCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT,
                OverlayView.DEFAULT_CROP_GRID_ROW_COUNT
            )
        )
        mOverlayView.setCropGridColumnCount(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT,
                OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT
            )
        )
        mOverlayView.setCropGridColor(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_COLOR,
                resources.getColor(R.color.picker_color_default_crop_grid)
            )
        )
        mOverlayView.setCropGridStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.picker_default_crop_grid_stoke_width)
            )
        )
        mOverlayView.setDimmedStrokeWidth(
            intent.getIntExtra(
                UCrop.Options.EXTRA_CIRCLE_STROKE_WIDTH_LAYER,
                resources.getDimensionPixelSize(R.dimen.picker_default_crop_grid_stoke_width)
            )
        )
        // Aspect ratio options
        val aspectRatioX = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_X, -1f)
        val aspectRatioY = intent.getFloatExtra(UCrop.EXTRA_ASPECT_RATIO_Y, -1f)
        val aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        val aspectRatioList =
            intent.getParcelableArrayListExtra<AspectRatio>(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
        if (aspectRatioX >= 0 && aspectRatioY >= 0) {
            mWrapperStateAspectRatio.visibility = View.GONE
            val targetAspectRatio = aspectRatioX / aspectRatioY
            mGestureCropImageView.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size) {
            val targetAspectRatio =
                aspectRatioList[aspectRationSelectedByDefault].aspectRatioX / aspectRatioList[aspectRationSelectedByDefault].aspectRatioY
            mGestureCropImageView.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else {
            mGestureCropImageView.targetAspectRatio = CropImageView.SOURCE_IMAGE_ASPECT_RATIO
        }

        // Result bitmap max size options
        val maxSizeX = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_X, 0)
        val maxSizeY = intent.getIntExtra(UCrop.EXTRA_MAX_SIZE_Y, 0)
        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView.setMaxResultImageSizeX(maxSizeX)
            mGestureCropImageView.setMaxResultImageSizeY(maxSizeY)
        }
    }

    private fun setupViews(intent: Intent) {
        isForbidCropGifWebp =
            intent.getBooleanExtra(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)
        mActiveControlsWidgetColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE,
            ContextCompat.getColor(this, R.color.picker_color_active_controls_color)
        )
        mToolbarTitle = intent.getStringExtra(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR)
        mToolbarTitle =
            if (mToolbarTitle.isNonEmpty()) mToolbarTitle else resources.getString(R.string.picker_crop_label_edit_photo)
        mShowBottomControls =
            !intent.getBooleanExtra(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)
        mRootViewBackgroundColor = intent.getIntExtra(
            UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR,
            ContextCompat.getColor(this, R.color.picker_color_crop_background)
        )
        setupAppBar()
        initiateRootViews()
        if (mShowBottomControls) {
            controlsWrapper.isVisible = true
            controlsWrapper.addView(controlsBinding.root)
            mControlsTransition.duration = CONTROLS_ANIMATION_DURATION
            mWrapperStateAspectRatio.setOnClickListener(mStateClickListener)
            mWrapperStateRotate.setOnClickListener(mStateClickListener)
            mWrapperStateScale.setOnClickListener(mStateClickListener)
            setupAspectRatioWidget(intent)
            setupRotateWidget()
            setupScaleWidget()
            setupStatesWrapper()
        }
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private fun setupAppBar() {
        val attrs =
            intArrayOf(R.attr.pickerToolbarCropMenuDrawable, android.R.attr.colorControlNormal)
        val ta = obtainStyledAttributes(attrs)
        mToolbarCropDrawable =
            ta.getResourceId( /* index */0,  /* defValue */R.drawable.picker_crop_ic_done)
        // Save toolbar height so that we can use it as padding for FragmentContainerView
        mToolBarIconColor = ta.getColor( /* index */1,  /* defValue */Color.WHITE)
        ta.recycle()
        // Set all of the Toolbar coloring
        binding.toolbar.title = mToolbarTitle
        // Color buttons inside the Toolbar
        setSupportActionBar(binding.toolbar)
//        val actionBar = supportActionBar
        supportActionBar?.setDisplayShowTitleEnabled(false)
        setStatusBarColor(Color.BLACK)
    }

    private fun initiateRootViews() {
        mGestureCropImageView.setTransformImageListener(mImageListener)
        ucorpFrame.setBackgroundColor(mRootViewBackgroundColor)
        if (!mShowBottomControls) {
            val params = ucorpFrame.layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = 0
            ucorpFrame.requestLayout()
        }
    }

    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {
            setAngleText(currentAngle)
        }

        override fun onScale(currentScale: Float) {
            setScaleText(currentScale)
        }

        override fun onLoadComplete() {
            mUCropView.animate().alpha(1f).setDuration(300).interpolator =
                AccelerateInterpolator()
            mBlockingView.isClickable = false
            if (intent.getBooleanExtra(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)) {
                val inputUri = intent.getParcelableExtra<Uri>(UCrop.EXTRA_INPUT_URI)
                val mimeType =
                    FileUtils.getMimeTypeFromMediaContentUri(this@UCropActivity, inputUri)
                if (FileUtils.isGif(mimeType) || FileUtils.isWebp(mimeType)) {
                    mBlockingView.isClickable = true
                }
            }
            mShowLoader = false
            supportInvalidateOptionsMenu()
        }

        override fun onLoadFailure(e: Exception) {
            setResultError(e)
            finish()
        }
    }

    /**
     * Use [.mActiveControlsWidgetColor] for color filter
     */
    private fun setupStatesWrapper() {
        controlsBinding.apply {
            imageViewStateScale.setImageDrawable(
                SelectedStateListDrawable(
                    imageViewStateScale.drawable,
                    mActiveControlsWidgetColor
                )
            )
            imageViewStateRotate.setImageDrawable(
                SelectedStateListDrawable(
                    imageViewStateRotate.drawable,
                    mActiveControlsWidgetColor
                )
            )
            imageViewStateAspectRatio.setImageDrawable(
                SelectedStateListDrawable(
                    imageViewStateAspectRatio.drawable,
                    mActiveControlsWidgetColor
                )
            )
        }
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    private fun setStatusBarColor(@ColorInt color: Int) {
        val window = window
        if (window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.decorView.apply {
                var option = systemUiVisibility
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    option = option or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
                systemUiVisibility = option or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            window.statusBarColor = color
        }
    }

    private fun setupAspectRatioWidget(intent: Intent) {
        var aspectRationSelectedByDefault =
            intent.getIntExtra(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        var aspectRatioList =
            intent.getParcelableArrayListExtraCompat(
                UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS,
                AspectRatio::class.java
            )
        if (aspectRatioList.isEmpty()) {
            aspectRationSelectedByDefault = 2
            aspectRatioList = ArrayList()
            aspectRatioList.add(AspectRatio(null, 1f, 1f))
            aspectRatioList.add(AspectRatio(null, 3f, 4f))
            aspectRatioList.add(
                AspectRatio(
                    getString(R.string.picker_crop_label_original).uppercase(Locale.getDefault()),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO
                )
            )
            aspectRatioList.add(AspectRatio(null, 3f, 2f))
            aspectRatioList.add(AspectRatio(null, 16f, 9f))
        }
        var wrapperAspectRatio: FrameLayout
        var aspectRatioTextView: AspectRatioTextView
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (aspectRatio in aspectRatioList) {
            wrapperAspectRatio =
                layoutInflater.inflate(R.layout.picker_crop_aspect_ratio, null) as FrameLayout
            wrapperAspectRatio.layoutParams = lp
            aspectRatioTextView = wrapperAspectRatio.getChildAt(0) as AspectRatioTextView
            aspectRatioTextView.setActiveColor(mActiveControlsWidgetColor)
            aspectRatioTextView.setAspectRatio(aspectRatio)
            controlsBinding.layoutAspectRatio.addView(wrapperAspectRatio)
            mCropAspectRatioViews.add(wrapperAspectRatio)
        }
        mCropAspectRatioViews[aspectRationSelectedByDefault].isSelected = true
        for (cropAspectRatioView in mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener { v ->
                mGestureCropImageView.targetAspectRatio =
                    ((v as ViewGroup).getChildAt(0) as AspectRatioTextView).getAspectRatio(v.isSelected)
                mGestureCropImageView.setImageToWrapCropBounds()
                if (!v.isSelected) {
                    for (cropAspectRatioView in mCropAspectRatioViews) {
                        cropAspectRatioView.isSelected = cropAspectRatioView === v
                    }
                }
            }
        }
    }

    private fun setupRotateWidget() {
        controlsBinding.layoutRotateWheel.apply {
            rotateScrollWheel.setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    mGestureCropImageView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
                }

                override fun onScrollEnd() {
                    mGestureCropImageView.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView.cancelAllAnimations()
                }
            })
            rotateScrollWheel.setMiddleLineColor(mActiveControlsWidgetColor)
            wrapperResetRotate.setOnClickListener { resetRotation() }
            wrapperRotateByAngle.setOnClickListener { rotateByAngle(90) }
            setAngleTextColor(mActiveControlsWidgetColor)
        }
    }

    private fun setupScaleWidget() {
        controlsBinding.layoutScaleWheel.apply {
            scaleScrollWheel.setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    if (delta > 0) {
                        mGestureCropImageView.zoomInImage(
                            mGestureCropImageView.currentScale
                                    + delta * ((mGestureCropImageView.maxScale - mGestureCropImageView.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    } else {
                        mGestureCropImageView.zoomOutImage(
                            mGestureCropImageView.currentScale
                                    + delta * ((mGestureCropImageView.maxScale - mGestureCropImageView.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    }
                }

                override fun onScrollEnd() {
                    mGestureCropImageView.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView.cancelAllAnimations()
                }
            })
            scaleScrollWheel.setMiddleLineColor(
                mActiveControlsWidgetColor
            )
        }
        setScaleTextColor(mActiveControlsWidgetColor)
    }

    private fun setAngleText(angle: Float) {
        mTextViewRotateAngle.text = String.format(Locale.getDefault(), "%.1f°", angle)
    }

    private fun setAngleTextColor(textColor: Int) {
        mTextViewRotateAngle.setTextColor(textColor)
    }

    private fun setScaleText(scale: Float) {
        mTextViewScalePercent.text =
            String.format(Locale.getDefault(), "%d%%", (scale * 100).toInt())
    }

    private fun setScaleTextColor(textColor: Int) {
        mTextViewScalePercent.setTextColor(textColor)
    }

    private fun resetRotation() {
        mGestureCropImageView.postRotate(-mGestureCropImageView.currentAngle)
        mGestureCropImageView.setImageToWrapCropBounds()
    }

    private fun rotateByAngle(angle: Int) {
        mGestureCropImageView.postRotate(angle.toFloat())
        mGestureCropImageView.setImageToWrapCropBounds()
    }

    private val mStateClickListener = View.OnClickListener { v ->
        if (!v.isSelected) {
            setWidgetState(v.id)
        }
    }

    private fun setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio.isVisible) {
                setWidgetState(R.id.state_aspect_ratio)
            } else {
                setWidgetState(R.id.state_scale)
            }
        } else {
            setAllowedGestures(0)
        }
    }

    private fun setWidgetState(@IdRes stateViewId: Int) {
        if (!mShowBottomControls) return
        controlsBinding.apply {
            stateAspectRatio.isSelected = stateViewId == R.id.state_aspect_ratio
            stateRotate.isSelected = stateViewId == R.id.state_rotate
            stateScale.isSelected = stateViewId == R.id.state_scale
            layoutAspectRatio.isVisible = stateViewId == R.id.state_aspect_ratio
            layoutRotateWheel.root.isVisible = stateViewId == R.id.state_rotate
            layoutScaleWheel.root.isVisible = stateViewId == R.id.state_scale
        }
        changeSelectedTab(stateViewId)
        if (stateViewId == R.id.state_scale) {
            setAllowedGestures(0)
        } else if (stateViewId == R.id.state_rotate) {
            setAllowedGestures(1)
        } else {
            setAllowedGestures(2)
        }
    }

    private fun changeSelectedTab(stateViewId: Int) {
        binding.apply {
            TransitionManager.beginDelayedTransition(ucropPhotobox, mControlsTransition)
        }
        controlsBinding.apply {
            textViewRotate.isVisible = stateViewId == R.id.state_rotate
            textViewCrop.isVisible = stateViewId == R.id.state_aspect_ratio
            textViewScale.isVisible = stateViewId == R.id.state_scale
        }
    }

    private fun setAllowedGestures(tab: Int) {
        mGestureCropImageView.isScaleEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE
        mGestureCropImageView.isRotateEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE
        mGestureCropImageView.isGestureEnabled =
            intent.getBooleanExtra(UCrop.Options.EXTRA_DRAG_IMAGES, true)
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private fun addBlockingView() {
        ucropPhotoBox.addView(mBlockingView)
    }

    protected fun cropAndSaveImage() {
        mBlockingView.isClickable = true
        mShowLoader = true
        supportInvalidateOptionsMenu()
        mGestureCropImageView.cropAndSaveImage(
            mCompressFormat,
            mCompressQuality,
            object : BitmapCropCallback {
                override fun onBitmapCropped(
                    resultUri: Uri,
                    offsetX: Int,
                    offsetY: Int,
                    imageWidth: Int,
                    imageHeight: Int,
                ) {
                    setResultUri(
                        resultUri,
                        mGestureCropImageView.targetAspectRatio,
                        offsetX,
                        offsetY,
                        imageWidth,
                        imageHeight
                    )
                    finish()
                }

                override fun onCropFailure(t: Throwable) {
                    setResultError(t)
                    finish()
                }
            })
    }

    protected fun setResultUri(
        uri: Uri?,
        resultAspectRatio: Float,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        val inputUri = intent.getParcelableExtraCompat(UCrop.EXTRA_INPUT_URI, Uri::class.java)
        setResult(
            RESULT_OK, Intent()
                .setData(uri)
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                .putExtra(UCrop.EXTRA_CROP_INPUT_ORIGINAL, inputUri)
        )
    }

    protected fun setResultError(throwable: Throwable?) {
        val inputUri =
            intent.getParcelableExtraCompat<Uri>(UCrop.EXTRA_INPUT_URI, Uri::class.java)
        setResult(
            UCrop.RESULT_ERROR, Intent().setData(inputUri)
                .putExtra(UCrop.EXTRA_OUTPUT_URI, inputUri)
                .putExtra(UCrop.EXTRA_ERROR, throwable)
        )
    }

    override fun onDestroy() {
//        UCropDevelopConfig.destroy();
        super.onDestroy()
    }

    companion object {
        const val DEFAULT_COMPRESS_QUALITY = 90
        val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        const val NONE = 0
        const val SCALE = 1
        const val ROTATE = 2
        const val ALL = 3
        private const val TAG = "UCropActivity"
        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}