package com.ailiveoverflow

import android.content.Context
import android.util.Log
import java.io.File

/** 首次启动时把内置皮肤从 assets 复制到私有目录 */
class SkinInitializer(private val context: Context) {
    companion object {
        private const val TAG = "SkinInitializer"
    }

    fun init() {
        val skinManager = SkinManager(context)
        val assets = context.assets

        try {
            val assetSkinNames = assets.list("skins") ?: return

            for (assetSkin in assetSkinNames) {
                if (!skinManager.hasSkin(assetSkin)) {
                    Log.d(TAG, "Installing built-in skin: $assetSkin")
                    skinManager.copyAssetSkin(assetSkin, assetSkin)
                }
            }

            // 设置默认皮肤为 cat
            if (skinManager.listSkins().isNotEmpty()) {
                val sm = StateManager(context)
                if (sm.currentSkin == "default") {
                    sm.currentSkin = "pixel_head"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Skin init failed", e)
        }
    }
}