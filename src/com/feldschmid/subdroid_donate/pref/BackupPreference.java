package com.feldschmid.subdroid_donate.pref;

import java.io.File;
import java.io.IOException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.feldschmid.subdroid_donate.R;
import com.feldschmid.subdroid_donate.db.ChangeDbAdapter;
import com.feldschmid.subdroid_donate.db.RepositoryDbAdapter;
import com.feldschmid.subdroid_donate.db.RevisionDbAdapter;
import com.feldschmid.subdroid_donate.util.FileUtil;


public class BackupPreference extends Preference {

  private static final String SUBDROID_BACKUP = "subdroid/backup";

  private static final String TAG             = "Backup";

  public BackupPreference(Context context) {
    super(context);
  }

  public BackupPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BackupPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onClick() {
    super.onClick();
    Context ctx = getContext();
    if (ctx.getString(R.string.prefs_backup).equals(getTitle())) {
      doBackup();
    }
    else if (ctx.getString(R.string.prefs_restore).equals(getTitle())) {
      doImport();
    }
  }

  private void doBackup() {
    new AlertDialog.Builder(getContext()).setMessage(R.string.prefs_backup_text)
        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (isExternalStorageAvail()) {
              new ExportDatabaseTask().execute();
            }
            else {
              Toast.makeText(getContext(), R.string.no_sdcard_found, Toast.LENGTH_SHORT).show();
            }
          }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        }).show();
  }

  private void doImport() {

    new AlertDialog.Builder(getContext()).setMessage(R.string.prefs_restore_text)
        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            if (isExternalStorageAvail()) {
              new ImportDatabaseTask().execute();
            }
            else {
              Toast.makeText(getContext(), R.string.no_sdcard_found, Toast.LENGTH_SHORT).show();
            }
          }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        }).show();
  }

  class ExportDatabaseTask extends AsyncTask<Void, Void, String> {

    private final ProgressDialog dialog = new ProgressDialog(BackupPreference.this.getContext());

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      dialog.setMessage(getContext().getText(R.string.backup_in_progress));
      dialog.show();
    }

    @Override
    protected String doInBackground(Void... params) {
      dialog.setMessage(getContext().getText(R.string.backup_in_progress));
      dialog.show();

      String[] exportDbFiles = {
          Environment.getDataDirectory() + "/data/com.feldschmid.subdroid_donate/databases/"
              + RepositoryDbAdapter.DATABASE_NAME,
          Environment.getDataDirectory() + "/data/com.feldschmid.subdroid_donate/databases/" + RevisionDbAdapter.DATABASE_NAME,
          Environment.getDataDirectory() + "/data/com.feldschmid.subdroid_donate/databases/" + ChangeDbAdapter.DATABASE_NAME };

      File exportDir = new File(Environment.getExternalStorageDirectory(), SUBDROID_BACKUP);
      if (!exportDir.exists()) {
        exportDir.mkdirs();
      }

      try {
        for (String dbPath : exportDbFiles) {
          File dbFile = new File(dbPath);
          File exportFile = new File(exportDir, dbFile.getName());
          exportFile.createNewFile();
          FileUtil.copyFile(dbFile, exportFile);
        }
        return null;
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
        return e.getMessage();
      }
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if (dialog.isShowing()) {
        dialog.dismiss();
      }
      if (result == null) {
        Toast.makeText(getContext(), getContext().getText(R.string.backup_complete), Toast.LENGTH_SHORT).show();
      }
      else {
        Toast.makeText(getContext(), getContext().getText(R.string.backup_failed) + " " + result, Toast.LENGTH_LONG)
            .show();
      }
    }

  }

  class ImportDatabaseTask extends AsyncTask<Void, Void, String> {
    private final ProgressDialog dialog = new ProgressDialog(BackupPreference.this.getContext());

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      dialog.setMessage(getContext().getText(R.string.restore_in_progress));
      dialog.show();
    }

    @Override
    protected String doInBackground(final Void... args) {

      String[] importFiles = {
          Environment.getExternalStorageDirectory() + "/" + SUBDROID_BACKUP + "/" + RepositoryDbAdapter.DATABASE_NAME,
          Environment.getExternalStorageDirectory() + "/" + SUBDROID_BACKUP + "/" + RevisionDbAdapter.DATABASE_NAME,
          Environment.getExternalStorageDirectory() + "/" + SUBDROID_BACKUP + "/" + ChangeDbAdapter.DATABASE_NAME };

      try {
        for (String importFile : importFiles) {
          File dbImportFile = new File(importFile);

          if (!dbImportFile.exists()) {
            return getContext().getString(R.string.backup_file_not_found);
          }
          else if (!dbImportFile.canRead()) {
            return getContext().getString(R.string.backup_not_readable);
          }

          File dbFile = new File(Environment.getDataDirectory() + "/data/com.feldschmid.subdroid_donate/databases/"
              + dbImportFile.getName());
          if (dbFile.exists()) {
            dbFile.delete();
          }

          dbFile.createNewFile();
          FileUtil.copyFile(dbImportFile, dbFile);
        }
      } catch (IOException e) {
        Log.e(TAG, e.getMessage(), e);
        return e.getMessage();
      }
      return null;
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      if (dialog.isShowing()) {
        dialog.dismiss();
      }

      if (result == null) {
        Toast.makeText(getContext(), R.string.import_ok, Toast.LENGTH_SHORT).show();
      }
      else {
        Toast.makeText(getContext(), getContext().getString(R.string.import_failed) + " " + result, Toast.LENGTH_SHORT)
            .show();
      }
    }
  }

  private boolean isExternalStorageAvail() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

}
