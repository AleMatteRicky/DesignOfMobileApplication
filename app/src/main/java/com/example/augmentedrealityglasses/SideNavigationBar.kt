package com.example.augmentedrealityglasses

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun SideNavigationBar(navController: NavController, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    val items = listOf(
        BottomNavItem(
            "Home",
            ScreenName.HOME.name,
            painterResource(id = R.drawable.home_bottom_bar)
        ),
        BottomNavItem(
            "Translation",
            ScreenName.TRANSLATION_HOME_SCREEN.name,
            painterResource(id = R.drawable.translate_bottom_bar)
        ),
        BottomNavItem(
            "Weather",
            ScreenName.WEATHER_HOME_SCREEN.name,
            painterResource(id = R.drawable.weather_bottom_bar)
        ),
        BottomNavItem(
            "Settings",
            ScreenName.SETTINGS.name,
            painterResource(id = R.drawable.settings_bottom_bar)
        )
    )

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val selected = currentDestination == item.route

                val lineHeight by animateDpAsState(
                    targetValue = if (selected) 24.dp else 0.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = ""
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 25.dp)
                        .clickable {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(ScreenName.HOME.name)
                                }
                            }
                        }
                ) {

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(lineHeight)
                            .background(colorScheme.primary)
                    )

                    Icon(
                        painter = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) colorScheme.primary else colorScheme.secondary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(24.dp)
                    )
                }
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = Dp.Hairline,
            color = colorScheme.secondary.copy(alpha = 0.6f)
        )
    }
}