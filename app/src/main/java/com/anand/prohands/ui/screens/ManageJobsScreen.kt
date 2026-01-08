package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.JobResponse
import com.anand.prohands.data.ReviewRequest
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ManageJobsViewModel
import com.anand.prohands.viewmodel.ManageJobsViewModelFactory

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
                        color = ProColors.OnSurface
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
                    color = ProColors.OnSurface
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
            // In a real app, you'd select which worker to review if there are multiple.
            // For now, we'll prompt for worker ID or assume a single worker flow.
            // Since the API requires workerId, we need it. 
            // NOTE: The current JobResponse doesn't have assigned workerId.
            // We'll add a placeholder text field for Worker ID for now, 
            // or assume the user knows it (which is bad UX, but fits the current data model constraints).
            // Better: Let's assume the user enters the Worker ID or name for now, 
            // or better yet, we just submit a dummy worker ID for this demo if not available.
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
    
    // Ideally we pick this from a list of assigned workers
    var workerId by remember { mutableStateOf("") } 

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rate Worker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = workerId,
                    onValueChange = { workerId = it },
                    label = { Text("Worker ID (Manual)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Rating: $rating/5")
                Row {
                    (1..5).forEach { index ->
                        Icon(
                            imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = ProColors.Secondary,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { rating = index }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ScoreSlider("Punctuality", punctuality) { punctuality = it }
                ScoreSlider("Quality", quality) { quality = it }
                ScoreSlider("Behaviour", behaviour) { behaviour = it }
                
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Comments") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProColors.TextPrimary,
                        unfocusedTextColor = ProColors.TextPrimary,
                        focusedBorderColor = ProColors.Primary,
                        unfocusedBorderColor = ProColors.Divider,
                        focusedLabelColor = ProColors.Primary,
                        cursorColor = ProColors.Primary
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (workerId.isNotEmpty()) {
                                onSubmit(
                                    ReviewRequest(
                                        workerId = workerId,
                                        reviewerId = reviewerId,
                                        rating = rating,
                                        reviewText = reviewText,
                                        punctualityScore = punctuality.toInt(),
                                        qualityScore = quality.toInt(),
                                        behaviourScore = behaviour.toInt(),
                                        jobId = jobId
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                    ) {
                        Text("Submit", color = ProColors.OnPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.toInt()}", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = ProColors.Primary,
                activeTrackColor = ProColors.Primary,
                inactiveTrackColor = ProColors.PrimaryVariant
            )
        )
    }
}
