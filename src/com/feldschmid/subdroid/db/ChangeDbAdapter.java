/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.feldschmid.subdroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Simple change database access helper class. Defines the basic CRUD
 * operations and gives the ability to list all changes as well as retrieve
 * or modify a specific set of changes.
 */
public class ChangeDbAdapter {

	public static final String KEY_ROWID = "_id";

	public static final String KEY_FK_REPID = "repid";
	public static final String KEY_FK_REVISIONID = "revisionid";
	public static final String KEY_ACTION = "action";
	public static final String KEY_PATH = "path";

	private static final String TAG = "ChangesDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static final String DATABASE_NAME = "changesdata";
	private static final String DATABASE_TABLE = "changes";
	private static final int DATABASE_VERSION = 2;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table "+DATABASE_TABLE+" (_id integer primary key autoincrement, "
			+ "repid integer, revisionid integer, path text, action text, "
			+ "FOREIGN KEY(repid) REFERENCES repositories(_id), FOREIGN KEY(revisionid) REFERENCES revisions(_id))";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// no changes between version 4 and 10. 0.8.3 -> 0.8.8
			if(oldVersion >= 4 && newVersion <= 10) {
				return;
			}
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS "+DATABASE_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 *
	 * @param ctx
	 *            the Context within which to work
	 */
	public ChangeDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the changes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 *
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public ChangeDbAdapter open() throws SQLException {
		if (mDbHelper == null) {
			mDbHelper = new DatabaseHelper(mCtx);
		}
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Create a new change. If the change is successfully created return the
	 * new rowId, otherwise return -1 to indicate failure.
	 *
	 */
	public long createChange(long repositoryId, int revisionId, String action,
			String path) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_FK_REPID, repositoryId);
		initialValues.put(KEY_FK_REVISIONID, revisionId);
		initialValues.put(KEY_ACTION, action);
		initialValues.put(KEY_PATH, path);

		checkMDbAndReopen();
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the changes for the given revisionId
	 *
	 * @param revisionId
	 *            id of revision to delete rows for
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteChangesForRevision(long revisionId) {
	    checkMDbAndReopen();
		return mDb.delete(DATABASE_TABLE, KEY_FK_REVISIONID + "=" + revisionId, null) > 0;
	}

	/**
	 * Delete the changes for the given repositoryId
	 *
	 * @param repositoryId
	 *            id of repository to delete rows for
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteChangesForRepositoryId(long repositoryId) {
	    checkMDbAndReopen();
		return mDb.delete(DATABASE_TABLE, KEY_FK_REPID + "=" + repositoryId, null) > 0;
	}

	/**
	 * Return a Cursor positioned at the first row of changes that matches the
	 * given revisionId for the given repository
	 *
	 * @param repId id of the current active repository
	 * @param revisionId
	 *            id of revision for which revisions shall be retrieve
	 * @return Cursor positioned to matching first row of changes, if found
	 * @throws SQLException
	 *             if changes for revisionId could not be found/retrieved
	 */
	public Cursor fetchChangesForRevisionId(long repId, long revisionId)
			throws SQLException {
	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_FK_REPID, KEY_FK_REVISIONID, KEY_ACTION,
				KEY_PATH }, KEY_FK_REVISIONID + "=" + revisionId +" AND "+ KEY_FK_REPID + "=" +repId,
				null, null, null, null, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

    public void checkMDbAndReopen() {
      if(mDb == null || !mDb.isOpen()) {
        mDb = mDbHelper.getWritableDatabase();
        Log.e("RevisionDbAdapter", "Had to reopen Db!");
      }
    }
}
