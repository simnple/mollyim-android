package org.thoughtcrime.securesms.components.settings.conversation

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.CircularProgressWrapper
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.app.privacy.expire.CustomExpireTimerSelectorView
import org.thoughtcrime.securesms.util.ExpirationUtil
import org.thoughtcrime.securesms.util.TextSecurePreferences

/**
 * Custom fork: full-screen settings for the per-conversation "강제 자동 삭제 메시지"
 * (outgoing message expiry override). Select a value and press Save; no alert.
 */
class ForceExpirySettingsFragment : ComposeFragment() {

  private val args: ForceExpirySettingsFragmentArgs by navArgs()

  @Composable
  override fun FragmentContent() {
    val threadId = args.threadId
    var selection by remember {
      mutableStateOf(TextSecurePreferences.getExpiryOverrideSecondsForThread(requireContext(), threadId))
    }
    val context = LocalContext.current

    val labels = listOf(
      stringResource(R.string.ExpiryDialog__follow_room),
      stringResource(R.string.ExpireTimerSettingsFragment__off),
      stringResource(R.string.ExpireTimerSettingsFragment__30_seconds),
      stringResource(R.string.ExpireTimerSettingsFragment__5_minutes),
      stringResource(R.string.ExpireTimerSettingsFragment__1_hour),
      stringResource(R.string.ExpireTimerSettingsFragment__8_hours),
      stringResource(R.string.ExpireTimerSettingsFragment__1_day),
      stringResource(R.string.ExpireTimerSettingsFragment__1_week),
      stringResource(R.string.ExpireTimerSettingsFragment__4_weeks)
    )
    val values = listOf(
      TextSecurePreferences.EXPIRY_OVERRIDE_UNSET,
      0,
      30,
      300,
      3600,
      28800,
      86400,
      604800,
      2419200
    )

    SignalTheme {
      Scaffolds.Settings(
        title = stringResource(R.string.ConversationSettingsFragment__force_disappearing_messages),
        onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
        navigationIcon = SignalIcons.ArrowStart.imageVector
      ) { paddingValues ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          LazyColumn {
            item {
              Rows.TextRow(
                label = stringResource(R.string.ForceExpirySettingsFragment__description)
              )
            }

            items(labels.size) { index ->
              Rows.RadioRow(
                selected = selection == values[index],
                text = labels[index],
                modifier = Modifier.clickable { selection = values[index] },
                enabled = true
              )
            }

            item {
              val hasCustom = values.none { it == selection }
              Rows.RadioRow(
                selected = hasCustom,
                text = stringResource(R.string.ExpiryDialog__custom),
                label = if (hasCustom) ExpirationUtil.getExpirationDisplayValue(context, selection) else null,
                modifier = Modifier.clickable { showCustomExpiryInput(context, selection) { value -> selection = value } },
                enabled = true
              )
            }
          }

          CircularProgressWrapper(
            isLoading = false,
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .horizontalGutters()
              .padding(bottom = 16.dp)
          ) {
            Buttons.LargeTonal(
              onClick = {
                TextSecurePreferences.setExpiryOverrideSecondsForThread(requireContext(), threadId, selection)
                requireActivity().onBackPressedDispatcher.onBackPressed()
              },
              enabled = true
            ) {
              Text(text = stringResource(R.string.ExpireTimerSettingsFragment__save))
            }
          }
        }
      }
    }
  }

  private fun showCustomExpiryInput(context: Context, initialSeconds: Int, onSet: (Int) -> Unit) {
    val view = LayoutInflater.from(context).inflate(R.layout.custom_expire_timer_select_dialog, null, false)
    val selector = view.findViewById<CustomExpireTimerSelectorView>(R.id.custom_expire_timer_select_dialog_selector)

    if (initialSeconds > 0) {
      selector.setTimer(initialSeconds)
    }

    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.ExpireTimerSettingsFragment__custom_time)
      .setView(view)
      .setPositiveButton(R.string.ExpireTimerSettingsFragment__set) { _, _ ->
        onSet(selector.getTimer())
      }
      .setNegativeButton(R.string.ExpiryDialog__cancel, null)
      .show()
  }
}
