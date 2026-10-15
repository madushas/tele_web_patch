package app.template.patches.telegram.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.telegram.ChatActivityAddSponsoredMessagesFingerprint
import app.template.patches.telegram.ChatActivityGetSponsoredMessagesCountFingerprint
import app.template.patches.telegram.MessageObjectIsSponsoredFingerprint
import app.template.patches.telegram.MessagesControllerGetSponsoredMessagesFingerprint

@Suppress("unused")
val telegramRemoveAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes sponsored messages from all chats and channels.",
) {
    compatibleWith(TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        // Block sponsored messages from being injected into chat list
        ChatActivityAddSponsoredMessagesFingerprint.method.addInstructions(0, "return-void")

        // Return 0 count so UI never shows a sponsored message slot
        ChatActivityGetSponsoredMessagesCountFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """)

        // Return null from getSponsoredMessages so no data is fetched
        MessagesControllerGetSponsoredMessagesFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            return-object v0
        """)

        // No message object is ever marked as sponsored
        MessageObjectIsSponsoredFingerprint.method.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """)
    }
}
