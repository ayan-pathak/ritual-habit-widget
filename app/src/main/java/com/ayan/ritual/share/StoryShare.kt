package com.ayan.ritual.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.ayan.ritual.render.ShareCardRenderer
import com.ayan.ritual.render.SlabModel
import java.io.File
import java.io.FileOutputStream

/**
 * Puts a streak card into an Instagram story.
 *
 * Instagram's story intent is the good path: it drops the image straight onto
 * the story canvas with the app's colours behind it. It is only available when
 * Instagram is installed and new enough, so every call falls back to the normal
 * share sheet rather than dead-ending.
 *
 * **A story cannot be made tappable from out here.** Instagram's `content_url`
 * extra attaches an attribution link, but only for a sharing app registered
 * with a Facebook app id and only for accounts Instagram considers eligible
 * for links; without both it is dropped silently, and there is no callback to
 * say which happened. A link sticker, which any account can now add, is placed
 * by the person posting inside Instagram's own editor, and nothing an intent
 * carries can place one for them.
 *
 * So the reliable half is done here instead: the store link goes on the
 * clipboard on the way out, which turns adding a sticker into a paste. The
 * attribution link is still attached when there is an app id to attach it
 * with, because it costs nothing and lands for the accounts it is allowed to.
 */
object StoryShare {

    private const val IG_PACKAGE = "com.instagram.android"
    private const val IG_STORY_ACTION = "com.instagram.share.ADD_TO_STORY"

    /** Where a stranger who sees the story should end up. */
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.ayan.ritual"

    /**
     * The Facebook app id Instagram wants before it will consider an
     * attribution link at all. Blank until there is one registered, and
     * `content_url` is simply ignored while it is.
     */
    private const val FACEBOOK_APP_ID = ""

    /** Where share cards are written; cleared each time so they never accumulate. */
    private fun shareDir(context: Context): File =
        File(context.cacheDir, "share").apply { mkdirs() }

    private fun writeCard(context: Context, bitmap: Bitmap): Uri {
        val dir = shareDir(context)
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "ritual-streak.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun isInstagramInstalled(context: Context): Boolean =
        runCatching {
            context.packageManager.getPackageInfo(IG_PACKAGE, 0)
            true
        }.getOrDefault(false)

    /**
     * Renders [model] and hands it to Instagram Stories, falling back to the
     * system share sheet. Returns false only if nothing at all could handle it.
     */
    fun shareStreak(context: Context, model: SlabModel): Boolean {
        val bitmap = ShareCardRenderer.render(model)
        val uri = runCatching { writeCard(context, bitmap) }.getOrNull() ?: return false
        bitmap.recycle()

        if (isInstagramInstalled(context)) {
            val story = Intent(IG_STORY_ACTION).apply {
                // The card is the background asset and nothing else. Handing
                // Instagram the same image again as `interactive_asset_uri`
                // makes it composite the card twice — once scaled to fill a
                // screen taller than 9:16, once as a sticker on top — and the
                // background's edges show past the sticker down either side.
                setDataAndType(uri, "image/png")
                // The two brand colours become the story's backdrop gradient,
                // filling whatever the card doesn't cover on a taller screen.
                putExtra("top_background_color", "#E7E3D4")
                putExtra("bottom_background_color", "#C9F73F")
                // source_application is a Facebook app id, not a package name;
                // content_url is only looked at when one is present.
                if (FACEBOOK_APP_ID.isNotEmpty()) {
                    putExtra("source_application", FACEBOOK_APP_ID)
                    putExtra("content_url", STORE_URL)
                }
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.grantUriPermission(IG_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (story.resolveActivity(context.packageManager) != null) {
                copyStoreLink(context)
                return runCatching { context.startActivity(story); true }.getOrDefault(false)
            }
        }

        // No Instagram, or an older build without the story intent.
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Everywhere that is not a story, the link can simply ride along.
            putExtra(Intent.EXTRA_TEXT, "${model.streak} days of ${model.title}. $STORE_URL")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share your streak")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(chooser); true }.getOrDefault(false)
    }

    /**
     * Puts the store link on the clipboard on the way to Instagram, so adding
     * a link sticker is a long press and a paste rather than typing a URL from
     * memory. Android 13 and up shows its own confirmation, so nothing here
     * announces it twice.
     */
    private fun copyStoreLink(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Ritual", STORE_URL))
    }
}
