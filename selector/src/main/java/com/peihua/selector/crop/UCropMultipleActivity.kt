package com.peihua.selector.crop

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fz.common.utils.getDrawableCompat
import com.fz.common.utils.getParcelableArrayListExtraCompat
import com.fz.common.utils.getParcelableCompat
import com.fz.common.utils.toMapOf
import com.peihua.photopicker.R
import com.peihua.selector.crop.UCropFragment.UCropResult
import com.peihua.selector.crop.decoration.GridSpacingItemDecoration
import com.peihua.selector.crop.model.AspectRatio
import com.peihua.selector.crop.model.CustomIntentKey
import com.peihua.selector.crop.util.DensityUtil
import com.peihua.selector.crop.util.FileUtils
import com.peihua.selector.result.contract.PhotoCropVisualMedia
import com.peihua.selector.util.isAtLeastM
import org.json.JSONArray
import java.io.File


/**
 * @author：luck
 * @date：2021/11/28 7:59 下午
 * @describe：UCropMultipleActivity
 */
class UCropMultipleActivity : AppCompatActivity(), UCropFragmentCallback {
    private var mToolbarTitle: String? = null

    @DrawableRes
    private var mToolbarCropDrawable = 0
    @ColorInt
    private var mToolBarIconColor = Color.WHITE
    private var mShowLoader = false
    private val fragments: MutableList<UCropFragment> = ArrayList()
    private var uCropCurrentFragment: UCropFragment? = null
    private var currentFragmentPosition = 0
    private var uCropSupportList: ArrayList<String>? = null
    private var uCropNotSupportList: ArrayList<String>? = null
    private val uCropTotalQueue = LinkedHashMap<Uri, Bundle>()
    private var outputCropFileName: String? = null
    private var galleryAdapter: UCropGalleryAdapter? = null
    private var isForbidCropGifWebp = false
    private var isSkipCropForbid = false
    private var aspectRatioList: ArrayList<AspectRatio>? = null
    private val filterSet = HashSet<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(R.style.PickerCropMaterialTheme,  /* force */false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.picker_crop_activity_multiple)
        //        mToolbarWidgetColor = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget));
//        mToolbarCropDrawable = intent.getIntExtra(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE, R.drawable.ucrop_ic_done);
        setupViews(intent)
        initCropFragments(intent)
    }

    private fun initCropFragments(intent: Intent) {
        isSkipCropForbid = intent.getBooleanExtra(UCrop.Options.EXTRA_CROP_FORBID_SKIP, false)
        val totalCropData =
            intent.getParcelableArrayListExtraCompat(UCrop.EXTRA_CROP_TOTAL_DATA_SOURCE, Uri::class.java)
        require(totalCropData.size > 0) { "Missing required parameters, count cannot be less than 1" }
        uCropSupportList = ArrayList()
        uCropNotSupportList = ArrayList()
        for (i in totalCropData.indices) {
            val uri = totalCropData[i]
            uCropTotalQueue[uri] = Bundle()
            val realPath = FileUtils.getPath(this, uri)
            val mimeType = getPathToMimeType(realPath)
            if (FileUtils.isUrlHasVideo(realPath) || FileUtils.isHasVideo(mimeType) || FileUtils.isHasAudio(mimeType)) {
                // not crop type
                uCropNotSupportList!!.add(realPath)
            } else {
                uCropSupportList!!.add(realPath)
                val extras = intent.extras ?: continue
                val postfix = FileUtils.getPostfixDefaultJPEG(
                    this@UCropMultipleActivity,
                    isForbidCropGifWebp, uri
                )
                val fileName =
                    if (TextUtils.isEmpty(outputCropFileName)) FileUtils.getCreateFileName("CROP_" + (i + 1)) + postfix else (i + 1).toString() + FileUtils.getCreateFileName() + "_" + outputCropFileName
                val destinationUri = Uri.fromFile(File(sandboxPathDir, fileName))
                extras.putParcelable(UCrop.EXTRA_INPUT_URI, uri)
                extras.putParcelable(UCrop.EXTRA_OUTPUT_URI, destinationUri)
                val aspectRatio = aspectRatioList?.getOrNull(i)
                extras.putFloat(UCrop.EXTRA_ASPECT_RATIO_X, aspectRatio?.aspectRatioX ?: -1f)
                extras.putFloat(UCrop.EXTRA_ASPECT_RATIO_Y, aspectRatio?.aspectRatioY ?: -1f)
                val uCropFragment = UCropFragment.newInstance(extras)
                fragments.add(uCropFragment)
            }
        }
        require(uCropSupportList!!.size != 0) { "No clipping data sources are available" }
        setGalleryAdapter()
        val uCropFragment = fragments[cropSupportPosition]
        switchCropFragment(uCropFragment, cropSupportPosition)
        galleryAdapter!!.currentSelectPosition = cropSupportPosition
    }

    private val cropSupportPosition: Int
        /**
         * getCropSupportPosition
         *
         * @return
         */
        get() {
            var position = 0
            val intent = intent
            val extras = intent.extras ?: return position
            val skipCropMimeType = extras.getStringArrayList(UCrop.Options.EXTRA_SKIP_CROP_MIME_TYPE)
            if (skipCropMimeType != null && skipCropMimeType.size > 0) {
                position = -1
                filterSet.addAll(skipCropMimeType)
                for (i in uCropSupportList!!.indices) {
                    val path = uCropSupportList!![i]
                    val mimeType = getPathToMimeType(path)
                    position++
                    if (!filterSet.contains(mimeType)) {
                        break
                    }
                }
                if (position == -1 || position > fragments.size) {
                    position = 0
                }
            }
            return position
        }

    /**
     * getPathToMimeType
     *
     * @param path
     * @return
     */
    private fun getPathToMimeType(path: String): String {
        val mimeType: String
        mimeType = if (FileUtils.isContent(path)) {
            FileUtils.getMimeTypeFromMediaContentUri(this, Uri.parse(path))
        } else {
            FileUtils.getMimeTypeFromMediaContentUri(
                this,
                Uri.fromFile(File(path))
            )
        }
        return mimeType
    }

    /**
     * switch crop fragment tab
     *
     * @param targetFragment target fragment
     * @param position       target index
     */
    private fun switchCropFragment(targetFragment: UCropFragment, position: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        if (!targetFragment.isAdded) {
            if (uCropCurrentFragment != null) {
                transaction.hide(uCropCurrentFragment!!)
            }
            transaction.add(R.id.fragment_container, targetFragment, UCropFragment.TAG + "-" + position)
        } else {
            transaction.hide(uCropCurrentFragment!!).show(targetFragment)
            targetFragment.fragmentReVisible()
        }
        currentFragmentPosition = position
        uCropCurrentFragment = targetFragment
        transaction.commitAllowingStateLoss()
    }

    private fun setGalleryAdapter() {
        val galleryRecycle = findViewById<RecyclerView>(R.id.recycler_gallery)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        galleryRecycle.layoutManager = layoutManager
        if (galleryRecycle.itemDecorationCount == 0) {
            galleryRecycle.addItemDecoration(
                GridSpacingItemDecoration(
                    Int.MAX_VALUE,
                    DensityUtil.dip2px(this, 6f), true
                )
            )
        }
        val animation = AnimationUtils
            .loadLayoutAnimation(this, R.anim.picker_crop_layout_animation_fall_down)
        galleryRecycle.layoutAnimation = animation
        val galleryBarBackground = intent.getIntExtra(
            UCrop.Options.EXTRA_GALLERY_BAR_BACKGROUND,
            R.drawable.picker_crop_gallery_bg
        )
        galleryRecycle.setBackgroundResource(galleryBarBackground)
        galleryAdapter = UCropGalleryAdapter(uCropSupportList)
        galleryAdapter!!.setOnItemClickListener OnItemClickListener@{ position, view ->
            if (isSkipCropForbid) {
                return@OnItemClickListener
            }
            val path = uCropSupportList!![position]
            val mimeType = getPathToMimeType(path)
            if (filterSet.contains(mimeType)) {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.picker_crop_not_crop), Toast.LENGTH_SHORT
                ).show()
                return@OnItemClickListener
            }
            if (galleryAdapter!!.currentSelectPosition == position) {
                return@OnItemClickListener
            }
            galleryAdapter!!.notifyItemChanged(galleryAdapter!!.currentSelectPosition)
            galleryAdapter!!.currentSelectPosition = position
            galleryAdapter!!.notifyItemChanged(position)
            val uCropFragment = fragments[position]
            switchCropFragment(uCropFragment, position)
        }
        galleryRecycle.adapter = galleryAdapter
    }

    private val sandboxPathDir: String
        /**
         * create crop output path dir
         *
         * @return
         */
        private get() {
            val customFile: File
            val outputDir = intent.getStringExtra(UCrop.Options.EXTRA_CROP_OUTPUT_DIR)
            customFile = if (outputDir == null || "" == outputDir) {
                File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath, "Sandbox")
            } else {
                File(outputDir)
            }
            if (!customFile.exists()) {
                customFile.mkdirs()
            }
            return customFile.absolutePath + File.separator
        }

    private fun setupViews(intent: Intent) {
        aspectRatioList = getIntent().getParcelableArrayListExtra(UCrop.Options.EXTRA_MULTIPLE_ASPECT_RATIO)
        isForbidCropGifWebp = intent.getBooleanExtra(UCrop.Options.EXTRA_CROP_FORBID_GIF_WEBP, false)
        outputCropFileName = intent.getStringExtra(UCrop.Options.EXTRA_CROP_OUTPUT_FILE_NAME)
        mToolbarTitle = intent.getStringExtra(UCrop.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR)
        mToolbarTitle =
            if (mToolbarTitle != null) mToolbarTitle else resources.getString(R.string.picker_crop_label_edit_photo)
        setupAppBar()
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private fun setupAppBar() {
        val attrs = intArrayOf(R.attr.pickerToolbarCropMenuDrawable, android.R.attr.colorControlNormal)
        val ta = obtainStyledAttributes(attrs)
        mToolbarCropDrawable = ta.getResourceId( /* index */0,  /* defValue */R.drawable.picker_crop_ic_done)
        // Save toolbar height so that we can use it as padding for FragmentContainerView
        mToolBarIconColor = ta.getColor( /* index */1,  /* defValue */Color.WHITE)
        ta.recycle()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set all of the Toolbar coloring
        toolbar.title = mToolbarTitle
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)
        setStatusBarColor(if (isAtLeastM) Color.TRANSPARENT else Color.parseColor("#20000000"))
    }

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    private fun setStatusBarColor(@ColorInt color: Int) {
        val window = window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = color
        }
    }

    override fun loadingProgress(showLoader: Boolean) {
        mShowLoader = showLoader
        supportInvalidateOptionsMenu()
    }

    override fun onCropFinish(result: UCropResult) {
        when (result.mResultCode) {
            RESULT_OK -> {
                val realPosition = currentFragmentPosition + uCropNotSupportList!!.size
                val realTotalSize = uCropNotSupportList!!.size + uCropSupportList!!.size - 1
                mergeCropResult(result.mResultData)
                if (realPosition == realTotalSize) {
                    onCropCompleteFinish()
                } else {
                    var nextFragmentPosition = currentFragmentPosition + 1
                    var path = uCropSupportList!![nextFragmentPosition]
                    var mimeType = getPathToMimeType(path)
                    var isCropCompleteFinish = false
                    while (filterSet.contains(mimeType)) {
                        if (nextFragmentPosition == realTotalSize) {
                            isCropCompleteFinish = true
                            break
                        } else {
                            nextFragmentPosition += 1
                            path = uCropSupportList!![nextFragmentPosition]
                            mimeType = getPathToMimeType(path)
                        }
                    }
                    if (isCropCompleteFinish) {
                        onCropCompleteFinish()
                    } else {
                        val uCropFragment = fragments[nextFragmentPosition]
                        switchCropFragment(uCropFragment, nextFragmentPosition)
                        galleryAdapter!!.notifyItemChanged(galleryAdapter!!.currentSelectPosition)
                        galleryAdapter!!.currentSelectPosition = nextFragmentPosition
                        galleryAdapter!!.notifyItemChanged(galleryAdapter!!.currentSelectPosition)
                    }
                }
            }

            UCrop.RESULT_ERROR -> handleCropError(result.mResultData)
        }
    }

    /**
     * onCropCompleteFinish
     */
    private fun onCropCompleteFinish() {
        val array = JSONArray()
        val uris = arrayListOf<Uri>()
        for ((_, item) in uCropTotalQueue.entries) {
            val uri = item.getParcelableCompat(CustomIntentKey.EXTRA_OUT_PUT_PATH, Uri::class.java)
            if (uri != null) {
                uris.add(uri)
                array.put(item.toMapOf())
            }
        }
        val intent = Intent()
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uris)
        intent.putExtra(PhotoCropVisualMedia.EXTRA_OUTPUT_WHOLE_DATA, array.toString())
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * merge crop result
     *
     * @param intent
     */
    private fun mergeCropResult(intent: Intent) {
        try {
            val outUri = intent.getParcelableExtra<Uri>(UCrop.EXTRA_CROP_INPUT_ORIGINAL)
            if (outUri != null) {
                val bundle = uCropTotalQueue[outUri]
                if (bundle != null) {
                    val output = UCrop.getOutput(intent)
                    bundle.putParcelable(CustomIntentKey.EXTRA_OUT_PUT_PATH, output)
                    bundle.putInt(CustomIntentKey.EXTRA_IMAGE_WIDTH, UCrop.getOutputImageWidth(intent))
                    bundle.putInt(CustomIntentKey.EXTRA_IMAGE_HEIGHT, UCrop.getOutputImageHeight(intent))
                    bundle.putInt(CustomIntentKey.EXTRA_OFFSET_X, UCrop.getOutputImageOffsetX(intent))
                    bundle.putInt(CustomIntentKey.EXTRA_OFFSET_Y, UCrop.getOutputImageOffsetY(intent))
                    bundle.putFloat(CustomIntentKey.EXTRA_ASPECT_RATIO, UCrop.getOutputCropAspectRatio(intent))
                    uCropTotalQueue[outUri] = bundle
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCropError(result: Intent) {
        val cropError = UCrop.getError(result)
        if (cropError != null) {
            Toast.makeText(this@UCropMultipleActivity, cropError.message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this@UCropMultipleActivity, "Unexpected error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picker_menu_activity, menu)

        // Change crop & loader menu icons color to match the rest of the UI colors
        val menuItemLoader = menu.findItem(R.id.menu_loader)
        val menuItemLoaderIcon = menuItemLoader.icon
        if (menuItemLoaderIcon != null) {
            try {
                val drawable= menuItemLoaderIcon.mutate()
                drawable.setTint(mToolBarIconColor)
                menuItemLoader.icon = drawable
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            (menuItemLoader.icon as Animatable?)!!.start()
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
            if (uCropCurrentFragment != null && uCropCurrentFragment!!.isAdded) {
                uCropCurrentFragment!!.cropAndSaveImage()
            }
        } else if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}