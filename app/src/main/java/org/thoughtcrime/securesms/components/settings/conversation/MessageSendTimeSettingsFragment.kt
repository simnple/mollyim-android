package org.thoughtcrime.securesms.components.settings.conversation

import android.os.Bundle
import android.view.View
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
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.TextSecurePreferences

/**
 * Custom fork: full-screen settings for the per-conversation "메시지 전송 시간"
 * (send-time spoofing). Presets match the auto-delete message times; "직접 입력"
 * opens date + time pickers (like scheduling a message). Save writes the pref.
 */
class MessageSendTimeSettingsFragment : ComposeFragment() {

  private val args: MessageSendTimeSettingsFragmentArgs by navArgs()

  @Composable
  override fun FragmentContent() {
    val threadId = args.threadId
    val context = LocalContext.current
    var selection by remember { mutableStateOf(TextSecurePreferences.getTimestampOffsetMillisForThread(requireContext(), threadId)) }

    val labels = listOf(
      stringResource(R.string.TimestampDialog__off),
      stringResource(R.string.ExpireTimerSettingsFragment__30_seconds),
      stringResource(R.string.ExpireTimerSettingsFragment__5_minutes),
      stringResource(R.string.ExpireTimerSettingsFragment__1_hour),
      stringResource(R.string.ExpireTimerSettingsFragment__8_hours),
      stringResource(R.string.ExpireTimerSettingsFragment__1_day),
      stringResource(R.string.ExpireTimerSettingsFragment__1_week),
      stringResource(R.string.ExpireTimerSettingsFragment__4_weeks)
    )
    val offsets = listOf(
      0L,
      -30_000L,
      -5 * 60_000L,
      -60 * 60_000L,
      -8 * 60 * 60_000L,
      -24 * 60 * 60_000L,
      -7 * 24 * 60 * 60_000L,
      -4 * 7 * 24 * 60 * 60_000L
    )

    fun isPreset(value: Long) = offsets.contains(value)

    SignalTheme {
      Scaffolds.Settings(
        title = stringResource(R.string.ConversationSettingsFragment__message_timestamp_spoof),
        onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
        navigationIcon = SignalIcons.ArrowStart.imageVector
      ) { paddingValues ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          LazyColumn {
            items(labels.size) { index ->
              Rows.RadioRow(
                selected = selection == offsets[index],
                text = labels[index],
                modifier = Modifier.clickable { selection = offsets[index] },
                enabled = true
              )
            }

            item {
              val hasCustom = !isPreset(selection)
              Rows.RadioRow(
                selected = hasCustom,
                text = stringResource(R.string.TimestampDialog__custom),
                label = if (hasCustom) android.text.format.DateFormat.getTimeFormat(context).format(selection + System.currentTimeMillis()) else null,
                modifier = Modifier.clickable { showCustom(context, threadId) { offset -> selection = offset } },
                enabled = true
              )
            }
          }

          Buttons.LargeTonal(
            onClick = {
              TextSecurePreferences.setTimestampOffsetMillisForThread(requireContext(), threadId, selection)
              requireActivity().onBackPressedDispatcher.onBackPressed()
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp)
          ) {
            Text(text = stringResource(R.string.ExpireTimerSettingsFragment__save))
          }
        }
      }
    }
  }

  private fun showCustom(context: android.content.Context, threadId: Long, onPicked: (Long) -> Unit) {
    val cal = java.util.Calendar.getInstance()

    val onTime = object : android.app.TimePickerDialog.OnTimeSetListener {
      override fun onTimeSet(view: android.widget.TimePicker, hourOfDay: Int, minute: Int) {
        cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        onPicked(minOf(cal.timeInMillis - System.currentTimeMillis(), 0L))
      }
    }

    android.app.DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month)
        cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
        android.app.TimePickerDialog(context, onTime, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
      },
      cal.get(java.util.Calendar.YEAR),
      cal.get(java.util.Calendar.MONTH),
      cal.get(java.util.Calendar.DAY_OF_MONTH)
    ).show()
  }
}
