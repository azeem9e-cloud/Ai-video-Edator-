package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.projects.ProjectsListScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.templates.TemplatesScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    PROJECTS,
    TEMPLATES,
    SETTINGS
}

@Composable
fun AppNavigation(
    editorViewModel: EditorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val projectRepo = remember { ProjectRepository(db.projectDao()) }
    val projectsList by projectRepo.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var isEditorOpen by remember { mutableStateOf(false) }

    if (isEditorOpen) {
        EditorScreen(
            viewModel = editorViewModel,
            onNavigateBack = { isEditorOpen = false },
            modifier = modifier
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = VibeSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.HOME,
                        onClick = { currentTab = MainTab.HOME },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldLight,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldDark.copy(alpha = 0.35f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.PROJECTS,
                        onClick = { currentTab = MainTab.PROJECTS },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.PROJECTS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "Projects"
                            )
                        },
                        label = { Text("Projects", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldLight,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldDark.copy(alpha = 0.35f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.TEMPLATES,
                        onClick = { currentTab = MainTab.TEMPLATES },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.TEMPLATES) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Templates"
                            )
                        },
                        label = { Text("Templates", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldLight,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldDark.copy(alpha = 0.35f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldLight,
                            selectedTextColor = EmeraldLight,
                            indicatorColor = EmeraldDark.copy(alpha = 0.35f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            },
            containerColor = VibeBackground,
            modifier = modifier
        ) { innerPadding ->
            when (currentTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        recentProjects = projectsList,
                        onOpenProject = { projectId ->
                            editorViewModel.loadProject(projectId)
                            isEditorOpen = true
                        },
                        onImportVideo = { uri ->
                            editorViewModel.loadTemplate("template_cyberpunk")
                            editorViewModel.importVideoUri(uri)
                            isEditorOpen = true
                        },
                        onSelectTemplate = { templateId ->
                            editorViewModel.loadTemplate(templateId)
                            isEditorOpen = true
                        },
                        onNewProject = {
                            editorViewModel.loadTemplate("template_cyberpunk")
                            isEditorOpen = true
                        },
                        onOpenTemplates = {
                            currentTab = MainTab.TEMPLATES
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.PROJECTS -> {
                    ProjectsListScreen(
                        projects = projectsList,
                        onOpenProject = { projectId ->
                            editorViewModel.loadProject(projectId)
                            isEditorOpen = true
                        },
                        onDuplicateProject = { projectId ->
                            coroutineScope.launch {
                                projectRepo.duplicateProject(projectId)
                            }
                        },
                        onDeleteProject = { projectId ->
                            coroutineScope.launch {
                                projectRepo.deleteProject(projectId)
                            }
                        },
                        onNavigateBack = { currentTab = MainTab.HOME },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.TEMPLATES -> {
                    TemplatesScreen(
                        onSelectTemplate = { templateId ->
                            editorViewModel.loadTemplate(templateId)
                            isEditorOpen = true
                        },
                        onNavigateBack = { currentTab = MainTab.HOME },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                MainTab.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = { currentTab = MainTab.HOME },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
