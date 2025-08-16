package com.example.augmentedrealityglasses

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavController, modifier: Modifier) {
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

    NavigationBar(
        modifier = modifier,
        containerColor = Color(0xFFFCF8F8) //FIXME: fix color
    ) {
        val currentDestination =
            navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { item ->
            val selected = currentDestination == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp), //default value for bottom bar icons
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = item.label,
                            color = if (selected) Color.Black else Color.Gray
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
                                .background(Color.Black)
                        )
                    }
                },
                selected = selected,
                onClick = {
                    if (currentDestination != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                        }
                    }
                }
            )
        }
    }
}

/**
 * It represents a single item of the bottom navigation bar (a single section)
 */
data class BottomNavItem(val label: String, val route: String, val icon: Painter)