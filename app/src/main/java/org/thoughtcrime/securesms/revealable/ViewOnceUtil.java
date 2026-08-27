package org.thoughtcrime.securesms.revealable;

import androidx.annotation.NonNull;

import org.thoughtcrime.securesms.database.AttachmentTable;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;

import java.util.concurrent.TimeUnit;

public class ViewOnceUtil {

  public static final long MAX_LIFESPAN = TimeUnit.DAYS.toMillis(30);

  public static boolean isViewable(@NonNull MmsMessageRecord message) {
    if (!message.isViewOnce()) {
      return true;
    }

    // Custom fork: allow the sender to view their own sent view-once media (previously outgoing
    // returned false here, so you could never reopen your own photo).

    if (message.getSlideDeck().getThumbnailSlide() == null) {
      return false;
    }

    if (message.getSlideDeck().getThumbnailSlide().getUri() == null) {
      return false;
    }

    if (message.getSlideDeck().getThumbnailSlide().getTransferState() != AttachmentTable.TRANSFER_PROGRESS_DONE) {
      return false;
    }

    if (isViewed(message)) {
      return false;
    }

    return true;
  }

  public static boolean isViewed(@NonNull MmsMessageRecord message) {
    if (!message.isViewOnce()) {
      return false;
    }

    if (message.getDateReceived() + MAX_LIFESPAN <= System.currentTimeMillis()) {
      return true;
    }

    if (message.getSlideDeck().getThumbnailSlide() != null && message.getSlideDeck().getThumbnailSlide().getTransferState() != AttachmentTable.TRANSFER_PROGRESS_DONE) {
      return false;
    }

    if (message.getSlideDeck().getThumbnailSlide() == null) {
      return true;
    }

    if (message.getSlideDeck().getThumbnailSlide().getUri() == null) {
      return true;
    }

    // Custom fork: sender's own sent view-once media is no longer forced to "viewed", so they can
    // reopen their own photo while the local file is retained.
    return false;
  }
}
