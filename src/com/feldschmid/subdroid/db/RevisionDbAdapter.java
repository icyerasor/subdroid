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
 * Simple revision database access helper class. Defines the basic CRUD
 * operations and gives the ability to list all revisions as well as retrieve
 * or modify a specific revision.
 */
public class RevisionDbAdapter {

	public static final String KEY_ROWID = "_id";

	public static final String KEY_FK_REPID = "repid";
	public static final String KEY_REVISION = "revision";
	public static final String KEY_ACTION = "action";
	public static final String KEY_AUTHOR = "author";
	public static final String KEY_DATE = "revdate";
	public static final String KEY_MESSAGE = "message";

	private static final String TAG = "RevisionDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static final String DATABASE_NAME = "revisionsdata";
	private static final String DATABASE_TABLE = "revisions";
	private static final int DATABASE_VERSION = 2;

	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table "+DATABASE_TABLE+" (_id integer primary key autoincrement, "
			+ "repid integer, revision integer, action text, author text, revdate text, message text, "
			+ "FOREIGN KEY(repid) REFERENCES repositories(_id))";

	private static final String LIKE = " LIKE ";

	private static final String OR = " OR ";

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
	public RevisionDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the revision database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 *
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public RevisionDbAdapter open() throws SQLException {
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
	 * Create a new revision. If the revision is successfully created return the
	 * new rowId, otherwise return -1 to indicate failure.
	 *
	 */
	public long createRevision(long repositoryId, int revision, String actions,
			String author, String date, String message) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_FK_REPID, repositoryId);
		initialValues.put(KEY_REVISION, revision);
		initialValues.put(KEY_ACTION, actions);
		initialValues.put(KEY_AUTHOR, author);
		initialValues.put(KEY_DATE, date);
		initialValues.put(KEY_MESSAGE, message);

		checkMDbAndReopen();
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the revision with the given rowId
	 *
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteRevision(long rowId) {
	    checkMDbAndReopen();
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Delete all revisions for the given RepositoryId
	 *
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteRevisionForRepositoryId(long repositoryId) {
	    checkMDbAndReopen();
		return mDb.delete(DATABASE_TABLE, KEY_FK_REPID + "=" + repositoryId,
				null) > 0;
	}

	/**
	 * Return a Cursor positioned at the first row of revisions that match the
	 * given repositoryId
	 *
	 * @param repositoryId
	 *            id of repository for which revisions shall be retrieved
	 * @return Cursor positioned at the first row of matching revisions, if
	 *         found
	 * @throws SQLException
	 *             if repository could not be found/retrieved
	 */
	public Cursor fetchRevisionsForRepositoryId(long repositoryId)
			throws SQLException {

	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_FK_REPID, KEY_REVISION, KEY_ACTION, KEY_AUTHOR,
				KEY_DATE, KEY_MESSAGE }, KEY_FK_REPID + "=" + repositoryId,
				null, null, null, KEY_REVISION + " DESC", null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * @param repositoryId
	 * @return the lowest revision currently in the DB, or null if the DB is empty
	 */
	public Integer getLowestRevisionForRepositoryId(long repositoryId) {
	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_FK_REPID, KEY_REVISION }, KEY_FK_REPID + "=" + repositoryId,
				null, null, null, KEY_REVISION +" ASC", "1");

		if (mCursor != null && mCursor.getCount() != 0) {
			mCursor.moveToFirst();
			String rev = mCursor.getString(1);
			mCursor.close();
			return Integer.valueOf(rev);
		}
		mCursor.close();
		return null;
	}

	/**
	 * @param repositoryId
	 * @return the lowest revision currently in the DB, or null if the DB is empty
	 */
	public Integer getHighestRevisionForRepositoryId(long repositoryId) {
	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_FK_REPID, KEY_REVISION }, KEY_FK_REPID + "=" + repositoryId,
				null, null, null, KEY_REVISION +" DESC", "1");

		if (mCursor != null && mCursor.getCount() != 0) {
			mCursor.moveToFirst();
			String rev = mCursor.getString(1);
			mCursor.close();
			return Integer.valueOf(rev);
		}
		mCursor.close();
		return null;
	}

	public long getRevisionByRowId(long id) {
	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE,
				new String[] { KEY_REVISION }, KEY_ROWID + "=" + id, null,
				null, null, null, null);

		if (mCursor != null && mCursor.getCount() != 0) {
			mCursor.moveToFirst();
			Integer rev = mCursor.getInt(0);
			mCursor.close();
			return rev;
		}
		mCursor.close();
		return -1;
	}

    public Cursor search(String q) {
      checkMDbAndReopen();
      q = "'%"+q+"%'";
      Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_FK_REPID, KEY_REVISION, KEY_ACTION,
          KEY_AUTHOR, KEY_DATE, KEY_MESSAGE }, KEY_REVISION + LIKE + q + OR + KEY_AUTHOR + LIKE + q + OR + KEY_MESSAGE
          + LIKE + q, null, null, null, KEY_REVISION + " DESC", null);

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
