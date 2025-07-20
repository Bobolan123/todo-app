package com.example.todolist;

import java.util.regex.Pattern;

public class TaskValidationUtils {
    private static final int MIN_TASK_LENGTH = 3;
    private static final int MAX_TASK_LENGTH = 200;
    private static final Pattern SPECIAL_CHARS_ONLY = Pattern.compile("^[^a-zA-Z0-9\\s]+$");
    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("\\s{3,}");
    private static final Pattern PROFANITY_CHECK = Pattern.compile("(?i).*(spam|test123|dummy).*");

    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static ValidationResult validateTaskDescription(String description) {
        if (description == null) {
            return new ValidationResult(false, "Task description cannot be null");
        }

        String trimmedDescription = description.trim();

        if (trimmedDescription.isEmpty()) {
            return new ValidationResult(false, "Task description cannot be empty");
        }

        if (trimmedDescription.length() < MIN_TASK_LENGTH) {
            return new ValidationResult(false, 
                String.format("Task must be at least %d characters long", MIN_TASK_LENGTH));
        }

        if (trimmedDescription.length() > MAX_TASK_LENGTH) {
            return new ValidationResult(false, 
                String.format("Task description too long (max %d characters)", MAX_TASK_LENGTH));
        }

        if (SPECIAL_CHARS_ONLY.matcher(trimmedDescription).matches()) {
            return new ValidationResult(false, "Task must contain letters or numbers");
        }

        if (EXCESSIVE_WHITESPACE.matcher(trimmedDescription).find()) {
            return new ValidationResult(false, "Task contains excessive whitespace");
        }

        if (PROFANITY_CHECK.matcher(trimmedDescription).matches()) {
            return new ValidationResult(false, "Please use a more descriptive task name");
        }

        return new ValidationResult(true, null);
    }

    public static String sanitizeTaskDescription(String description) {
        if (description == null) {
            return "";
        }

        return description.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\x00-\\x1F\\x7F]", "");
    }

    public static boolean isTaskDescriptionUnique(String description, java.util.List<TaskItem> existingTasks) {
        if (description == null || existingTasks == null) {
            return true;
        }

        String sanitizedInput = sanitizeTaskDescription(description);
        
        return existingTasks.stream()
                .noneMatch(task -> 
                    sanitizeTaskDescription(task.getDescription())
                        .equalsIgnoreCase(sanitizedInput));
    }

    public static String generateValidationSummary(String description) {
        if (description == null) {
            description = "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Task Validation Summary:\n\n");
        summary.append("Length: ").append(description.length()).append(" characters\n");
        summary.append("Requirements:\n");
        summary.append("✓ Minimum 3 characters\n");
        summary.append("✓ Maximum 200 characters\n");
        summary.append("✓ Contains letters or numbers\n");
        summary.append("✓ No excessive whitespace\n");
        summary.append("✓ Descriptive content\n");

        ValidationResult result = validateTaskDescription(description);
        if (!result.isValid()) {
            summary.append("\nIssue: ").append(result.getErrorMessage());
        }

        return summary.toString();
    }
}