package app.jammes.boletim.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class BottomItem(
    val route: String,
    val label: String,
    val iconActive: ImageVector,
    val iconInactive: ImageVector
)

private val bottomItens = listOf(
    BottomItem(Routes.ANO_LETIVO, "Ano Letivo", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomItem(Routes.BOLETIM, "Boletim", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    BottomItem(Routes.ALUNO, "Aluno", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomItens.any { it.route == currentRoute } || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomItens.forEach { item ->
                        val selected = item.route == currentRoute
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigate( item.route) },
                            icon = {
                                Icon(
                                    if (selected) item.iconActive else item.iconInactive,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.BOLETIM,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.BOLETIM) {
                Text("Boletim")
            }
            composable(Routes.ANO_LETIVO) {
                Text("Ano Letivo")
            }
            composable(Routes.ALUNO) {
                Text("Aluno")
            }
        }
    }
}