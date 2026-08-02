package com.ailiveoverflow

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import android.util.Base64

class SkinManager(private val context: Context) {
    companion object {
        private const val TAG = "SkinManager"
        private const val SKIN_DIR_NAME = "skins"
        private const val SKIN_FILE = "skin.png"
    }

    private val skinsDir: File
        get() = File(context.filesDir, SKIN_DIR_NAME)

    init {
        skinsDir.mkdirs()
    }

    fun listSkins(): List<String> {
        return skinsDir.listFiles()
            ?.filter { it.isDirectory && it.name != "." && it.name != ".." }
            ?.map { it.name }.orEmpty().sorted()
    }

    fun hasSkin(name: String): Boolean {
        return File(skinsDir, "$name/$SKIN_FILE").exists()
    }

    /** 把皮肤 PNG 读成 base64 数据字符串 */
    fun getSkinBase64(name: String): String? {
        val file = File(skinsDir, "$name/$SKIN_FILE")
        return if (file.exists()) {
            try {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read skin $name", e)
                null
            }
        } else null
    }

    /** 删除皮肤 */
    fun deleteSkin(name: String): Boolean {
        val dir = File(skinsDir, name)
        if (!dir.exists()) return false
        dir.deleteRecursively()
        return true
    }

    /** 从 assets 复制内置皮肤 */
    fun copyAssetSkin(assetPath: String, destName: String): Boolean {
        return try {
            val destDir = File(skinsDir, destName)
            destDir.mkdirs()
            val assetStream = context.assets.open("skins/$assetPath/$SKIN_FILE")
            val destFile = File(destDir, SKIN_FILE)
            assetStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset skin", e)
            false
        }
    }

    /** 从 assets 目录列出所有皮肤文件夹 */
    fun listAssetSkins(): List<String> {
        return try {
            context.assets.list("skins")?.toList().orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 渲染皮肤到 HTML——使用 PNG + base64 + CSS 动画表达不同状态 */
    fun renderSkinToHtml(name: String): String {
        val pngBase64 = getSkinBase64(name) ?: ""
        val imgSrc = if (pngBase64.isNotEmpty()) {
            "data:image/png;base64,$pngBase64"
        } else {
            ""
        }

        return """
<!DOCTYPE html>
<html>
<head>
<meta content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" name="viewport"/>
<style>
* { margin: 0; padding: 0; }
html, body { width: 100%; height: 100%; overflow: hidden; background: transparent; }
.stage { position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.character {
    position: relative; width: 100%; height: 100%;
    object-fit: contain;
    animation: breathe 3s ease-in-out infinite;
    transition: transform 0.3s ease, opacity 0.3s ease;
    transform-origin: center bottom;
}
.bubble {
    position: absolute; bottom: 95%; left: 50%;
    transform: translateX(-50%);
    background: white; border-radius: 16px;
    padding: 8px 14px; font-size: 13px; max-width: 200px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    display: none; text-align: center; font-family: sans-serif;
}
.bubble.visible { display: block; animation: fadeIn 0.3s; }
.bubble.pink { background: #FFE4F0; }
.bubble.gray { background: #E8E8E8; }
.bubble.red { background: #FFE4E4; }
.bubble.green { background: #E4F5E4; }
.bubble::after {
    content: \'\'; position: absolute; top: 100%; left: 50%;
    border: 8px solid transparent; border-top-color: white;
    transform: translateX(-50%);
}
.bubble.pink::after { border-top-color: #FFE4F0; }
.bubble.gray::after { border-top-color: #E8E8E8; }
.bubble.red::after { border-top-color: #FFE4E4; }
.bubble.green::after { border-top-color: #E4F5E4; }
.heat-overlay {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    pointer-events: none; border-radius: 50%;
    background: radial-gradient(circle, rgba(255,0,0,0.3) 0%, rgba(255,0,0,0) 70%);
    opacity: 0;
}
@keyframes breathe {
    0%, 100% { transform: translateY(0) scale(1); }
    50% { transform: translateY(-3px) scale(1.01); }
}
@keyframes fadeIn {
    from { opacity: 0; transform: translateX(-50%) translateY(-10px); }
    to { opacity: 1; transform: translateX(-50%) translateY(0); }
}
</style>
</head>
<body>
<div class="stage">
    <div id="heat" class="heat-overlay"></div>
    <img id="char" class="character" src="$imgSrc" alt=""/>
    <div id="bubble" class="bubble"></div>
</div>
<script>
function setFrame(name) {
    var char = document.getElementById("char");
    if (!char) return;
    // reset
    char.style.animation = "";
    char.style.transform = "";
    char.style.opacity = "1";
    if (name === "idle") {
        char.style.animation = "breathe 3s ease-in-out infinite";
    } else if (name === "blink") {
        char.style.animation = "none";
        char.style.opacity = "0.3";
        setTimeout(function(){ char.style.opacity = "1"; char.style.animation = "breathe 3s ease-in-out infinite"; }, 300);
    } else if (name === "happy") {
        char.style.animation = "none";
        char.style.transform = "scale(1.08)";
        setTimeout(function(){ char.style.transform = ""; char.style.animation = "breathe 3s ease-in-out infinite"; }, 600);
    } else if (name === "sleep") {
        char.style.animation = "none";
        char.style.opacity = "0.5";
        char.style.transform = "scale(0.92)";
    } else if (name === "angry") {
        char.style.animation = "none";
        char.style.transform = "scale(0.95)";
    } else if (name === "sad") {
        char.style.animation = "none";
        char.style.transform = "scale(0.95)";
        char.style.opacity = "0.7";
    } else if (name === "wake") {
        char.style.animation = "none";
        char.style.transform = "scale(1.12)";
        setTimeout(function(){ char.style.transform = ""; char.style.animation = "breathe 3s ease-in-out infinite"; }, 500);
    } else if (name === "charging") {
        char.style.animation = "breathe 2s ease-in-out infinite";
    }
}
function showBubble(text, style) {
    var bubble = document.getElementById("bubble");
    if (!bubble) return;
    bubble.textContent = text;
    bubble.className = "bubble visible " + (style || "white");
    clearTimeout(bubble._timer);
    bubble._timer = setTimeout(function() {
        bubble.classList.remove("visible");
    }, 4000);
}
function setHeat(level) {
    var heat = document.getElementById("heat");
    if (heat) heat.style.opacity = (level / 100).toString();
}
</script>
</body>
</html>
""".trimIndent()
    }
}
