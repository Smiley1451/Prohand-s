package com.anand.prohands.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.chat.ChatDatabase
import com.anand.prohands.data.local.UserWageProfile
import com.anand.prohands.data.local.WageProfileOptions
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.EditProfileViewModel
import com.anand.prohands.viewmodel.EditProfileViewModelFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userId: String
) {
    val context = LocalContext.current


    val database = remember { ChatDatabase.getDatabase(context) }
    val wageProfileDao = remember { database.userWageProfileDao() }

    val editProfileViewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModelFactory(wageProfileDao)
    )

    // Basic profile state
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") } // Just the 10-digit number without country code
    var skills by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Wage profile state
    var age by remember { mutableIntStateOf(30) }
    var experienceYears by remember { mutableIntStateOf(2) }
    var workingHours by remember { mutableIntStateOf(8) }
    var educationLevel by remember { mutableStateOf("secondary") }
    var employmentType by remember { mutableStateOf("casual") }
    var sector by remember { mutableStateOf("construction") }
    var skillLevel by remember { mutableIntStateOf(3) }

    // Construction specific
    var jobRole by remember { mutableStateOf("helper") }
    var cityTier by remember { mutableStateOf("Tier-2") }
    var projectType by remember { mutableStateOf("residential") }

    // Agriculture specific
    var occupation by remember { mutableStateOf("farm labourer") }
    var state by remember { mutableStateOf("MH") }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imageUri = it
                try {
                    val stream = context.contentResolver.openInputStream(it)
                    val requestBody = stream?.readBytes()?.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    val multipartBody = requestBody?.let { body ->
                        MultipartBody.Part.createFormData("file", "profile.jpg", body)
                    }
                    multipartBody?.let { part ->
                        editProfileViewModel.uploadProfilePicture(userId, part) { _, _ -> }
                    }
                } catch (e: Exception) {

                }
            }
        }
    )

    // Load Data
    LaunchedEffect(key1 = userId) {
        editProfileViewModel.loadProfile(userId)
    }

    // Populate form from loaded profile
    val profile = editProfileViewModel.profile
    val wageProfile = editProfileViewModel.wageProfile

    LaunchedEffect(key1 = profile) {
        profile?.let {
            name = it.name ?: ""
            // Extract phone number without country code (+91)
            phoneNumber = it.phone?.let { phone ->
                when {
                    phone.startsWith("+91") -> phone.removePrefix("+91")
                    phone.startsWith("91") && phone.length > 10 -> phone.removePrefix("91")
                    phone.startsWith("0") -> phone.removePrefix("0")
                    else -> phone
                }.take(10) // Ensure max 10 digits
            } ?: ""
            skills = it.skills?.joinToString(", ") ?: ""
        }
    }

    LaunchedEffect(key1 = wageProfile) {
        wageProfile?.let {
            age = it.age
            experienceYears = it.experienceYears
            workingHours = it.workingHours
            educationLevel = it.educationLevel
            employmentType = it.employmentType
            sector = it.sector
            skillLevel = it.skillLevel
            jobRole = it.jobRole
            cityTier = it.cityTier
            projectType = it.projectType
            occupation = it.occupation
            state = it.state
        }
    }

    // Show success message
    LaunchedEffect(showSuccessMessage) {
        if (showSuccessMessage) {
            snackbarHostState.showSnackbar("Profile saved successfully!")
            showSuccessMessage = false
        }
    }

    Scaffold(
        containerColor = ProColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", color = ProColors.OnBackground) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ProColors.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ProColors.Background
                )
            )
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Image Upload Section ---
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(140.dp)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(ProColors.SurfaceVariant)
                        .border(BorderStroke(4.dp, ProColors.Surface), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (editProfileViewModel.isImageUploading) {
                        CircularProgressIndicator(color = ProColors.Primary)
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri ?: profile?.profilePictureUrl ?: "https://via.placeholder.com/150"),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = ProColors.Primary,
                    border = BorderStroke(2.dp, ProColors.Surface),
                    modifier = Modifier.size(40.dp).offset(x = (-4).dp, y = (-4).dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Edit Image",
                            tint = ProColors.OnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. Basic Profile Fields ---
            ProfileSectionHeader("Basic Information")

            ProTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                icon = Icons.Outlined.Person,
                keyboardType = KeyboardType.Text
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number with +91 prefix
            PhoneNumberField(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { newValue ->
                    // Only allow digits and max 10 characters
                    phoneNumber = newValue.filter { it.isDigit() }.take(10)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            ProTextField(
                value = skills,
                onValueChange = { skills = it },
                label = "Skills (comma-separated)",
                placeholder = "e.g., Electrician, Plumber",
                icon = Icons.Default.Build,
                singleLine = false,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. Wage Calculation Fields ---
            ProfileSectionHeader("Work Details (for AI Wage Calculation)")

            // Sector Selection
            ProDropdown(
                label = "Work Sector",
                options = WageProfileOptions.sectors,
                selectedOption = sector,
                onOptionSelected = { sector = it },
                displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Age
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProNumberField(
                    modifier = Modifier.weight(1f),
                    value = age,
                    onValueChange = { age = it.coerceIn(18, 70) },
                    label = "Age",
                    icon = Icons.Default.Cake
                )

                ProNumberField(
                    modifier = Modifier.weight(1f),
                    value = experienceYears,
                    onValueChange = { experienceYears = it.coerceIn(0, 50) },
                    label = "Experience (Years)",
                    icon = Icons.Default.WorkHistory
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProNumberField(
                    modifier = Modifier.weight(1f),
                    value = workingHours,
                    onValueChange = { workingHours = it.coerceIn(1, 16) },
                    label = "Working Hours/Day",
                    icon = Icons.Default.Schedule
                )

                ProNumberField(
                    modifier = Modifier.weight(1f),
                    value = skillLevel,
                    onValueChange = { skillLevel = it.coerceIn(1, 5) },
                    label = "Skill Level (1-5)",
                    icon = Icons.Default.Star
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Education Level
            ProDropdown(
                label = "Education Level",
                options = WageProfileOptions.educationLevels,
                selectedOption = educationLevel,
                onOptionSelected = { educationLevel = it },
                displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Employment Type
            ProDropdown(
                label = "Employment Type",
                options = WageProfileOptions.employmentTypes,
                selectedOption = employmentType,
                onOptionSelected = { employmentType = it },
                displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sector-specific fields
            if (sector == "construction") {
                ProfileSectionHeader("Construction Details")

                ProDropdown(
                    label = "Job Role",
                    options = WageProfileOptions.jobRoles,
                    selectedOption = jobRole,
                    onOptionSelected = { jobRole = it },
                    displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProDropdown(
                    label = "City Tier",
                    options = WageProfileOptions.cityTiers,
                    selectedOption = cityTier,
                    onOptionSelected = { cityTier = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProDropdown(
                    label = "Project Type",
                    options = WageProfileOptions.projectTypes,
                    selectedOption = projectType,
                    onOptionSelected = { projectType = it },
                    displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
                )
            } else {
                ProfileSectionHeader("Agriculture Details")

                ProDropdown(
                    label = "Occupation",
                    options = WageProfileOptions.occupations,
                    selectedOption = occupation,
                    onOptionSelected = { occupation = it },
                    displayMapper = { it.replaceFirstChar { c -> c.uppercase() } }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ProDropdown(
                    label = "State",
                    options = WageProfileOptions.states,
                    selectedOption = state,
                    onOptionSelected = { state = it },
                    displayMapper = {
                        when (it) {
                            "UP" -> "Uttar Pradesh"
                            "MH" -> "Maharashtra"
                            "TN" -> "Tamil Nadu"
                            else -> it
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 4. Save Button ---
            Button(
                onClick = {
                    isSaving = true

                    // Save wage profile first
                    val updatedWageProfile = UserWageProfile(
                        userId = userId,
                        age = age,
                        experienceYears = experienceYears,
                        workingHours = workingHours,
                        educationLevel = educationLevel,
                        employmentType = employmentType,
                        sector = sector,
                        skillLevel = skillLevel,
                        jobRole = jobRole,
                        cityTier = cityTier,
                        projectType = projectType,
                        occupation = occupation,
                        state = state
                    )

                    editProfileViewModel.saveWageProfile(updatedWageProfile) { wageSuccess, _ ->
                        // Then save backend profile
                        profile?.let {
                            // Format phone number with +91 prefix
                            val formattedPhone = if (phoneNumber.isNotEmpty()) "+91$phoneNumber" else null

                            val updatedProfile = it.copy(
                                name = name,
                                phone = formattedPhone,
                                skills = skills.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                            )
                            editProfileViewModel.updateProfile(userId, updatedProfile) { profileSuccess, _ ->
                                isSaving = false
                                if (profileSuccess || wageSuccess) {
                                    showSuccessMessage = true
                                    navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefresh", true)
                                    navController.popBackStack()
                                }
                            }
                        } ?: run {
                            isSaving = false
                            if (wageSuccess) {
                                showSuccessMessage = true
                                navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefresh", true)
                                navController.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProColors.Primary,
                    disabledContainerColor = ProColors.Primary.copy(alpha = 0.6f)
                ),
                enabled = !isSaving && !editProfileViewModel.isLoading
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ProColors.OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...", color = ProColors.OnPrimary)
                } else {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ProColors.OnPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ProColors.PrimaryVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    displayMapper: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayMapper(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ProColors.TextPrimary,
                unfocusedTextColor = ProColors.TextPrimary,
                focusedBorderColor = ProColors.Primary,
                unfocusedBorderColor = ProColors.Divider,
                focusedLabelColor = ProColors.Primary,
                unfocusedContainerColor = ProColors.Surface,
                focusedContainerColor = ProColors.Surface
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayMapper(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun ProNumberField(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { newValue ->
            val intValue = newValue.filter { it.isDigit() }.toIntOrNull() ?: 0
            onValueChange(intValue)
        },
        label = { Text(label, maxLines = 1) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProColors.TextPrimary,
            unfocusedTextColor = ProColors.TextPrimary,
            focusedBorderColor = ProColors.Primary,
            unfocusedBorderColor = ProColors.Divider,
            focusedLabelColor = ProColors.Primary,
            unfocusedContainerColor = ProColors.Surface,
            focusedContainerColor = ProColors.Surface
        ),
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = ProColors.TextSecondary, modifier = Modifier.size(20.dp))
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        singleLine = true
    )
}

// --- Reusable Component for Consistent Text Fields ---
@Composable
fun ProTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = ProColors.TextTertiary) } } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProColors.TextPrimary,
            unfocusedTextColor = ProColors.TextPrimary,
            focusedBorderColor = ProColors.Primary,
            unfocusedBorderColor = ProColors.Divider,
            focusedLabelColor = ProColors.Primary,
            unfocusedContainerColor = ProColors.Surface,
            focusedContainerColor = ProColors.Surface
        ),
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = ProColors.TextSecondary)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        singleLine = singleLine,
        maxLines = maxLines
    )
}

/**
 * Phone number field with fixed +91 country code prefix
 */
@Composable
fun PhoneNumberField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Phone Number",
            style = MaterialTheme.typography.bodySmall,
            color = ProColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed +91 Country Code Box
            Surface(
                modifier = Modifier
                    .height(56.dp)
                    .width(72.dp),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                color = ProColors.PrimaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🇮🇳",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+91",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = ProColors.Primary
                    )
                }
            }

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = {
                    Text(
                        "Enter 10 digit number",
                        color = ProColors.TextTertiary,
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ProColors.TextPrimary,
                    unfocusedTextColor = ProColors.TextPrimary,
                    focusedBorderColor = ProColors.Primary,
                    unfocusedBorderColor = ProColors.Divider,
                    focusedContainerColor = ProColors.Surface,
                    unfocusedContainerColor = ProColors.Surface
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            )
        }

        // Helper text showing character count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (phoneNumber.length == 10) "✓ Valid number" else "Enter 10 digits",
                style = MaterialTheme.typography.labelSmall,
                color = if (phoneNumber.length == 10) ProColors.Success else ProColors.TextTertiary
            )
            Text(
                text = "${phoneNumber.length}/10",
                style = MaterialTheme.typography.labelSmall,
                color = if (phoneNumber.length == 10) ProColors.Success else ProColors.TextTertiary
            )
        }
    }
}

