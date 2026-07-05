package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.ViewModelFactory
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable Edge-to-Edge display
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Initialize database, DAO, repository, and ViewModel using standard, clean patterns
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context)
                val repository = AppRepository(
                    noteDao = database.noteDao(),
                    chatMessageDao = database.chatMessageDao()
                )
                
                val mainViewModel: MainViewModel = viewModel(
                    factory = ViewModelFactory(repository)
                )

                AppMainLayout(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun AppMainLayout(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.NOTES,
                    onClick = { viewModel.setScreen(Screen.NOTES) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = "Notes Screen Icon"
                        )
                    },
                    label = {
                        Text(
                            text = "Workspace",
                            fontWeight = if (currentScreen == Screen.NOTES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(),
                    modifier = Modifier.testTag("nav_notes_item")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CHAT,
                    onClick = { viewModel.setScreen(Screen.CHAT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = "Chat Screen Icon"
                        )
                    },
                    label = {
                        Text(
                            text = "AI Chat",
                            fontWeight = if (currentScreen == Screen.CHAT) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(),
                    modifier = Modifier.testTag("nav_chat_item")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.NOTES -> {
                    NotesScreen(viewModel = viewModel)
                }
                Screen.CHAT -> {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}
