/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peihua.selector.photos.ui

import android.view.View
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.peihua.photopicker.R
import com.peihua.selector.data.MuteStatus
import com.peihua.selector.data.model.Item

/**
 * A wrapper class to assist in initializing [ViewPager2] and [PreviewAdapter]. This
 * class also supports some of [ViewPager2] and [PreviewAdapter] methods to avoid
 * exposing these objects outside this class.
 * The class also supports registering [ViewPager2.OnPageChangeCallback] and unregister the
 * same onDestroy().
 */
internal class ViewPager2Wrapper(
    private val mViewPager: ViewPager2,
    selectedItems: List<Item>,
    muteStatus: MuteStatus?
) {
    private val mAdapter: PreviewAdapter
    private val mOnPageChangeCallbacks: MutableList<ViewPager2.OnPageChangeCallback> = ArrayList()

    init {
        val context = mViewPager.context
        mAdapter = PreviewAdapter(context, muteStatus)
        mAdapter.updateItemList(selectedItems)
        mViewPager.adapter = mAdapter
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(
            MarginPageTransformer(
                context.resources.getDimensionPixelSize(R.dimen.preview_viewpager_margin)
            )
        )
        compositePageTransformer.addTransformer(PlayerPageTransformer())
        mViewPager.setPageTransformer(compositePageTransformer)
    }

    /**
     * Registers given [ViewPager2.OnPageChangeCallback] to the [ViewPager2]. This class
     * also takes care of unregistering the callback onDestroy()
     */
    fun addOnPageChangeCallback(onPageChangeCallback: ViewPager2.OnPageChangeCallback) {
        mOnPageChangeCallbacks.add(onPageChangeCallback)
        mViewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    fun getItemAt(position: Int): Item {
        return getItemAtInternal(position)
    }

    val currentItem: Item
        get() = getItemAtInternal(mViewPager.currentItem)

    private fun getItemAtInternal(position: Int): Item {
        return mAdapter.getItem(position)
    }

    fun onStop() {
        mAdapter.onStop()
    }

    fun onStart() {
        // TODO(b/197083539): Restore the playback state here.
        // This forces PageTransformer#transformPage call and assists in ExoPlayer initialization.
        mViewPager.requestTransform()
    }

    fun onDestroy() {
        for (callback in mOnPageChangeCallbacks) {
            mViewPager.unregisterOnPageChangeCallback(callback)
        }
        mOnPageChangeCallbacks.clear()
        mAdapter.onDestroy()
    }

    private inner class PlayerPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            // We are only interested in position == 0.0. Only position=0.0 indicates that the page
            // is selected.
            if (position != 0f) return
            mAdapter.onHandlePageSelected(view)
        }
    }
}