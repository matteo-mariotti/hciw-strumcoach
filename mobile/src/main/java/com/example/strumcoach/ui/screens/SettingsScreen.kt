package com.example.strumcoach.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.strumcoach.ui.theme.ThemeMode

enum class SensitivityLevel(val multiplier: Float, val label: String) {
    LOW(0.7f, "Low"),
    MEDIUM(1.0f, "Medium"),
    HIGH(1.3f, "High");

    companion object {
        fun fromMultiplier(multiplier: Float): SensitivityLevel =
            entries.minByOrNull { kotlin.math.abs(it.multiplier - multiplier) } ?: MEDIUM
    }
}

enum class HapticLevel(val amplitude: Int, val label: String) {
    OFF(0, "Off"),
    LOW(85, "Low"),
    MEDIUM(170, "Medium"),
    HIGH(255, "High");

    companion object {
        fun fromAmplitude(amplitude: Int): HapticLevel =
            entries.minByOrNull { kotlin.math.abs(it.amplitude - amplitude) } ?: MEDIUM
    }
}

enum class ReminderTime(val hour: Int, val label: String) {
    MORNING(9, "Morning"),
    AFTERNOON(14, "Afternoon"),
    EVENING(19, "Evening")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    sensitivity: SensitivityLevel,
    onSensitivityChange: (SensitivityLevel) -> Unit,
    hapticLevel: HapticLevel,
    onHapticLevelChange: (HapticLevel) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderTime: ReminderTime,
    onReminderTimeChange: (ReminderTime) -> Unit,
    backendUrl: String,
    onBackendUrlSave: (String) -> Unit,
    onReplayTutorial: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Practice") {
                Text(
                    "Detection sensitivity",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Higher sensitivity picks up lighter strums, but may pick up false ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SensitivityLevel.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = sensitivity == level,
                            onClick = { onSensitivityChange(level) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = SensitivityLevel.entries.size)
                        ) {
                            Text(level.label)
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Watch") {
                Text(
                    "Haptic feedback intensity",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HapticLevel.entries.forEachIndexed { index, level ->
                        SegmentedButton(
                            selected = hapticLevel == level,
                            onClick = { onHapticLevelChange(level) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = HapticLevel.entries.size)
                        ) {
                            Text(level.label)
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Reminders") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily practice reminder", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "A notification if you haven't practiced yet that day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = onReminderEnabledChange)
                }
                if (reminderEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ReminderTime.entries.forEachIndexed { index, time ->
                            SegmentedButton(
                                selected = reminderTime == time,
                                onClick = { onReminderTimeChange(time) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ReminderTime.entries.size)
                            ) {
                                Text(time.label)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Help") {
                SettingsRow(
                    icon = Icons.Default.RestartAlt,
                    label = "Replay Tutorial",
                    onClick = onReplayTutorial
                )
            }
        }

        item {
            SettingsSection(title = "Account") {
                SettingsRow(
                    icon = Icons.Default.Logout,
                    label = "Sign Out",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onSignOut
                )
            }
        }

        item {
            SettingsSection(title = "Advanced") {
                Text(
                    "Backend server address",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "Only needed for local development, if the backend's network address changed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var editedUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
                OutlinedTextField(
                    value = editedUrl,
                    onValueChange = { editedUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("http://192.168.1.100:8000/") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onBackendUrlSave(editedUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = editedUrl.isNotBlank() && editedUrl != backendUrl
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
