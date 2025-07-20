package com.example.todolist;

import java.util.Date;

public class TaskItem {
    private int taskId;
    private String description;
    private boolean isFinished;
    private long creationTimestamp;
    private long lastModifiedTimestamp;

    public TaskItem(int taskId, String description, boolean isFinished, long creationTimestamp) {
        this.taskId = taskId;
        this.description = description;
        this.isFinished = isFinished;
        this.creationTimestamp = creationTimestamp;
        this.lastModifiedTimestamp = System.currentTimeMillis();
    }

    public TaskItem(String description) {
        this.description = description;
        this.isFinished = false;
        this.creationTimestamp = System.currentTimeMillis();
        this.lastModifiedTimestamp = this.creationTimestamp;
    }

    public int getId() {
        return taskId;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setId(int taskId) {
        this.taskId = taskId;
        updateModificationTime();
    }

    public void setDescription(String description) {
        this.description = description;
        updateModificationTime();
    }

    public void setFinished(boolean finished) {
        this.isFinished = finished;
        updateModificationTime();
    }

    public void setCreationTimestamp(long timestamp) {
        this.creationTimestamp = timestamp;
    }

    private void updateModificationTime() {
        this.lastModifiedTimestamp = System.currentTimeMillis();
    }

    public String getFormattedCreationDate() {
        return new Date(creationTimestamp).toString();
    }

    public boolean hasBeenModified() {
        return lastModifiedTimestamp > creationTimestamp;
    }

    @Override
    public String toString() {
        return "TaskItem{" +
                "id=" + taskId +
                ", description='" + description + '\'' +
                ", finished=" + isFinished +
                ", created=" + getFormattedCreationDate() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TaskItem taskItem = (TaskItem) obj;
        return taskId == taskItem.taskId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(taskId);
    }
}
