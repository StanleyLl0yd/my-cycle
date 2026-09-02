package com.sl.mycycle.ui.settings

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sl.mycycle.BuildConfig
import com.sl.mycycle.R
import com.sl.mycycle.data.transfer.BackupPreview
import com.sl.mycycle.data.transfer.CsvImportPreview
import com.sl.mycycle.domain.model.CycleStage
import com.sl.mycycle.domain.model.ThemeMode
import com.sl.mycycle.privacy.AppAuthenticator
import com.sl.mycycle.ui.MainActivity
import com.sl.mycycle.ui.theme.CycleColors
import com.sl.mycycle.util.runSuspendCatching
import java.io.OutputStreamWriter
import java.nio.file.FileSystems
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

private const val GITHUB_URL = "https://github.com/StanleyLl0yd/my-cycle"
private const val LICENSE_URL = "https://polyformproject.org/licenses/noncommercial/1.0.0"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val genericErrorMessage = stringResource(R.string.error_generic)
    val exportSuccessMessage = stringResource(R.string.dialog_export_success)
    val importSuccessMessage = stringResource(R.string.dialog_import_success)
    val backupSuccessMessage = stringResource(R.string.dialog_backup_success)
    val restoreSuccessMessage = stringResource(R.string.dialog_restore_success)
    val permissionDeniedMessage = stringResource(R.string.settings_reminder_permission_denied)
    val appLockErrorMessage = stringResource(R.string.settings_app_lock_error)
    val dynamicColorsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appLockSupported = AppAuthenticator.isSupported

    var showClearDialog by remember { mutableStateOf(false) }
    var dataOperationRunning by remember { mutableStateOf(false) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var csvPreview by remember { mutableStateOf<CsvImportPreview?>(null) }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    var backupPreview by remember { mutableStateOf<BackupPreview?>(null) }
    val interactionsEnabled = !state.isClearingData && !dataOperationRunning

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                dataOperationRunning = true
                val result = runSuspendCatching {
                    writeText(context, uri, viewModel.buildCsvExport())
                }
                dataOperationRunning = false
                showToast(context, if (result.isSuccess) exportSuccessMessage else genericErrorMessage)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                dataOperationRunning = true
                val result = runSuspendCatching {
                    val csv = readText(context, uri)
                    csv to viewModel.previewCsvImport(csv)
                }
                dataOperationRunning = false
                result.onSuccess { (csv, preview) ->
                    pendingCsv = csv
                    csvPreview = preview
                }.onFailure {
                    showToast(context, genericErrorMessage)
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                dataOperationRunning = true
                val result = runSuspendCatching {
                    writeText(context, uri, viewModel.buildBackup())
                }
                dataOperationRunning = false
                showToast(context, if (result.isSuccess) backupSuccessMessage else genericErrorMessage)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                dataOperationRunning = true
                val result = runSuspendCatching {
                    val backup = readText(context, uri)
                    backup to viewModel.previewBackup(backup)
                }
                dataOperationRunning = false
                result.onSuccess { (backup, preview) ->
                    pendingBackup = backup
                    backupPreview = preview
                }.onFailure {
                    showToast(context, genericErrorMessage)
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
        } else {
            showToast(context, permissionDeniedMessage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CycleColors.backgroundGradient())
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (state.hasOperationError) {
            Text(
                text = stringResource(R.string.error_generic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        SettingsSection(title = stringResource(R.string.settings_cycle_stage)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_cycle_stage_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CycleStage.entries
                        .filterNot { it == CycleStage.NOT_SET }
                        .forEach { stage ->
                            FilterChip(
                                selected = state.cycleStage == stage,
                                onClick = { viewModel.setCycleStage(stage) },
                                enabled = interactionsEnabled,
                                label = { Text(stringResource(stage.labelRes)) }
                            )
                        }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(state.cycleStage.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.settings_appearance)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            enabled = interactionsEnabled,
                            label = { Text(stringResource(mode.labelRes)) }
                        )
                    }
                }
            }
            HorizontalDivider()
            SettingsItemWithSwitch(
                title = stringResource(R.string.settings_dynamic_colors),
                subtitle = stringResource(
                    if (dynamicColorsAvailable) {
                        R.string.settings_dynamic_colors_desc
                    } else {
                        R.string.settings_dynamic_colors_unavailable
                    }
                ),
                checked = state.useDynamicColors,
                enabled = dynamicColorsAvailable && interactionsEnabled,
                onCheckedChange = viewModel::setDynamicColors
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.settings_privacy_security)) {
            SettingsItemWithSwitch(
                title = stringResource(R.string.settings_app_lock),
                subtitle = stringResource(
                    if (appLockSupported) {
                        R.string.settings_app_lock_desc
                    } else {
                        R.string.settings_app_lock_unavailable
                    }
                ),
                checked = state.appLockEnabled,
                enabled = appLockSupported && interactionsEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setAppLockEnabled(false)
                    } else {
                        val activity = context.findActivity()
                        if (activity == null) {
                            showToast(context, appLockErrorMessage)
                        } else {
                            AppAuthenticator.authenticate(
                                activity = activity,
                                onSuccess = {
                                    (activity as? MainActivity)?.markUnlocked()
                                    viewModel.setAppLockEnabled(true)
                                },
                                onError = { showToast(context, appLockErrorMessage) }
                            )
                        }
                    }
                }
            )
            HorizontalDivider()
            SettingsItemWithSwitch(
                title = stringResource(R.string.settings_protect_screen),
                subtitle = stringResource(R.string.settings_protect_screen_desc),
                checked = state.protectScreenEnabled,
                enabled = interactionsEnabled,
                onCheckedChange = viewModel::setProtectScreenEnabled
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.settings_reminders)) {
            SettingsItemWithSwitch(
                title = stringResource(R.string.settings_daily_reminder),
                subtitle = stringResource(R.string.settings_daily_reminder_desc),
                checked = state.dailyReminderEnabled,
                enabled = interactionsEnabled,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setReminderEnabled(false)
                    } else if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setReminderEnabled(true)
                    }
                }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_reminder_time),
                subtitle = String.format(
                    Locale.ROOT,
                    "%02d:%02d",
                    state.reminderHour,
                    state.reminderMinute
                ),
                enabled = interactionsEnabled,
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> viewModel.setReminderTime(hour, minute) },
                        state.reminderHour,
                        state.reminderMinute,
                        true
                    ).show()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.settings_data)) {
            SettingsItem(
                title = stringResource(R.string.settings_export),
                subtitle = stringResource(R.string.settings_export_desc),
                enabled = interactionsEnabled,
                onClick = { exportLauncher.launch("my-cycle-${LocalDate.now()}.csv") }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_import_csv),
                subtitle = stringResource(R.string.settings_import_csv_desc),
                enabled = interactionsEnabled,
                onClick = { importLauncher.launch(arrayOf("text/*", "application/csv")) }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_backup),
                subtitle = stringResource(R.string.settings_backup_desc),
                enabled = interactionsEnabled,
                onClick = {
                    backupLauncher.launch("my-cycle-backup-${LocalDate.now()}.mycycle")
                }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_restore),
                subtitle = stringResource(R.string.settings_restore_desc),
                enabled = interactionsEnabled,
                onClick = { restoreLauncher.launch(arrayOf("*/*")) }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_clear_data),
                subtitle = stringResource(R.string.settings_clear_data_desc),
                enabled = interactionsEnabled,
                onClick = { showClearDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource(R.string.settings_about)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_author),
                subtitle = stringResource(R.string.settings_author_name),
                onClick = null
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = null
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_license),
                subtitle = stringResource(R.string.settings_license_value),
                enabled = interactionsEnabled,
                onClick = { openUrl(context, LICENSE_URL) }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_github),
                subtitle = stringResource(R.string.settings_github_desc),
                enabled = interactionsEnabled,
                onClick = { openUrl(context, GITHUB_URL) }
            )
            HorizontalDivider()
            SettingsItem(
                title = stringResource(R.string.settings_privacy),
                subtitle = stringResource(R.string.settings_privacy_local_desc),
                onClick = null
            )
        }
    }

    csvPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                pendingCsv = null
                csvPreview = null
            },
            title = { Text(stringResource(R.string.dialog_import_csv_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_import_csv_message,
                        preview.totalRecords,
                        preview.newRecords,
                        preview.replacedRecords
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val csv = pendingCsv ?: return@TextButton
                        pendingCsv = null
                        csvPreview = null
                        scope.launch {
                            dataOperationRunning = true
                            val result = runSuspendCatching { viewModel.importCsv(csv) }
                            dataOperationRunning = false
                            showToast(
                                context,
                                if (result.isSuccess) importSuccessMessage else genericErrorMessage
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_import_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingCsv = null
                        csvPreview = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    backupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                pendingBackup = null
                backupPreview = null
            },
            title = { Text(stringResource(R.string.dialog_restore_title)) },
            text = {
                Text(stringResource(R.string.dialog_restore_message, preview.diaryRecords))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val backup = pendingBackup ?: return@TextButton
                        pendingBackup = null
                        backupPreview = null
                        scope.launch {
                            dataOperationRunning = true
                            val result = runSuspendCatching { viewModel.restoreBackup(backup) }
                            dataOperationRunning = false
                            showToast(
                                context,
                                if (result.isSuccess) restoreSuccessMessage else genericErrorMessage
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingBackup = null
                        backupPreview = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_data_title)) },
            text = { Text(stringResource(R.string.dialog_clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearAllData()
                    },
                    enabled = interactionsEnabled
                ) {
                    Text(stringResource(R.string.dialog_clear_data_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    enabled = interactionsEnabled
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    validateDocumentUri(uri)
    val input = context.contentResolver.openInputStream(uri)
        ?: error("Could not open selected file")
    input.bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    validateDocumentUri(uri)
    val output = context.contentResolver.openOutputStream(uri)
        ?: error("Could not open destination")
    output.use { stream ->
        OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { writer ->
            writer.write(text)
        }
    }
}

private fun validateDocumentUri(uri: Uri) {
    if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority.isNullOrBlank()) {
        throw SecurityException("Unsupported document URI")
    }
    val path = uri.path ?: throw SecurityException("Document URI has no path")
    val normalizedPath = FileSystems.getDefault().getPath(path).normalize()
    if (normalizedPath.startsWith("/data")) {
        throw SecurityException("Private data paths are not allowed")
    }
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
    onClick: (() -> Unit)?,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
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
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
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

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
