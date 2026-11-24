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
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_questions` (`accountId` TEXT NOT NULL, `questionId` TEXT NOT NULL, `subject` TEXT NOT NULL, `questionSnippet` TEXT NOT NULL, `hasImage` INTEGER NOT NULL, `imageUrl` TEXT, `isBookmarked` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` TEXT, `questionText` TEXT NOT NULL, `answerJson` TEXT, PRIMARY KEY(`accountId`, `questionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_bookmarks` (`accountId` TEXT NOT NULL, `questionId` TEXT NOT NULL, `subject` TEXT NOT NULL, `questionSnippet` TEXT NOT NULL, `hasImage` INTEGER NOT NULL, `imageUrl` TEXT, `status` TEXT NOT NULL, `createdAt` TEXT, `lastViewedAt` TEXT, PRIMARY KEY(`accountId`, `questionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_quiz_results` (`accountId` TEXT NOT NULL, `attemptId` TEXT NOT NULL, `quizId` TEXT NOT NULL, `subject` TEXT NOT NULL, `score` INTEGER NOT NULL, `totalQuestions` INTEGER NOT NULL, `correctCount` INTEGER NOT NULL, `submittedAt` TEXT, `detailsJson` TEXT NOT NULL, PRIMARY KEY(`accountId`, `attemptId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_bookmark_changes` (`accountId` TEXT NOT NULL, `questionId` TEXT NOT NULL, `isBookmarked` INTEGER NOT NULL, PRIMARY KEY(`accountId`, `questionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `offline_sync_state` (`accountId` TEXT NOT NULL, `cursor` TEXT NOT NULL, PRIMARY KEY(`accountId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `cached_dashboard_metrics` (`accountId` TEXT NOT NULL, `range` TEXT NOT NULL, `timezone` TEXT NOT NULL, `metricsJson` TEXT NOT NULL, `cachedAt` TEXT NOT NULL, PRIMARY KEY(`accountId`, `range`, `timezone`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notification_preferences` (`accountId` TEXT NOT NULL, `bookmarkReviewEnabled` INTEGER NOT NULL, `repeatedMistakeEnabled` INTEGER NOT NULL, `weeklySummaryEnabled` INTEGER NOT NULL, `quietHoursStart` TEXT, `quietHoursEnd` TEXT, `timezone` TEXT NOT NULL, `pendingUpload` INTEGER NOT NULL, PRIMARY KEY(`accountId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_notification_deliveries` (`accountId` TEXT NOT NULL, `type` TEXT NOT NULL, `periodKey` TEXT NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`accountId`, `type`, `periodKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '127f1486c6cf58d669fcee27f832ad4e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `cached_questions`");
        db.execSQL("DROP TABLE IF EXISTS `cached_bookmarks`");
        db.execSQL("DROP TABLE IF EXISTS `cached_quiz_results`");
        db.execSQL("DROP TABLE IF EXISTS `pending_bookmark_changes`");
        db.execSQL("DROP TABLE IF EXISTS `offline_sync_state`");
        db.execSQL("DROP TABLE IF EXISTS `cached_dashboard_metrics`");
        db.execSQL("DROP TABLE IF EXISTS `notification_preferences`");
        db.execSQL("DROP TABLE IF EXISTS `local_notification_deliveries`");
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
        final HashMap<String, TableInfo.Column> _columnsCachedQuestions = new HashMap<String, TableInfo.Column>(11);
        _columnsCachedQuestions.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("questionId", new TableInfo.Column("questionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("questionSnippet", new TableInfo.Column("questionSnippet", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("hasImage", new TableInfo.Column("hasImage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("isBookmarked", new TableInfo.Column("isBookmarked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("createdAt", new TableInfo.Column("createdAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("questionText", new TableInfo.Column("questionText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedQuestions.put("answerJson", new TableInfo.Column("answerJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedQuestions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedQuestions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedQuestions = new TableInfo("cached_questions", _columnsCachedQuestions, _foreignKeysCachedQuestions, _indicesCachedQuestions);
        final TableInfo _existingCachedQuestions = TableInfo.read(db, "cached_questions");
        if (!_infoCachedQuestions.equals(_existingCachedQuestions)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_questions(com.aistudymentor.data.local.CachedQuestionEntity).\n"
                  + " Expected:\n" + _infoCachedQuestions + "\n"
                  + " Found:\n" + _existingCachedQuestions);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedBookmarks = new HashMap<String, TableInfo.Column>(9);
        _columnsCachedBookmarks.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("questionId", new TableInfo.Column("questionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("questionSnippet", new TableInfo.Column("questionSnippet", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("hasImage", new TableInfo.Column("hasImage", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("createdAt", new TableInfo.Column("createdAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedBookmarks.put("lastViewedAt", new TableInfo.Column("lastViewedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        final HashMap<String, TableInfo.Column> _columnsPendingBookmarkChanges = new HashMap<String, TableInfo.Column>(3);
        _columnsPendingBookmarkChanges.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingBookmarkChanges.put("questionId", new TableInfo.Column("questionId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPendingBookmarkChanges.put("isBookmarked", new TableInfo.Column("isBookmarked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPendingBookmarkChanges = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPendingBookmarkChanges = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPendingBookmarkChanges = new TableInfo("pending_bookmark_changes", _columnsPendingBookmarkChanges, _foreignKeysPendingBookmarkChanges, _indicesPendingBookmarkChanges);
        final TableInfo _existingPendingBookmarkChanges = TableInfo.read(db, "pending_bookmark_changes");
        if (!_infoPendingBookmarkChanges.equals(_existingPendingBookmarkChanges)) {
          return new RoomOpenHelper.ValidationResult(false, "pending_bookmark_changes(com.aistudymentor.data.local.PendingBookmarkChangeEntity).\n"
                  + " Expected:\n" + _infoPendingBookmarkChanges + "\n"
                  + " Found:\n" + _existingPendingBookmarkChanges);
        }
        final HashMap<String, TableInfo.Column> _columnsOfflineSyncState = new HashMap<String, TableInfo.Column>(2);
        _columnsOfflineSyncState.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOfflineSyncState.put("cursor", new TableInfo.Column("cursor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOfflineSyncState = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOfflineSyncState = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOfflineSyncState = new TableInfo("offline_sync_state", _columnsOfflineSyncState, _foreignKeysOfflineSyncState, _indicesOfflineSyncState);
        final TableInfo _existingOfflineSyncState = TableInfo.read(db, "offline_sync_state");
        if (!_infoOfflineSyncState.equals(_existingOfflineSyncState)) {
          return new RoomOpenHelper.ValidationResult(false, "offline_sync_state(com.aistudymentor.data.local.OfflineSyncStateEntity).\n"
                  + " Expected:\n" + _infoOfflineSyncState + "\n"
                  + " Found:\n" + _existingOfflineSyncState);
        }
        final HashMap<String, TableInfo.Column> _columnsCachedDashboardMetrics = new HashMap<String, TableInfo.Column>(5);
        _columnsCachedDashboardMetrics.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedDashboardMetrics.put("range", new TableInfo.Column("range", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedDashboardMetrics.put("timezone", new TableInfo.Column("timezone", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedDashboardMetrics.put("metricsJson", new TableInfo.Column("metricsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedDashboardMetrics.put("cachedAt", new TableInfo.Column("cachedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCachedDashboardMetrics = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCachedDashboardMetrics = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCachedDashboardMetrics = new TableInfo("cached_dashboard_metrics", _columnsCachedDashboardMetrics, _foreignKeysCachedDashboardMetrics, _indicesCachedDashboardMetrics);
        final TableInfo _existingCachedDashboardMetrics = TableInfo.read(db, "cached_dashboard_metrics");
        if (!_infoCachedDashboardMetrics.equals(_existingCachedDashboardMetrics)) {
          return new RoomOpenHelper.ValidationResult(false, "cached_dashboard_metrics(com.aistudymentor.data.local.CachedDashboardMetricsEntity).\n"
                  + " Expected:\n" + _infoCachedDashboardMetrics + "\n"
                  + " Found:\n" + _existingCachedDashboardMetrics);
        }
        final HashMap<String, TableInfo.Column> _columnsNotificationPreferences = new HashMap<String, TableInfo.Column>(8);
        _columnsNotificationPreferences.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("bookmarkReviewEnabled", new TableInfo.Column("bookmarkReviewEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("repeatedMistakeEnabled", new TableInfo.Column("repeatedMistakeEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("weeklySummaryEnabled", new TableInfo.Column("weeklySummaryEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("quietHoursStart", new TableInfo.Column("quietHoursStart", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("quietHoursEnd", new TableInfo.Column("quietHoursEnd", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("timezone", new TableInfo.Column("timezone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationPreferences.put("pendingUpload", new TableInfo.Column("pendingUpload", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotificationPreferences = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotificationPreferences = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotificationPreferences = new TableInfo("notification_preferences", _columnsNotificationPreferences, _foreignKeysNotificationPreferences, _indicesNotificationPreferences);
        final TableInfo _existingNotificationPreferences = TableInfo.read(db, "notification_preferences");
        if (!_infoNotificationPreferences.equals(_existingNotificationPreferences)) {
          return new RoomOpenHelper.ValidationResult(false, "notification_preferences(com.aistudymentor.data.local.NotificationPreferencesEntity).\n"
                  + " Expected:\n" + _infoNotificationPreferences + "\n"
                  + " Found:\n" + _existingNotificationPreferences);
        }
        final HashMap<String, TableInfo.Column> _columnsLocalNotificationDeliveries = new HashMap<String, TableInfo.Column>(4);
        _columnsLocalNotificationDeliveries.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalNotificationDeliveries.put("type", new TableInfo.Column("type", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalNotificationDeliveries.put("periodKey", new TableInfo.Column("periodKey", "TEXT", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalNotificationDeliveries.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocalNotificationDeliveries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocalNotificationDeliveries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocalNotificationDeliveries = new TableInfo("local_notification_deliveries", _columnsLocalNotificationDeliveries, _foreignKeysLocalNotificationDeliveries, _indicesLocalNotificationDeliveries);
        final TableInfo _existingLocalNotificationDeliveries = TableInfo.read(db, "local_notification_deliveries");
        if (!_infoLocalNotificationDeliveries.equals(_existingLocalNotificationDeliveries)) {
          return new RoomOpenHelper.ValidationResult(false, "local_notification_deliveries(com.aistudymentor.data.local.LocalNotificationDeliveryEntity).\n"
                  + " Expected:\n" + _infoLocalNotificationDeliveries + "\n"
                  + " Found:\n" + _existingLocalNotificationDeliveries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "127f1486c6cf58d669fcee27f832ad4e", "1a3fee72bb5ea6a65280125b900d55d6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "cached_questions","cached_bookmarks","cached_quiz_results","pending_bookmark_changes","offline_sync_state","cached_dashboard_metrics","notification_preferences","local_notification_deliveries");
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
      _db.execSQL("DELETE FROM `pending_bookmark_changes`");
      _db.execSQL("DELETE FROM `offline_sync_state`");
      _db.execSQL("DELETE FROM `cached_dashboard_metrics`");
      _db.execSQL("DELETE FROM `notification_preferences`");
      _db.execSQL("DELETE FROM `local_notification_deliveries`");
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
