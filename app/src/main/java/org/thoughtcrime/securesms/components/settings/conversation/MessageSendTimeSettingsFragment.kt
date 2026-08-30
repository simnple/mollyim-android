package org.thoughtcrime.securesms.components.settings.conversation

import android.content.Context
import android.text.format.DateFormat
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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.CircularProgressWrapper
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.horizontalGutters
import org.signal.core.ui.compose.theme.SignalTheme
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.TextSecurePreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Custom fork: full-screen settings for the per-conversation "메시지 전송 시간"
 * (send-time spoofing). Presets match the auto-delete message times; "직접 입력"
 * opens the Molly date + time pickers. Save writes the pref.
 */
class MessageSendTimeSettingsFragment : ComposeFragment() {

  private val args: MessageSendTimeSettingsFragmentArgs by navArgs()

  private var pendingUtcDateMillis: Long = System.currentTimeMillis()
  private var pendingHour: Int = 0
  private var pendingMinute: Int = 0

  @Composable
  override fun FragmentContent() {
    val threadId = args.threadId
    val context = LocalContext.current
    var selection by remember { mutableStateOf(TextSecurePreferences.getTimestampOffsetMillisForThread(requireContext(), threadId)) }

    val labels = listOf(
      stringResource(R.string.TimestampDialog__off),
      agoLabel(R.string.ExpireTimerSettingsFragment__30_seconds),
      agoLabel(R.string.ExpireTimerSettingsFragment__5_minutes),
      agoLabel(R.string.ExpireTimerSettingsFragment__1_hour),
      agoLabel(R.string.ExpireTimerSettingsFragment__8_hours),
      agoLabel(R.string.ExpireTimerSettingsFragment__1_day),
      agoLabel(R.string.ExpireTimerSettingsFragment__1_week),
      agoLabel(R.string.ExpireTimerSettingsFragment__4_weeks)
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
            item {
              Rows.TextRow(
                label = stringResource(R.string.MessageSendTimeSettingsFragment__description)
              )
            }

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
                label = if (hasCustom) formatCustomOffset(context, selection) else null,
                modifier = Modifier.clickable { showCustomDatePicker(context, selection) { offset -> selection = offset } },
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
                TextSecurePreferences.setTimestampOffsetMillisForThread(requireContext(), threadId, selection)
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

  private fun showCustomDatePicker(context: Context, selection: Long, onPicked: (Long) -> Unit) {
    val target = if (selection == 0L) System.currentTimeMillis() else selection + System.currentTimeMillis()

    val localDateTime = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault()).toLocalDateTime()
    pendingHour = localDateTime.hour
    pendingMinute = localDateTime.minute
    pendingUtcDateMillis = localDateTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    val datePicker = MaterialDatePicker.Builder.datePicker()
      .setTitleText(R.string.ScheduleMessageTimePickerBottomSheet__select_date_title)
      .setSelection(pendingUtcDateMillis)
      .build()

    datePicker.addOnPositiveButtonClickListener { utcMillis ->
      pendingUtcDateMillis = utcMillis
      showCustomTimePicker(context, onPicked)
    }

    datePicker.show(childFragmentManager, "DATE_PICKER")
  }

  private fun showCustomTimePicker(context: Context, onPicked: (Long) -> Unit) {
    val timeFormat = if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

    val timePicker = MaterialTimePicker.Builder()
      .setTimeFormat(timeFormat)
      .setHour(pendingHour)
      .setMinute(pendingMinute)
      .setTitleText(R.string.ScheduleMessageTimePickerBottomSheet__select_time_title)
      .build()

    timePicker.addOnPositiveButtonClickListener {
      pendingHour = timePicker.hour
      pendingMinute = timePicker.minute

      val selectedDate: LocalDate = Instant.ofEpochMilli(pendingUtcDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
      val target = selectedDate.atTime(pendingHour, pendingMinute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
      onPicked(minOf(target - System.currentTimeMillis(), 0L))
    }

    timePicker.show(childFragmentManager, "TIME_PICKER")
  }

  @Composable
  private fun agoLabel(labelResId: Int): String {
    return stringResource(R.string.TimestampDialog__ago, stringResource(labelResId))
  }

  private fun formatCustomOffset(context: Context, offset: Long): String {
    val target = offset + System.currentTimeMillis()
    val dateFormat = DateFormat.getDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    return "${dateFormat.format(target)} ${timeFormat.format(target)}"
  }
}
