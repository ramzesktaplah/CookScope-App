package com.rork.cookscope.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rork.cookscope.data.UserManager
import com.rork.cookscope.data.model.UserRole
import com.rork.cookscope.ui.theme.BlackCard
import com.rork.cookscope.ui.theme.BlackSurface
import com.rork.cookscope.ui.theme.GoldPrimary
import com.rork.cookscope.ui.theme.TextSecondary

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val profile by UserManager.profile.collectAsState()

    var displayName by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username) }
    var bio by remember { mutableStateOf(profile.bio) }
    var photoUri by remember { mutableStateOf(profile.profilePhotoUri) }
    var selectedRole by remember { mutableStateOf(profile.role) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        UserManager.updateProfile(
                            username = username,
                            displayName = displayName,
                            bio = bio,
                            profilePhotoUri = photoUri,
                            role = selectedRole,
                        )
                        onBack()
                    }
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = GoldPrimary,
                    )
                }
            }
        }

        // Profile photo
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f))
                        .border(2.dp, GoldPrimary, CircleShape)
                        .clickable { /* Would open photo picker */ },
                    contentAlignment = Alignment.Center,
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Would open photo picker */ },
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Change Photo",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Form fields
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FormField(
                    label = "Display Name",
                    value = displayName,
                    onValueChange = { displayName = it },
                )
                FormField(
                    label = "Username",
                    value = username,
                    onValueChange = { username = it },
                    prefix = "@",
                )
                FormField(
                    label = "Bio",
                    value = bio,
                    onValueChange = { bio = it },
                    singleLine = false,
                )
            }
        }

        // Role selector
        item {
            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Account Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                RoleOption(
                    title = "Chef / Teacher",
                    description = "Post cooking content, receive Stars from learners",
                    isSelected = selectedRole == UserRole.CHEF_TEACHER,
                    onClick = { selectedRole = UserRole.CHEF_TEACHER },
                )
                Spacer(Modifier.height(8.dp))
                RoleOption(
                    title = "Learner",
                    description = "Watch, learn, and give Stars to chefs you love",
                    isSelected = selectedRole == UserRole.LEARNER,
                    onClick = { selectedRole = UserRole.LEARNER },
                )
            }
        }

        // Save button
        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    UserManager.updateProfile(
                        username = username,
                        displayName = displayName,
                        bio = bio,
                        profilePhotoUri = photoUri,
                        role = selectedRole,
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    singleLine: Boolean = true,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            prefix = if (prefix != null) {
                { Text(prefix, color = TextSecondary) }
            } else null,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BlackCard,
                unfocusedContainerColor = BlackCard,
                focusedIndicatorColor = GoldPrimary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoleOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) GoldPrimary.copy(alpha = 0.12f) else BlackCard)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
