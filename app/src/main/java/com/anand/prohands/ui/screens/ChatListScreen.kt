package com.anand.prohands.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.chat.ConversationWithParticipants
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatListViewModel
import com.anand.prohands.viewmodel.SearchViewModel
import com.anand.prohands.viewmodel.SearchViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    currentUserId: String,
    viewModel: ChatListViewModel
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "Status", "Calls")
    
    // State for searching new conversation
    var isSearching by remember { mutableStateOf(false) }
    
    // Using SearchViewModel for "New Chat" search logic
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
    
    if (isSearching) {
        NewConversationSearchScreen(
            onBack = { isSearching = false },
            onUserSelected = { userId ->
                isSearching = false
                navController.navigate("chat/$userId")
            },
            viewModel = searchViewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ProHands Chat", fontWeight = FontWeight.Bold, color = ProColors.OnPrimary) },
                    actions = {
                        // Camera icon - disabled for now to prevent crash
                        IconButton(
                            onClick = {
                                // TODO: Implement camera feature properly
                                // Currently disabled to prevent crash
                            },
                            enabled = false
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                "Camera",
                                tint = ProColors.OnPrimary.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = { isSearching = true }) { Icon(Icons.Default.Search, "Search", tint = ProColors.OnPrimary) }
                        IconButton(onClick = { /* TODO: More Actions */ }) { Icon(Icons.Default.MoreVert, "More", tint = ProColors.OnPrimary) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ProColors.Primary)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isSearching = true }, 
                    containerColor = ProColors.PrimaryVariant,
                    contentColor = ProColors.OnPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Chat")
                }
            },
            containerColor = ProColors.Surface // White background
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ProColors.Primary,
                    contentColor = ProColors.OnPrimary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = ProColors.OnPrimary
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Show content based on selected tab
                when (selectedTab) {
                    0 -> { // Chats
                        ChatListContent(
                            conversations = conversations,
                            currentUserId = currentUserId,
                            isLoading = isLoading,
                            error = error,
                            onRefresh = { viewModel.refreshConversations() },
                            onChatClick = { userId -> navController.navigate("chat/$userId") }
                        )
                    }
                    1 -> { // Status
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Status feature coming soon!", color = ProColors.TextSecondary)
                        }
                    }
                    2 -> { // Calls
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Calls feature coming soon!", color = ProColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    conversations: List<ConversationWithParticipants>,
    currentUserId: String,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onChatClick: (String) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(ProColors.Surface)
    ) {
        when {
            // Show error state
            error != null && conversations.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ProColors.Error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                        ) {
                            Text("Retry", color = ProColors.OnPrimary)
                        }
                    }
                }
            }
            // Show empty state
            conversations.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = ProColors.PrimaryLight
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = ProColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap + to start a new chat!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ProColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            // Show conversations list
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = conversations,
                        key = { it.conversation.chatId }
                    ) { conversation ->
                        val otherParticipant = conversation.participants.find { it.userId != currentUserId }
                        if (otherParticipant != null) {
                            ConversationItem(
                                conversation = conversation,
                                otherParticipant = otherParticipant,
                                currentUserId = currentUserId
                            ) {
                                onChatClick(otherParticipant.userId)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationSearchScreen(
    onBack: () -> Unit,
    onUserSelected: (String) -> Unit,
    viewModel: SearchViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChanged(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search users...", color = ProColors.OnPrimary.copy(alpha = 0.7f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = ProColors.OnPrimary,
                            focusedTextColor = ProColors.OnPrimary,
                            unfocusedTextColor = ProColors.OnPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.titleMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ProColors.OnPrimary)
                    }
                },
                actions = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Clear", tint = ProColors.OnPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProColors.Primary)
            )
        },
        containerColor = ProColors.Background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading && searchResults.isEmpty()) {
                CircularProgressIndicator(
                    color = ProColors.Primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (searchResults.isEmpty() && searchQuery.length >= 2 && !isLoading) {
                 Text(
                    text = "No users found matching \"$searchQuery\"",
                    color = ProColors.TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { result ->
                        UserSearchItem(
                            user = result.profile,
                            onClick = { onUserSelected(result.profile.userId) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: ClientProfileDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter = if (!user.profilePictureUrl.isNullOrEmpty()) {
            rememberAsyncImagePainter(user.profilePictureUrl)
        } else {
            rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${user.name ?: "User"}&background=random")
        }
        
        Image(
            painter = painter,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = user.name ?: "Unknown User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ProColors.TextPrimary
            )
            
            // Fixed: Safely check for skills list being null or empty
            val skillsText = if (user.skills != null && user.skills.isNotEmpty()) {
                user.skills.take(2).joinToString(", ")
            } else {
                "Worker"
            }
            
            Text(
                text = skillsText,
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationWithParticipants,
    otherParticipant: com.anand.prohands.data.chat.ParticipantEntity,
    currentUserId: String,
    onClick: () -> Unit
) {
    val unreadCount = conversation.conversation.unreadCounts[currentUserId] ?: 0
    val hasUnread = unreadCount > 0

    // Card-based design for conversation item
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnread) ProColors.PrimaryContainer.copy(alpha = 0.3f) else ProColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (hasUnread) 2.dp else 0.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture with Online Status Indicator
            Box {
                val painter = if (!otherParticipant.profilePictureUrl.isNullOrEmpty()) {
                    rememberAsyncImagePainter(
                        model = otherParticipant.profilePictureUrl
                            ?.replace("http://", "https://")
                            ?.replace(".heic", ".jpg")
                    )
                } else {
                    rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${otherParticipant.name}&background=26A69A&color=fff&size=128")
                }

                Image(
                    painter = painter,
                    contentDescription = "Profile Picture of ${otherParticipant.name}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ProColors.PrimaryContainer),
                    contentScale = ContentScale.Crop
                )

                // Online status indicator
                if (otherParticipant.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .background(ProColors.Surface, CircleShape)
                            .padding(2.dp)
                            .background(ProColors.Online, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User Info & Message Content
            Column(modifier = Modifier.weight(1f)) {
                // Row 1: Username and Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Username
                    Text(
                        text = otherParticipant.name.ifEmpty { "User" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = ProColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Timestamp
                    conversation.conversation.lastMessageTimestamp?.let {
                        Text(
                            text = formatTime(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasUnread) ProColors.Primary else ProColors.TextTertiary,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Row 2: User ID
                Text(
                    text = "@${otherParticipant.userId.take(12)}${if (otherParticipant.userId.length > 12) "..." else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Row 3: Last Message and Unread Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Last message preview
                    Text(
                        text = conversation.conversation.lastMessage ?: "No messages yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) ProColors.TextPrimary else ProColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread count badge
                    if (hasUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                                .clip(CircleShape)
                                .background(ProColors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                color = ProColors.OnPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: String): String {
    val date = try {
        // Parse ISO 8601 timestamp compatible with API 24+
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        isoFormat.parse(timestamp.substringBefore('.').substringBefore('Z')) ?: Date()
    } catch (_: Exception) {
        Date() // Fallback to current time
    }
    
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.YEAR) != messageDate.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DAY_OF_YEAR) - messageDate.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    }
}
