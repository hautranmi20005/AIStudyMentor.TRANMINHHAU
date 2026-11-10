package com.aistudymentor.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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

  private final EntityInsertionAdapter<CachedQuizResultEntity> __insertionAdapterOfCachedQuizResultEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearQuestions;

  private final SharedSQLiteStatement __preparedStmtOfClearBookmarks;

  private final SharedSQLiteStatement __preparedStmtOfClearQuizResults;

  public OfflineDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCachedQuestionEntity = new EntityInsertionAdapter<CachedQuestionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_questions` (`accountId`,`questionId`,`subject`,`questionSnippet`,`hasImage`,`isBookmarked`,`status`,`createdAt`) VALUES (?,?,?,?,?,?,?,?)";
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
        final int _tmp_1 = entity.isBookmarked() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindString(7, entity.getStatus());
        if (entity.getCreatedAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCreatedAt());
        }
      }
    };
    this.__insertionAdapterOfCachedBookmarkEntity = new EntityInsertionAdapter<CachedBookmarkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_bookmarks` (`accountId`,`questionId`,`subject`,`questionSnippet`,`hasImage`,`status`,`createdAt`) VALUES (?,?,?,?,?,?,?)";
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
        statement.bindString(6, entity.getStatus());
        if (entity.getCreatedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCreatedAt());
        }
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
    this.__preparedStmtOfClearQuizResults = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM cached_quiz_results WHERE accountId = ?";
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
          final int _cursorIndexOfIsBookmarked = CursorUtil.getColumnIndexOrThrow(_cursor, "isBookmarked");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
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
            _item = new CachedQuestionEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpIsBookmarked,_tmpStatus,_tmpCreatedAt);
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
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
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
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpCreatedAt;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmpCreatedAt = null;
            } else {
              _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            }
            _item = new CachedBookmarkEntity(_tmpAccountId,_tmpQuestionId,_tmpSubject,_tmpQuestionSnippet,_tmpHasImage,_tmpStatus,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
