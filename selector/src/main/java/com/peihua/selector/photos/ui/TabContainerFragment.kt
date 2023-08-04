package com.peihua.selector.photos.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.peihua.photopicker.R
import java.lang.ref.WeakReference
import java.lang.reflect.Field

/**
 * The tab container fragment
 */
class TabContainerFragment : Fragment() {
    private var mTabContainerAdapter: TabContainerAdapter? = null
    private var mTabLayoutMediator: TabLayoutMediator? = null
    private var mViewPager: ViewPager2? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.picker_fragment_picker_tab_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewPager = view.findViewById(R.id.picker_tab_viewpager)
        mTabContainerAdapter = TabContainerAdapter( /* fragment */this)
        mViewPager?.adapter = mTabContainerAdapter

        // If the ViewPager2 has more than one page with BottomSheetBehavior, the scrolled view
        // (e.g. RecyclerView) on the second page can't be scrolled. The workaround is to update
        // nestedScrollingChildRef to the scrolled view on the current page. b/145334244
        var fieldNestedScrollingChildRef: Field? = null
        try {
            fieldNestedScrollingChildRef = BottomSheetBehavior::class.java.getDeclaredField("nestedScrollingChildRef")
            fieldNestedScrollingChildRef.isAccessible = true
        } catch (ex: NoSuchFieldException) {
            Log.d(TAG, "Can't get the field nestedScrollingChildRef from BottomSheetBehavior", ex)
        }
        val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(
            requireActivity().findViewById(R.id.bottom_sheet)
        )
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(AnimationPageTransformer())
        compositePageTransformer.addTransformer(
            NestedScrollPageTransformer(bottomSheetBehavior, fieldNestedScrollingChildRef)
        )
        mViewPager?.apply {
            setPageTransformer(compositePageTransformer)
            // The BottomSheetBehavior looks for the first nested scrolling child to determine how to
            // handle nested scrolls, it finds the inner recyclerView on ViewPager2 in this case. So, we
            // need to work around it by setNestedScrollingEnabled false. b/145351873
            val firstChild = getChildAt(0)
            if (firstChild is RecyclerView) {
                getChildAt(0).isNestedScrollingEnabled = false
            }
            val tabLayout = requireActivity().findViewById<TabLayout>(R.id.tab_layout)
            mTabLayoutMediator = TabLayoutMediator(tabLayout, this) { tab: TabLayout.Tab, pos: Int ->
                if (pos == PHOTOS_TAB_POSITION) {
                    tab.setText(R.string.picker_photos)
                } else if (pos == ALBUMS_TAB_POSITION) {
                    tab.setText(R.string.picker_albums)
                }
            }
            mTabLayoutMediator!!.attach()
            // TabLayout only supports colorDrawable in xml. And if we set the color in the drawable by
            // setSelectedTabIndicator method, it doesn't apply the color. So, we set color in xml and
            // set the drawable for the shape here.
            tabLayout.setSelectedTabIndicator(R.drawable.picker_tab_indicator)
        }
    }

    override fun onResume() {
        super.onResume()
        mTabContainerAdapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        mTabLayoutMediator?.detach()
        super.onDestroyView()
    }

    private class AnimationPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, pos: Float) {
            view.alpha = 1.0f - Math.abs(pos)
        }
    }

    private class NestedScrollPageTransformer(
        private val mBottomSheetBehavior: BottomSheetBehavior<*>,
        private val mFieldNestedScrollingChildRef: Field?
    ) : ViewPager2.PageTransformer {
        override fun transformPage(view: View, pos: Float) {
            // If pos != 0, it is not in current page, don't update the nested scrolling child
            // reference.
            if (pos != 0f || mFieldNestedScrollingChildRef == null) {
                return
            }
            try {
                val childView = view.findViewById<View>(R.id.picker_tab_recyclerview)
                if (childView != null) {
                    mFieldNestedScrollingChildRef[mBottomSheetBehavior] = WeakReference<Any?>(childView)
                }
            } catch (ex: IllegalAccessException) {
                Log.d(TAG, "Set nestedScrollingChildRef to BottomSheetBehavior fail", ex)
            }
        }
    }

    companion object {
        private const val TAG = "TabContainerFragment"
        private const val PHOTOS_TAB_POSITION = 0
        private const val ALBUMS_TAB_POSITION = 1

        /**
         * Create the fragment and add it into the FragmentManager
         *
         * @param fm the fragment manager
         */
        fun show(fm: FragmentManager) {
            val ft = fm.beginTransaction()
            val fragment = TabContainerFragment()
            ft.replace(R.id.fragment_container, fragment, TAG)
            ft.commitAllowingStateLoss()
        }
    }
}