package com.example.augmentedrealityglasses

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController, modifier: Modifier) {

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.background)
    ) {

        HorizontalDivider(
            thickness = Dp.Hairline,
            color = MaterialTheme.colorScheme.secondary //todo
                .copy(alpha = 0.6f)
        )


        NavigationBar(
            modifier = modifier, containerColor = colorScheme.background
        ) {
            val currentDestination =
                navController.currentBackStackEntryAsState().value?.destination?.route

            items.forEach { item ->
                val selected = currentDestination == item.route

                NavigationBarItem(
                    icon = { //todo fill
                        Icon(
                            painter = item.icon,
                            contentDescription = item.label,
                            tint = if(selected) colorScheme.primary else colorScheme.secondary,
                            modifier = Modifier.size(24.dp), //default value for bottom bar icons
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                    ),
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
                                color = if (selected) colorScheme.primary else colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            //Animation of the black line under the text of the nav items
                            val lineWidth by animateDpAsState(
                                targetValue = if (selected) 24.dp else 0.dp,
                                animationSpec = tween(durationMillis = 300), label = ""
                            )

                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(lineWidth)
                                    .background(colorScheme.primary)
                            )
                        }
                    },
                    selected = selected,
                    onClick = {
                        if (currentDestination != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(ScreenName.HOME.name)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * It represents a single item of the bottom navigation bar (a single section)
 */
data class BottomNavItem(val label: String, val route: String, val icon: Painter)