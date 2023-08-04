package com.peihua.selector.photos.ui

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for [TabContainerFragment]'s ViewPager2 to show [PhotosTabFragment] and
 * [AlbumsTabFragment].
 */
class TabContainerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return TAB_COUNT
    }

    override fun createFragment(pos: Int): Fragment {
        return if (pos == 0) {
            PhotosTabFragment()
        } else AlbumsTabFragment()
    }

    companion object {
        private const val TAB_COUNT = 2
    }
}