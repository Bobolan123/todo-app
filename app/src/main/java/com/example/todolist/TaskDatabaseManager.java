package com.example.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TaskDatabaseManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "personal_task_manager.db";
    private static final int DATABASE_VERSION = 2;
    private static final String LOG_TAG = "TaskDatabaseManager";

    private static final String TASK_TABLE_NAME = "task_items";
    private static final String COLUMN_TASK_ID = "task_id";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IS_FINISHED = "is_finished";
    private static final String COLUMN_CREATION_TIME = "creation_timestamp";
    private static final String COLUMN_LAST_MODIFIED = "last_modified_timestamp";

    public TaskDatabaseManager(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String createTableQuery = "CREATE TABLE " + TASK_TABLE_NAME + " (" +
                COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                COLUMN_IS_FINISHED + " INTEGER DEFAULT 0, " +
                COLUMN_CREATION_TIME + " INTEGER NOT NULL, " +
                COLUMN_LAST_MODIFIED + " INTEGER NOT NULL)";
        
        try {
            database.execSQL(createTableQuery);
            Log.d(LOG_TAG, "Database table created successfully");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating database table: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        try {
            database.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE_NAME);
            onCreate(database);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error upgrading database: " + e.getMessage());
        }
    }

    public long insertTask(TaskItem taskItem) {
        SQLiteDatabase database = null;
        long insertedRowId = -1;
        
        try {
            database = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            
            contentValues.put(COLUMN_DESCRIPTION, taskItem.getDescription());
            contentValues.put(COLUMN_IS_FINISHED, taskItem.isFinished() ? 1 : 0);
            contentValues.put(COLUMN_CREATION_TIME, taskItem.getCreationTimestamp());
            contentValues.put(COLUMN_LAST_MODIFIED, taskItem.getLastModifiedTimestamp());
            
            insertedRowId = database.insert(TASK_TABLE_NAME, null, contentValues);
            
            if (insertedRowId != -1) {
                taskItem.setId((int) insertedRowId);
                Log.d(LOG_TAG, "Task inserted successfully with ID: " + insertedRowId);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error inserting task: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
        
        return insertedRowId;
    }

    public List<TaskItem> retrieveAllTasks() {
        return retrieveTasksWithFilter(null, null, COLUMN_CREATION_TIME + " DESC");
    }
    
    public List<TaskItem> retrieveCompletedTasks() {
        return retrieveTasksWithFilter(
            COLUMN_IS_FINISHED + "=?", 
            new String[]{"1"}, 
            COLUMN_LAST_MODIFIED + " DESC"
        );
    }
    
    public List<TaskItem> retrievePendingTasks() {
        return retrieveTasksWithFilter(
            COLUMN_IS_FINISHED + "=?", 
            new String[]{"0"}, 
            COLUMN_CREATION_TIME + " DESC"
        );
    }
    
    private List<TaskItem> retrieveTasksWithFilter(String whereClause, String[] whereArgs, String orderBy) {
        List<TaskItem> taskItemsList = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        
        try {
            database = this.getReadableDatabase();
            cursor = database.query(
                TASK_TABLE_NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                orderBy
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TaskItem taskItem = createTaskItemFromCursor(cursor);
                    if (taskItem != null) {
                        taskItemsList.add(taskItem);
                    }
                } while (cursor.moveToNext());
            }
            
            String filterDesc = whereClause != null ? " with filter" : "";
            Log.d(LOG_TAG, "Retrieved " + taskItemsList.size() + " tasks" + filterDesc);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error retrieving filtered tasks: " + e.getMessage());
        } finally {
            closeDatabaseResources(cursor, database);
        }
        
        return taskItemsList;
    }

    public boolean modifyTask(TaskItem taskItem) {
        SQLiteDatabase database = null;
        int affectedRows = 0;
        
        try {
            database = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            
            contentValues.put(COLUMN_DESCRIPTION, taskItem.getDescription());
            contentValues.put(COLUMN_IS_FINISHED, taskItem.isFinished() ? 1 : 0);
            contentValues.put(COLUMN_LAST_MODIFIED, System.currentTimeMillis());
            
            affectedRows = database.update(
                TASK_TABLE_NAME,
                contentValues,
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(taskItem.getId())}
            );
            
            Log.d(LOG_TAG, "Task modification affected " + affectedRows + " rows");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error modifying task: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
        
        return affectedRows > 0;
    }

    public boolean removeTask(int taskId) {
        SQLiteDatabase database = null;
        int deletedRows = 0;
        
        try {
            database = this.getWritableDatabase();
            deletedRows = database.delete(
                TASK_TABLE_NAME,
                COLUMN_TASK_ID + "=?",
                new String[]{String.valueOf(taskId)}
            );
            
            Log.d(LOG_TAG, "Task removal affected " + deletedRows + " rows");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error removing task: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
        
        return deletedRows > 0;
    }

    public int getTaskCount() {
        return executeCountQuery("SELECT COUNT(*) FROM " + TASK_TABLE_NAME);
    }
    
    public int getCompletedTaskCount() {
        return executeCountQuery("SELECT COUNT(*) FROM " + TASK_TABLE_NAME + 
                                " WHERE " + COLUMN_IS_FINISHED + " = 1");
    }
    
    public int getPendingTaskCount() {
        return executeCountQuery("SELECT COUNT(*) FROM " + TASK_TABLE_NAME + 
                                " WHERE " + COLUMN_IS_FINISHED + " = 0");
    }
    
    private int executeCountQuery(String query) {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        int count = 0;
        
        try {
            database = this.getReadableDatabase();
            cursor = database.rawQuery(query, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            Log.d(LOG_TAG, "Count query result: " + count);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error executing count query: " + e.getMessage());
        } finally {
            closeDatabaseResources(cursor, database);
        }
        
        return count;
    }
    
    public List<TaskItem> searchTasksByDescription(String searchTerm) {
        List<TaskItem> searchResults = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return searchResults;
        }
        
        try {
            database = this.getReadableDatabase();
            String searchQuery = "SELECT * FROM " + TASK_TABLE_NAME + 
                               " WHERE " + COLUMN_DESCRIPTION + " LIKE ? " +
                               " ORDER BY " + COLUMN_CREATION_TIME + " DESC";
            
            cursor = database.rawQuery(searchQuery, new String[]{"%" + searchTerm.trim() + "%"});
            
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    TaskItem taskItem = createTaskItemFromCursor(cursor);
                    if (taskItem != null) {
                        searchResults.add(taskItem);
                    }
                } while (cursor.moveToNext());
            }
            
            Log.d(LOG_TAG, "Search found " + searchResults.size() + " tasks for term: " + searchTerm);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error searching tasks: " + e.getMessage());
        } finally {
            closeDatabaseResources(cursor, database);
        }
        
        return searchResults;
    }
    
    public boolean clearAllCompletedTasks() {
        SQLiteDatabase database = null;
        int deletedRows = 0;
        
        try {
            database = this.getWritableDatabase();
            deletedRows = database.delete(
                TASK_TABLE_NAME,
                COLUMN_IS_FINISHED + "=?",
                new String[]{"1"}
            );
            
            Log.d(LOG_TAG, "Cleared " + deletedRows + " completed tasks");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error clearing completed tasks: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
        
        return deletedRows > 0;
    }
    
    private void closeDatabaseResources(Cursor cursor, SQLiteDatabase database) {
        if (cursor != null) {
            cursor.close();
        }
        if (database != null) {
            database.close();
        }
    }

    private TaskItem createTaskItemFromCursor(Cursor cursor) {
        try {
            validateCursorColumns(cursor);
            
            int taskId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TASK_ID));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));
            boolean isFinished = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FINISHED)) == 1;
            long creationTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATION_TIME));
            
            if (description == null || description.trim().isEmpty()) {
                Log.w(LOG_TAG, "Found task with empty description, ID: " + taskId);
                return null;
            }
            
            TaskItem taskItem = new TaskItem(taskId, description.trim(), isFinished, creationTime);
            
            long lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED));
            if (lastModified > 0) {
                taskItem.setCreationTimestamp(creationTime);
            }
            
            return taskItem;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error creating TaskItem from cursor: " + e.getMessage());
            return null;
        }
    }
    
    private void validateCursorColumns(Cursor cursor) throws IllegalStateException {
        String[] requiredColumns = {
            COLUMN_TASK_ID, COLUMN_DESCRIPTION, 
            COLUMN_IS_FINISHED, COLUMN_CREATION_TIME
        };
        
        for (String column : requiredColumns) {
            if (cursor.getColumnIndex(column) == -1) {
                throw new IllegalStateException("Missing required column: " + column);
            }
        }
    }
    
    public void optimizeDatabase() {
        SQLiteDatabase database = null;
        try {
            database = this.getWritableDatabase();
            database.execSQL("VACUUM");
            database.execSQL("ANALYZE");
            Log.d(LOG_TAG, "Database optimization completed");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error optimizing database: " + e.getMessage());
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }
    
    public boolean backupDatabase(String backupPath) {
        try {
            java.io.File currentDbFile = new java.io.File(this.getReadableDatabase().getPath());
            java.io.File backupDbFile = new java.io.File(backupPath);
            
            if (currentDbFile.exists()) {
                java.nio.file.Files.copy(
                    currentDbFile.toPath(), 
                    backupDbFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                Log.d(LOG_TAG, "Database backed up to: " + backupPath);
                return true;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error backing up database: " + e.getMessage());
        }
        return false;
    }
}
