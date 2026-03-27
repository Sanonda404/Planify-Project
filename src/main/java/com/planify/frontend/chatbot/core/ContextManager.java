// core/ContextManager.java
package com.planify.frontend.chatbot.core;

import com.planify.frontend.chatbot.models.QueryContext;

import java.util.*;

public class ContextManager {

    private static final int MAX_HISTORY = 10;
    private final Deque<QueryContext> history = new LinkedList<>();
    private QueryContext currentContext;

    public ContextManager() {
        this.currentContext = new QueryContext();
    }

    public QueryContext getCurrentContext() {
        return currentContext;
    }

    public void updateContext(QueryContext newContext) {
        // Save previous context to history
        if (currentContext != null && currentContext.getOriginalQuery() != null) {
            history.addFirst(currentContext);
            if (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }

        // Merge with previous context if needed
        if (isFollowUpQuestion(newContext)) {
            mergeContexts(currentContext, newContext);
        }

        this.currentContext = newContext;
    }

    private boolean isFollowUpQuestion(QueryContext newContext) {
        if (currentContext == null || currentContext.getOriginalQuery() == null) {
            return false;
        }

        String newQuery = newContext.getOriginalQuery().toLowerCase();
        return newQuery.matches(".*\\b(it|that|this|its|their)\\b.*") ||
                newQuery.matches(".*\\b(and|also|additionally)\\b.*") ||
                newContext.getTargetProject() == null && currentContext.getTargetProject() != null;
    }

    private void mergeContexts(QueryContext oldContext, QueryContext newContext) {
        if (newContext.getTargetProject() == null && oldContext.getTargetProject() != null) {
            newContext.setTargetProject(oldContext.getTargetProject());
        }

        if (newContext.getTargetGroup() == null && oldContext.getTargetGroup() != null) {
            newContext.setTargetGroup(oldContext.getTargetGroup());
        }

        if (newContext.getTimeRange().getStartDate() == null && oldContext.getTimeRange().getStartDate() != null) {
            newContext.setTimeRange(oldContext.getTimeRange());
        }
    }

    public QueryContext getPreviousContext() {
        return history.isEmpty() ? null : history.getFirst();
    }

    public void clearContext() {
        history.clear();
        currentContext = new QueryContext();
    }
}