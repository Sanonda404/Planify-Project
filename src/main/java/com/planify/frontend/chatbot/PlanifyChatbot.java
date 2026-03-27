// PlanifyChatbot.java
package com.planify.frontend.chatbot;

import com.planify.frontend.chatbot.core.ContextManager;
import com.planify.frontend.chatbot.core.IntentRecognizer;
import com.planify.frontend.chatbot.core.QueryParser;
import com.planify.frontend.chatbot.handlers.*;
import com.planify.frontend.chatbot.models.Intent;
import com.planify.frontend.chatbot.models.QueryContext;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.group.GroupDetails;
import com.planify.frontend.models.project.ProjectDetails;
import com.planify.frontend.models.tasks.TaskDetails;

import java.util.*;

public class PlanifyChatbot {

    // Data sources
    private final List<EventGetRequest> events;
    private final List<TaskDetails> tasks;
    private final List<ProjectDetails> projects;
    private final List<GroupDetails> groups;
    private final Map<String, MemberInfo> currentUser;

    // Core components
    private final IntentRecognizer intentRecognizer;
    private final QueryParser queryParser;
    private final ContextManager contextManager;

    // Handlers
    private final DeadlineHandler deadlineHandler;
    private final EventHandler eventHandler;
    private final TaskHandler taskHandler;
    private final ProjectHandler projectHandler;
    private final GroupHandler groupHandler;
    private final PriorityHandler priorityHandler;
    private final AnalyticsHandler analyticsHandler;
    private final RecommendationHandler recommendationHandler;

    public PlanifyChatbot(List<EventGetRequest> events, List<TaskDetails> tasks,
                          List<ProjectDetails> projects, List<GroupDetails> groups,
                          Map<String, MemberInfo> currentUser) {
        this.events = events != null ? events : new ArrayList<>();
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.projects = projects != null ? projects : new ArrayList<>();
        this.groups = groups != null ? groups : new ArrayList<>();
        this.currentUser = currentUser;

        // Initialize components
        this.intentRecognizer = new IntentRecognizer();
        this.queryParser = new QueryParser();
        this.contextManager = new ContextManager();

        // Initialize handlers
        this.deadlineHandler = new DeadlineHandler(this.events);
        this.eventHandler = new EventHandler(this.events);
        this.taskHandler = new TaskHandler(this.tasks, this.projects);
        this.projectHandler = new ProjectHandler(this.projects);
        this.groupHandler = new GroupHandler(this.groups);
        this.priorityHandler = new PriorityHandler(this.events, this.tasks);
        this.analyticsHandler = new AnalyticsHandler(this.tasks, this.projects, this.events);
        this.recommendationHandler = new RecommendationHandler(this.events, this.tasks);
    }

    public String answerQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            return getHelpMessage();
        }

        // Parse and recognize intent
        QueryContext context = new QueryContext();
        context.setOriginalQuery(question);

        context = queryParser.parse(question, context);
        Intent intent = intentRecognizer.recognize(context);
        context.setIntent(intent);

        // Update context with previous context if needed
        contextManager.updateContext(context);

        // Route to appropriate handler
        return routeToHandler(context);
    }

    private String routeToHandler(QueryContext context) {
        switch (context.getIntent()) {
            // Deadline related
            case DEADLINE_COUNT:
            case DEADLINE_LIST:
            case DEADLINE_TODAY:
            case DEADLINE_TOMORROW:
            case DEADLINE_THIS_WEEK:
            case DEADLINE_UPCOMING:
            case ASSIGNMENT_LIST:
            case CLASS_TEST_LIST:
                return deadlineHandler.handle(context);

            // Event related
            case EVENT_TODAY:
            case EVENT_TOMORROW:
            case EVENT_THIS_WEEK:
            case EVENT_SPECIFIC_TIME:
            case EVENT_SEARCH:
                return eventHandler.handle(context);

            // Task related
            case TASK_DAILY:
            case TASK_PENDING:
            case TASK_COMPLETED:
            case TASK_IN_PROGRESS:
            case TASK_OVERDUE:
            case TASK_BY_PROJECT:
            case TASK_BY_CATEGORY:
                return taskHandler.handle(context);

            // Project related
            case PROJECT_LIST:
            case PROJECT_PROGRESS:
            case PROJECT_DETAILS:
            case PROJECT_MILESTONES:
            case PROJECT_VELOCITY:
            case PROJECT_COMPLETION_DATE:
                return projectHandler.handle(context);

            // Group related
            case GROUP_LIST:
            case GROUP_MEMBERS:
            case GROUP_EVENTS:
            case GROUP_ROLE:
                return groupHandler.handle(context);

            // Priority and recommendations
            case WHAT_TO_DO_FIRST:
            case URGENT_TASKS:
                return priorityHandler.handle(context);

            // Analytics
            case PRODUCTIVITY_ANALYSIS:
            case WEEKLY_SUMMARY:
            case TRENDS:
                return analyticsHandler.handle(context);

            // Smart recommendations
            case FREE_TIME:
            case RECOMMENDATION:
                return recommendationHandler.handle(context);

            case GREETING:
                return getGreetingMessage();

            case HELP:
                return getHelpMessage();

            default:
                return getHelpMessage();
        }
    }

    private String getGreetingMessage() {
        String userName = currentUser != null && currentUser.containsKey("name") ?
                currentUser.get("name").getName() : "there";

        return "👋 **Hello " + userName + "!**\n\n" +
                "I'm your Planify AI assistant. I can help you with:\n" +
                "• 📅 Checking deadlines and events\n" +
                "• ✅ Managing your tasks and todos\n" +
                "• 📊 Tracking project progress\n" +
                "• 👥 Getting group information\n" +
                "• 💡 Providing smart recommendations\n\n" +
                "What would you like to know?";
    }

    private String getHelpMessage() {
        return "🤖 **Planify Chatbot Help**\n\n" +
                "I can answer questions about:\n\n" +
                "**Deadlines & Events**\n" +
                "• \"How many deadlines this week?\"\n" +
                "• \"What deadlines do I have today?\"\n" +
                "• \"What events are scheduled for tomorrow?\"\n" +
                "• \"How many assignments are due?\"\n" +
                "• \"What CTs do I have this week?\"\n\n" +

                "**Tasks**\n" +
                "• \"What are my daily tasks for today?\"\n" +
                "• \"What tasks are pending?\"\n" +
                "• \"Show me overdue tasks\"\n" +
                "• \"What tasks are in progress?\"\n\n" +

                "**Projects**\n" +
                "• \"What's the progress of my project?\"\n" +
                "• \"Show me all my projects\"\n" +
                "• \"What milestones are left?\"\n" +
                "• \"When will my project be completed?\"\n\n" +

                "**Groups**\n" +
                "• \"What groups am I in?\"\n" +
                "• \"Who are the members of my group?\"\n" +
                "• \"What's my role in the group?\"\n\n" +

                "**Smart Assistance**\n" +
                "• \"What should I do first?\"\n" +
                "• \"What's urgent?\"\n" +
                "• \"When am I free today?\"\n" +
                "• \"Give me productivity tips\"\n\n" +

                "**Analytics**\n" +
                "• \"How productive am I?\"\n" +
                "• \"Show me weekly summary\"\n" +
                "• \"What are my productivity trends?\"\n\n" +

                "Just ask me anything about your schedule and tasks!";
    }
}