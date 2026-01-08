package com.anand.prohands.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.components.MapPickerComponent
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.PostJobViewModel
import com.anand.prohands.viewmodel.PostJobViewModelFactory
import com.google.android.gms.maps.model.LatLng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostJobScreen(
    navController: NavController,
    currentUserId: String
) {
    val context = LocalContext.current
    val viewModel: PostJobViewModel = viewModel(factory = PostJobViewModelFactory(context))

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var wage by remember { mutableStateOf("") }
    var employees by remember { mutableStateOf("1") }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    val postResult by viewModel.postResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
    }

    LaunchedEffect(postResult) {
        if (postResult == true) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppHeader(title = "Create New Job", onBackClick = { navController.popBackStack() })
        },
        containerColor = ProColors.Background
    ) { padding ->
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = wage,
                        onValueChange = { wage = it },
                        label = { Text("Wage (₹/hr)") },
                        modifier = Modifier.weight(1f),
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
                    OutlinedTextField(
                        value = employees,
                        onValueChange = { employees = it },
                        label = { Text("Workers") },
                        modifier = Modifier.weight(1f),
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
                    MapPickerComponent(initialLocation = currentLocation) { location ->
                        selectedLocation = location
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isNotEmpty() && wage.isNotEmpty() && selectedLocation != null) {
                            viewModel.postJob(
                                JobRequest(
                                    title = title,
                                    description = description,
                                    wage = wage.toDoubleOrNull() ?: 0.0,
                                    latitude = selectedLocation!!.latitude,
                                    longitude = selectedLocation!!.longitude,
                                    numberOfEmployees = employees.toIntOrNull() ?: 1,
                                    providerId = currentUserId
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
                        Text("Post Job", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProColors.OnPrimary)
                    }
                }
            }
        }
    }
}
