package com.braden.coffee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Dev-only preview of the adaptive launcher icon under common launcher masks. */
@Composable
private fun AdaptiveIcon(shape: Shape) {
    Box(Modifier.size(160.dp).clip(shape)) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(160.dp))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(160.dp))
    }
}

@Preview(widthDp = 560, heightDp = 200)
@Composable
private fun LauncherIconPreview() {
    Row(
        Modifier.background(Color(0xFFE8E8E8)).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AdaptiveIcon(CircleShape)
        AdaptiveIcon(RoundedCornerShape(40.dp))
        AdaptiveIcon(RoundedCornerShape(0.dp))
    }
}
