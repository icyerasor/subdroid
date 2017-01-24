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
 * Simple repository database access helper class. Defines the basic CRUD
 * operations and gives the ability to list all repositories as well as retrieve
 * or modify a specific repository.
 */
public class RepositoryDbAdapter {

	public static final String KEY_ROWID = "_id";

	// basic stuff
	public static final String KEY_NAME = "name";
	public static final String KEY_BASEURL = "url";
	public static final String KEY_PATH = "path";
	public static final String KEY_USER = "user";
	public static final String KEY_PASS = "pass";
	public static final String KEY_LIMIT = "revlmt";
	public static final String KEY_IGNORE_SSL = "ignoressl";
	public static final String KEY_RETRIEVE_CHANGED_PATHS = "retrievecp";

	// configuration
	public static final String KEY_INTERVAL = "interval";
	public static final String KEY_NOTIFICATION = "notification";

	// regex
	public static final String KEY_REGEX_AUTHOR = "regexauthor";
	public static final String KEY_REGEX_MESSAGE = "regexmessage";

	// metadata
	public static final String KEY_REVNUMBER = "revnumber";
	public static final String KEY_LASTUSER = "lastuser";
	public static final String KEY_CHANGEDATE = "changedate";
	public static final String KEY_NEWREVISIONS = "newrevisions";

	private static final String TAG = "RepositoryDbAdapter";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	public static final String DATABASE_NAME = "subdroiddata";
	private static final String DATABASE_TABLE = "repositories";
	private static final int DATABASE_VERSION = 3;


	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE = "create table "+DATABASE_TABLE+" (_id integer primary key autoincrement, "
			+ "name text not null, url text not null, path text not null, user text not null, pass text not null, revlmt integer, "
			+ "interval integer, notification text, revnumber text, lastuser text, changedate text, newrevisions text, ignoressl text, "
			+ "retrievecp text, regexauthor text, regexmessage text);";

	private static final String INSERT_DEMO = "insert into "
			+ DATABASE_TABLE
			+ " (name, url, path, user, pass, revlmt, notification, ignoressl, retrievecp, regexauthor, regexmessage) VALUES "
			+ "('Demo', 'http://subdroid.googlecode.com/svn', '/trunk', '', '', '15', 'true', 'false', 'true', '', '');";

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE);
			db.execSQL(INSERT_DEMO);

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// no changes between version 0 and 2. 0.8.3 -> 0.8.8
			if (oldVersion >= 0 && newVersion <= 2) {
				return;
			}
			// added regex in version 3, 0.8.9
			if (oldVersion >= 0 && newVersion <= 3) {
				Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", adding regex columns");
				db.execSQL("alter table " + DATABASE_TABLE +" add column regexauthor text");
				db.execSQL("alter table " + DATABASE_TABLE +" add column regexmessage text");
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
	public RepositoryDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	/**
	 * Open the repository database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 *
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public RepositoryDbAdapter open() throws SQLException {
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
	 * Create a new repository. If the repository is successfully created return
	 * the new rowId, otherwise return -1 to indicate failure.
	 *
	 */
	public long createRepository(String name, String url, String path,
			String user, String pass, String limit, String interval,
			String notification, String ignoreSSL, String retrieveChangedPaths,
			String regexAuthor, String regexMessage) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_BASEURL, url);
		initialValues.put(KEY_PATH, path);
		initialValues.put(KEY_USER, user);
		initialValues.put(KEY_PASS, pass);
		initialValues.put(KEY_LIMIT, limit);
		initialValues.put(KEY_INTERVAL, interval);
		initialValues.put(KEY_NOTIFICATION, notification);
		initialValues.put(KEY_IGNORE_SSL, ignoreSSL);
		initialValues.put(KEY_RETRIEVE_CHANGED_PATHS, retrieveChangedPaths);
		initialValues.put(KEY_REGEX_AUTHOR, regexAuthor);
		initialValues.put(KEY_REGEX_MESSAGE, regexMessage);

		checkMDbAndReopen();
		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	/**
	 * Delete the repository with the given rowId
	 *
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteRepository(long rowId) {
	    checkMDbAndReopen();
		return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all repositories in the database
	 *
	 * @return Cursor over all repositories
	 */
	public Cursor fetchAllRepositories() {

	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID,
				KEY_NAME, KEY_BASEURL, KEY_PATH, KEY_USER, KEY_PASS, KEY_LIMIT, KEY_INTERVAL,
				KEY_NOTIFICATION, KEY_REVNUMBER, KEY_LASTUSER, KEY_CHANGEDATE,
				KEY_NEWREVISIONS }, null, null, null, null, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Return a Cursor positioned at first row of repositories that matches the
	 * given rowId
	 *
	 * @param rowId
	 *            id of repository to retrieve
	 * @return Cursor positioned at the first row of matching repository, if
	 *         found
	 * @throws SQLException
	 *             if repository could not be found/retrieved
	 */
	public Cursor fetchRepository(long rowId) throws SQLException {

	    checkMDbAndReopen();
		Cursor mCursor = mDb.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_NAME, KEY_BASEURL, KEY_PATH, KEY_USER, KEY_PASS, KEY_LIMIT,
				KEY_INTERVAL, KEY_NOTIFICATION, KEY_REVNUMBER, KEY_LASTUSER,
				KEY_CHANGEDATE, KEY_NEWREVISIONS, KEY_IGNORE_SSL,
				KEY_RETRIEVE_CHANGED_PATHS, KEY_REGEX_AUTHOR, KEY_REGEX_MESSAGE }, KEY_ROWID + "=" + rowId, null,
				null, null, null, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Update the repository using the details provided. The repository to be
	 * updated is specified using the rowId, and it is altered to use the title
	 * and body values passed in
	 *
	 * @param rowId
	 *            id of repository to update
	 * @return true if the repository was successfully updated, false otherwise
	 */
	public boolean updateRepository(long rowId, String name, String url,
			String path, String user, String pass, String limit,
			String interval, String notification, String ignoreSSL,
			String retrieveChangedPaths, String regexAuthor, String regexMessage) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_BASEURL, url);
		args.put(KEY_PATH, path);
		args.put(KEY_USER, user);
		args.put(KEY_PASS, pass);
		args.put(KEY_LIMIT, limit);
		args.put(KEY_INTERVAL, interval);
		args.put(KEY_NOTIFICATION, notification);
		args.put(KEY_IGNORE_SSL, ignoreSSL);
		args.put(KEY_RETRIEVE_CHANGED_PATHS, retrieveChangedPaths);
		args.put(KEY_REGEX_AUTHOR, regexAuthor);
		args.put(KEY_REGEX_MESSAGE, regexMessage);

		checkMDbAndReopen();
		return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public void copyRepoInfo(long rowId) {
	    checkMDbAndReopen();
		mDb.execSQL("insert into " + DATABASE_TABLE + " (" + KEY_NAME + ", "
				+ KEY_BASEURL + ", " + KEY_PATH + ", " + KEY_USER + ", "
				+ KEY_PASS + ", " + KEY_LIMIT + ", " + KEY_INTERVAL + ", "
				+ KEY_NOTIFICATION + ", " + KEY_IGNORE_SSL + ", "
				+ KEY_RETRIEVE_CHANGED_PATHS + ", "+ KEY_REGEX_AUTHOR + ", "
				+ KEY_REGEX_MESSAGE + ") select " + KEY_NAME + ", "
				+ KEY_BASEURL + ", " + KEY_PATH + ", " + KEY_USER + ", "
				+ KEY_PASS + ", " + KEY_LIMIT + ", " + KEY_INTERVAL + ", "
				+ KEY_NOTIFICATION + ", " + KEY_IGNORE_SSL + ", "
				+ KEY_RETRIEVE_CHANGED_PATHS + ", " + KEY_REGEX_AUTHOR + ", "
				+ KEY_REGEX_MESSAGE + " from " + DATABASE_TABLE
				+ " where " + KEY_ROWID + " = " + rowId + ";");
	}

    public void checkMDbAndReopen() {
      if(mDb == null || !mDb.isOpen()) {
        mDb = mDbHelper.getWritableDatabase();
        Log.e("RevisionDbAdapter", "Had to reopen Db!");
      }
    }
}
