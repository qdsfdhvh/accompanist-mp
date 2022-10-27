package moe.tlaster.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun DialogImpl(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        content = content,
    )
}

actual typealias DialogProperties = androidx.compose.ui.window.DialogProperties

actual fun DialogProperties(
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean
) = androidx.compose.ui.window.DialogProperties(
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
)
