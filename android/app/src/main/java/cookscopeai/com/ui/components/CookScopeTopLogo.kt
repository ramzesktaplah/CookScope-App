package cookscopeai.com.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cookscopeai.com.R

/**
 * Small centered brand logo shown at the top of main app screens.
 * Uses the rotating blue atom logo as the app identity mark.
 */
@Composable
fun CookScopeTopLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_brand_logo),
        contentDescription = "CookScope",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .size(48.dp),
        alignment = Alignment.Center,
    )
}
