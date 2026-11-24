package com.aistudymentor.data.repository

import android.content.Context
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.IOException

@OptIn(coil.annotation.ExperimentalCoilApi::class)
class OfflineImageCache(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    suspend fun prefetch(urls: List<String>) {
        urls.distinct().forEach { url ->
            when (val result = imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            )) {
                is SuccessResult -> Unit
                is ErrorResult -> throw IOException("Could not cache a synced question image.", result.throwable)
            }
        }
    }

    suspend fun clear() {
        imageLoader.diskCache?.clear()
    }
}
