/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.features

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.TextSecurePreferences

/**
 * Custom fork: "추가 기능" — global on/off switches for the always-on Molly features, plus
 * backup/restore of these additional settings to/from a JSON document.
 */
class AdditionalFeaturesSettingsFragment : ComposeFragment() {

  @Composable
  override fun FragmentContent() {
    val context = LocalContext.current
    var forceExpiryEnabled by remember { mutableStateOf(TextSecurePreferences.isForceExpiryEnabled(requireContext())) }
    var timestampSpoofEnabled by remember { mutableStateOf(TextSecurePreferences.isTimestampSpoofEnabled(requireContext())) }
    var echoEnabled by remember { mutableStateOf(TextSecurePreferences.isEchoFeatureEnabled(requireContext())) }
    var showBlockedEnabled by remember { mutableStateOf(TextSecurePreferences.isShowBlockedMessagesEnabled(requireContext())) }
    var massSendEnabled by remember { mutableStateOf(TextSecurePreferences.isMassSendEnabled(requireContext())) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let {
        val json = runCatching {
          requireContext().contentResolver.openInputStream(it)?.bufferedReader()?.readText()
        }.getOrNull()
        if (!json.isNullOrEmpty()) {
          TextSecurePreferences.importAdditionalSettingsJson(context, json)
          forceExpiryEnabled = TextSecurePreferences.isForceExpiryEnabled(context)
          timestampSpoofEnabled = TextSecurePreferences.isTimestampSpoofEnabled(context)
          echoEnabled = TextSecurePreferences.isEchoFeatureEnabled(context)
          showBlockedEnabled = TextSecurePreferences.isShowBlockedMessagesEnabled(context)
          massSendEnabled = TextSecurePreferences.isMassSendEnabled(context)
        }
      }
    }

    SignalTheme {
      Scaffolds.Settings(
        title = stringResource(R.string.AdditionalFeatures__title),
        navigationIcon = SignalIcons.ArrowStart.imageVector,
        onNavigationClick = { findNavController().popBackStack() }
      ) { paddingValues ->
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = paddingValues
        ) {
          item {
            Rows.TextRow(
              label = stringResource(R.string.AdditionalFeatures__description)
            )
          }

          item {
            Rows.ToggleRow(
              checked = forceExpiryEnabled,
              text = stringResource(R.string.AdditionalFeatures__force_expiry),
              label = stringResource(R.string.AdditionalFeatures__force_expiry_desc),
              onCheckChanged = { checked ->
                forceExpiryEnabled = checked
                TextSecurePreferences.setForceExpiryEnabled(context, checked)
              }
            )
          }

          item {
            Rows.ToggleRow(
              checked = timestampSpoofEnabled,
              text = stringResource(R.string.AdditionalFeatures__timestamp_spoof),
              label = stringResource(R.string.AdditionalFeatures__timestamp_spoof_desc),
              onCheckChanged = { checked ->
                timestampSpoofEnabled = checked
                TextSecurePreferences.setTimestampSpoofEnabled(context, checked)
              }
            )
          }

          item {
            Rows.ToggleRow(
              checked = echoEnabled,
              text = stringResource(R.string.AdditionalFeatures__echo),
              label = stringResource(R.string.AdditionalFeatures__echo_desc),
              onCheckChanged = { checked ->
                echoEnabled = checked
                TextSecurePreferences.setEchoFeatureEnabled(context, checked)
              }
            )
          }

          item {
            Rows.ToggleRow(
              checked = showBlockedEnabled,
              text = stringResource(R.string.AdditionalFeatures__show_blocked),
              label = stringResource(R.string.AdditionalFeatures__show_blocked_desc),
              onCheckChanged = { checked ->
                showBlockedEnabled = checked
                TextSecurePreferences.setShowBlockedMessagesEnabled(context, checked)
              }
            )
          }

          item {
            Rows.ToggleRow(
              checked = massSendEnabled,
              text = stringResource(R.string.AdditionalFeatures__mass_send),
              label = stringResource(R.string.AdditionalFeatures__mass_send_desc),
              onCheckChanged = { checked ->
                massSendEnabled = checked
                TextSecurePreferences.setMassSendEnabled(context, checked)
              }
            )
          }

          item {
            Rows.TextRow(
              text = stringResource(R.string.AdditionalFeatures__export),
              onClick = {
                val json = TextSecurePreferences.exportAdditionalSettingsJson(context)
                val share = Intent(Intent.ACTION_SEND).apply {
                  type = "application/json"
                  putExtra(Intent.EXTRA_SUBJECT, "molly-additional-settings.json")
                  putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(share, null))
              }
            )
          }

          item {
            Rows.TextRow(
              text = stringResource(R.string.AdditionalFeatures__import),
              onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
            )
          }
        }
      }
    }
  }
}
