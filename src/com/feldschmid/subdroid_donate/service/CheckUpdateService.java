package com.feldschmid.subdroid_donate.service;

import java.text.MessageFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.feldschmid.subdroid_donate.R;
import com.feldschmid.subdroid_donate.Revisions;
import com.feldschmid.subdroid_donate.Subdroid;
import com.feldschmid.subdroid_donate.db.ChangeDbAdapter;
import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.db.RevisionDbAdapter;
import com.feldschmid.subdroid_donate.pref.Prefs;
import com.feldschmid.subdroid_donate.util.UserPw;
import com.feldschmid.svn.ReportRetriever;
import com.feldschmid.svn.base.MyException;
import com.feldschmid.svn.cmd.Propfind;
import com.feldschmid.svn.model.LogItem;
import com.feldschmid.svn.model.ReportList;

public class CheckUpdateService extends Service {
	private RevisionDbAdapter mRevisionDbHelper;
	private RepositoryDbAdapter mRepoDbHelper;
	private ChangeDbAdapter mChangeDbHelper;

	private NotificationManager mNotificationManager;
	private PendingIntent mPIntent;
	private Intent mIntent;
	private Notification mNotification;
	private String mNewRevs;
	private String mNewRevsInfo;
	private String mNewRevsText;
	private String mRegexError;
	private String mNotificationError;

	private static PendingIntent mAlarmIntent;

	private static boolean mFlashNotification = false;
	private static boolean mSoundNotification = false;
	private static boolean mVibrateNotification = false;
	private static long mUpdateInterval = 5 * 60000;
	private static boolean mBatteryFriendly = false;

    public static final String SERVICE_RUNNING_KEY = "service_running";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		mNewRevs = getString(R.string.notification_new_revisions);
		mNewRevsInfo = getString(R.string.notification_new_revisions_info);
		mNewRevsText = getString(R.string.notification_new_revisions_text);
		mRegexError = getString(R.string.notification_regex_error);
		mNotificationError = getString(R.string.notification_error);

		mRevisionDbHelper = new RevisionDbAdapter(this);
		mRepoDbHelper = new RepositoryDbAdapter(this);
		mChangeDbHelper = new ChangeDbAdapter(this);

        Thread thr = new Thread(null, mTask, "CheckUpdateService");
        thr.start();
	}


	@Override
  public void onDestroy() {
		super.onDestroy();

		// just in case..
		mRevisionDbHelper.close();
		mRepoDbHelper.close();
		mChangeDbHelper.close();
	}

    Runnable mTask = new Runnable() {
        public void run() {
			Log.d("Service", "starting update for all repos at "+new Date());
			long start = System.currentTimeMillis();

			mRevisionDbHelper.open();
			mRepoDbHelper.open();
			mChangeDbHelper.open();

			// for each repo in the list
			Cursor repos = mRepoDbHelper.fetchAllRepositories();
			if (repos.getCount() > 0) {
				while(!repos.isAfterLast()) {
					// get repo id
					final Integer mRepositoryId = repos.getInt(repos.getColumnIndex(RepositoryDbAdapter.KEY_ROWID));

					Log.d("System", "Starting update for: "+mRepositoryId);
					// get info of repo
			        Cursor repo = mRepoDbHelper.fetchRepository(mRepositoryId);

			        final String name = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_NAME));
					final String url = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_BASEURL))
							+ repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PATH));
			        //final String limit = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_LIMIT));
			        final String user = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_USER));
			        final String pass = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_PASS));
			        final String ignoreSSL = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_IGNORE_SSL));
			        final String notification = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_NOTIFICATION));
			        final String regexAuthor = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_REGEX_AUTHOR));
			        final String regexMessage = repo.getString(repo.getColumnIndexOrThrow(RepositoryDbAdapter.KEY_REGEX_MESSAGE));
			        repo.close();

			        Log.d("System", "Notification is set to: "+notification);
			        if(Boolean.valueOf(notification)  && UserPw.isConsistent(user, pass)) {
						try {
							Log.d("Service", "Starting Propfind");
							long startPropfind = System.currentTimeMillis();
							String remoteRevision = new Propfind(url, user, pass, Boolean.valueOf(ignoreSSL)).execute().get(0).getVersion();
							Log.d("Service", "Propfind took: "+(System.currentTimeMillis()-startPropfind)+" ms!");
							Integer highestRemoteRevision = Integer.valueOf(remoteRevision);

							Integer highestLocalRevision = mRevisionDbHelper.getHighestRevisionForRepositoryId(mRepositoryId);

							if (highestLocalRevision == null || highestRemoteRevision > highestLocalRevision) {
								if (doesIgnoreRegexMatchAll(regexAuthor, regexMessage, url, user, pass, ignoreSSL, highestLocalRevision, highestRemoteRevision)) {
									Log.d("Service", "Regexes matched all revisions for "+name+", no notification will be sent.");
									repos.moveToNext();
									continue;
								}
								Log.d("Service", "Revision for "+name+" is: "+highestRemoteRevision+" your local is: "+highestLocalRevision);

								mIntent = new Intent(CheckUpdateService.this.getApplicationContext(), Revisions.class);
						        mIntent.putExtra(RepositoryDbAdapter.KEY_ROWID, (long) mRepositoryId);
						        mIntent.putExtra(RepositoryDbAdapter.KEY_NAME, name);

						        // the flags will get ignored and be replaced by Intent.FLAG_ACTIVITY_NEW_TASK because the intent is sent from service
						        // behavior could probably be changed by setting singleTask launch mode, but is okay atm
						        mPIntent = PendingIntent.getActivity(CheckUpdateService.this, mRepositoryId, mIntent, Intent.FLAG_ACTIVITY_NEW_TASK);

								mNotification = new Notification(R.drawable.notification, MessageFormat.format(mNewRevsInfo, name), System.currentTimeMillis());
								mNotification.setLatestEventInfo(CheckUpdateService.this, mNewRevs, MessageFormat.format(mNewRevsText, name, highestRemoteRevision, highestLocalRevision), mPIntent);
								mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
								//mNotification.number = numberOfNewRevisions

								if (mFlashNotification) {
									mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
									mNotification.ledOffMS = 1000;
									mNotification.ledOnMS = 4000;
									mNotification.ledARGB = Color.parseColor("#ACB41C");
								}

								if (mSoundNotification) {
									mNotification.defaults |= Notification.DEFAULT_SOUND;
								}

								if (mVibrateNotification) {
									mNotification.vibrate = new long[] { 250, 100, 250, 100, 250, 100, 250, 100, 250 };
								}

								mNotificationManager.notify(mRepositoryId, mNotification);
							} else {
								Log.d("Service", "Your Revisions for "+name+" are up to date!");
							}
						} catch (Exception e) {
							Log.d("Service", e.toString());

							// unpack one layer of MyException for better readability
							if(e.getCause() != null && e.getCause() instanceof MyException) {
								e = (MyException) e.getCause();
							}

							mNotification = new Notification(R.drawable.notification_error, MessageFormat.format(mNotificationError, name), System.currentTimeMillis());
							mIntent = new Intent(CheckUpdateService.this.getApplicationContext(), Subdroid.class);
							mPIntent = PendingIntent.getActivity(CheckUpdateService.this, mRepositoryId, mIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
							mNotification.setLatestEventInfo(CheckUpdateService.this, MessageFormat.format(mNotificationError, name), e.getMessage(), mPIntent);
							mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
							mNotificationManager.notify(mRepositoryId, mNotification);
						}
			        }
			        repos.moveToNext();
				}
				repos.close();
			}

			mRevisionDbHelper.close();
			mRepoDbHelper.close();
			mChangeDbHelper.close();

			Log.d("Service", "Update took: " + (System.currentTimeMillis() - start) + " ms!");
			CheckUpdateService.this.stopSelf();
		}

        /**
         * Returns true if the regexes match all the new revisions, false otherwise.
         */
		private boolean doesIgnoreRegexMatchAll(String regexAuthor, String regexMessage, String url, String user, String pass,
				String ignoreSSL, Integer highestLocalRevision, Integer highestRemoteRevision) throws MyException {
			if(highestLocalRevision == null) {
				highestLocalRevision = new Integer(0);
			}

			Matcher author = null;
			Matcher message = null;
			try {
				if(regexAuthor != null && regexAuthor.trim().length() > 0) {
					author = Pattern.compile(regexAuthor, Pattern.DOTALL).matcher("");
				}
				if(regexMessage != null && regexMessage.trim().length() > 0) {
					message = Pattern.compile(regexMessage, Pattern.DOTALL).matcher("");
				}
			} catch(PatternSyntaxException e) {
				throw new MyException(mRegexError, e);
			}
			// if both regex are empty, return false, so nothing will be ignored
			if(author == null && message == null) {
				return false;
			}

			ReportList list = ReportRetriever.retrieveReport(url, user, pass, Boolean.valueOf(ignoreSSL),
					highestRemoteRevision, Integer.valueOf(highestRemoteRevision-highestLocalRevision), false);
			for(LogItem item : list) {
				if(author != null) {
					author.reset(item.getAuthor());
					if (!author.matches()) {
						return false;
					}
				}
				if(message != null) {
					message.reset(item.getComment());
					if (!message.matches()) {
						return false;
					}
				}
			}

			return true;
		}
    };

	public static void scheduleAlarm(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        mFlashNotification = prefs.getBoolean(Prefs.SHOW_LIGHTS, false);
        mSoundNotification = prefs.getBoolean(Prefs.DEFAULT_SOUND, false);
        mVibrateNotification = prefs.getBoolean(Prefs.VIBRATE, false);
        mUpdateInterval = Long.valueOf(prefs.getString(Prefs.UPDATE_INTERVAL, "15"))*60000;
        mBatteryFriendly = prefs.getBoolean(Prefs.BATTERY_FRIENDLY, false);

        mAlarmIntent = PendingIntent.getService(ctx,
                0, new Intent(ctx, CheckUpdateService.class), PendingIntent.FLAG_CANCEL_CURRENT);

        // Start update immediately
        long firstTime = SystemClock.elapsedRealtime();

        // Schedule the alarm
        AlarmManager am = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);

        if(mBatteryFriendly) {
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					firstTime, mUpdateInterval, mAlarmIntent);
        }
        else {
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    firstTime, mUpdateInterval, mAlarmIntent);
        }

        prefs.edit().putBoolean(SERVICE_RUNNING_KEY, true).commit();
	}

	public static void unScheduleAlarm(Context ctx) {
		AlarmManager am = (AlarmManager)ctx.getSystemService(ALARM_SERVICE);
        am.cancel(mAlarmIntent);

		PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(
				SERVICE_RUNNING_KEY, false).commit();
	}

	public static void setNotification(Context ctx) {
		NotificationManager nM = (NotificationManager)ctx.getSystemService(NOTIFICATION_SERVICE);

		String info = ctx.getString(R.string.notification_service_running_info);
		String text = ctx.getString(R.string.notification_service_running_text);

		PendingIntent pIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, Subdroid.class), PendingIntent.FLAG_CANCEL_CURRENT);
		Notification notification = new Notification(R.drawable.notification_service, info, System.currentTimeMillis());
		notification.setLatestEventInfo(ctx, info, text, pIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;

		nM.notify(R.id.notification, notification);
	}

	public static void removeNotification(Context ctx) {
		NotificationManager nM = (NotificationManager)ctx.getSystemService(NOTIFICATION_SERVICE);
		nM.cancel(R.id.notification);
	}

}
