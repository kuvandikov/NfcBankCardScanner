package io.github.kuvandikov.scancardnfc.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuvandikov.scancardnfc.NfcCardScanner
import io.github.kuvandikov.scancardnfc.model.CardExpiry
import io.github.kuvandikov.scancardnfc.model.EmvCard
import io.github.kuvandikov.scancardnfc.model.EmvCardScheme
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

private val Background = Color(0xFF0F1115)
private val Surface = Color(0xFF1A1D24)
private val Accent = Color(0xFF7F52FF) // Kotlin purple
private val TextPrimary = Color(0xFFF2F3F5)
private val TextSecondary = Color(0xFF9AA0AB)

private sealed interface UiState {
    data object Idle : UiState
    data object Scanning : UiState
    data class Done(val result: NfcScanResult) : UiState
}

/** Demo screen shared by the Android and iOS sample apps. */
@Composable
fun App(scanner: NfcCardScanner) {
    var state by remember { mutableStateOf<UiState>(UiState.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeContentPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BasicText("NFC Card Reader", style = TextStyle(TextPrimary, 28.sp, FontWeight.Bold))
        BasicText(
            "Kotlin Multiplatform • Android & iOS",
            style = TextStyle(TextSecondary, 14.sp),
        )

        Spacer(Modifier.height(8.dp))

        when (val s = state) {
            UiState.Idle -> Hint(if (scanner.isSupported) "Tap “Scan card” and hold a contactless card to the phone." else "This device does not support NFC.")
            UiState.Scanning -> Hint("Waiting for a card…")
            is UiState.Done -> ResultView(s.result)
        }

        Spacer(Modifier.weight(1f))

        val scanning = state == UiState.Scanning
        ActionButton(
            text = if (scanning) "Cancel" else "Scan card",
            enabled = scanner.isSupported,
        ) {
            if (scanning) {
                job?.cancel()
                state = UiState.Idle
            } else {
                state = UiState.Scanning
                job = scope.launch { state = UiState.Done(scanner.scan()) }
            }
        }
    }
}

@Composable
private fun ResultView(result: NfcScanResult) {
    when (result) {
        is NfcScanResult.Success -> CardView(result.card)
        NfcScanResult.CardMovedTooFast -> Hint("The card was removed too early. Hold it still and try again.")
        NfcScanResult.CardBlocked -> Hint("This card is blocked. Contact the issuing bank.")
        NfcScanResult.NfcDisabled -> Hint("NFC is turned off. Enable it in system settings.")
        NfcScanResult.NfcNotSupported -> Hint("This device does not support NFC.")
        NfcScanResult.Cancelled -> Hint("Scan cancelled.")
        is NfcScanResult.Error -> Hint("Error: ${result.type}${result.message?.let { "\n$it" } ?: ""}")
    }
}

@Composable
private fun CardView(card: EmvCard) {
    val mono = TextStyle(TextPrimary, 22.sp, FontWeight.Medium, fontFamily = FontFamily.Monospace)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // ID-1 card proportions
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Accent, Color(0xFFE44857))))
            .padding(20.dp),
    ) {
        BasicText(card.scheme.displayName, Modifier.align(Alignment.TopEnd), TextStyle(TextPrimary, 18.sp, FontWeight.Bold))
        card.applicationLabel?.let {
            BasicText(it, Modifier.align(Alignment.TopStart), TextStyle(TextPrimary.copy(alpha = 0.8f), 13.sp))
        }
        BasicText(card.maskedPan, Modifier.align(Alignment.CenterStart), mono)
        Column(Modifier.align(Alignment.BottomStart)) {
            card.holderName?.let { BasicText(it, style = TextStyle(TextPrimary, 14.sp)) }
            BasicText("VALID THRU ${card.expiry ?: "--/--"}", style = TextStyle(TextPrimary, 14.sp))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        BasicText(text, style = TextStyle(TextSecondary, 16.sp))
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Accent else Surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TextStyle(TextPrimary, 17.sp, FontWeight.SemiBold))
    }
}

/** Scanner stub for previews: never reads a card. */
private class PreviewScanner(override val isSupported: Boolean) : NfcCardScanner {
    override val isEnabled: Boolean = isSupported
    override suspend fun scan(): NfcScanResult = awaitCancellation()
}

private val PreviewCard = EmvCard(
    pan = "4111111111111111",
    expiry = CardExpiry(month = 8, year = 2029),
    scheme = EmvCardScheme.VISA,
    holderName = "JOHN DOE",
    aid = "A0000000031010",
    applicationLabel = "VISA DEBIT",
)

@Preview
@Composable
private fun AppPreview() {
    App(PreviewScanner(isSupported = true))
}

@Preview
@Composable
private fun AppNfcNotSupportedPreview() {
    App(PreviewScanner(isSupported = false))
}

@Preview
@Composable
private fun ResultViewPreview() {
    Column(
        Modifier.background(Background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResultView(NfcScanResult.Success(PreviewCard))
        ResultView(NfcScanResult.CardMovedTooFast)
    }
}
