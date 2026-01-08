package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.JobRequest
import com.anand.prohands.ui.components.MapPickerComponent
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.EditJobViewModel
import com.anand.prohands.viewmodel.EditJobViewModelFactory
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJobScreen(
    navController: NavController,
    jobId: String,
    viewModel: EditJobViewModel = viewModel(factory = EditJobViewModelFactory())
) {
    val context = LocalContext.current
    val job by viewModel.job.collectAsState()
    val updateResult by viewModel.updateResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Initial fetch
    LaunchedEffect(jobId) {
        viewModel.fetchJob(jobId)
    }

    // Populate fields when job data loads
    LaunchedEffect(job) {
        job?.let {
            title = it.title
            description = it.description
            wage = it.wage.toString()
            selectedLocation = LatLng(it.latitude, it.longitude)
        }
    }

    // Handle update result
    LaunchedEffect(updateResult) {
        if (updateResult == true) {
            Toast.makeText(context, "Job updated successfully", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Job", color = ProColors.OnPrimary, fontWeight = FontWeight.Bold) },
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
        if (job == null && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator(color = ProColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Job Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProColors.OnBackground,
                            unfocusedTextColor = ProColors.OnBackground,
                            cursorColor = ProColors.Primary,
                            focusedBorderColor = ProColors.Primary,
                            focusedLabelColor = ProColors.Primary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Wage (₹/hr)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProColors.OnBackground,
                            unfocusedTextColor = ProColors.OnBackground,
                            cursorColor = ProColors.Primary,
                            focusedBorderColor = ProColors.Primary,
                            focusedLabelColor = ProColors.Primary
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ProColors.OnBackground,
                            unfocusedTextColor = ProColors.OnBackground,
                            cursorColor = ProColors.Primary,
                            focusedBorderColor = ProColors.Primary,
                            focusedLabelColor = ProColors.Primary
                        )
                    )
                }

                item {
                    Box(modifier = Modifier.height(300.dp)) {
                        MapPickerComponent(initialLocation = selectedLocation?.let { android.location.Location("").apply { latitude = it.latitude; longitude = it.longitude } }) { location ->
                            selectedLocation = location
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && wage.isNotEmpty() && selectedLocation != null) {
                                viewModel.updateJob(
                                    jobId,
                                    JobRequest(
                                        providerId = job!!.providerId, // Keep existing provider
                                        title = title,
                                        description = description,
                                        wage = wage.toDoubleOrNull() ?: 0.0,
                                        latitude = selectedLocation!!.latitude,
                                        longitude = selectedLocation!!.longitude,
                                        numberOfEmployees = 1 // Simplified
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = ProColors.OnPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Update Job", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProColors.OnPrimary)
                        }
                    }
                }
            }
        }
    }
}
