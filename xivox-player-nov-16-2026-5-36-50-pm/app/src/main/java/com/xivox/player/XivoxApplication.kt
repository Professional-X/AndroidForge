package com.xivox.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.xivox.player.data.local.db.XivoxDatabase
import com.xivox.player.data.local.prefs.XivoxPreferencesRepository
import com.xivox.player.data.media.MediaScannerRepository

class XivoxApplication : Application(), ImageLoaderFactory {

    val database: XivoxDatabase by lazy { XivoxDatabase.getInstance(this) }
    val preferencesRepository: XivoxPreferencesRepository by lazy { XivoxPreferencesRepository(this) }
    val mediaScannerRepository: MediaScannerRepository by lazy { MediaScannerRepository(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("xivox_thumb_cache"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
