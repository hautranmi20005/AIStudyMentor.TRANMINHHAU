package com.aistudymentor.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class StudyMentorDatabase_Impl extends StudyMentorDatabase {
  private volatile OfflineDao _offlineDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_questions` (`accountId` TEXT NOT NULL, `questionId` TEXT NOT NULL, `subject` TEXT NOT NULL, `questionSnippet` TEXT NOT NULL, `hasImage` INTEGER NOT NULL, `isBookmarked` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` TEXT, PRIMARY KEY(`accountId`, `questionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_bookmarks` (`accountId` TEXT NOT NULL, `questionId` TEXT NOT NULL, `subject` TEXT NOT NULL, `questionSnippet` TEXT NOT NULL, `hasImage` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` TEXT, PRIMARY KEY(`accountId`, `questionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_quiz_results` (`accountId` TEXT NOT NULL, `attemptId` TEXT NOT NULL, `quizId` TEXT NOT NULL, `subject` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `submittedAt` TEXT, `detailsJson` TEXT NOT NULL, PRIMARY KEY(`accountId`, `attemptId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '138da620d503af21aa9153eaa86819b0')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `cached_questions`");
        db.execSQL("DROP TABLE IF EXISTS `cached_bookmarks`");
        db.execSQL("DROP TABLE IF EXISTS `cached_quiz_results`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCachedQuestions = new HashMap<String, TableInfo.Column>(8);
        _columnsCachedQuestions.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("questionId", new TableInfo.Column("questionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("questionSnippet", new TableInfo.Column("questionSnippet", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("hasImage", new TableInfo.Column("hasImage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("isBookmarked", new TableInfo.Column("isBookmarked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("createdAt", new TableInfo.Column("createdAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedQuestions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedQuestions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedQuestions = new TableInfo("cached_questions", _columnsCachedQuestions, _foreignKeysCachedQuestions, _indicesCachedQuestions);
        final TableInfo _existingCachedQuestions = TableInfo.read(db, "cached_questions");
        if (!_infoCachedQuestions.equals(_existingCachedQuestions)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_questions(com.aistudymentor.data.local.CachedQuestionEntity).\n"
                  + " Expected:\n" + _infoCachedQuestions + "\n"
                  + " Found:\n" + _existingCachedQuestions);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedBookmarks = new HashMap<String, TableInfo.Column>(7);
        _columnsCachedBookmarks.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("questionId", new TableInfo.Column("questionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("questionSnippet", new TableInfo.Column("questionSnippet", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("hasImage", new TableInfo.Column("hasImage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("createdAt", new TableInfo.Column("createdAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedBookmarks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedBookmarks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedBookmarks = new TableInfo("cached_bookmarks", _columnsCachedBookmarks, _foreignKeysCachedBookmarks, _indicesCachedBookmarks);
        final TableInfo _existingCachedBookmarks = TableInfo.read(db, "cached_bookmarks");
        if (!_infoCachedBookmarks.equals(_existingCachedBookmarks)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_bookmarks(com.aistudymentor.data.local.CachedBookmarkEntity).\n"
                  + " Expected:\n" + _infoCachedBookmarks + "\n"
                  + " Found:\n" + _existingCachedBookmarks);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedQuizResults = new HashMap<String, TableInfo.Column>(9);
        _columnsCachedQuizResults.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("attemptId", new TableInfo.Column("attemptId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("quizId", new TableInfo.Column("quizId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("totalQuestions", new TableInfo.Column("totalQuestions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("correctCount", new TableInfo.Column("correctCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("submittedAt", new TableInfo.Column("submittedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuizResults.put("detailsJson", new TableInfo.Column("detailsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedQuizResults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedQuizResults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedQuizResults = new TableInfo("cached_quiz_results", _columnsCachedQuizResults, _foreignKeysCachedQuizResults, _indicesCachedQuizResults);
        final TableInfo _existingCachedQuizResults = TableInfo.read(db, "cached_quiz_results");
        if (!_infoCachedQuizResults.equals(_existingCachedQuizResults)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_quiz_results(com.aistudymentor.data.local.CachedQuizResultEntity).\n"
                  + " Expected:\n" + _infoCachedQuizResults + "\n"
                  + " Found:\n" + _existingCachedQuizResults);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "138da620d503af21aa9153eaa86819b0", "189e789f98cabb58d6fb1f8c0d7bbfbc");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_questions","cached_bookmarks","cached_quiz_results");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `cached_questions`");
      _db.execSQL("DELETE FROM `cached_bookmarks`");
      _db.execSQL("DELETE FROM `cached_quiz_results`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(OfflineDao.class, OfflineDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public OfflineDao offlineDao() {
    if (_offlineDao != null) {
      return _offlineDao;
    } else {
      synchronized(this) {
        if(_offlineDao == null) {
          _offlineDao = new OfflineDao_Impl(this);
        }
        return _offlineDao;
      }
    }
  }
}
