package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.widget.Toast;

import java.util.List;

public class TaskRecyclerAdapter extends RecyclerView.Adapter<TaskRecyclerAdapter.TaskItemViewHolder> {
    private final List<TaskItem> taskItemsList;
    private final TaskDatabaseManager databaseManager;
    private final TaskActionHandler actionHandler;

    public interface TaskActionHandler {
        void onTaskEdit(int position);
        void onTaskRemove(int position);
    }

    public TaskRecyclerAdapter(List<TaskItem> taskItemsList, TaskDatabaseManager databaseManager, TaskActionHandler actionHandler) {
        this.taskItemsList = taskItemsList;
        this.databaseManager = databaseManager;
        this.actionHandler = actionHandler;
    }

    @NonNull
    @Override
    public TaskItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskItemViewHolder holder, int position) {
        TaskItem currentTask = taskItemsList.get(position);
        
        holder.bindTaskData(currentTask);
        holder.setupClickListeners(position);
    }

    @Override
    public int getItemCount() {
        return taskItemsList != null ? taskItemsList.size() : 0;
    }

    public void updateTaskList(List<TaskItem> newTaskList) {
        if (newTaskList != null) {
            this.taskItemsList.clear();
            this.taskItemsList.addAll(newTaskList);
            notifyDataSetChanged();
        }
    }
    
    public void addTaskItem(TaskItem taskItem) {
        if (taskItem != null) {
            taskItemsList.add(0, taskItem);
            notifyItemInserted(0);
        }
    }
    
    public void removeTaskItem(int position) {
        if (isValidPosition(position)) {
            taskItemsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
    }
    
    public void updateTaskItem(int position, TaskItem updatedTask) {
        if (isValidPosition(position) && updatedTask != null) {
            taskItemsList.set(position, updatedTask);
            notifyItemChanged(position);
        }
    }
    
    private boolean isValidPosition(int position) {
        return position >= 0 && position < taskItemsList.size();
    }
    
    public List<TaskItem> getCompletedTasks() {
        return taskItemsList.stream()
                .filter(TaskItem::isFinished)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<TaskItem> getPendingTasks() {
        return taskItemsList.stream()
                .filter(task -> !task.isFinished())
                .collect(java.util.stream.Collectors.toList());
    }

    class TaskItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskDescriptionText;
        private final MaterialButton editTaskButton;
        private final MaterialButton removeTaskButton;
        private final MaterialCheckBox taskCompletionCheckbox;

        public TaskItemViewHolder(@NonNull View itemView) {
            super(itemView);
            taskDescriptionText = itemView.findViewById(R.id.task_name);
            editTaskButton = itemView.findViewById(R.id.edit_btn);
            removeTaskButton = itemView.findViewById(R.id.delete_btn);
            taskCompletionCheckbox = itemView.findViewById(R.id.task_checkbox);
        }

        public void bindTaskData(TaskItem taskItem) {
            if (taskItem == null) return;
            
            taskDescriptionText.setText(taskItem.getDescription());
            taskCompletionCheckbox.setChecked(taskItem.isFinished());
            
            // Update timestamp
            TextView timestampView = itemView.findViewById(R.id.task_timestamp);
            if (timestampView != null) {
                timestampView.setText(getRelativeTimeString(taskItem.getCreationTimestamp()));
            }
            
            updateTaskAppearanceBasedOnCompletion(taskItem.isFinished());
            applyAccessibilityFeatures(taskItem);
            setupLongClickListener(taskItem);
        }
        
        private String getRelativeTimeString(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;
            
            if (diff < 60000) { // Less than 1 minute
                return "Just now";
            } else if (diff < 3600000) { // Less than 1 hour
                int minutes = (int) (diff / 60000);
                return minutes + "m ago";
            } else if (diff < 86400000) { // Less than 1 day
                int hours = (int) (diff / 3600000);
                return hours + "h ago";
            } else if (diff < 604800000) { // Less than 1 week
                int days = (int) (diff / 86400000);
                return days + "d ago";
            } else {
                return "Last week";
            }
        }
        
        private void applyAccessibilityFeatures(TaskItem taskItem) {
            String contentDescription = String.format(
                "Task: %s. Status: %s. Created: %s",
                taskItem.getDescription(),
                taskItem.isFinished() ? "Completed" : "Pending",
                taskItem.getFormattedCreationDate()
            );
            itemView.setContentDescription(contentDescription);
        }
        
        private void setupLongClickListener(TaskItem taskItem) {
            itemView.setOnLongClickListener(view -> {
                showTaskDetailsDialog(taskItem);
                return true;
            });
        }
        
        private void showTaskDetailsDialog(TaskItem taskItem) {
            String details = String.format(
                "Task Details:\n\n" +
                "Description: %s\n\n" +
                "Status: %s\n\n" +
                "Created: %s\n\n" +
                "Last Modified: %s\n\n" +
                "Characters: %d",
                taskItem.getDescription(),
                taskItem.isFinished() ? "Completed" : "Pending",
                taskItem.getFormattedCreationDate(),
                new java.util.Date(taskItem.getLastModifiedTimestamp()).toString(),
                taskItem.getDescription().length()
            );
            
            new MaterialAlertDialogBuilder(itemView.getContext())
                    .setTitle("Task Information")
                    .setMessage(details)
                    .setPositiveButton("Close", null)
                    .show();
        }

        public void setupClickListeners(int position) {
            setupCheckboxListener(position);
            setupEditButtonListener(position);
            setupDeleteButtonListener(position);
        }
        
        private void setupCheckboxListener(int position) {
            taskCompletionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    handleTaskCompletionToggle(position, isChecked);
                    provideFeedbackForCompletion(isChecked);
                }
            });
        }
        
        private void setupEditButtonListener(int position) {
            editTaskButton.setOnClickListener(view -> {
                if (actionHandler != null && isValidPosition(position)) {
                    view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                        .withEndAction(() -> {
                            view.animate().scaleX(1f).scaleY(1f).setDuration(100);
                            actionHandler.onTaskEdit(position);
                        });
                }
            });
        }
        
        private void setupDeleteButtonListener(int position) {
            removeTaskButton.setOnClickListener(view -> {
                if (actionHandler != null && isValidPosition(position)) {
                    showDeleteConfirmationDialog(position);
                }
            });
        }
        
        private void provideFeedbackForCompletion(boolean isCompleted) {
            android.content.Context context = itemView.getContext();
            String message = isCompleted ? "Task completed! ✓" : "Task marked as pending";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        
        private void showDeleteConfirmationDialog(int position) {
            android.content.Context context = itemView.getContext();
            TaskItem taskToDelete = taskItemsList.get(position);
            
            String confirmMessage = String.format(
                "Are you sure you want to delete this task?\n\n\"%s\"",
                taskToDelete.getDescription().length() > 50 ? 
                    taskToDelete.getDescription().substring(0, 47) + "..." :
                    taskToDelete.getDescription()
            );
            
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Confirm Deletion")
                    .setMessage(confirmMessage)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (actionHandler != null) {
                            actionHandler.onTaskRemove(position);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

        private void handleTaskCompletionToggle(int position, boolean isCompleted) {
            if (position >= 0 && position < taskItemsList.size()) {
                TaskItem taskToUpdate = taskItemsList.get(position);
                taskToUpdate.setFinished(isCompleted);
                databaseManager.modifyTask(taskToUpdate);
                
                updateTaskAppearanceBasedOnCompletion(isCompleted);
            }
        }

        private void updateTaskAppearanceBasedOnCompletion(boolean isCompleted) {
            animateTaskStateChange(isCompleted);
            updateTextAppearance(isCompleted);
            updateButtonStates(isCompleted);
        }
        
        private void animateTaskStateChange(boolean isCompleted) {
            itemView.animate()
                .alpha(isCompleted ? 0.7f : 1.0f)
                .setDuration(300)
                .start();
        }
        
        private void updateTextAppearance(boolean isCompleted) {
            if (isCompleted) {
                taskDescriptionText.setPaintFlags(
                    taskDescriptionText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskDescriptionText.setTextColor(
                    itemView.getContext().getColor(android.R.color.darker_gray));
            } else {
                taskDescriptionText.setPaintFlags(
                    taskDescriptionText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskDescriptionText.setTextColor(
                    itemView.getContext().getColor(android.R.color.black));
            }
        }
        
        private void updateButtonStates(boolean isCompleted) {
            float buttonAlpha = isCompleted ? 0.5f : 1.0f;
            editTaskButton.setAlpha(buttonAlpha);
            editTaskButton.setEnabled(!isCompleted);
        }
    }
}
