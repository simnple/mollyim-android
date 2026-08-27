package org.thoughtcrime.securesms.revealable;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.MessageTable;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.dependencies.AppDependencies;
import org.thoughtcrime.securesms.service.ExportedBroadcastReceiver;
import org.thoughtcrime.securesms.service.TimedEventManager;

/**
 * Manages clearing removable message content after they're opened.
 */
public class ViewOnceMessageManager extends TimedEventManager<ViewOnceExpirationInfo> {

  private static final String TAG = Log.tag(ViewOnceMessageManager.class);

  private final MessageTable mmsDatabase;

  public ViewOnceMessageManager(@NonNull Application application) {
    super(application, "RevealableMessageManager");

    this.mmsDatabase = SignalDatabase.messages();

    scheduleIfNecessary();
  }

  @WorkerThread
  @Override
  protected @Nullable ViewOnceExpirationInfo getNextClosestEvent() {
    ViewOnceExpirationInfo expirationInfo = mmsDatabase.getNearestExpiringViewOnceMessage();

    if (expirationInfo != null) {
      Log.i(TAG, "Next closest expiration is in " + getDelayForEvent(expirationInfo) + " ms for messsage " + expirationInfo.getMessageId() + ".");
    } else {
      Log.i(TAG, "No messages to schedule.");
    }

    return expirationInfo;
  }

  @WorkerThread
  @Override
  protected void executeEvent(@NonNull ViewOnceExpirationInfo event) {
    // Custom fork: retain view-once media indefinitely instead of auto-clearing it after
    // MAX_LIFESPAN. Received view-once media stays available. This mirrors the "keep sent and
    // aged view-once media" patches in the j0j1j2/Signal-Desktop fork.
    Log.i(TAG, "View-once expiration fired for message " + event.getMessageId() + ", but media retention is enabled. Skipping deletion.");
  }

  @WorkerThread
  @Override
  protected long getDelayForEvent(@NonNull ViewOnceExpirationInfo event) {
    long expiresAt = event.getReceiveTime() + ViewOnceUtil.MAX_LIFESPAN;
    long timeLeft  = expiresAt - System.currentTimeMillis();

    return Math.max(0, timeLeft);
  }

  @AnyThread
  @Override
  protected void scheduleAlarm(@NonNull Application application, ViewOnceExpirationInfo event, long delay) {
    setAlarm(application, delay, ViewOnceAlarm.class);
  }

  public static class ViewOnceAlarm extends ExportedBroadcastReceiver {

    private static final String TAG = Log.tag(ViewOnceAlarm.class);

    @Override
    public void onReceiveUnlock(Context context, Intent intent) {
      Log.d(TAG, "onReceive()");
      AppDependencies.getViewOnceMessageManager().scheduleIfNecessary();
    }
  }
}
