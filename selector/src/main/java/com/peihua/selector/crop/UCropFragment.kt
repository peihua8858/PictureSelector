package com.peihua.selector.crop

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.fz.common.utils.getColorCompat
import com.peihua.photopicker.R
import com.peihua.selector.crop.callback.BitmapCropCallback
import com.peihua.selector.crop.model.AspectRatio
import com.peihua.selector.crop.util.FileUtils
import com.peihua.selector.crop.util.SelectedStateListDrawable
import com.peihua.selector.crop.widget.AspectRatioTextView
import com.peihua.selector.crop.widget.CropImageView
import com.peihua.selector.crop.widget.GestureCropImageView
import com.peihua.selector.crop.widget.HorizontalProgressWheelView
import com.peihua.selector.crop.widget.HorizontalProgressWheelView.ScrollingListener
import com.peihua.selector.crop.widget.OverlayView
import com.peihua.selector.crop.widget.TransformImageView.TransformImageListener
import com.peihua.selector.crop.widget.UCropView
import java.util.Locale

class UCropFragment : Fragment() {
    @IntDef(NONE, SCALE, ROTATE, ALL)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GestureTypes

    private var callback: UCropFragmentCallback? = null
    private var isUseCustomBitmap = false
    private var mActiveControlsWidgetColor = 0

    @ColorInt
    private var mRootViewBackgroundColor = 0
    private var mShowBottomControls = false
    private var mControlsTransition: Transition = AutoTransition()
    private var mUCropView: UCropView? = null
    private var mGestureCropImageView: GestureCropImageView? = null
    private var mOverlayView: OverlayView? = null
    private var mWrapperStateAspectRatio: ViewGroup? = null
    private var mWrapperStateRotate: ViewGroup? = null
    private var mWrapperStateScale: ViewGroup? = null
    private var mLayoutAspectRatio: ViewGroup? = null
    private var mLayoutRotate: ViewGroup? = null
    private var mLayoutScale: ViewGroup? = null
    private val mCropAspectRatioViews: MutableList<ViewGroup> = ArrayList()
    private var mTextViewRotateAngle: TextView? = null
    private var mTextViewScalePercent: TextView? = null
    private var mBlockingView: View? = null
    private var mCompressFormat = DEFAULT_COMPRESS_FORMAT
    private var mCompressQuality = DEFAULT_COMPRESS_QUALITY
    private var mAllowedGestures = intArrayOf(SCALE, ROTATE, ALL)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback =
            if (parentFragment is UCropFragmentCallback) parentFragment as UCropFragmentCallback? else if (context is UCropFragmentCallback) context else throw IllegalArgumentException(
                context.toString()
                        + " must implement UCropFragmentCallback"
            )
    }

    fun setCallback(callback: UCropFragmentCallback?) {
        this.callback = callback
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.ucrop_fragment_photobox, container, false)
        val bundle = arguments ?: Bundle()
        setupViews(rootView, bundle)
        setImageData(bundle)
        setInitialState()
        addBlockingView(rootView)
        return rootView
    }

    /**
     * Fragment重新可见
     */
    fun fragmentReVisible() {
        val bundle = arguments ?: Bundle()
        setImageData(bundle)
        mUCropView!!.animate().alpha(1f).setDuration(300).interpolator = AccelerateInterpolator()
        callback!!.loadingProgress(false)
        var isClickable = false
        if (bundle.getBoolean(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)) {
            val inputUri = bundle.getParcelable<Uri>(UCrop.EXTRA_INPUT_URI)
            val mimeType = FileUtils.getMimeTypeFromMediaContentUri(context, inputUri)
            isClickable = FileUtils.isGif(mimeType) || FileUtils.isWebp(mimeType)
        }
        mBlockingView!!.isClickable = isClickable
    }

    fun setupViews(view: View, args: Bundle) {
        mActiveControlsWidgetColor = args.getInt(
            UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE, getColorCompat(
                R.color.ucrop_color_active_controls_color
            )
        )
        mShowBottomControls = !args.getBoolean(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false)
        mRootViewBackgroundColor = args.getInt(
            UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, getColorCompat(R.color.ucrop_color_crop_background)
        )
        initiateRootViews(view)
        callback!!.loadingProgress(true)
        if (mShowBottomControls) {
            val wrapper = view.findViewById<ViewGroup>(R.id.controls_wrapper)
            wrapper.visibility = View.VISIBLE
            LayoutInflater.from(context).inflate(R.layout.ucrop_controls, wrapper, true)
            mControlsTransition.duration = CONTROLS_ANIMATION_DURATION
            mWrapperStateAspectRatio = view.findViewById(R.id.state_aspect_ratio)
            mWrapperStateAspectRatio?.setOnClickListener(mStateClickListener)
            mWrapperStateRotate = view.findViewById(R.id.state_rotate)
            mWrapperStateRotate?.setOnClickListener(mStateClickListener)
            mWrapperStateScale = view.findViewById(R.id.state_scale)
            mWrapperStateScale?.setOnClickListener(mStateClickListener)
            mLayoutAspectRatio = view.findViewById(R.id.layout_aspect_ratio)
            mLayoutRotate = view.findViewById(R.id.layout_rotate_wheel)
            mLayoutScale = view.findViewById(R.id.layout_scale_wheel)
            setupAspectRatioWidget(args, view)
            setupRotateWidget(view)
            setupScaleWidget(view)
            setupStatesWrapper(view)
        } else {
            val params = view.findViewById<View>(R.id.ucrop_frame).layoutParams as RelativeLayout.LayoutParams
            params.bottomMargin = 0
            view.findViewById<View>(R.id.ucrop_frame).requestLayout()
        }
    }

    private fun setImageData(bundle: Bundle) {
        val inputUri = bundle.getParcelable<Uri>(UCrop.EXTRA_INPUT_URI)
        var outputUri = bundle.getParcelable<Uri>(UCrop.EXTRA_OUTPUT_URI)
        processOptions(bundle)
        if (inputUri != null && outputUri != null) {
            try {
                val isForbidCropGifWebp = bundle.getBoolean(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)
                outputUri = FileUtils.replaceOutputUri(context, isForbidCropGifWebp, inputUri, outputUri)
                mGestureCropImageView!!.setImageUri(inputUri, outputUri, isUseCustomBitmap)
            } catch (e: Exception) {
                callback!!.onCropFinish(getError(e))
            }
        } else {
            callback!!.onCropFinish(getError(NullPointerException(getString(R.string.ucrop_error_input_data_is_absent))))
        }
    }

    /**
     * This method extracts [#optionsBundle][UCrop.Options] from incoming bundle
     * and setups fragment, [OverlayView] and [CropImageView] properly.
     */
    @Suppress("deprecation")
    private fun processOptions(bundle: Bundle) {
        // Bitmap compression options
        val compressionFormatName = bundle.getString(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME)
        var compressFormat: CompressFormat? = null
        if (!TextUtils.isEmpty(compressionFormatName)) {
            compressFormat = CompressFormat.valueOf(compressionFormatName!!)
        }
        mCompressFormat = compressFormat ?: DEFAULT_COMPRESS_FORMAT
        mCompressQuality =
            bundle.getInt(UCrop.Options.EXTRA_COMPRESSION_QUALITY, UCropActivity.DEFAULT_COMPRESS_QUALITY)
        isUseCustomBitmap = bundle.getBoolean(UCrop.Options.EXTRA_CROP_CUSTOM_LOADER_BITMAP, false)

        // Gestures options
        val allowedGestures = bundle.getIntArray(UCrop.Options.EXTRA_ALLOWED_GESTURES)
        if (allowedGestures != null && allowedGestures.size == TABS_COUNT) {
            mAllowedGestures = allowedGestures
        }

        // Crop image view options
        mGestureCropImageView!!.maxBitmapSize =
            bundle.getInt(UCrop.Options.EXTRA_MAX_BITMAP_SIZE, CropImageView.DEFAULT_MAX_BITMAP_SIZE)
        mGestureCropImageView!!.setMaxScaleMultiplier(
            bundle.getFloat(
                UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER,
                CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER
            )
        )
        mGestureCropImageView!!.setImageToWrapCropBoundsAnimDuration(
            bundle.getInt(
                UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION,
                CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION
            ).toLong()
        )

        // Overlay view options
        mOverlayView!!.isFreestyleCropEnabled = bundle.getBoolean(
            UCrop.Options.EXTRA_FREE_STYLE_CROP,
            OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE
        )
        mOverlayView!!.setDragSmoothToCenter(bundle.getBoolean(UCrop.Options.EXTRA_CROP_DRAG_CENTER, false))
        mOverlayView!!.setDimmedColor(
            bundle.getInt(
                UCrop.Options.EXTRA_DIMMED_LAYER_COLOR,
                resources.getColor(R.color.ucrop_color_default_dimmed)
            )
        )
        mOverlayView!!.setCircleStrokeColor(
            bundle.getInt(
                UCrop.Options.EXTRA_CIRCLE_STROKE_COLOR,
                resources.getColor(R.color.ucrop_color_default_dimmed)
            )
        )
        mOverlayView!!.setCircleDimmedLayer(
            bundle.getBoolean(
                UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER,
                OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER
            )
        )
        mOverlayView!!.setShowCropFrame(
            bundle.getBoolean(
                UCrop.Options.EXTRA_SHOW_CROP_FRAME,
                OverlayView.DEFAULT_SHOW_CROP_FRAME
            )
        )
        mOverlayView!!.setCropFrameColor(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_FRAME_COLOR,
                resources.getColor(R.color.ucrop_color_default_crop_frame)
            )
        )
        mOverlayView!!.setCropFrameStrokeWidth(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)
            )
        )
        mOverlayView!!.setShowCropGrid(
            bundle.getBoolean(
                UCrop.Options.EXTRA_SHOW_CROP_GRID,
                OverlayView.DEFAULT_SHOW_CROP_GRID
            )
        )
        mOverlayView!!.setCropGridRowCount(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT,
                OverlayView.DEFAULT_CROP_GRID_ROW_COUNT
            )
        )
        mOverlayView!!.setCropGridColumnCount(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT,
                OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT
            )
        )
        mOverlayView!!.setCropGridColor(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_GRID_COLOR,
                resources.getColor(R.color.ucrop_color_default_crop_grid)
            )
        )
        mOverlayView!!.setCropGridStrokeWidth(
            bundle.getInt(
                UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)
            )
        )
        mOverlayView!!.setDimmedStrokeWidth(
            bundle.getInt(
                UCrop.Options.EXTRA_CIRCLE_STROKE_WIDTH_LAYER,
                resources.getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)
            )
        )
        // Aspect ratio options
        val aspectRatioX = bundle.getFloat(UCrop.EXTRA_ASPECT_RATIO_X, -1f)
        val aspectRatioY = bundle.getFloat(UCrop.EXTRA_ASPECT_RATIO_Y, -1f)
        val aspectRationSelectedByDefault = bundle.getInt(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        val aspectRatioList = bundle.getParcelableArrayList<AspectRatio>(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
        if (aspectRatioX >= 0 && aspectRatioY >= 0) {
            if (mWrapperStateAspectRatio != null) {
                mWrapperStateAspectRatio!!.visibility = View.GONE
            }
            val targetAspectRatio = aspectRatioX / aspectRatioY
            mGestureCropImageView!!.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size) {
            val targetAspectRatio =
                aspectRatioList[aspectRationSelectedByDefault].aspectRatioX / aspectRatioList[aspectRationSelectedByDefault].aspectRatioY
            mGestureCropImageView!!.targetAspectRatio =
                if (java.lang.Float.isNaN(targetAspectRatio)) CropImageView.SOURCE_IMAGE_ASPECT_RATIO else targetAspectRatio
        } else {
            mGestureCropImageView!!.targetAspectRatio = CropImageView.SOURCE_IMAGE_ASPECT_RATIO
        }

        // Result bitmap max size options
        val maxSizeX = bundle.getInt(UCrop.EXTRA_MAX_SIZE_X, 0)
        val maxSizeY = bundle.getInt(UCrop.EXTRA_MAX_SIZE_Y, 0)
        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView!!.setMaxResultImageSizeX(maxSizeX)
            mGestureCropImageView!!.setMaxResultImageSizeY(maxSizeY)
        }
    }

    private fun initiateRootViews(view: View) {
        mUCropView = view.findViewById(R.id.ucrop)
        mGestureCropImageView = mUCropView?.cropImageView
        mOverlayView = mUCropView?.overlayView
        mGestureCropImageView?.setTransformImageListener(mImageListener)
        view.findViewById<View>(R.id.ucrop_frame)?.setBackgroundColor(mRootViewBackgroundColor)
    }

    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {
            setAngleText(currentAngle)
        }

        override fun onScale(currentScale: Float) {
            setScaleText(currentScale)
        }

        override fun onLoadComplete() {
            mUCropView!!.animate().alpha(1f).setDuration(300).interpolator = AccelerateInterpolator()
            mBlockingView!!.isClickable = false
            callback!!.loadingProgress(false)
            if (arguments!!.getBoolean(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)) {
                val inputUri = arguments!!.getParcelable<Uri>(UCrop.EXTRA_INPUT_URI)
                val mimeType = FileUtils.getMimeTypeFromMediaContentUri(context, inputUri)
                if (FileUtils.isGif(mimeType) || FileUtils.isWebp(mimeType)) {
                    mBlockingView!!.isClickable = true
                }
            }
        }

        override fun onLoadFailure(e: Exception) {
            callback!!.onCropFinish(getError(e))
        }
    }

    /**
     * Use [.mActiveControlsWidgetColor] for color filter
     */
    private fun setupStatesWrapper(view: View) {
        val stateScaleImageView = view.findViewById<ImageView>(R.id.image_view_state_scale)
        val stateRotateImageView = view.findViewById<ImageView>(R.id.image_view_state_rotate)
        val stateAspectRatioImageView = view.findViewById<ImageView>(R.id.image_view_state_aspect_ratio)
        stateScaleImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateScaleImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
        stateRotateImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateRotateImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
        stateAspectRatioImageView.setImageDrawable(
            SelectedStateListDrawable(
                stateAspectRatioImageView.drawable,
                mActiveControlsWidgetColor
            )
        )
    }

    private fun setupAspectRatioWidget(bundle: Bundle, view: View) {
        var aspectRationSelectedByDefault = bundle.getInt(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0)
        var aspectRatioList = bundle.getParcelableArrayList<AspectRatio?>(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
        if (aspectRatioList == null || aspectRatioList.isEmpty()) {
            aspectRationSelectedByDefault = 2
            aspectRatioList = ArrayList()
            aspectRatioList.add(AspectRatio(null, 1f, 1f))
            aspectRatioList.add(AspectRatio(null, 3f, 4f))
            aspectRatioList.add(
                AspectRatio(
                    getString(R.string.ucrop_label_original).uppercase(Locale.getDefault()),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO
                )
            )
            aspectRatioList.add(AspectRatio(null, 3f, 2f))
            aspectRatioList.add(AspectRatio(null, 16f, 9f))
        }
        val wrapperAspectRatioList = view.findViewById<LinearLayout>(R.id.layout_aspect_ratio)
        var wrapperAspectRatio: FrameLayout
        var aspectRatioTextView: AspectRatioTextView
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        lp.weight = 1f
        for (aspectRatio in aspectRatioList) {
            wrapperAspectRatio = layoutInflater.inflate(R.layout.ucrop_aspect_ratio, null) as FrameLayout
            wrapperAspectRatio.layoutParams = lp
            aspectRatioTextView = wrapperAspectRatio.getChildAt(0) as AspectRatioTextView
            aspectRatioTextView.setActiveColor(mActiveControlsWidgetColor)
            aspectRatioTextView.setAspectRatio(aspectRatio!!)
            wrapperAspectRatioList.addView(wrapperAspectRatio)
            mCropAspectRatioViews.add(wrapperAspectRatio)
        }
        mCropAspectRatioViews[aspectRationSelectedByDefault].isSelected = true
        for (cropAspectRatioView in mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener { v ->
                mGestureCropImageView!!.targetAspectRatio =
                    ((v as ViewGroup).getChildAt(0) as AspectRatioTextView).getAspectRatio(v.isSelected())
                mGestureCropImageView!!.setImageToWrapCropBounds()
                if (!v.isSelected()) {
                    for (cropAspectRatioView in mCropAspectRatioViews) {
                        cropAspectRatioView.isSelected = cropAspectRatioView === v
                    }
                }
            }
        }
    }

    private fun setupRotateWidget(view: View) {
        mTextViewRotateAngle = view.findViewById(R.id.text_view_rotate)
        (view.findViewById<View>(R.id.rotate_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    mGestureCropImageView!!.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
                }

                override fun onScrollEnd() {
                    mGestureCropImageView!!.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView!!.cancelAllAnimations()
                }
            })
        (view.findViewById<View>(R.id.rotate_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor
        )
        view.findViewById<View>(R.id.wrapper_reset_rotate).setOnClickListener { resetRotation() }
        view.findViewById<View>(R.id.wrapper_rotate_by_angle).setOnClickListener { rotateByAngle(90) }
        setAngleTextColor(mActiveControlsWidgetColor)
    }

    private fun setupScaleWidget(view: View) {
        mTextViewScalePercent = view.findViewById(R.id.text_view_scale)
        (view.findViewById<View>(R.id.scale_scroll_wheel) as HorizontalProgressWheelView)
            .setScrollingListener(object : ScrollingListener {
                override fun onScroll(delta: Float, totalDistance: Float) {
                    if (delta > 0) {
                        mGestureCropImageView!!.zoomInImage(
                            mGestureCropImageView!!.currentScale
                                    + delta * ((mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    } else {
                        mGestureCropImageView!!.zoomOutImage(
                            mGestureCropImageView!!.currentScale
                                    + delta * ((mGestureCropImageView!!.maxScale - mGestureCropImageView!!.minScale) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT)
                        )
                    }
                }

                override fun onScrollEnd() {
                    mGestureCropImageView!!.setImageToWrapCropBounds()
                }

                override fun onScrollStart() {
                    mGestureCropImageView!!.cancelAllAnimations()
                }
            })
        (view.findViewById<View>(R.id.scale_scroll_wheel) as HorizontalProgressWheelView).setMiddleLineColor(
            mActiveControlsWidgetColor
        )
        setScaleTextColor(mActiveControlsWidgetColor)
    }

    private fun setAngleText(angle: Float) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle!!.text = String.format(Locale.getDefault(), "%.1f°", angle)
        }
    }

    private fun setAngleTextColor(textColor: Int) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle!!.setTextColor(textColor)
        }
    }

    private fun setScaleText(scale: Float) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent!!.text = String.format(Locale.getDefault(), "%d%%", (scale * 100).toInt())
        }
    }

    private fun setScaleTextColor(textColor: Int) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent!!.setTextColor(textColor)
        }
    }

    private fun resetRotation() {
        mGestureCropImageView!!.postRotate(-mGestureCropImageView!!.currentAngle)
        mGestureCropImageView!!.setImageToWrapCropBounds()
    }

    private fun rotateByAngle(angle: Int) {
        mGestureCropImageView!!.postRotate(angle.toFloat())
        mGestureCropImageView!!.setImageToWrapCropBounds()
    }

    private val mStateClickListener = View.OnClickListener { v: View ->
        if (!v.isSelected) {
            setWidgetState(v.id)
        }
    }

    private fun setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio!!.visibility == View.VISIBLE) {
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
        mWrapperStateAspectRatio!!.isSelected = stateViewId == R.id.state_aspect_ratio
        mWrapperStateRotate!!.isSelected = stateViewId == R.id.state_rotate
        mWrapperStateScale!!.isSelected = stateViewId == R.id.state_scale
        mLayoutAspectRatio!!.visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mLayoutRotate!!.visibility = if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
        mLayoutScale!!.visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
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
        if (view != null) {
            TransitionManager.beginDelayedTransition(
                (view?.findViewById<View>(R.id.ucrop_photobox) as ViewGroup),
                mControlsTransition
            )
        }
        mWrapperStateScale!!.findViewById<View>(R.id.text_view_scale).visibility =
            if (stateViewId == R.id.state_scale) View.VISIBLE else View.GONE
        mWrapperStateAspectRatio!!.findViewById<View>(R.id.text_view_crop).visibility =
            if (stateViewId == R.id.state_aspect_ratio) View.VISIBLE else View.GONE
        mWrapperStateRotate!!.findViewById<View>(R.id.text_view_rotate).visibility =
            if (stateViewId == R.id.state_rotate) View.VISIBLE else View.GONE
    }

    private fun setAllowedGestures(tab: Int) {
        mGestureCropImageView!!.isScaleEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE
        mGestureCropImageView!!.isRotateEnabled =
            mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE
        mGestureCropImageView!!.isGestureEnabled = arguments?.getBoolean(UCrop.Options.EXTRA_DRAG_IMAGES, true)?:true
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private fun addBlockingView(view: View) {
        if (mBlockingView == null) {
            mBlockingView = View(context)
            val lp =
                RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            mBlockingView!!.layoutParams = lp
            mBlockingView!!.isClickable = true
        }
        (view.findViewById<View>(R.id.ucrop_photobox) as RelativeLayout).addView(mBlockingView)
    }

    fun cropAndSaveImage() {
        mBlockingView!!.isClickable = true
        callback!!.loadingProgress(true)
        mGestureCropImageView!!.cropAndSaveImage(mCompressFormat, mCompressQuality, object : BitmapCropCallback {
            override fun onBitmapCropped(
                resultUri: Uri,
                offsetX: Int,
                offsetY: Int,
                imageWidth: Int,
                imageHeight: Int
            ) {
                callback!!.onCropFinish(
                    getResult(
                        resultUri,
                        mGestureCropImageView!!.targetAspectRatio,
                        offsetX,
                        offsetY,
                        imageWidth,
                        imageHeight
                    )
                )
                callback!!.loadingProgress(false)
            }

            override fun onCropFailure(t: Throwable) {
                callback!!.onCropFinish(getError(t))
            }
        })
    }

    protected fun getResult(
        uri: Uri?,
        resultAspectRatio: Float,
        offsetX: Int,
        offsetY: Int,
        imageWidth: Int,
        imageHeight: Int
    ): UCropResult {
        val inputUri = arguments?.getParcelable<Uri>(UCrop.EXTRA_INPUT_URI)
        return UCropResult(
            Activity.RESULT_OK, Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                .putExtra(UCrop.EXTRA_CROP_INPUT_ORIGINAL, inputUri)
        )
    }

    protected fun getError(throwable: Throwable?): UCropResult {
        return UCropResult(
            UCrop.RESULT_ERROR, Intent()
                .putExtra(UCrop.EXTRA_ERROR, throwable)
        )
    }

    class UCropResult(var mResultCode: Int, var mResultData: Intent)
    companion object {
        const val DEFAULT_COMPRESS_QUALITY = 90
        val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        const val NONE = 0
        const val SCALE = 1
        const val ROTATE = 2
        const val ALL = 3
        val TAG = UCropFragment::class.java.simpleName
        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }

        @JvmStatic
        fun newInstance(uCrop: Bundle?): UCropFragment {
            val fragment = UCropFragment()
            fragment.arguments = uCrop
            return fragment
        }
    }
}