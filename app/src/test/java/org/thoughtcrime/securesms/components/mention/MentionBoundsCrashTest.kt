/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.mention

import android.app.Application
import android.text.Spannable
import android.text.SpannableStringBuilder
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thoughtcrime.securesms.database.model.Mention
import org.thoughtcrime.securesms.recipients.RecipientId

/**
 * Reproduces the remote-crash finding: an incoming message whose mention bodyRange
 * (start/length) exceeds the actual body length is applied with setSpan() without bounds
 * validation, so SpannableStringBuilder.setSpan() throws IndexOutOfBoundsException.
 *
 * @RunWith(RobolectricTestRunner::class)
 * @Config(manifest = Config.NONE, application = Application::class)
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class MentionBoundsCrashTest {

  @Test
  fun `out-of-bounds mention range causes IndexOutOfBoundsException on render`() {
    val body = SpannableStringBuilder("a") // length() == 1
    val mentions = listOf(Mention(RecipientId.from(1L), 0, Int.MAX_VALUE)) // end = 0 + 2147483647 >> 1

    var threw = false
    try {
      MentionAnnotation.setMentionAnnotations(body, mentions)
    } catch (e: IndexOutOfBoundsException) {
      threw = true
    }

    assertTrue("Expected IndexOutOfBoundsException when mention range exceeds body length", threw)
  }

  @Test
  fun `in-bounds mention range does not throw`() {
    val body = SpannableStringBuilder("hello")
    val mentions = listOf(Mention(RecipientId.from(1L), 0, 5))

    MentionAnnotation.setMentionAnnotations(body, mentions)

    assertTrue(true)
  }
}
