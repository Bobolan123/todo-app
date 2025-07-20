package com.example.todolist;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TaskRecyclerAdapter taskRecyclerAdapter;
    private TaskDatabaseManager databaseManager;
    private List<TaskItem> taskItems;
    private RecyclerView tasksRecyclerView;
    private TextInputEditText taskInputField;
    private TextInputLayout taskInputLayout;
    private MaterialButton addTaskButton;
    private FloatingActionButton fabAddTask;
    private MaterialToolbar appToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        setupDatabase();
        configureRecyclerView();
        setupEventListeners();
        loadExistingTasks();
    }

    private void initializeComponents() {
        appToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(appToolbar);
        
        tasksRecyclerView = findViewById(R.id.task_recycler);
        taskInputField = findViewById(R.id.input_task);
        taskInputLayout = findViewById(R.id.input_layout);
        addTaskButton = findViewById(R.id.add_task_btn);
        fabAddTask = findViewById(R.id.fab_add_task);
    }

    private void setupDatabase() {
        databaseManager = new TaskDatabaseManager(this);
        taskItems = new ArrayList<>();
    }

    private void configureRecyclerView() {
        taskRecyclerAdapter = new TaskRecyclerAdapter(taskItems, databaseManager, new TaskRecyclerAdapter.TaskActionHandler() {
            @Override
            public void onTaskEdit(int position) {
                displayTaskEditDialog(position);
            }

            @Override
            public void onTaskRemove(int position) {
                removeTaskAtPosition(position);
            }
        });

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskRecyclerAdapter);
    }

    private void setupEventListeners() {
        addTaskButton.setOnClickListener(view -> processNewTaskInput());
        fabAddTask.setOnClickListener(view -> processNewTaskInput());
        
        appToolbar.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == android.R.id.home) {
                showTaskStatistics();
                return true;
            }
            return false;
        });
        
        taskInputField.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                processNewTaskInput();
                return true;
            }
            return false;
        });
    }

    private void loadExistingTasks() {
        refreshTaskList();
    }

    private void processNewTaskInput() {
        String taskDescription = taskInputField.getText().toString().trim();
        
        if (isValidTaskInput(taskDescription)) {
            if (isDuplicateTask(taskDescription)) {
                showDuplicateTaskDialog(taskDescription);
            } else {
                createNewTask(taskDescription);
                clearInputField();
                refreshTaskList();
                animateTaskAddition();
            }
        } else {
            displayInputValidationDialog();
        }
    }
    
    private boolean isDuplicateTask(String description) {
        return !TaskValidationUtils.isTaskDescriptionUnique(description, taskItems);
    }
    
    private void showDuplicateTaskDialog(String description) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Duplicate Task")
                .setMessage("A task with this description already exists. Would you like to add it anyway?")
                .setPositiveButton("Add Anyway", (dialog, which) -> {
                    createNewTask(description + " (Copy)");
                    clearInputField();
                    refreshTaskList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void animateTaskAddition() {
        if (taskItems.size() > 0) {
            tasksRecyclerView.smoothScrollToPosition(0);
        }
    }

    private boolean isValidTaskInput(String input) {
        TaskValidationUtils.ValidationResult result = 
            TaskValidationUtils.validateTaskDescription(input);
        return result.isValid();
    }
    
    
    private void validateAndShowInputErrors(String input) {
        TaskValidationUtils.ValidationResult result = 
            TaskValidationUtils.validateTaskDescription(input);
        
        if (result.isValid()) {
            taskInputLayout.setError(null);
        } else {
            taskInputLayout.setError(result.getErrorMessage());
        }
    }

    private void createNewTask(String description) {
        TaskItem newTaskItem = new TaskItem(description.trim());
        long insertResult = databaseManager.insertTask(newTaskItem);
        
        if (insertResult != -1) {
            String successMessage = String.format("✓ '%s' added to your tasks", 
                truncateText(description, 30));
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save task. Please try again.", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private void clearInputField() {
        taskInputField.setText("");
        taskInputLayout.setError(null);
    }

    private void refreshTaskList() {
        taskItems.clear();
        taskItems.addAll(databaseManager.retrieveAllTasks());
        taskRecyclerAdapter.notifyDataSetChanged();
        
        updateTaskCountDisplay();
        updateEmptyState();
    }
    
    private void updateTaskCountDisplay() {
        TextView taskCountView = findViewById(R.id.task_count);
        if (taskCountView != null && taskItems != null) {
            int totalTasks = taskItems.size();
            int completedTasks = (int) taskItems.stream().filter(TaskItem::isFinished).count();
            
            String countText = String.format("%d tasks • %d done", totalTasks, completedTasks);
            taskCountView.setText(countText);
        }
    }
    
    private void updateEmptyState() {
        View emptyStateView = findViewById(R.id.empty_state);
        View tasksContainer = findViewById(R.id.tasks_container_card);
        
        if (emptyStateView != null && tasksContainer != null && taskItems != null) {
            if (taskItems.isEmpty()) {
                emptyStateView.setVisibility(View.VISIBLE);
                tasksContainer.setVisibility(View.GONE);
            } else {
                emptyStateView.setVisibility(View.GONE);
                tasksContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void removeTaskAtPosition(int position) {
        if (position >= 0 && position < taskItems.size()) {
            TaskItem taskToRemove = taskItems.get(position);
            databaseManager.removeTask(taskToRemove.getId());
            taskItems.remove(position);
            taskRecyclerAdapter.notifyItemRemoved(position);
            
            Toast.makeText(this, "Task removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayInputValidationDialog() {
        String currentInput = taskInputField.getText().toString();
        validateAndShowInputErrors(currentInput);
        
        String validationMessage = buildValidationMessage(currentInput);
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Task Input Guidelines")
                .setMessage(validationMessage)
                .setPositiveButton("Got it", (dialog, which) -> {
                    taskInputField.requestFocus();
                    taskInputField.selectAll();
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    
    private String buildValidationMessage(String input) {
        StringBuilder message = new StringBuilder("Please ensure your task description:");
        message.append("\n\n• Contains at least 3 characters");
        message.append("\n• Is under 200 characters");
        message.append("\n• Includes letters or numbers");
        message.append("\n• Is not just empty spaces");
        
        if (input != null) {
            message.append("\n\nCurrent length: ").append(input.length()).append(" characters");
        }
        
        return message.toString();
    }

    private void displayTaskEditDialog(int position) {
        if (position < 0 || position >= taskItems.size()) {
            return;
        }
        
        TaskItem currentTask = taskItems.get(position);
        
        final TextInputEditText editInput = new TextInputEditText(this);
        editInput.setText(currentTask.getDescription());
        editInput.setSelection(currentTask.getDescription().length());
        
        final TextInputLayout editLayout = new TextInputLayout(this);
        editLayout.setHint("Task description");
        editLayout.addView(editInput);
        editLayout.setPadding(48, 16, 48, 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Modify Task")
                .setView(editLayout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String updatedDescription = editInput.getText().toString().trim();
                    if (isValidTaskInput(updatedDescription)) {
                        updateTaskAtPosition(position, updatedDescription);
                    } else {
                        Toast.makeText(this, "Invalid task description", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        
        editInput.requestFocus();
    }
    
    private void updateTaskAtPosition(int position, String newDescription) {
        TaskItem taskToUpdate = taskItems.get(position);
        taskToUpdate.setDescription(newDescription);
        databaseManager.modifyTask(taskToUpdate);
        taskRecyclerAdapter.notifyItemChanged(position);
        
        Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        handleKeyboardDismissalOnTouchOutside(motionEvent);
        return super.dispatchTouchEvent(motionEvent);
    }

    private void handleKeyboardDismissalOnTouchOutside(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View focusedView = getCurrentFocus();
            if (shouldDismissKeyboard(focusedView)) {
                dismissSoftKeyboard(focusedView);
            }
        }
    }

    private boolean shouldDismissKeyboard(View focusedView) {
        return focusedView != null && 
               (focusedView instanceof TextInputEditText || 
                focusedView.getId() == R.id.input_task);
    }

    private void dismissSoftKeyboard(View view) {
        InputMethodManager keyboardManager = 
            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        if (keyboardManager != null) {
            keyboardManager.hideSoftInputFromWindow(
                view.getWindowToken(), 
                InputMethodManager.HIDE_NOT_ALWAYS
            );
            view.clearFocus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTaskList();
    }

    @Override
    protected void onDestroy() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        super.onDestroy();
    }

    private void showTaskStatistics() {
        int totalTasks = databaseManager.getTaskCount();
        int completedTasks = (int) taskItems.stream().filter(TaskItem::isFinished).count();
        int pendingTasks = totalTasks - completedTasks;
        
        String statisticsMessage = String.format(
            "Task Statistics:\n\nTotal: %d\nCompleted: %d\nPending: %d",
            totalTasks, completedTasks, pendingTasks
        );
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Task Overview")
                .setMessage(statisticsMessage)
                .setPositiveButton("OK", null)
                .show();
    }
}
