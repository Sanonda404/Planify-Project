package com.planify.frontend.controllers.bot;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.group.GroupProjectDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.data.personal.ProjectDataManager;
import com.planify.frontend.utils.data.personal.TaskDataManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PlanifyBotController extends SceneParent implements Initializable {

    // ========== FXML COMPONENTS ==========
    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;

    // ========== DATA ==========
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    private List<EventGetRequest> allEvents = new ArrayList<>();
    private List<TaskDetails> allTasks = new ArrayList<>();
    private List<ProjectDetails> allProjects = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupNotificationPanel();
        init();
        loadData();
        addWelcomeMessage();

        // Auto-scroll to bottom when messages are added
        chatScrollPane.vvalueProperty().bind(chatContainer.heightProperty());
    }

    public void refresh(){

    }

    private void setupNotificationPanel() {
        notificationsList.setItems(NotificationManager.getNotifications());
        notificationsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(NotificationResponse notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createNotificationItem(notif));
                }
            }
        });
        NotificationManager.setParent(this);
    }

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
    }

    private void loadData() {
        // Load events
        List<EventGetRequest> personalEvents = EventDataManager.getAll();
        List<EventGetRequest> groupEvents = GroupEventDataManager.getAll();
        if (personalEvents != null) allEvents.addAll(personalEvents);
        if (groupEvents != null) allEvents.addAll(groupEvents);

        // Load tasks
        List<TaskDetails> personalTasks = TaskDataManager.getAllPersonalTasks();
        List<TaskDetails> projectTasks = GroupProjectDataManager.getAllTasks();
        if (personalTasks != null) allTasks.addAll(personalTasks);
        if (projectTasks != null) allTasks.addAll(projectTasks);

        // Load projects
        List<ProjectDetails> personalProjects = ProjectDataManager.getAllPersonalProjects();
        List<ProjectDetails> groupProjects = GroupProjectDataManager.getAllGroupProjects();
        if (personalProjects != null) allProjects.addAll(personalProjects);
        if (groupProjects != null) allProjects.addAll(groupProjects);
    }

    private void addWelcomeMessage() {
        addBotMessage("Hi there! 👋 I'm your Planify assistant. I can help you manage your tasks, events, and boost your productivity. What would you like to do today?");
    }

    private void addBotMessage(String message) {
        HBox messageContainer = new HBox(12);
        messageContainer.setAlignment(Pos.TOP_LEFT);
        messageContainer.getStyleClass().add("bot-message-container");

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("bot-avatar");
        Label avatarLabel = new Label("🤖");
        avatarLabel.getStyleClass().add("avatar-emoji");
        avatar.getChildren().add(avatarLabel);

        VBox bubble = new VBox(6);
        bubble.getStyleClass().addAll("message-bubble", "bot-bubble");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("message-text");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(LocalDateTime.now().format(timeFormatter));
        timeLabel.getStyleClass().add("message-time");

        bubble.getChildren().addAll(messageLabel, timeLabel);
        messageContainer.getChildren().addAll(avatar, bubble);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void addUserMessage(String message) {
        HBox messageContainer = new HBox(12);
        messageContainer.setAlignment(Pos.TOP_RIGHT);
        messageContainer.getStyleClass().add("user-message-container");

        VBox bubble = new VBox(6);
        bubble.getStyleClass().addAll("message-bubble", "user-bubble");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("message-text");
        messageLabel.setWrapText(true);

        Label timeLabel = new Label(LocalDateTime.now().format(timeFormatter));
        timeLabel.getStyleClass().add("message-time");

        bubble.getChildren().addAll(messageLabel, timeLabel);
        messageContainer.getChildren().addAll(bubble);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    // ========== COMMAND HANDLERS ==========

    @FXML
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        addUserMessage(message);
        messageField.clear();

        // Process the message and generate response
        String response = processCommand(message);
        addBotMessage(response);
    }

    @FXML
    private void askSchedule() {
        addUserMessage("What's my schedule today?");
        String response = getTodaySchedule();
        addBotMessage(response);
    }

    @FXML
    private void askPendingTasks() {
        addUserMessage("Show my pending tasks");
        String response = getPendingTasks();
        addBotMessage(response);
    }

    @FXML
    private void askProductivity() {
        addUserMessage("How's my productivity?");
        String response = getProductivityReport();
        addBotMessage(response);
    }

    @FXML
    private void askPriorities() {
        addUserMessage("Suggest priorities");
        String response = getPrioritySuggestions();
        addBotMessage(response);
    }

    @FXML
    private void voiceInput() {
        // Placeholder for voice input functionality
        addBotMessage("🎤 Voice input is coming soon! For now, please type your message.");
    }

    // ========== COMMAND PROCESSING ==========

    private String processCommand(String message) {
        String lowerMsg = message.toLowerCase();

        if (lowerMsg.contains("schedule") || lowerMsg.contains("today") || lowerMsg.contains("events")) {
            return getTodaySchedule();
        } else if (lowerMsg.contains("task") && (lowerMsg.contains("pending") || lowerMsg.contains("todo"))) {
            return getPendingTasks();
        } else if (lowerMsg.contains("productivity") || lowerMsg.contains("progress") || lowerMsg.contains("how am i")) {
            return getProductivityReport();
        } else if (lowerMsg.contains("priority") || lowerMsg.contains("important") || lowerMsg.contains("focus")) {
            return getPrioritySuggestions();
        } else if (lowerMsg.contains("create task") || lowerMsg.contains("add task")) {
            return "To create a task, please go to the Task Management section and click 'New Task'. I'll help you there! 📝";
        } else if (lowerMsg.contains("create event") || lowerMsg.contains("add event")) {
            return "To create an event, please go to the Schedules section and click 'New Event'. I'll assist you! 📅";
        } else if (lowerMsg.contains("help") || lowerMsg.contains("what can you do")) {
            return getHelpMessage();
        } else {
            return getRandomMotivation();
        }
    }

    private String getTodaySchedule() {
        LocalDate today = LocalDate.now();
        List<EventGetRequest> todayEvents = allEvents.stream()
                .filter(e -> {
                    try {
                        LocalDateTime date = LocalDateTime.parse(e.getStartDateTime());
                        return date.toLocalDate().equals(today);
                    } catch (Exception ex) { return false; }
                })
                .collect(Collectors.toList());

        if (todayEvents.isEmpty()) {
            return "📅 You have no events scheduled for today! It's a great day to focus on your tasks and get ahead. 🚀";
        }

        StringBuilder response = new StringBuilder("📅 **Your Schedule Today:**\n\n");
        for (EventGetRequest event : todayEvents) {
            String type = event.getType() != null ? event.getType() : "Event";
            String emoji = getEventEmoji(event.getType());
            String time = formatEventTime(event.getStartDateTime());
            response.append(emoji).append(" ").append(event.getTitle())
                    .append(" (").append(type).append(")\n   ⏰ ").append(time).append("\n\n");
        }
        return response.toString();
    }

    private String getPendingTasks() {
        List<TaskDetails> pendingTasks = allTasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getDueDate() != null && !t.getDueDate().isEmpty())
                .sorted((a, b) -> {
                    try {
                        return LocalDate.parse(a.getDueDate()).compareTo(LocalDate.parse(b.getDueDate()));
                    } catch (Exception e) { return 0; }
                })
                .limit(5)
                .collect(Collectors.toList());

        if (pendingTasks.isEmpty()) {
            return "✅ Great job! You have no pending tasks. Time to celebrate or take on new challenges! 🎉";
        }

        StringBuilder response = new StringBuilder("📋 **Your Pending Tasks:**\n\n");
        for (TaskDetails task : pendingTasks) {
            String priority = task.getPriority() != null ? task.getPriority() : "Medium";
            String priorityEmoji = getPriorityEmoji(priority);
            response.append(priorityEmoji).append(" ").append(task.getTitle())
                    .append("\n   📅 Due: ").append(formatDate(task.getDueDate()))
                    .append("\n   🏷️ ").append(task.getCategory() != null ? task.getCategory() : "General")
                    .append("\n\n");
        }

        long overdueCount = pendingTasks.stream()
                .filter(t -> {
                    try {
                        return LocalDate.parse(t.getDueDate()).isBefore(LocalDate.now());
                    } catch (Exception e) { return false; }
                })
                .count();

        if (overdueCount > 0) {
            response.append("⚠️ *Tip:* ").append(overdueCount).append(" task(s) are overdue. Consider prioritizing them!");
        }

        return response.toString();
    }

    private String getProductivityReport() {
        int totalTasks = allTasks.size();
        long completedTasks = allTasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();
        int completionRate = totalTasks > 0 ? (int)(completedTasks * 100 / totalTasks) : 0;

        int totalEvents = allEvents.size();
        int activeProjects = (int) allProjects.stream().filter(p -> !p.isCompleted()).count();

        // Calculate weekly streak
        int streak = calculateStreak();

        StringBuilder response = new StringBuilder("📊 **Your Productivity Report:**\n\n");
        response.append("✅ **Tasks:** ").append(completedTasks).append("/").append(totalTasks)
                .append(" completed (").append(completionRate).append("%)\n\n");
        response.append("📅 **Events:** ").append(totalEvents).append(" total\n\n");
        response.append("🚀 **Active Projects:** ").append(activeProjects).append("\n\n");
        response.append("🔥 **Current Streak:** ").append(streak).append(" day(s)\n\n");

        if (completionRate >= 80) {
            response.append("🌟 Excellent work! You're crushing your goals. Keep up the momentum!");
        } else if (completionRate >= 50) {
            response.append("👍 Good progress! A little more focus and you'll reach your targets.");
        } else if (completionRate > 0) {
            response.append("💪 You're making progress! Try breaking down large tasks into smaller ones.");
        } else {
            response.append("🎯 Ready to start? Create your first task and let's get productive!");
        }

        return response.toString();
    }

    private String getPrioritySuggestions() {
        // Get high priority tasks
        List<TaskDetails> highPriorityTasks = allTasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> {
                    if (t.getPriority() != null) return "High".equalsIgnoreCase(t.getPriority());
                    try {
                        if (t.getDueDate() != null && !t.getDueDate().isEmpty()) {
                            LocalDate dueDate = LocalDate.parse(t.getDueDate());
                            return dueDate.isBefore(LocalDate.now().plusDays(2));
                        }
                    } catch (Exception e) {}
                    return false;
                })
                .limit(3)
                .collect(Collectors.toList());

        // Get urgent deadlines
        List<EventGetRequest> urgentEvents = allEvents.stream()
                .filter(e -> "deadline".equalsIgnoreCase(e.getType()))
                .filter(e -> {
                    try {
                        LocalDateTime date = LocalDateTime.parse(e.getStartDateTime());
                        return date.toLocalDate().isBefore(LocalDate.now().plusDays(3));
                    } catch (Exception ex) { return false; }
                })
                .limit(2)
                .collect(Collectors.toList());

        StringBuilder response = new StringBuilder("🎯 **Priority Suggestions:**\n\n");

        if (!highPriorityTasks.isEmpty()) {
            response.append("🔥 **Urgent Tasks:**\n");
            for (TaskDetails task : highPriorityTasks) {
                response.append("   • ").append(task.getTitle());
                if (task.getDueDate() != null) {
                    response.append(" (Due: ").append(formatDate(task.getDueDate())).append(")");
                }
                response.append("\n");
            }
            response.append("\n");
        }

        if (!urgentEvents.isEmpty()) {
            response.append("⏰ **Upcoming Deadlines:**\n");
            for (EventGetRequest event : urgentEvents) {
                response.append("   • ").append(event.getTitle());
                if (event.getStartDateTime() != null) {
                    response.append(" (Due: ").append(formatEventDate(event.getStartDateTime())).append(")");
                }
                response.append("\n");
            }
            response.append("\n");
        }

        if (highPriorityTasks.isEmpty() && urgentEvents.isEmpty()) {
            response.append("✨ No urgent tasks at the moment! Great planning!\n\n");
            response.append("💡 **Suggestions:**\n");
            response.append("   • Review your long-term goals\n");
            response.append("   • Plan ahead for next week\n");
            response.append("   • Take some time to relax and recharge");
        } else {
            response.append("💡 **Tip:** Focus on the above items first, then tackle less urgent tasks.");
        }

        return response.toString();
    }

    private String getHelpMessage() {
        return "🤖 **What I Can Help You With:**\n\n" +
                "📅 **Schedule** - Ask about your today's events\n" +
                "✅ **Tasks** - Check pending tasks\n" +
                "📊 **Productivity** - Get performance report\n" +
                "🎯 **Priorities** - Get priority suggestions\n\n" +
                "**Examples:**\n" +
                "• 'What's my schedule today?'\n" +
                "• 'Show my pending tasks'\n" +
                "• 'How's my productivity?'\n" +
                "• 'Suggest priorities'\n\n" +
                "I'm here to help you stay productive! 🚀";
    }

    private String getRandomMotivation() {
        String[] motivations = {
                "🌟 You're doing great! Keep pushing forward!",
                "💪 Every small step counts towards your goals!",
                "🎯 Stay focused and you'll achieve amazing things!",
                "✨ Believe in yourself - you've got this!",
                "🚀 Progress is progress, no matter how small!",
                "📈 Consistency is the key to success. Keep going!",
                "💡 Take a moment to appreciate how far you've come!"
        };
        return motivations[new Random().nextInt(motivations.length)];
    }

    private int calculateStreak() {
        int streak = 0;
        LocalDate today = LocalDate.now();
        Set<LocalDate> completionDates = allTasks.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .filter(t -> t.getDueDate() != null && !t.getDueDate().isEmpty())
                .map(t -> {
                    try { return LocalDate.parse(t.getDueDate()); }
                    catch (Exception e) { return null; }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LocalDate checkDate = today;
        while (completionDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }
        return streak;
    }

    private String formatEventTime(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private String formatEventDate(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String getEventEmoji(String type) {
        if (type == null) return "📌";
        switch (type.toLowerCase()) {
            case "slot": return "📅";
            case "deadline": return "⏰";
            case "span": return "📊";
            case "marker": return "📍";
            default: return "📌";
        }
    }

    private String getPriorityEmoji(String priority) {
        if (priority == null) return "📌";
        switch (priority.toLowerCase()) {
            case "high": return "🔴";
            case "medium": return "🟠";
            case "low": return "🟢";
            default: return "📌";
        }
    }


    @FXML private void toggleNotifications() {
        boolean isVisible = notifPanel.isVisible();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    @FXML private void clearChat(){

    }
    @FXML private void askDailySummary(){

    }

    @FXML private void goBack() { SceneManager.switchScene("dashboard-view.fxml", "Dashboard"); }
    @FXML private void goDashboard() { SceneManager.switchScene("dashboard-view.fxml", "Dashboard"); }
    @FXML private void handleLogout() { SceneManager.switchScene("login-view.fxml", "Login"); }
}