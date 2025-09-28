package com.example.augmentedrealityglasses.credits

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.augmentedrealityglasses.R

data class CreditEntry(
    val imageRes: Int,
    val title: String,
    val author: String,
    val licenseName: String? = null,
    val licenseUrl: String? = null
)

@Composable
fun CreditScreen(
    credits: List<CreditEntry> = getDefaultCredits()
) {
    val theme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CreditsTopBar()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = theme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    credits.forEachIndexed { index, entry ->
                        CreditRow(entry)
                        if (index != credits.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                thickness = 1.dp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CreditsTopBar() {
    val theme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.info),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = theme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Image Credits",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 25.sp),
            color = theme.primary
        )
    }
}

@Composable
private fun CreditRow(entry: CreditEntry) {
    val theme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.tertiaryContainer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = entry.imageRes),
            contentDescription = entry.title,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Transparent)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = theme.primary
            )
            Text(
                text = "Author: ${entry.author}",
                style = MaterialTheme.typography.bodySmall,
                color = theme.secondary
            )

            if (!entry.licenseName.isNullOrBlank()) {
                if (!entry.licenseUrl.isNullOrBlank()) {
                    val annotated = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = theme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("License: ")
                        }
                        pushStringAnnotation(tag = "URL", annotation = entry.licenseUrl)
                        withStyle(SpanStyle(color = theme.primary)) { append(entry.licenseName) }
                        pop()
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.primary,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(entry.licenseUrl))
                                )
                            }
                        }
                    )
                } else {
                    Text(
                        text = "License: ${entry.licenseName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.secondary
                    )
                }
            }
        }
    }
}

private fun getDefaultCredits(): List<CreditEntry> = listOf(
    CreditEntry(
        imageRes = R.drawable.clear,
        title = "Clear day",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.clear_night,
        title = "Clear night",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),

    CreditEntry(
        imageRes = R.drawable.clouds_1,
        title = "Clouds with sun",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.clouds_1_night,
        title = "Clouds with moon",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.clouds_2,
        title = "Clouds_1",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.clouds_3,
        title = "Clouds_2",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.fog,
        title = "Fog",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.humidity,
        title = "Humidity",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.pressure,
        title = "Pressure",
        author = "Good Ware – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/free-icon/gauge_4284060?related_id=4283902&origin=search"
    ),
    CreditEntry(
        imageRes = R.drawable.rain_1,
        title = "Rain with sun",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.rain_1_night,
        title = "Rain with moon",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.rain_2,
        title = "Rain_1",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.rain_3,
        title = "Rain_2",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.rain_4,
        title = "Rain_3",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.snow_1,
        title = "Snow with sun",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.snow_1_night,
        title = "Snow with moon",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.snow_2,
        title = "Snow_1",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.snow_3,
        title = "Snow_2",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.squall,
        title = "Squall",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.sunrise,
        title = "Sunrise",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.sunset,
        title = "Sunset",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.thunderstorm_1,
        title = "Thunderstorm with sun",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.thunderstorm_1_3_night,
        title = "Thunderstorm with moon",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.thunderstorm_2,
        title = "Thunderstorm_1",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.thunderstorm_3,
        title = "Thunderstorm_2",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.tornado,
        title = "Tornado",
        author = "iconixar – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/packs/weather-161"
    ),
    CreditEntry(
        imageRes = R.drawable.wind,
        title = "Wind",
        author = "kmg design – Flaticon",
        licenseName = "Flaticon License",
        licenseUrl = "https://www.flaticon.com/free-icon/wind_2529971?term=wind&page=3&position=43&origin=search&related_id=2529971"
    )
)
