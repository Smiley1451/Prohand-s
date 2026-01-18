package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.JobResponse
import com.anand.prohands.data.ReviewRequest
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ManageJobsViewModel
import com.anand.prohands.viewmodel.ManageJobsViewModelFactory
import com.anand.prohands.viewmodel.SearchViewModel
import com.anand.prohands.viewmodel.SearchViewModelFactory

@Composable
fun ManageJobsScreen(
    navController: NavController,
    currentUserId: String,
    viewModel: ManageJobsViewModel = viewModel(factory = ManageJobsViewModelFactory())
) {
    val jobs by viewModel.jobs.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            viewModel.fetchJobs(currentUserId)
        }
    }

    LaunchedEffect(actionSuccess) {
        actionSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            AppHeader(title = "My Posted Jobs")
        },
        containerColor = ProColors.Background
    ) { padding ->
        val openJobs = jobs.filter { it.status == "OPEN" }
        val inProgressJobs = jobs.filter { it.status == "IN_PROGRESS" }
        val completedJobs = jobs.filter { it.status == "COMPLETED" || it.status == "CLOSED" }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (openJobs.isNotEmpty()) {
                item { SectionHeader("Active Jobs") }
                items(openJobs) { job ->
                    ProviderJobCard(job, navController, viewModel, currentUserId)
                }
            }

            if (inProgressJobs.isNotEmpty()) {
                item { SectionHeader("In Progress") }
                items(inProgressJobs) { job ->
                    ProviderJobCard(job, navController, viewModel, currentUserId)
                }
            }

            if (completedJobs.isNotEmpty()) {
                item { SectionHeader("History") }
                items(completedJobs) { job ->
                    ProviderJobCard(job, navController, viewModel, currentUserId)
                }
            }
            
            if (jobs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                         Text("No posted jobs found.", color = ProColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = ProColors.Primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ProviderJobCard(
    job: JobResponse, 
    navController: NavController, 
    viewModel: ManageJobsViewModel,
    currentUserId: String
) {
    var expanded by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${job.wage}/hr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProColors.Success,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (job.status == "OPEN") {
                        IconButton(
                            onClick = { navController.navigate("edit_job/${job.jobId}") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Job",
                                tint = ProColors.Primary
                            )
                        }
                    }
                    Badge(
                        containerColor = when (job.status) {
                            "OPEN" -> ProColors.Primary
                            "IN_PROGRESS" -> ProColors.Secondary
                            else -> Color.Gray
                        }
                    ) {
                        Text(
                            text = job.status,
                            modifier = Modifier.padding(4.dp),
                            color = ProColors.OnPrimary
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (job.status == "OPEN") {
                        Button(
                            onClick = {
                                navController.navigate(
                                    "worker_recommendations/${job.jobId}/${job.latitude}/${job.longitude}/${job.title}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Find Workers", color = ProColors.OnPrimary)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.updateJobStatus(job.jobId, "IN_PROGRESS", currentUserId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start Job", color = ProColors.OnSecondary)
                        }
                    } else if (job.status == "IN_PROGRESS") {
                         Button(
                            onClick = {
                                viewModel.updateJobStatus(job.jobId, "COMPLETED", currentUserId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Success),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Complete Job", color = Color.White)
                        }
                    } else if (job.status == "COMPLETED") {
                         Button(
                            onClick = { showReviewDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Leave Review", color = ProColors.OnPrimary)
                        }
                    }
                }
            }
        }
    }
    
    if (showReviewDialog) {
        ReviewDialog(
            jobId = job.jobId,
            reviewerId = currentUserId,
            onDismiss = { showReviewDialog = false },
            onSubmit = { review ->
                viewModel.submitReview(review)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun ReviewDialog(
    jobId: String,
    reviewerId: String,
    onDismiss: () -> Unit,
    onSubmit: (ReviewRequest) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var punctuality by remember { mutableStateOf(100f) }
    var quality by remember { mutableStateOf(100f) }
    var behaviour by remember { mutableStateOf(100f) }
    
    // Search related state
    var selectedWorker by remember { mutableStateOf<ClientProfileDto?>(null) }
    var isSearchingWorker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Leave a Review",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ProColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Worker Selection Section
                    Text(
                        text = "Select Worker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.TextPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedWorker == null) {
                        OutlinedButton(
                            onClick = { isSearchingWorker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ProColors.Primary)
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ProColors.Primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search and Select Worker", color = ProColors.Primary)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ProColors.PrimaryContainer, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedWorker?.profilePictureUrl ?: "https://via.placeholder.com/150"),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedWorker?.name ?: "Unknown", fontWeight = FontWeight.Bold, color = ProColors.TextPrimary)
                                Text("@${selectedWorker?.userId}", style = MaterialTheme.typography.bodySmall, color = ProColors.TextSecondary)
                            }
                            IconButton(onClick = { selectedWorker = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = ProColors.Error)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Rating Section
                    Text(
                        text = "Overall Rating",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.TextPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (index <= rating) Color(0xFFFFC107) else ProColors.Divider,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable { rating = index }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Detailed Scores
                    ScoreSlider("Punctuality", punctuality) { punctuality = it }
                    ScoreSlider("Quality of Work", quality) { quality = it }
                    ScoreSlider("Professional Behavior", behaviour) { behaviour = it }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("Your Experience", color = ProColors.TextSecondary) },
                        placeholder = { Text("Tell others about your experience...", color = ProColors.TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProColors.TextPrimary,
                            unfocusedTextColor = ProColors.TextPrimary,
                            focusedBorderColor = ProColors.Primary,
                            unfocusedBorderColor = ProColors.Divider,
                            focusedLabelColor = ProColors.Primary,
                            cursorColor = ProColors.Primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Cancel", color = ProColors.TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (selectedWorker != null) {
                                    onSubmit(
                                        ReviewRequest(
                                            workerId = selectedWorker!!.userId,
                                            reviewerId = reviewerId,
                                            rating = rating,
                                            reviewText = reviewText,
                                            punctualityScore = punctuality.toInt(),
                                            qualityScore = quality.toInt(),
                                            behaviourScore = behaviour.toInt(),
                                            jobId = jobId
                                        )
                                    )
                                } else {
                                    // Could add a toast here
                                }
                            },
                            enabled = selectedWorker != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProColors.Primary,
                                disabledContainerColor = ProColors.Primary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Submit Review", color = ProColors.OnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (isSearchingWorker) {
        WorkerSearchDialog(
            onDismiss = { isSearchingWorker = false },
            onWorkerSelected = {
                selectedWorker = it
                isSearchingWorker = false
            }
        )
    }
}

@Composable
fun WorkerSearchDialog(
    onDismiss: () -> Unit,
    onWorkerSelected: (ClientProfileDto) -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(containerColor = ProColors.Surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Worker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        searchViewModel.onSearchQueryChanged(it)
                    },
                    placeholder = { Text("Search by name or skills...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProColors.TextPrimary,
                        unfocusedTextColor = ProColors.TextPrimary,
                        focusedBorderColor = ProColors.Primary
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ProColors.Primary)
                    } else if (searchResults.isEmpty() && searchQuery.length >= 2) {
                        Text("No workers found.", modifier = Modifier.align(Alignment.Center), color = ProColors.TextSecondary)
                    } else {
                        LazyColumn {
                            items(searchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onWorkerSelected(result.profile) }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(result.profile.profilePictureUrl ?: "https://via.placeholder.com/150"),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(result.profile.name ?: "Unknown", fontWeight = FontWeight.Bold, color = ProColors.TextPrimary)
                                        Text(result.profile.skills?.joinToString(", ") ?: "Worker", style = MaterialTheme.typography.bodySmall, color = ProColors.TextSecondary)
                                    }
                                }
                                HorizontalDivider(color = ProColors.Divider.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun ScoreSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ProColors.TextPrimary)
            Text("${value.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ProColors.Primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = ProColors.Primary,
                activeTrackColor = ProColors.Primary,
                inactiveTrackColor = ProColors.Divider
            )
        )
    }
}
