package app.morphe.patches.tiktok.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.util.addInstruction
import app.morphe.util.addInstructions
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val WHAT = "pixkk settings row"
private const val SCREEN = "Lapp/pixkk/extension/core/SettingsScreen;"
private const val WHATS_NEW_VM =
    "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/cells/WhatsNewCellVM;"
private const val SUPPORT_GROUP_VM =
    "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/SupportGroupVM;"

/** The What's New row's state: its constructor writes the row's title as a plain string. */
internal object WhatsNewStateFingerprint : Fingerprint(
    name = "<init>",
    returnType = "V",
    strings = listOf("What's New"),
)

/** What a tap on the What's New row does, from both of the places that call it. */
internal object WhatsNewClickFingerprint : Fingerprint(
    definingClass = WHATS_NEW_VM,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, _ -> method.name != "<init>" },
)

/** The support group builds its rows; What's New is added only behind two checks. */
internal object SupportGroupRowsFingerprint : Fingerprint(
    definingClass = SUPPORT_GROUP_VM,
    custom = { method, _ ->
        method.implementation?.instructions?.any {
            it.getReference<FieldReference>()?.name == "WHATS_NEW"
        } == true
    },
)

@Suppress("unused")
val settingsRowPatch = bytecodePatch(
    name = WHAT,
    description = "Turns the What's New row in Settings and privacy into the pixkk settings. The " +
        "Hushfeed row, if its bundle is applied too, is left alone.",
) {
    category("Interface")
    compatibleWith(*AppCompatibilities.tiktok())
    dependsOn(sharedExtensionPatch)

    execute {
        // The row is only built behind a test-build flag and a feature switch; show it always.
        SupportGroupRowsFingerprint.method.apply {
            val instructions = implementation!!.instructions.toList()
            val whatsNew = instructions.indices.filter {
                instructions[it].getReference<FieldReference>()?.name == "WHATS_NEW"
            }
            if (whatsNew.size < 2) throw PatchException("$WHAT: the What's New checks are gone")

            // The switch: `move-result` then `if-eqz` right before the row is added.
            val switchResult = whatsNew[1] - 2
            if (instructions[switchResult].opcode != Opcode.MOVE_RESULT) {
                throw PatchException("$WHAT: the What's New switch has changed")
            }
            // The flag: the last boolean read of the group's own field before the first mention.
            val flagRead = (whatsNew[0] - 1 downTo 0).firstOrNull {
                instructions[it].opcode == Opcode.IGET_BOOLEAN &&
                    instructions[it].getReference<FieldReference>()?.definingClass == SUPPORT_GROUP_VM
            } ?: throw PatchException("$WHAT: the What's New flag is gone")

            // The later one first, so the earlier index stays where it is.
            val switchRegister = (instructions[switchResult] as OneRegisterInstruction).registerA
            addInstruction(switchResult + 1, "const/4 v$switchRegister, 0x1")
            val flagRegister = (instructions[flagRead] as OneRegisterInstruction).registerA
            addInstruction(flagRead + 1, "const/4 v$flagRegister, 0x1")
        }

        WhatsNewStateFingerprint.method.apply {
            val index = implementation!!.instructions.indexOfFirst {
                it.getReference<StringReference>()?.string == "What's New"
            }
            val register = (getInstruction(index) as OneRegisterInstruction).registerA
            replaceInstruction(index, "const-string v$register, \"pixkk\"")
        }

        WhatsNewClickFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $SCREEN->show(Landroid/content/Context;)V
                return-void
            """,
        )
    }
}
