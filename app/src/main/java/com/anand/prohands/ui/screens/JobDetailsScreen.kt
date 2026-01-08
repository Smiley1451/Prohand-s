package com.anand.prohands.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.JobDetailsViewModel
import com.anand.prohands.viewmodel.JobDetailsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    navController: NavController,
    viewModel: JobDetailsViewModel = viewModel(factory = JobDetailsViewModelFactory())
) {
    val job by viewModel.job.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(jobId) {
        viewModel.fetchJob(jobId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", color = ProColors.OnPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProColors.OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProColors.Primary)
            )
        },
        containerColor = ProColors.Background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ProColors.Primary)
            } else if (job != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(job!!.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ProColors.OnBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Wage: ₹${job!!.wage}/hr", color = ProColors.Success, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${job!!.status}", style = MaterialTheme.typography.labelLarge, color = ProColors.Secondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(job!!.description, style = MaterialTheme.typography.bodyLarge, color = ProColors.TextPrimary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Required Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (job!!.requiredSkills.isNotEmpty()) {
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            job!!.requiredSkills.forEach { skill ->
                                AssistChip(onClick = {}, label = { Text(skill) })
                            }
                        }
                    } else {
                        Text("No specific skills listed", color = ProColors.TextSecondary)
                    }
                }
            } else if (error != null) {
                Text("Error: $error", modifier = Modifier.align(Alignment.Center), color = ProColors.Error)
            }
        }
    }
}
