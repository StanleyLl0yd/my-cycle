package com.example.mycycle.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycycle.BuildConfig
import com.example.mycycle.R
import com.example.mycycle.ui.theme.CycleColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Appearance section
        SettingsSection(title = stringResource(R.string.settings_appearance)) {
            SettingsItemWithSwitch(
                title = stringResource(R.string.settings_dynamic_colors),
                subtitle = stringResource(R.string.settings_dynamic_colors_desc),
                checked = state.useDynamicColors,
                onCheckedChange = viewModel::setDynamicColors
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data section
        SettingsSection(title = stringResource(R.string.settings_data)) {
            SettingsItem(
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_desc),
                onClick = { /* TODO */ }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_clear_data),
                subtitle = stringResource(R.string.settings_clear_data_desc),
                onClick = { /* TODO */ }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About section
        SettingsSection(title = stringResource(R.string.settings_about)) {
            SettingsItem(
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = null
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_desc),
                onClick = null
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        androidx.compose.material3.Card {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
