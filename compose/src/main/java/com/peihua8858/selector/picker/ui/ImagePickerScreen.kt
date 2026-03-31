package com.peihua8858.selector.picker.ui

import android.net.Uriimport androidx.compose.foundation.shape.RoundedCornerShapeimport androidx.compose.material3.MaterialThemeimport androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun ImagePickerScreen(
    state: PickerState,
    onBack: () -> Unit,
    onConfirm: (List<Uri>) -> Unit,
    theme: PickerTheme = PickerThemeDefaults.theme()
) {
    // 使用 theme.colors / typography / shapes
}
data class PickerColors(
    val topBarColor: Color,
    val backgroundColor: Color,
    val albumTextColor: Color,
    val selectedColor: Color,
    val confirmButtonColor: Color,
)

data class PickerTypography(
    val titleTextStyle: TextStyle,
    val labelTextStyle: TextStyle,
)

data class PickerShapes(
    val itemShape: Shape,
)

data class PickerTheme(
    val colors: PickerColors,
    val typography: PickerTypography,
    val shapes: PickerShapes,
)
object PickerThemeDefaults {
    @Composable
    fun theme(): PickerTheme {
        return PickerTheme(
            colors = PickerColors(
                topBarColor = MaterialTheme.colorScheme.surface,
                backgroundColor = MaterialTheme.colorScheme.background,
                albumTextColor = MaterialTheme.colorScheme.onBackground,
                selectedColor = MaterialTheme.colorScheme.primary,
                confirmButtonColor = MaterialTheme.colorScheme.primary,
            ),
            typography = PickerTypography(
                titleTextStyle = MaterialTheme.typography.titleLarge,
                labelTextStyle = MaterialTheme.typography.bodyMedium,
            ),
            shapes = PickerShapes(
                itemShape = RoundedCornerShape(8.dp),
            )
        )
    }
}