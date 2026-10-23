/*
 * Copyright 2026 Hushfeed contributors
 * https://github.com/SysAdminDoc/hushfeed
 */
package app.morphe.patches.tiktok.interaction.videooverlays

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.interaction.authorregion.DetailActivityOnCreateFingerprint
import app.morphe.patches.tiktok.misc.extension.MainActivityOnCreateFingerprint
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.util.addInstruction

private const val EXTENSION = "Lapp/pixkk/extension/feed/RailSpacing;"

@Suppress("unused")
val railSpacingPatch = bytecodePatch(
    name = "Right column spacing",
    description = "Lets you widen or close up the gap between the buttons down the right of a " +
        "video: avatar, like, comments, save and share. The gap is set in Hushfeed settings > " +
        "Feed screen > Right column, and 0 leaves TikTok's own spacing.",
) {
    category("Interface")
    compatibleWith(*AppCompatibilities.tiktok())
    dependsOn(sharedExtensionPatch)

    execute {
        // p0 is the activity. /range because a parameter register is usually above v15. The
        // detail page too: a video opened from a profile or a search plays there, not in the feed.
        listOf(MainActivityOnCreateFingerprint.method, DetailActivityOnCreateFingerprint.method).forEach {
            it.addInstruction(
                0,
                "invoke-static/range { p0 .. p0 }, $EXTENSION->install(Landroid/app/Activity;)V",
            )
        }
    }
}
