package com.anand.prohands.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anand.prohands.data.chat.ChatDatabase
import com.anand.prohands.data.chat.ChatRepository
import com.anand.prohands.network.ChatService
import com.anand.prohands.network.RetrofitClient
import com.anand.prohands.ui.screens.*
import com.anand.prohands.viewmodel.*

@Composable
fun NavGraph(
    navController: NavHostController,
    currentUserId: String,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
) {
    // --- Dependency Setup for Chat --- //
    val context = LocalContext.current
    val chatDb = remember { ChatDatabase.getDatabase(context) }
    val chatService = remember { RetrofitClient.instance.create(ChatService::class.java) }
    val chatRepository = remember { ChatRepository(chatDb.chatDao(), chatService) }
    
    // Create WhatsAppViewModel for global state (e.g. tracking active chat) if needed
    val whatsAppViewModel: WhatsAppViewModel = viewModel(
        factory = WhatsAppViewModelFactory(chatRepository, currentUserId)
    )

    LaunchedEffect(currentUserId) {
        chatRepository.currentUserId = currentUserId
    }

    NavHost(
        navController = navController,
        startDestination = BottomNavigation.Home.route
    ) {
        composable(BottomNavigation.Home.route) {
            HomeScreen(
                navController = navController,
                currentUserId = currentUserId,
                onProfileClick = { navController.navigate(BottomNavigation.Profile.route) },
                onMessagesClick = { navController.navigate("chat_list") },
                onNotificationsClick = { /* Navigate to notifications */ }
            )
        }
        composable(BottomNavigation.Search.route) {
            SearchScreen(navController = navController)
        }
        // ... other composables ...

        composable("chat_list") {
            // Ensure global chat state knows we aren't in a specific chat
            LaunchedEffect(Unit) {
                whatsAppViewModel.setActiveChat(null)
            }
            
            val chatListViewModel: ChatListViewModel = viewModel(
                factory = ChatListViewModelFactory(chatRepository, currentUserId)
            )
            ChatListScreen(
                navController = navController, 
                currentUserId = currentUserId,
                viewModel = chatListViewModel
            )
        }

        composable(
            "chat/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipientId = backStackEntry.arguments?.getString("userId") ?: ""
            val chatId = if (currentUserId < recipientId) "${currentUserId}_${recipientId}" else "${recipientId}_${currentUserId}"

            // Update active chat ID in global state
            LaunchedEffect(chatId) {
                whatsAppViewModel.setActiveChat(chatId)
            }

            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(chatRepository, currentUserId, recipientId, chatId)
            )
            MessagesScreen(
                navController = navController,
                chatViewModel = chatViewModel,
                // Passing these as arguments for the top bar
                recipientName = "", // You would fetch the user's name from a repository here
                recipientAvatar = "" // And the avatar URL
            )
        }
         // ... other composables ...
         composable(BottomNavigation.PostJob.route) {
            PostJobScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(BottomNavigation.Jobs.route) {
            ManageJobsScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(BottomNavigation.Profile.route) {
            val shouldRefresh = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefresh")
            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh == true) {
                    authViewModel.fetchProfile()
                    navController.currentBackStackEntry?.savedStateHandle?.set("shouldRefresh", false)
                }
            }
            ProfileScreen(
                userId = currentUserId,
                isReadOnly = false,
                onEditProfile = { navController.navigate("edit_profile/$currentUserId") },
                onLogout = onLogout,
                onJobClick = { jobId -> navController.navigate("job_details/$jobId") }
            )
        }
        composable("create_job") {
            PostJobScreen(navController = navController, currentUserId = currentUserId)
        }
        composable(
            "edit_job/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            EditJobScreen(navController = navController, jobId = jobId)
        }
        composable(
            "edit_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId") ?: ""
            EditProfileScreen(navController = navController, userId = userId)
        }
        composable(
            "worker_recommendations/{jobId}/{lat}/{lon}/{title}",
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 0.0
            val title = backStackEntry.arguments?.getString("title") ?: ""
            WorkerRecommendationsScreen(
                navController = navController,
                jobId = jobId,
                latitude = lat,
                longitude = lon,
                jobTitle = title
            )
        }
        composable(
            "worker_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(
                userId = userId, 
                isReadOnly = true,
                onMessageClick = { navController.navigate("chat/$userId") }
            )
        }
        composable(
            "job_details/{jobId}",
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDetailsScreen(jobId = jobId, navController = navController)
        }
    }
}
