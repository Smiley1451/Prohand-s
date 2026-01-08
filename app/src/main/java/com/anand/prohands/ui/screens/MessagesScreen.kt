package com.anand.prohands.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.anand.prohands.data.chat.*
import com.anand.prohands.ui.components.ChatVideoPlayer
import com.anand.prohands.ui.components.FullscreenVideoPlayer
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatItem
import com.anand.prohands.viewmodel.ChatUiState
import com.anand.prohands.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    recipientName: String, // These can be fallbacks if ViewModel doesn't load them
    recipientAvatar: String?
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val recipientUser by chatViewModel.recipientUser.collectAsState()
    val isOnline by chatViewModel.isOnline.collectAsState()
    val lastSeenTimestamp by chatViewModel.lastSeenTimestamp.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    val isRecording by chatViewModel.isRecording.collectAsState()
    val shouldScrollToBottom by chatViewModel.shouldScrollToBottom.collectAsState()
    val isLoadingHistory by chatViewModel.isLoadingHistory.collectAsState()
    val hasMoreMessages by chatViewModel.hasMoreMessages.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // --- Media Pickers ---
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendMediaMessage(it, MessageType.IMAGE, context) }
    }
    
    val videoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendMediaMessage(it, MessageType.VIDEO, context) }
    }

    // Camera Logic with permission handling
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageFile != null) {
            // Verify file exists and has content
            if (tempImageFile!!.exists() && tempImageFile!!.length() > 0) {
                android.util.Log.d("MessagesScreen", "Camera captured image: ${tempImageFile!!.absolutePath}, size: ${tempImageFile!!.length()}")
                // Use file-based sending which is more reliable for camera captures
                chatViewModel.sendMediaFromFile(tempImageFile!!, MessageType.IMAGE)
            } else {
                android.util.Log.e("MessagesScreen", "Camera file is empty or doesn't exist")
            }
        } else {
            android.util.Log.e("MessagesScreen", "Camera capture failed or cancelled, success=$success, file=${tempImageFile?.absolutePath}")
        }
    }

    // Camera permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            try {
                val directory = File(context.cacheDir, "images")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val file = File(directory, "captured_image_${System.currentTimeMillis()}.jpg")
                tempImageFile = file
                val authority = "${context.packageName}.fileprovider"
                tempImageUri = FileProvider.getUriForFile(context, authority, file)
                android.util.Log.d("MessagesScreen", "Launching camera with URI: $tempImageUri")
                cameraLauncher.launch(tempImageUri!!)
            } catch (e: Exception) {
                android.util.Log.e("MessagesScreen", "Error launching camera", e)
            }
        }
    }

    fun launchCamera() {
        // Request camera permission first
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Voice Recording Logic
    val voiceRecorder = remember { com.anand.prohands.util.VoiceRecorder(context) }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            android.util.Log.w("MessagesScreen", "Audio permission denied")
        }
    }

    // Request audio permission on first load
    LaunchedEffect(Unit) {
        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun startVoiceRecording() {
        if (hasAudioPermission) {
            val file = voiceRecorder.startRecording()
            if (file != null) {
                chatViewModel.setRecordingState(true)
                android.util.Log.d("MessagesScreen", "Started voice recording")
            }
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopVoiceRecording() {
        val recordedFile = voiceRecorder.stopRecording()
        chatViewModel.setRecordingState(false)

        if (recordedFile != null && recordedFile.exists() && recordedFile.length() > 0) {
            android.util.Log.d("MessagesScreen", "Voice recording saved: ${recordedFile.absolutePath}, size: ${recordedFile.length()}")
            chatViewModel.sendVoiceMessage(recordedFile)
        } else {
            android.util.Log.e("MessagesScreen", "Voice recording failed or too short")
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
        chatViewModel.setRecordingState(false)
    }

    // Determine display name/avatar from DB or Fallback
    val displayName = recipientUser?.name ?: recipientName.ifEmpty { "User" }
    val displayAvatar = recipientUser?.profilePictureUrl ?: recipientAvatar
    val userIdForProfile = recipientUser?.userId ?: ""

    Scaffold(
        topBar = {
            ChatHeader(
                userName = displayName,
                avatarUrl = displayAvatar,
                isOnline = isOnline,
                isTyping = isTyping,
                lastSeen = lastSeenTimestamp,
                onBackClick = { navController.popBackStack() },
                onProfileClick = { 
                    if (userIdForProfile.isNotEmpty()) {
                        navController.navigate("worker_profile/$userIdForProfile") 
                    }
                }
            )
        },
        containerColor = Color(0xFFEFE7DE) // WhatsApp-like beige background
    ) { padding ->
        // Fullscreen image viewer state
        var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

        // Audio player
        val audioPlayer = remember { com.anand.prohands.util.AudioPlayer.getInstance(context) }
        val currentPlayingVoiceUrl by audioPlayer.currentPlayingUrl.collectAsState()

        // Background Doodle Pattern (Simulated with Box)
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFEFE7DE))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MessagesList(
                    uiState = uiState,
                    currentUserId = chatViewModel.currentUserId,
                    modifier = Modifier.weight(1f),
                    onImageClick = { imageUrl ->
                        fullscreenImageUrl = imageUrl
                    },
                    onVoicePlayClick = { voiceUrl ->
                        audioPlayer.playOrPause(voiceUrl)
                    },
                    currentPlayingVoiceUrl = currentPlayingVoiceUrl,
                    shouldScrollToBottom = shouldScrollToBottom,
                    onScrolledToBottom = { chatViewModel.onScrolledToBottom() },
                    isLoadingHistory = isLoadingHistory,
                    hasMoreMessages = hasMoreMessages,
                    onLoadMore = { chatViewModel.loadMoreMessages() }
                )

                MessageInputArea(
                    onMessageSend = { chatViewModel.sendMessage(it) }, 
                    onTyping = { chatViewModel.onUserTyping() },
                    onAttachImage = { imagePickerLauncher.launch("image/*") },
                    onAttachVideo = { videoPickerLauncher.launch("video/*") },
                    onCameraClick = { launchCamera() },
                    onStartRecording = { startVoiceRecording() },
                    onStopRecording = { stopVoiceRecording() },
                    onCancelRecording = { cancelVoiceRecording() },
                    isRecording = isRecording
                )
            }

            // Fullscreen image viewer dialog
            if (fullscreenImageUrl != null) {
                FullscreenImageViewer(
                    imageUrl = fullscreenImageUrl!!,
                    onDismiss = { fullscreenImageUrl = null }
                )
            }
        }
    }
}

/**
 * Fullscreen image viewer dialog
 */
@Composable
fun FullscreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Zoomable image
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            AsyncImage(
                model = imageUrl,
                contentDescription = "Fullscreen image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ChatHeader(
    userName: String, 
    avatarUrl: String?, 
    isOnline: Boolean, 
    isTyping: Boolean, 
    lastSeen: Long?, 
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // Display name - ensure it's not empty or just "User"
    val displayName = userName.takeIf { it.isNotBlank() && it != "User" } ?: "Unknown"

    // Generate avatar URL with proper formatting
    val safeAvatarUrl = avatarUrl
        ?.replace("http://", "https://")
        ?.replace(".heic", ".jpg")
        ?: "https://ui-avatars.com/api/?name=${displayName.replace(" ", "+")}&background=26A69A&color=fff&size=128"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ProColors.Primary)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ProColors.OnPrimary)
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onProfileClick)
                .padding(vertical = 4.dp, horizontal = 4.dp)
        ) {
            // Profile picture with online indicator
            Box {
                AsyncImage(
                    model = safeAvatarUrl,
                    contentDescription = "Profile picture of $displayName",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ProColors.PrimaryContainer),
                    contentScale = ContentScale.Crop
                )

                // Online indicator
                if (isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(ProColors.Surface, CircleShape)
                            .padding(2.dp)
                            .background(ProColors.Online, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))
            
            Column {
                Text(
                    text = displayName,
                    color = ProColors.OnPrimary,
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp,
                    maxLines = 1
                )
                
                val statusText = when {
                    isTyping -> "typing..."
                    isOnline -> "online"
                    else -> formatLastSeen(lastSeen)
                }
                
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText, 
                        color = if (isOnline || isTyping) ProColors.PrimaryLight else ProColors.OnPrimary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = if (isTyping) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        IconButton(onClick = { /* Video Call */ }) { 
            Icon(Icons.Default.Videocam, "Video Call", tint = ProColors.OnPrimary) 
        }
        IconButton(onClick = { /* Voice Call */ }) { 
            Icon(Icons.Default.Call, "Call", tint = ProColors.OnPrimary) 
        }
        IconButton(onClick = { /* More */ }) { 
            Icon(Icons.Default.MoreVert, "More", tint = ProColors.OnPrimary) 
        }
    }
}

@Composable
fun MessagesList(
    uiState: ChatUiState,
    currentUserId: String,
    modifier: Modifier,
    onImageClick: (String) -> Unit,
    onVoicePlayClick: (String) -> Unit,
    currentPlayingVoiceUrl: String?,
    shouldScrollToBottom: Boolean = false,
    onScrolledToBottom: () -> Unit = {},
    isLoadingHistory: Boolean = false,
    hasMoreMessages: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Track if user is at bottom of list
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleItem >= totalItems - 2
        }
    }

    // Auto-scroll to bottom when new messages arrive (if already at bottom or triggered)
    LaunchedEffect(uiState.messageCount, shouldScrollToBottom) {
        if (uiState.items.isNotEmpty() && (shouldScrollToBottom || isAtBottom)) {
            listState.animateScrollToItem(uiState.items.size - 1)
            onScrolledToBottom()
        }
    }

    // Detect scroll to top for loading more messages
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex <= 1 && hasMoreMessages && !isLoadingHistory) {
                    onLoadMore()
                }
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = false
        ) {
            // Loading indicator at top when loading more messages
            if (isLoadingHistory && hasMoreMessages) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ProColors.Primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            items(
                items = uiState.items,
                key = {
                    when(it) {
                        is ChatItem.Message -> it.message.messageId
                        is ChatItem.DateSeparator -> it.date
                    }
                }
            ) { item ->
                when (item) {
                    is ChatItem.DateSeparator -> DateSeparator(item.date)
                    is ChatItem.Message -> {
                        MessageBubble(
                            message = item.message,
                            isFromCurrentUser = item.message.senderId == currentUserId,
                            onImageClick = onImageClick,
                            onVoicePlayClick = onVoicePlayClick,
                            currentPlayingVoiceUrl = currentPlayingVoiceUrl
                        )
                    }
                }
            }
        }

        // Scroll to bottom FAB - shown when not at bottom
        AnimatedVisibility(
            visible = !isAtBottom && uiState.items.size > 5,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        if (uiState.items.isNotEmpty()) {
                            listState.animateScrollToItem(uiState.items.size - 1)
                        }
                    }
                },
                containerColor = ProColors.Surface,
                contentColor = ProColors.Primary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = Color.DarkGray,
            modifier = Modifier
                .background(Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isFromCurrentUser: Boolean,
    onRetryClick: (() -> Unit)? = null,
    onImageClick: ((String) -> Unit)? = null,
    onVoicePlayClick: ((String) -> Unit)? = null,
    isVoicePlaying: Boolean = false,
    currentPlayingVoiceUrl: String? = null
) {
    // Colors: Sent = #E7FFDB (WhatsApp Greenish), Received = White
    val bubbleColor = if (isFromCurrentUser) Color(0xFFE7FFDB) else Color.White
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    
    // Check if message is pending or failed
    val isPending = message.status == MessageStatus.PENDING
    val isFailed = message.status == MessageStatus.FAILED

    // Check if this voice message is currently playing
    val isThisVoicePlaying = message.type == MessageType.VOICE &&
        currentPlayingVoiceUrl != null &&
        message.content.replace("http://", "https://") == currentPlayingVoiceUrl

    val shape = if (isFromCurrentUser) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isFailed) ProColors.ErrorContainer else bubbleColor,
                shape = shape,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(1.dp, shape)
            ) {
                Column(modifier = Modifier.padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = 4.dp)) {

                    // Content based on message type
                    when (message.type) {
                        MessageType.IMAGE -> {
                            // Image with loading/error overlay - clickable for fullscreen
                            val imageUrl = message.content.let {
                                if (it.startsWith("http")) it.replace("http://", "https://")
                                else it
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ProColors.SurfaceVariant)
                                    .clickable(enabled = !isPending && !isFailed && onImageClick != null) {
                                        onImageClick?.invoke(imageUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Show image with loading state
                                var isImageLoading by remember { mutableStateOf(true) }
                                var hasImageError by remember { mutableStateOf(false) }

                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Image message - tap to view",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 150.dp, max = 280.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .alpha(if (isPending) 0.6f else 1f),
                                    contentScale = ContentScale.Crop,
                                    onLoading = { isImageLoading = true },
                                    onSuccess = {
                                        isImageLoading = false
                                        hasImageError = false
                                    },
                                    onError = {
                                        isImageLoading = false
                                        hasImageError = true
                                    }
                                )

                                // Loading overlay for pending messages
                                if (isPending || isImageLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = ProColors.OnPrimary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                // Error/Failed overlay
                                if (isFailed || hasImageError) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = "Failed",
                                                tint = ProColors.Error,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Failed to send",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        MessageType.VIDEO -> {
                            val videoUrl = message.content.let {
                                if (it.startsWith("http")) it.replace("http://", "https://")
                                else it
                            }

                            var showFullscreenVideo by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                if (!isPending && !isFailed) {
                                    // Use ChatVideoPlayer component
                                    ChatVideoPlayer(
                                        videoUrl = videoUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        isFullscreen = false,
                                        onFullscreenToggle = { showFullscreenVideo = it },
                                        autoPlay = false
                                    )
                                } else {
                                    // Placeholder for pending/failed videos
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.8f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPending) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Error,
                                                    contentDescription = "Failed",
                                                    tint = ProColors.Error,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Failed to send",
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fullscreen video dialog
                            if (showFullscreenVideo) {
                                FullscreenVideoPlayer(
                                    videoUrl = videoUrl,
                                    onDismiss = { showFullscreenVideo = false }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        MessageType.VOICE -> {
                            // Voice message UI - clickable to play/pause
                            val voiceUrl = message.content.replace("http://", "https://")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isPending && onVoicePlayClick != null) {
                                        onVoicePlayClick?.invoke(voiceUrl)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play/Pause button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ProColors.Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPending) {
                                        CircularProgressIndicator(
                                            color = ProColors.OnPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isThisVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isThisVoicePlaying) "Pause" else "Play",
                                            tint = ProColors.OnPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Waveform visualization (simplified as bars)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Generate pseudo-waveform bars
                                    val barHeights = remember { List(20) { (0.3f + Math.random().toFloat() * 0.7f) } }
                                    barHeights.forEach { height ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight(height)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (isThisVoicePlaying) ProColors.Primary
                                                    else ProColors.PrimaryContainer
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Duration text
                                Text(
                                    text = "0:00", // TODO: Get actual duration
                                    fontSize = 12.sp,
                                    color = ProColors.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        MessageType.TEXT -> {
                            Text(
                                text = message.content,
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        else -> {}
                    }

                    // Metadata (Time + Status)
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(bottom = 2.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = formatMessageTime(message.timestamp),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        if (isFromCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIcon(message.status)
                        }
                    }
                }
            }

            // Retry button for failed messages
            if (isFailed && onRetryClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onRetryClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(16.dp),
                        tint = ProColors.Error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Retry",
                        fontSize = 12.sp,
                        color = ProColors.Error
                    )
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.PENDING -> Icons.Default.Schedule to Color.Gray
        MessageStatus.SENT -> Icons.Default.Check to Color.Gray
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to Color.Gray
        MessageStatus.READ -> Icons.Default.DoneAll to ProColors.Info // Blue
        else -> Icons.Default.Error to ProColors.Error
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = Modifier.size(16.dp),
        tint = tint
    )
}

@Composable
fun MessageInputArea(
    onMessageSend: (String) -> Unit, 
    onTyping: () -> Unit,
    onAttachImage: () -> Unit,
    onAttachVideo: () -> Unit,
    onCameraClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit = {},
    isRecording: Boolean
) {
    var text by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }

    // Helper to sanitize text similar to backend/web
    fun sanitizeInput(input: String): String {
        return input.replace(Regex("""(\\\\n|/n|\n|\r)"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // Track recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0L
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }

    Column {
        // Recording UI - shown when actively recording
        AnimatedVisibility(visible = isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ProColors.ErrorContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                IconButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancel recording",
                        tint = ProColors.Error
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Recording indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(ProColors.Error, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Duration
                Text(
                    text = formatDuration(recordingDuration),
                    color = ProColors.Error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Recording...",
                    color = ProColors.TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Send button
                FloatingActionButton(
                    onClick = onStopRecording,
                    containerColor = ProColors.Primary,
                    contentColor = ProColors.OnPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send recording"
                    )
                }
            }
        }

        // Normal input area - hidden when recording
        AnimatedVisibility(visible = !isRecording) {
            Column {
                // Attachment Menu
                AnimatedVisibility(visible = showAttachMenu) {
                    AttachmentMenu(onAttachImage, onAttachVideo, onCameraClick)
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Input Field Container
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            IconButton(onClick = { /* Emoji */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.SentimentSatisfiedAlt, "Emoji", tint = Color.Gray)
                            }

                            BasicTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    onTyping()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .heightIn(max = 100.dp),
                                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                                cursorBrush = SolidColor(ProColors.Primary),
                                decorationBox = { innerTextField ->
                                    if (text.isEmpty()) {
                                        Text("Message", color = Color.Gray, fontSize = 16.sp)
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = { showAttachMenu = !showAttachMenu },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, "Attach", tint = Color.Gray)
                            }

                            if (text.isEmpty()) {
                                IconButton(onClick = onCameraClick, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.CameraAlt, "Camera", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Mic or Send Button
                    val sanitized = sanitizeInput(text)
                    val isSendMode = sanitized.isNotBlank()

                    FloatingActionButton(
                        onClick = {
                            if (isSendMode) {
                                onMessageSend(sanitized)
                                text = ""
                            } else {
                                // Start voice recording
                                onStartRecording()
                            }
                        },
                        containerColor = ProColors.Primary,
                        contentColor = ProColors.OnPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isSendMode) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if (isSendMode) "Send" else "Record"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentMenu(
    onAttachImage: () -> Unit,
    onAttachVideo: () -> Unit,
    onCameraClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AttachmentItem(Icons.Default.Image, Color(0xFFBF59CF), "Gallery") { onAttachImage() }
            AttachmentItem(Icons.Default.CameraAlt, Color(0xFFE91E63), "Camera") { onCameraClick() }
            AttachmentItem(Icons.Default.Videocam, Color(0xFF515151), "Video") { onAttachVideo() }
            AttachmentItem(Icons.Default.Headphones, Color(0xFFFF9800), "Audio") { /* Audio picker */ }
            AttachmentItem(Icons.Default.LocationOn, Color(0xFF0F9D58), "Location") { /* Location */ }
            AttachmentItem(Icons.Default.Person, Color(0xFF009688), "Contact") { /* Contact */ }
        }
    }
}

@Composable
fun RowScope.AttachmentItem(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

fun formatLastSeen(lastSeen: Long?): String {
    if (lastSeen == null || lastSeen == 0L) return ""
    val date = Date(lastSeen)
    val now = Calendar.getInstance()
    val last = Calendar.getInstance().apply { time = date }
    
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    
    return when {
        now.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR) -> "last seen today at $timeStr"
        now.get(Calendar.DAY_OF_YEAR) - last.get(Calendar.DAY_OF_YEAR) == 1 -> "last seen yesterday at $timeStr"
        else -> "last seen " + SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
    }
}

fun formatMessageTime(timestamp: String): String {
    val date = try {
        // Parse ISO 8601 timestamp compatible with API 24+
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        isoFormat.parse(timestamp.substringBefore('.').substringBefore('Z')) ?: Date()
    } catch (_: Exception) {
        Date() // Fallback
    }
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}
