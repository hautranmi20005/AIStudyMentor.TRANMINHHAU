package com.aistudymentor.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class OfflineDao_Impl implements OfflineDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CachedQuestionEntity> __insertionAdapterOfCachedQuestionEntity;

  private final EntityInsertionAdapter<CachedBookmarkEntity> __insertionAdapterOfCachedBookmarkEntity;

  private final EntityInsertionAdapter<PendingBookmarkChangeEntity> __insertionAdapterOfPendingBookmarkChangeEntity;

  private final EntityInsertionAdapter<CachedQuizResultEntity> __insertionAdapterOfCachedQuizResultEntity;

  private final EntityInsertionAdapter<OfflineSyncStateEntity> __insertionAdapterOfOfflineSyncStateEntity;

  private final EntityInsertionAdapter<NotificationPreferencesEntity> __insertionAdapterOfNotificationPreferencesEntity;

  private final EntityInsertionAdapter<LocalNotificationDeliveryEntity> __insertionAdapterOfLocalNotificationDeliveryEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearQuestions;

  private final SharedSQLiteStatement __preparedStmtOfClearBookmarks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBookmark;

  private final SharedSQLiteStatement __preparedStmtOfDeletePendingBookmarkChange;

  private final SharedSQLiteStatement __preparedStmtOfClearQuizResults;

  private final SharedSQLiteStatement __preparedStmtOfMarkNotificationPreferencesSynced;

  private final SharedSQLiteStatement __preparedStmtOfRecordLocalNotification;

  public OfflineDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedQuestionEntity = new EntityInsertionAdapter<CachedQuestionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_questions` (`accountId`,`questionId`,`subject`,`questionSnippet`,`hasImage`,`imageUrl`,`isBookmarked`,`status`,`createdAt`,`questionText`,`answerJson`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedQuestionEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getQuestionId());
        statement.bindString(3, entity.getSubject());
        statement.bindString(4, entity.getQuestionSnippet());
        final int _tmp = entity.getHasImage() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        final int _tmp_1 = entity.isBookmarked() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindString(8, entity.getStatus());
        if (entity.getCreatedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getCreatedAt());
        }
        statement.bindString(10, entity.getQuestionText());
        if (entity.getAnswerJson() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAnswerJson());
        }
      }
    };
    this.__insertionAdapterOfCachedBookmarkEntity = new EntityInsertionAdapter<CachedBookmarkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_bookmarks` (`accountId`,`questionId`,`subject`,`questionSnippet`,`hasImage`,`imageUrl`,`status`,`createdAt`,`lastViewedAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedBookmarkEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getQuestionId());
        statement.bindString(3, entity.getSubject());
        statement.bindString(4, entity.getQuestionSnippet());
        final int _tmp = entity.getHasImage() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getImageUrl() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getImageUrl());
        }
        statement.bindString(7, entity.getStatus());
        if (entity.getCreatedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCreatedAt());
        }
        if (entity.getLastViewedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getLastViewedAt());
        }
      }
    };
    this.__insertionAdapterOfPendingBookmarkChangeEntity = new EntityInsertionAdapter<PendingBookmarkChangeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pending_bookmark_changes` (`accountId`,`questionId`,`isBookmarked`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PendingBookmarkChangeEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getQuestionId());
        final int _tmp = entity.isBookmarked() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__insertionAdapterOfCachedQuizResultEntity = new EntityInsertionAdapter<CachedQuizResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_quiz_results` (`accountId`,`attemptId`,`quizId`,`subject`,`score`,`totalQuestions`,`correctCount`,`submittedAt`,`detailsJson`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CachedQuizResultEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getAttemptId());
        statement.bindString(3, entity.getQuizId());
        statement.bindString(4, entity.getSubject());
        statement.bindLong(5, entity.getScore());
        statement.bindLong(6, entity.getTotalQuestions());
        statement.bindLong(7, entity.getCorrectCount());
        if (entity.getSubmittedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSubmittedAt());
        }
        statement.bindString(9, entity.getDetailsJson());
      }
    };
    this.__insertionAdapterOfOfflineSyncStateEntity = new EntityInsertionAdapter<OfflineSyncStateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `offline_sync_state` (`accountId`,`cursor`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OfflineSyncStateEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getCursor());
      }
    };
    this.__insertionAdapterOfNotificationPreferencesEntity = new EntityInsertionAdapter<NotificationPreferencesEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notification_preferences` (`accountId`,`bookmarkReviewEnabled`,`repeatedMistakeEnabled`,`weeklySummaryEnabled`,`quietHoursStart`,`quietHoursEnd`,`timezone`,`pendingUpload`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotificationPreferencesEntity entity) {
        statement.bindString(1, entity.getAccountId());
        final int _tmp = entity.getBookmarkReviewEnabled() ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.getRepeatedMistakeEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        final int _tmp_2 = entity.getWeeklySummaryEnabled() ? 1 : 0;
        statement.bindLong(4, _tmp_2);
        if (entity.getQuietHoursStart() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getQuietHoursStart());
        }
        if (entity.getQuietHoursEnd() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getQuietHoursEnd());
        }
        statement.bindString(7, entity.getTimezone());
        final int _tmp_3 = entity.getPendingUpload() ? 1 : 0;
        statement.bindLong(8, _tmp_3);
      }
    };
    this.__insertionAdapterOfLocalNotificationDeliveryEntity = new EntityInsertionAdapter<LocalNotificationDeliveryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `local_notification_deliveries` (`accountId`,`type`,`periodKey`,`createdAt`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalNotificationDeliveryEntity entity) {
        statement.bindString(1, entity.getAccountId());
        statement.bindString(2, entity.getType());
        statement.bindString(3, entity.getPeriodKey());
        statement.bindString(4, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfClearQuestions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_questions WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearBookmarks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_bookmarks WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteBookmark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_bookmarks WHERE accountId = ? AND questionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePendingBookmarkChange = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM pending_bookmark_changes WHERE accountId = ? AND questionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearQuizResults = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_quiz_results WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkNotificationPreferencesSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notification_preferences SET pendingUpload = 0 WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRecordLocalNotification = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "INSERT OR IGNORE INTO local_notification_deliveries(accountId, type, periodKey, createdAt) VALUES(?, ?, ?, ?)";
        return _query;
      }
    };
  }

  @Override
  public Object insertQuestions(final List<CachedQuestionEntity> questions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedQuestionEntity.insert(questions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertQuestion(final CachedQuestionEntity question,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedQuestionEntity.insert(question);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBookmarks(final List<CachedBookmarkEntity> bookmarks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedBookmarkEntity.insert(bookmarks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object savePendingBookmarkChange(final PendingBookmarkChangeEntity change,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPendingBookmarkChangeEntity.insert(change);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertQuizResults(final List<CachedQuizResultEntity> results,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedQuizResultEntity.insert(results);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertQuizResult(final CachedQuizResultEntity result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCachedQuizResultEntity.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveSyncState(final OfflineSyncStateEntity state,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOfflineSyncStateEntity.insert(state);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveNotificationPreferences(final NotificationPreferencesEntity preferences,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNotificationPreferencesEntity.insert(preferences);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertLocalNotificationDelivery(final LocalNotificationDeliveryEntity delivery,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalNotificationDeliveryEntity.insert(delivery);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearQuestions(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearQuestions.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearQuestions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearBookmarks(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearBookmarks.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearBookmarks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBookmark(final String accountId, final String questionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBookmark.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, questionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteBookmark.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePendingBookmarkChange(final String accountId, final String questionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePendingBookmarkChange.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, questionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePendingBookmarkChange.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearQuizResults(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearQuizResults.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearQuizResults.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markNotificationPreferencesSynced(final String accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkNotificationPreferencesSynced.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkNotificationPreferencesSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object recordLocalNotification(final String accountId, final String type,
      final String periodKey, final String createdAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRecordLocalNotification.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, type);
        _argIndex = 3;
        _stmt.bindString(_argIndex, periodKey);
        _argIndex = 4;
        _stmt.bindString(_argIndex, createdAt);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeInsert();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRecordLocalNotification.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getQuestion(final String accountId, final String questionId,
      final Continuation<? super CachedQuestionEntity> $completion) {
    final String _sql = "SELECT * FROM cached_questions WHERE accountId = ? AND questionId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, questionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CachedQuestionEntity>() {
      @Override
      @Nullable
      public CachedQuestionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfQuestionSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "questionSnippet");
          final int _cursorIndexOfHasImage = CursorUtil.getColumnIndexOrThrow(_cursor, "hasImage");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfIsBookmarked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBookmarked");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfQuestionText = CursorUtil.getColumnIndexOrThrow(_cursor, "questionText");
          final int _cursorIndexOfAnswerJson = CursorUtil.getColumnIndexOrThrow(_cursor, "answerJson");
          final CachedQuestionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpQuestionSnippet;
            _tmpQuestionSnippet = _cursor.getString(_cursorIndexOfQuestionSnippet);
            final boolean _tmpHasImage;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImage);
            _tmpHasImage = _tmp != 0;
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final boolean _tmpIsBookmarked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBookmarked);
            _tmpIsBookmarked = _tmp_1 != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            final String _tmpQuestionText;
            _tmpQuestionText = _cursor.getString(_cursorIndexOfQuestionText);
            final String _tmpAnswerJson;
            if (_cursor.isNull(_cursorIndexOfAnswerJson)) {
              _tmpAnswerJson = null;
            } else {
              _tmpAnswerJson = _cursor.getString(_cursorIndexOfAnswerJson);
            }
            _result = new CachedQuestionEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpImageUrl,_tmpIsBookmarked,_tmpStatus,_tmpCreatedAt,_tmpQuestionText,_tmpAnswerJson);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CachedQuestionEntity>> observeQuestions(final String accountId) {
    final String _sql = "SELECT * FROM cached_questions WHERE accountId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_questions"}, new Callable<List<CachedQuestionEntity>>() {
      @Override
      @NonNull
      public List<CachedQuestionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfQuestionSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "questionSnippet");
          final int _cursorIndexOfHasImage = CursorUtil.getColumnIndexOrThrow(_cursor, "hasImage");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfIsBookmarked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBookmarked");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfQuestionText = CursorUtil.getColumnIndexOrThrow(_cursor, "questionText");
          final int _cursorIndexOfAnswerJson = CursorUtil.getColumnIndexOrThrow(_cursor, "answerJson");
          final List<CachedQuestionEntity> _result = new ArrayList<CachedQuestionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedQuestionEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpQuestionSnippet;
            _tmpQuestionSnippet = _cursor.getString(_cursorIndexOfQuestionSnippet);
            final boolean _tmpHasImage;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImage);
            _tmpHasImage = _tmp != 0;
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final boolean _tmpIsBookmarked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBookmarked);
            _tmpIsBookmarked = _tmp_1 != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            final String _tmpQuestionText;
            _tmpQuestionText = _cursor.getString(_cursorIndexOfQuestionText);
            final String _tmpAnswerJson;
            if (_cursor.isNull(_cursorIndexOfAnswerJson)) {
              _tmpAnswerJson = null;
            } else {
              _tmpAnswerJson = _cursor.getString(_cursorIndexOfAnswerJson);
            }
            _item = new CachedQuestionEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpImageUrl,_tmpIsBookmarked,_tmpStatus,_tmpCreatedAt,_tmpQuestionText,_tmpAnswerJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CachedBookmarkEntity>> observeBookmarks(final String accountId) {
    final String _sql = "SELECT * FROM cached_bookmarks WHERE accountId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_bookmarks"}, new Callable<List<CachedBookmarkEntity>>() {
      @Override
      @NonNull
      public List<CachedBookmarkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfQuestionSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "questionSnippet");
          final int _cursorIndexOfHasImage = CursorUtil.getColumnIndexOrThrow(_cursor, "hasImage");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastViewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastViewedAt");
          final List<CachedBookmarkEntity> _result = new ArrayList<CachedBookmarkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedBookmarkEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpQuestionSnippet;
            _tmpQuestionSnippet = _cursor.getString(_cursorIndexOfQuestionSnippet);
            final boolean _tmpHasImage;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImage);
            _tmpHasImage = _tmp != 0;
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            final String _tmpLastViewedAt;
            if (_cursor.isNull(_cursorIndexOfLastViewedAt)) {
              _tmpLastViewedAt = null;
            } else {
              _tmpLastViewedAt = _cursor.getString(_cursorIndexOfLastViewedAt);
            }
            _item = new CachedBookmarkEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpImageUrl,_tmpStatus,_tmpCreatedAt,_tmpLastViewedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object bookmarkLastViewedAt(final String accountId, final String questionId,
      final Continuation<? super String> $completion) {
    final String _sql = "SELECT lastViewedAt FROM cached_bookmarks WHERE accountId = ? AND questionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, questionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> observePendingBookmarkCount(final String accountId) {
    final String _sql = "SELECT COUNT(*) FROM pending_bookmark_changes WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pending_bookmark_changes"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object pendingBookmarkCount(final String accountId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM pending_bookmark_changes WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object pendingBookmarkChanges(final String accountId,
      final Continuation<? super List<PendingBookmarkChangeEntity>> $completion) {
    final String _sql = "SELECT * FROM pending_bookmark_changes WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PendingBookmarkChangeEntity>>() {
      @Override
      @NonNull
      public List<PendingBookmarkChangeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfIsBookmarked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBookmarked");
          final List<PendingBookmarkChangeEntity> _result = new ArrayList<PendingBookmarkChangeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PendingBookmarkChangeEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final boolean _tmpIsBookmarked;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBookmarked);
            _tmpIsBookmarked = _tmp != 0;
            _item = new PendingBookmarkChangeEntity(_tmpAccountId,_tmpQuestionId,_tmpIsBookmarked);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CachedQuizResultEntity>> observeQuizResults(final String accountId) {
    final String _sql = "SELECT * FROM cached_quiz_results WHERE accountId = ? ORDER BY submittedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"cached_quiz_results"}, new Callable<List<CachedQuizResultEntity>>() {
      @Override
      @NonNull
      public List<CachedQuizResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfAttemptId = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptId");
          final int _cursorIndexOfQuizId = CursorUtil.getColumnIndexOrThrow(_cursor, "quizId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfTotalQuestions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestions");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfSubmittedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "submittedAt");
          final int _cursorIndexOfDetailsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "detailsJson");
          final List<CachedQuizResultEntity> _result = new ArrayList<CachedQuizResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedQuizResultEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpAttemptId;
            _tmpAttemptId = _cursor.getString(_cursorIndexOfAttemptId);
            final String _tmpQuizId;
            _tmpQuizId = _cursor.getString(_cursorIndexOfQuizId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final int _tmpTotalQuestions;
            _tmpTotalQuestions = _cursor.getInt(_cursorIndexOfTotalQuestions);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final String _tmpSubmittedAt;
            if (_cursor.isNull(_cursorIndexOfSubmittedAt)) {
              _tmpSubmittedAt = null;
            } else {
              _tmpSubmittedAt = _cursor.getString(_cursorIndexOfSubmittedAt);
            }
            final String _tmpDetailsJson;
            _tmpDetailsJson = _cursor.getString(_cursorIndexOfDetailsJson);
            _item = new CachedQuizResultEntity(_tmpAccountId,_tmpAttemptId,_tmpQuizId,_tmpSubject,_tmpScore,_tmpTotalQuestions,_tmpCorrectCount,_tmpSubmittedAt,_tmpDetailsJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object syncCursor(final String accountId, final Continuation<? super String> $completion) {
    final String _sql = "SELECT cursor FROM offline_sync_state WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getNotificationPreferences(final String accountId,
      final Continuation<? super NotificationPreferencesEntity> $completion) {
    final String _sql = "SELECT * FROM notification_preferences WHERE accountId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NotificationPreferencesEntity>() {
      @Override
      @Nullable
      public NotificationPreferencesEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfBookmarkReviewEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "bookmarkReviewEnabled");
          final int _cursorIndexOfRepeatedMistakeEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatedMistakeEnabled");
          final int _cursorIndexOfWeeklySummaryEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklySummaryEnabled");
          final int _cursorIndexOfQuietHoursStart = CursorUtil.getColumnIndexOrThrow(_cursor, "quietHoursStart");
          final int _cursorIndexOfQuietHoursEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "quietHoursEnd");
          final int _cursorIndexOfTimezone = CursorUtil.getColumnIndexOrThrow(_cursor, "timezone");
          final int _cursorIndexOfPendingUpload = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingUpload");
          final NotificationPreferencesEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final boolean _tmpBookmarkReviewEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBookmarkReviewEnabled);
            _tmpBookmarkReviewEnabled = _tmp != 0;
            final boolean _tmpRepeatedMistakeEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfRepeatedMistakeEnabled);
            _tmpRepeatedMistakeEnabled = _tmp_1 != 0;
            final boolean _tmpWeeklySummaryEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfWeeklySummaryEnabled);
            _tmpWeeklySummaryEnabled = _tmp_2 != 0;
            final String _tmpQuietHoursStart;
            if (_cursor.isNull(_cursorIndexOfQuietHoursStart)) {
              _tmpQuietHoursStart = null;
            } else {
              _tmpQuietHoursStart = _cursor.getString(_cursorIndexOfQuietHoursStart);
            }
            final String _tmpQuietHoursEnd;
            if (_cursor.isNull(_cursorIndexOfQuietHoursEnd)) {
              _tmpQuietHoursEnd = null;
            } else {
              _tmpQuietHoursEnd = _cursor.getString(_cursorIndexOfQuietHoursEnd);
            }
            final String _tmpTimezone;
            _tmpTimezone = _cursor.getString(_cursorIndexOfTimezone);
            final boolean _tmpPendingUpload;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfPendingUpload);
            _tmpPendingUpload = _tmp_3 != 0;
            _result = new NotificationPreferencesEntity(_tmpAccountId,_tmpBookmarkReviewEnabled,_tmpRepeatedMistakeEnabled,_tmpWeeklySummaryEnabled,_tmpQuietHoursStart,_tmpQuietHoursEnd,_tmpTimezone,_tmpPendingUpload);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<NotificationPreferencesEntity> observeNotificationPreferences(
      final String accountId) {
    final String _sql = "SELECT * FROM notification_preferences WHERE accountId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notification_preferences"}, new Callable<NotificationPreferencesEntity>() {
      @Override
      @Nullable
      public NotificationPreferencesEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfBookmarkReviewEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "bookmarkReviewEnabled");
          final int _cursorIndexOfRepeatedMistakeEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatedMistakeEnabled");
          final int _cursorIndexOfWeeklySummaryEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "weeklySummaryEnabled");
          final int _cursorIndexOfQuietHoursStart = CursorUtil.getColumnIndexOrThrow(_cursor, "quietHoursStart");
          final int _cursorIndexOfQuietHoursEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "quietHoursEnd");
          final int _cursorIndexOfTimezone = CursorUtil.getColumnIndexOrThrow(_cursor, "timezone");
          final int _cursorIndexOfPendingUpload = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingUpload");
          final NotificationPreferencesEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final boolean _tmpBookmarkReviewEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfBookmarkReviewEnabled);
            _tmpBookmarkReviewEnabled = _tmp != 0;
            final boolean _tmpRepeatedMistakeEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfRepeatedMistakeEnabled);
            _tmpRepeatedMistakeEnabled = _tmp_1 != 0;
            final boolean _tmpWeeklySummaryEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfWeeklySummaryEnabled);
            _tmpWeeklySummaryEnabled = _tmp_2 != 0;
            final String _tmpQuietHoursStart;
            if (_cursor.isNull(_cursorIndexOfQuietHoursStart)) {
              _tmpQuietHoursStart = null;
            } else {
              _tmpQuietHoursStart = _cursor.getString(_cursorIndexOfQuietHoursStart);
            }
            final String _tmpQuietHoursEnd;
            if (_cursor.isNull(_cursorIndexOfQuietHoursEnd)) {
              _tmpQuietHoursEnd = null;
            } else {
              _tmpQuietHoursEnd = _cursor.getString(_cursorIndexOfQuietHoursEnd);
            }
            final String _tmpTimezone;
            _tmpTimezone = _cursor.getString(_cursorIndexOfTimezone);
            final boolean _tmpPendingUpload;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfPendingUpload);
            _tmpPendingUpload = _tmp_3 != 0;
            _result = new NotificationPreferencesEntity(_tmpAccountId,_tmpBookmarkReviewEnabled,_tmpRepeatedMistakeEnabled,_tmpWeeklySummaryEnabled,_tmpQuietHoursStart,_tmpQuietHoursEnd,_tmpTimezone,_tmpPendingUpload);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object notificationBookmarks(final String accountId,
      final Continuation<? super List<CachedBookmarkEntity>> $completion) {
    final String _sql = "SELECT * FROM cached_bookmarks WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedBookmarkEntity>>() {
      @Override
      @NonNull
      public List<CachedBookmarkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfQuestionSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "questionSnippet");
          final int _cursorIndexOfHasImage = CursorUtil.getColumnIndexOrThrow(_cursor, "hasImage");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastViewedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastViewedAt");
          final List<CachedBookmarkEntity> _result = new ArrayList<CachedBookmarkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedBookmarkEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpQuestionSnippet;
            _tmpQuestionSnippet = _cursor.getString(_cursorIndexOfQuestionSnippet);
            final boolean _tmpHasImage;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImage);
            _tmpHasImage = _tmp != 0;
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            final String _tmpLastViewedAt;
            if (_cursor.isNull(_cursorIndexOfLastViewedAt)) {
              _tmpLastViewedAt = null;
            } else {
              _tmpLastViewedAt = _cursor.getString(_cursorIndexOfLastViewedAt);
            }
            _item = new CachedBookmarkEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpImageUrl,_tmpStatus,_tmpCreatedAt,_tmpLastViewedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object notificationQuestions(final String accountId,
      final Continuation<? super List<CachedQuestionEntity>> $completion) {
    final String _sql = "SELECT * FROM cached_questions WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedQuestionEntity>>() {
      @Override
      @NonNull
      public List<CachedQuestionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfQuestionId = CursorUtil.getColumnIndexOrThrow(_cursor, "questionId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfQuestionSnippet = CursorUtil.getColumnIndexOrThrow(_cursor, "questionSnippet");
          final int _cursorIndexOfHasImage = CursorUtil.getColumnIndexOrThrow(_cursor, "hasImage");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfIsBookmarked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBookmarked");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfQuestionText = CursorUtil.getColumnIndexOrThrow(_cursor, "questionText");
          final int _cursorIndexOfAnswerJson = CursorUtil.getColumnIndexOrThrow(_cursor, "answerJson");
          final List<CachedQuestionEntity> _result = new ArrayList<CachedQuestionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedQuestionEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpQuestionId;
            _tmpQuestionId = _cursor.getString(_cursorIndexOfQuestionId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpQuestionSnippet;
            _tmpQuestionSnippet = _cursor.getString(_cursorIndexOfQuestionSnippet);
            final boolean _tmpHasImage;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasImage);
            _tmpHasImage = _tmp != 0;
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final boolean _tmpIsBookmarked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsBookmarked);
            _tmpIsBookmarked = _tmp_1 != 0;
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            final String _tmpQuestionText;
            _tmpQuestionText = _cursor.getString(_cursorIndexOfQuestionText);
            final String _tmpAnswerJson;
            if (_cursor.isNull(_cursorIndexOfAnswerJson)) {
              _tmpAnswerJson = null;
            } else {
              _tmpAnswerJson = _cursor.getString(_cursorIndexOfAnswerJson);
            }
            _item = new CachedQuestionEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpImageUrl,_tmpIsBookmarked,_tmpStatus,_tmpCreatedAt,_tmpQuestionText,_tmpAnswerJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object notificationQuizResults(final String accountId,
      final Continuation<? super List<CachedQuizResultEntity>> $completion) {
    final String _sql = "SELECT * FROM cached_quiz_results WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CachedQuizResultEntity>>() {
      @Override
      @NonNull
      public List<CachedQuizResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfAttemptId = CursorUtil.getColumnIndexOrThrow(_cursor, "attemptId");
          final int _cursorIndexOfQuizId = CursorUtil.getColumnIndexOrThrow(_cursor, "quizId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfTotalQuestions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestions");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfSubmittedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "submittedAt");
          final int _cursorIndexOfDetailsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "detailsJson");
          final List<CachedQuizResultEntity> _result = new ArrayList<CachedQuizResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CachedQuizResultEntity _item;
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpAttemptId;
            _tmpAttemptId = _cursor.getString(_cursorIndexOfAttemptId);
            final String _tmpQuizId;
            _tmpQuizId = _cursor.getString(_cursorIndexOfQuizId);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final int _tmpTotalQuestions;
            _tmpTotalQuestions = _cursor.getInt(_cursorIndexOfTotalQuestions);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final String _tmpSubmittedAt;
            if (_cursor.isNull(_cursorIndexOfSubmittedAt)) {
              _tmpSubmittedAt = null;
            } else {
              _tmpSubmittedAt = _cursor.getString(_cursorIndexOfSubmittedAt);
            }
            final String _tmpDetailsJson;
            _tmpDetailsJson = _cursor.getString(_cursorIndexOfDetailsJson);
            _item = new CachedQuizResultEntity(_tmpAccountId,_tmpAttemptId,_tmpQuizId,_tmpSubject,_tmpScore,_tmpTotalQuestions,_tmpCorrectCount,_tmpSubmittedAt,_tmpDetailsJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object hasLocalNotificationBeenSent(final String accountId, final String type,
      final String periodKey, final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM local_notification_deliveries WHERE accountId = ? AND type = ? AND periodKey = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, type);
    _argIndex = 3;
    _statement.bindString(_argIndex, periodKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteQuestions(final String accountId, final List<String> questionIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM cached_questions WHERE accountId = ");
        _stringBuilder.append("?");
        _stringBuilder.append(" AND questionId IN (");
        final int _inputSize = questionIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        for (String _item : questionIds) {
          _stmt.bindString(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBookmarks(final String accountId, final List<String> questionIds,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM cached_bookmarks WHERE accountId = ");
        _stringBuilder.append("?");
        _stringBuilder.append(" AND questionId IN (");
        final int _inputSize = questionIds.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        _stmt.bindString(_argIndex, accountId);
        _argIndex = 2;
        for (String _item : questionIds) {
          _stmt.bindString(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
