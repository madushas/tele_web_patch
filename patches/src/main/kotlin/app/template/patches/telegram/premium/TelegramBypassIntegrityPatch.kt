package app.template.patches.telegram.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.AndroidUtilitiesGetCertFingerprintFingerprint
import app.template.patches.telegram.SafetyNetCheckFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// SHA-256 of the original signing certificate (X.509 DER).
// Computed as: SHA-256(CertificateFactory("X509").generateCertificate(pkcs7Stream).encoded)
// i.e. the hash of the DER-encoded X.509 cert extracted from the APK's META-INF/[name].RSA PKCS7 blob.
// This is exactly what getCertificateSHA256Fingerprint() computes at runtime.
//
// Do NOT use the raw PKCS7 SHA-256 — that's a different (larger) byte sequence.
//
// org.telegram.messenger.web 10.2.0 (40569) — VK / Nikolay Kudashov signing key.
// The 10.2.0 APK uses the same key as current releases, so the value is unchanged.
private const val CERT_HASH = "49C1522548EBACD46CE322B6FD47F6092BB745D0F88082145CAF35E14DCC38E1"

@Suppress("unused")
val telegramBypassIntegrityPatch = bytecodePatch(
    name = "Bypass integrity check",
    description = "Spoofs certificate fingerprint and SafetyNet results so login works on patched APK.",
) {
    compatibleWith(TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        // Return the original cert SHA-256 so server-side cert checks pass
        AndroidUtilitiesGetCertFingerprintFingerprint.method.addInstructions(0, """
            const-string v0, "$CERT_HASH"
            return-object v0
        """)

        // Force basicIntegrity and ctsProfileMatch to true.
        // The two optBoolean() results land in the move-result right after the
        // JSONObject.optBoolean() call that follows each exact const-string.
        // Log messages that merely mention the key names are skipped by the
        // exact string comparison, so no unrelated instruction is touched.
        SafetyNetCheckFingerprint.method.apply {
            val instructions = implementation!!.instructions.toList()
            instructions.forEachIndexed { index, instruction ->
                val reference = (instruction as? ReferenceInstruction)?.reference as? StringReference
                    ?: return@forEachIndexed
                if (reference.string != "basicIntegrity" && reference.string != "ctsProfileMatch") {
                    return@forEachIndexed
                }
                for (patchIndex in index + 1 until minOf(index + 4, instructions.size)) {
                    val target = instructions[patchIndex]
                    if (target.opcode == Opcode.MOVE_RESULT ||
                        target.opcode == Opcode.MOVE_RESULT_OBJECT ||
                        target.opcode == Opcode.MOVE_RESULT_WIDE
                    ) {
                        val reg = (target as OneRegisterInstruction).registerA
                        replaceInstruction(patchIndex, "const/4 v$reg, 0x1")
                        break
                    }
                }
            }
        }
    }
}
