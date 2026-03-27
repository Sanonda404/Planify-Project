package com.planify.frontend.controllers.events;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.managers.SceneManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulesController extends SceneParent {

    @FXML private Label weekTitleLabel;
    @FXML private Label monDateLabel, tueDateLabel, wedDateLabel, thuDateLabel, friDateLabel, satDateLabel, sunDateLabel;
    @FXML private VBox spansContainer, groupFilterContainer, upcomingContainer;
    @FXML private AnchorPane mondayPane, tuesdayPane, wednesdayPane, thursdayPane, fridayPane, saturdayPane, sundayPane;
    @FXML private AnchorPane spansAnchor;
    @FXML private CheckBox personalFilterCheckbox;
    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;
    @FXML private ListView<NotificationResponse> notificationsList;

    private final List<EventGetRequest> allEvents = new ArrayList<>();
    private final Map<String, CheckBox> groupCheckBoxMap = new HashMap<>();
    private LocalDate currentWeekStart;

    private static final double HOUR_HEIGHT = 72.0;
    private static final double DAY_START_HOUR = 0.0;
    private static final double TOTAL_VISIBLE_HOURS = 24.0;
    private static final double DEADLINE_MARKER_HEIGHT = 48.0;
    private static final double SLOT_DEFAULT_HEIGHT = 72.0;

    private final DateTimeFormatter apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("MMM dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    @FXML private CheckBox slotFilterCheckbox;
    @FXML private CheckBox spanFilterCheckbox;
    @FXML private CheckBox deadlineFilterCheckbox;
    @FXML private CheckBox markerFilterCheckbox;

    private static final double SLOT_COLUMN_WIDTH = 120;
    private static final double MIN_CARD_WIDTH = 100;
    private static final double CARD_SPACING = 2;

    // Add this inner class to manage slot positioning
    private static class SlotPosition {
        LocalDateTime start;
        LocalDateTime end;
        EventGetRequest event;
        double y;
        double height;
        int column;
        int columnSpan;

        SlotPosition(EventGetRequest event, LocalDateTime start, LocalDateTime end, double y, double height) {
            this.event = event;
            this.start = start;
            this.end = end;
            this.y = y;
            this.height = height;
            this.column = 0;
            this.columnSpan = 1;
        }

        boolean overlaps(SlotPosition other) {
            if (this.start.isAfter(other.end) || other.start.isAfter(this.end)) {
                return false;
            }
            return true;
        }
    }


    // Default colors for different event types
    private static final Map<String, String> DEFAULT_COLORS = Map.of(
            "slot", "#3c98fa",      // Blue
            "span", "#00ba28",      // Green
            "deadline", "#ba0034",  // Red
            "marker", "#f59e0b"     // Yellow/Orange
    );

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
    }

    @FXML
    private void toggleNotifications() {
        boolean isVisible = notifPanel.isManaged();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    @FXML
    public void initialize() {
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

        init();
        NotificationManager.setParent(this);
        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        personalFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());

        refresh();
    }

    public void refresh() {
        allEvents.clear();
        allEvents.addAll(fetchLocalPersonalEvents());
        fetchBackendEvents();

        // Expand recurring events
        List<EventGetRequest> expandedEvents = new ArrayList<>();
        for (EventGetRequest event : allEvents) {
            expandedEvents.addAll(expandRecurringEvent(event));
        }
        allEvents.clear();
        allEvents.addAll(expandedEvents);
        slotFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        spanFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        deadlineFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        markerFilterCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> refreshCalendar());
        updateWeekHeader();
        renderGroupFilters();
        refreshCalendar();
    }

    private void fetchBackendEvents() {
        allEvents.addAll(GroupEventDataManager.getAll());
    }

    private List<EventGetRequest> fetchLocalPersonalEvents() {
        return EventDataManager.getAll();
    }

    /**
     * Expand recurring events based on repeat pattern
     */
    private List<EventGetRequest> expandRecurringEvent(EventGetRequest event) {
        List<EventGetRequest> expandedEvents = new ArrayList<>();

        String repeatPattern = event.getRepeatPattern();
        if (repeatPattern == null || repeatPattern.equalsIgnoreCase("NO_REPEAT") || repeatPattern.isEmpty()) {
            expandedEvents.add(event);
            return expandedEvents;
        }

        LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
        LocalDateTime end = !event.getEndDateTime().isEmpty() ? LocalDateTime.parse(event.getEndDateTime()) : null;
        List<String> excludedDays = event.getExcludedDays();

        // Generate recurring events for next 3 months (adjust as needed)
        LocalDateTime currentStart = start;
        LocalDateTime currentEnd = end;

        while (currentStart.isBefore(LocalDateTime.now().plusMonths(3))) {
            // Check if this occurrence should be excluded
            if (shouldExcludeOccurrence(currentStart.toLocalDate(), excludedDays)) {
                // Move to next occurrence without adding
                currentStart = getNextOccurrence(currentStart, repeatPattern, event.getMonthlyDate());
                if (currentEnd != null) {
                    currentEnd = getNextOccurrence(currentEnd, repeatPattern, event.getMonthlyDate());
                }
                continue;
            }

            // Create new event for this occurrence
            EventGetRequest recurringEvent = new EventGetRequest(
                    event.getUuid() + "_" + currentStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    event.getTitle(),
                    event.getDescription(),
                    event.getType(),
                    event.getColor(),
                    currentStart.format(apiFormatter),
                    currentEnd != null ? currentEnd.format(apiFormatter) : null,
                    event.getGroup(),
                    event.isMergeWithPersonal(),
                    "none", // Don't expand recursively
                    null,
                    null,
                    event.getReminderMinutesBefore(),
                    event.getReminderType(),
                    event.getAttachmentUrl(),
                    event.getCreator(),
                    event.isEditingPermission()
            );
            expandedEvents.add(recurringEvent);

            // Move to next occurrence
            currentStart = getNextOccurrence(currentStart, repeatPattern, event.getMonthlyDate());
            if (currentEnd != null) {
                currentEnd = getNextOccurrence(currentEnd, repeatPattern, event.getMonthlyDate());
            }
        }

        return expandedEvents;
    }

    private LocalDateTime getNextOccurrence(LocalDateTime current, String pattern, String monthlyDate) {
        switch (pattern.toLowerCase()) {
            case "daily":
                return current.plusDays(1);
            case "weekly":
                return current.plusWeeks(1);
            case "monthly":
                if (monthlyDate != null && !monthlyDate.isEmpty()) {
                    try {
                        int dayOfMonth = Integer.parseInt(monthlyDate);
                        LocalDateTime next = current.withDayOfMonth(dayOfMonth);
                        if (next.isBefore(current) || next.equals(current)) {
                            next = next.plusMonths(1);
                        }
                        return next;
                    } catch (NumberFormatException e) {
                        return current.plusMonths(1);
                    }
                }
                return current.plusMonths(1);
            default:
                return current.plusDays(1);
        }
    }

    private boolean shouldExcludeOccurrence(LocalDate date, List<String> excludedDays) {
        if (excludedDays == null || excludedDays.isEmpty()) {
            return false;
        }

        String dayOfWeek = date.getDayOfWeek().toString().substring(0, 3);
        return excludedDays.contains(dayOfWeek);
    }

    private void updateWeekHeader() {
        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        weekTitleLabel.setText(currentWeekStart.format(dayMonthFormatter) + " - " + endOfWeek.format(dayMonthFormatter));

        Label[] dateLabels = {monDateLabel, tueDateLabel, wedDateLabel, thuDateLabel, friDateLabel, satDateLabel, sunDateLabel};
        for (int i = 0; i < 7; i++) {
            LocalDate date = currentWeekStart.plusDays(i);
            dateLabels[i].setText(String.valueOf(date.getDayOfMonth()));
        }
    }

    private void renderGroupFilters() {
        groupFilterContainer.getChildren().clear();
        groupCheckBoxMap.clear();

        Set<String> groups = allEvents.stream()
                .filter(e -> e.getGroup() != null)
                .map(e -> e.getGroup().getName())
                .collect(Collectors.toSet());

        for (String groupName : groups) {
            CheckBox cb = new CheckBox(groupName);
            cb.setSelected(true);
            cb.getStyleClass().add("group-checkbox");
            cb.setOnAction(e -> refreshCalendar());
            groupCheckBoxMap.put(groupName, cb);
            groupFilterContainer.getChildren().add(cb);
        }

        if (groups.isEmpty()) {
            Label noGroupsLabel = new Label("No groups available");
            noGroupsLabel.getStyleClass().add("no-groups-label");
            groupFilterContainer.getChildren().add(noGroupsLabel);
        }
    }

    private void refreshCalendar() {
        AnchorPane[] panes = {mondayPane, tuesdayPane, wednesdayPane, thursdayPane, fridayPane, saturdayPane, sundayPane};
        for (AnchorPane pane : panes) {
            pane.getChildren().clear();
            drawTimeGrid(pane);
        }
        spansContainer.getChildren().clear();

        List<EventGetRequest> filtered = allEvents.stream()
                .filter(this::isEventInCurrentWeek)
                .filter(e -> {
                    boolean isPersonal = e.getGroup() == null;
                    if (isPersonal) {
                        return personalFilterCheckbox.isSelected();
                    } else {
                        String groupName = e.getGroup().getName();
                        CheckBox cb = groupCheckBoxMap.get(groupName);
                        return cb != null && cb.isSelected();
                    }
                })
                .filter(e -> {
                    String type = e.getType() != null ? e.getType().toLowerCase() : "slot";
                    switch (type) {
                        case "slot": return slotFilterCheckbox.isSelected();
                        case "span": return spanFilterCheckbox.isSelected();
                        case "deadline": return deadlineFilterCheckbox.isSelected();
                        case "marker": return markerFilterCheckbox.isSelected();
                        default: return true;
                    }
                })
                .collect(Collectors.toList());


        // Separate events by type for proper layout
        List<EventGetRequest> slotEvents = new ArrayList<>();
        List<EventGetRequest> spanEvents = new ArrayList<>();
        List<EventGetRequest> deadlineMarkers = new ArrayList<>();

        for (EventGetRequest event : filtered) {
            String eventType = event.getType() != null ? event.getType().toLowerCase() : "slot";
            switch (eventType) {
                case "span":
                    spanEvents.add(event);
                    break;
                case "deadline":
                case "marker":
                    deadlineMarkers.add(event);
                    break;
                case "slot":
                default:
                    slotEvents.add(event);
                    break;
            }
        }

        // Render span events (always on top, don't conflict)
        for (EventGetRequest event : spanEvents) {
            renderSpanEvent(event);
        }

        // Render slots with collision avoidance
        renderSlotsWithLayout(slotEvents);

        // Render deadlines and markers (floating, can overlap)
        for (EventGetRequest event : deadlineMarkers) {
            renderDeadlineOrMarkerEvent(event);
        }
    }

    private void renderSlotsWithLayout(List<EventGetRequest> slotEvents) {
        // Group slots by day
        Map<LocalDate, List<SlotPosition>> slotsByDay = new HashMap<>();

        for (EventGetRequest event : slotEvents) {
            LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
            LocalDateTime end = event.getEndDateTime() != null ?
                    LocalDateTime.parse(event.getEndDateTime()) :
                    start.plusHours(1);

            AnchorPane pane = getPaneForDate(start.toLocalDate());
            if (pane == null) continue;

            double y = calculateY(start.toLocalTime());
            long mins = java.time.Duration.between(start, end).toMinutes();
            double height = Math.max(SLOT_DEFAULT_HEIGHT, (mins / 60.0) * HOUR_HEIGHT);

            slotsByDay.computeIfAbsent(start.toLocalDate(), k -> new ArrayList<>())
                    .add(new SlotPosition(event, start, end, y, height));
        }

        // Process each day's slots
        for (Map.Entry<LocalDate, List<SlotPosition>> entry : slotsByDay.entrySet()) {
            AnchorPane pane = getPaneForDate(entry.getKey());
            if (pane == null) continue;

            List<SlotPosition> daySlots = entry.getValue();

            // Sort by start time
            daySlots.sort((a, b) -> a.start.compareTo(b.start));

            // Calculate column assignments to avoid overlaps
            assignSlotColumns(daySlots);

            // Calculate maximum columns needed
            int maxColumns = daySlots.stream()
                    .mapToInt(s -> s.column + s.columnSpan)
                    .max()
                    .orElse(1);

            // Render each slot with proper width and positioning
            for (SlotPosition slot : daySlots) {
                double width = (SLOT_COLUMN_WIDTH - CARD_SPACING * (maxColumns - 1)) / maxColumns;
                double x = slot.column * (width + CARD_SPACING);

                VBox card = createSlotCard(slot.event, slot.start, slot.end);
                card.setPrefWidth(width);
                card.setPrefHeight(slot.height);

                AnchorPane.setTopAnchor(card, slot.y);
                AnchorPane.setLeftAnchor(card, x);

                pane.getChildren().add(card);
            }
        }
    }

    /**
     * Assign column positions to slots to avoid overlaps
     * Uses greedy algorithm for optimal column packing
     */
    private void assignSlotColumns(List<SlotPosition> slots) {
        List<List<SlotPosition>> columns = new ArrayList<>();

        for (SlotPosition slot : slots) {
            // Find first column where this slot doesn't overlap
            int columnIndex = 0;
            boolean placed = false;

            while (!placed && columnIndex < columns.size()) {
                List<SlotPosition> column = columns.get(columnIndex);
                boolean overlaps = false;

                for (SlotPosition existing : column) {
                    if (slot.overlaps(existing)) {
                        overlaps = true;
                        break;
                    }
                }

                if (!overlaps) {
                    column.add(slot);
                    slot.column = columnIndex;
                    placed = true;
                }
                columnIndex++;
            }

            if (!placed) {
                // Create new column
                List<SlotPosition> newColumn = new ArrayList<>();
                newColumn.add(slot);
                columns.add(newColumn);
                slot.column = columns.size() - 1;
            }
        }
    }

    /**
     * Create styled slot card
     */
    private VBox createSlotCard(EventGetRequest event, LocalDateTime start, LocalDateTime end) {
        String color = getEventColor(event);

        VBox card = new VBox(4);
        card.getStyleClass().add("slot-card");

        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("event-title");
        titleLabel.setWrapText(true);

        Label timeLabel = new Label(formatEventTime(start, end));
        timeLabel.getStyleClass().add("event-time");

        if (event.getGroup() != null) {
            Label groupLabel = new Label(event.getGroup().getName());
            groupLabel.getStyleClass().add("event-group");
            card.getChildren().add(groupLabel);
        }

        card.getChildren().addAll(titleLabel, timeLabel);

        card.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.2) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        card.setOnMouseClicked(e -> openEventDetails(event));
        return card;
    }

    /**
     * Render deadline/marker events with optional visual indicators for overlap
     */
    private void renderDeadlineOrMarkerEvent(EventGetRequest event) {
        LocalDateTime target = LocalDateTime.parse(event.getStartDateTime());
        AnchorPane pane = getPaneForDate(target.toLocalDate());
        if (pane == null) return;

        double y = calculateY(target.toLocalTime());
        String color = getEventColor(event);
        boolean isDeadline = "deadline".equalsIgnoreCase(event.getType());

        // Check if this deadline/marker overlaps with any slot
        List<SlotPosition> overlappingSlots = findOverlappingSlots(target, pane);

        VBox card = createDeadlineMarkerCard(event, isDeadline, color);

        // Adjust positioning if overlapping with slots
        double xOffset = 0;
        if (!overlappingSlots.isEmpty()) {
            // Position deadline/marker to the right of slots
            xOffset = SLOT_COLUMN_WIDTH + 10;
            card.getStyleClass().add("overlap-indicator");

            // Add tooltip to indicate overlap
            Tooltip tooltip = new Tooltip("Overlaps with: " +
                    overlappingSlots.stream()
                            .map(s -> s.event.getTitle())
                            .collect(Collectors.joining(", ")));
            Tooltip.install(card, tooltip);
        }

        card.setPrefWidth(100);
        card.setPrefHeight(DEADLINE_MARKER_HEIGHT);

        AnchorPane.setTopAnchor(card, y);
        AnchorPane.setLeftAnchor(card, xOffset);

        card.setOnMouseClicked(e -> openEventDetails(event));
        pane.getChildren().add(card);
    }

    /**
     * Find slots that overlap with a given time
     */
    private List<SlotPosition> findOverlappingSlots(LocalDateTime time, AnchorPane pane) {
        List<SlotPosition> overlapping = new ArrayList<>();

        // Get all slot cards in this pane
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof VBox && node.getStyleClass().contains("slot-card")) {
                // We need to store slot positions separately for lookup
                // For simplicity, we'll check against the stored slot positions
                // In production, maintain a map of slot positions
            }
        }

        return overlapping;
    }

    /**
     * Create styled deadline/marker card
     */
    private VBox createDeadlineMarkerCard(EventGetRequest event, boolean isDeadline, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add(isDeadline ? "deadline-card" : "marker-card");

        HBox headerBox = new HBox(8);
        Label iconLabel = new Label(isDeadline ? "⏰" : "📍");
        iconLabel.getStyleClass().add(isDeadline ? "deadline-icon" : "marker-icon");
        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add(isDeadline ? "deadline-title" : "marker-title");
        titleLabel.setWrapText(true);
        headerBox.getChildren().addAll(iconLabel, titleLabel);

        LocalDateTime target = LocalDateTime.parse(event.getStartDateTime());
        Label timeLabel = new Label((isDeadline ? "Due: " : "At: ") + target.format(timeFormatter));
        timeLabel.getStyleClass().add(isDeadline ? "deadline-time" : "marker-time");

        card.getChildren().addAll(headerBox, timeLabel);

        card.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.15) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 3;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;"
        );

        return card;
    }

    /**
     * Render span as a continuous rectangle across the week.
     * Uses actual day pane widths for pixel-perfect alignment.
     */
    private void renderSpanEvent(EventGetRequest event) {
        LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
        LocalDateTime end = event.getEndDateTime() != null ?
                LocalDateTime.parse(event.getEndDateTime()) : null;

        if (end == null) {
            renderSingleDaySpan(event, start);
            return;
        }

        LocalDate weekStart = currentWeekStart;
        LocalDate weekEnd = currentWeekStart.plusDays(6);

        LocalDate spanStartDate = start.toLocalDate();
        LocalDate spanEndDate = end.toLocalDate();

        // Skip if span doesn't intersect current week
        if (spanEndDate.isBefore(weekStart) || spanStartDate.isAfter(weekEnd)) {
            return;
        }

        // We use a listener-based approach: schedule rendering after layout pass
        // so that mondayPane.getWidth() is valid.
        spansContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {}); // ensure scene attached

        HBox spanContainer = new HBox();
        spanContainer.getStyleClass().add("span-container");
        spanContainer.setPrefWidth(Double.MAX_VALUE);
        spanContainer.setMaxWidth(Double.MAX_VALUE);

        HBox spanBar = createSpanBar(event, start, end);
        spanContainer.getChildren().add(spanBar);
        spanContainer.setOnMouseClicked(e -> openEventDetails(event));
        spansContainer.getChildren().add(spanContainer);

        // Defer positioning until layout is computed
        Platform.runLater(() -> positionSpanBar(spanBar, spanContainer, event, start, end, weekStart, weekEnd, spanStartDate, spanEndDate));
    }

    /**
     * Position the span bar after layout pass so actual widths are available.
     */
    private void positionSpanBar(HBox spanBar, HBox spanContainer,
                                 EventGetRequest event,
                                 LocalDateTime start, LocalDateTime end,
                                 LocalDate weekStart, LocalDate weekEnd,
                                 LocalDate spanStartDate, LocalDate spanEndDate) {
        // Get actual day pane width; fall back to equal division of container
        double dayWidth = getDayWidth();
        if (dayWidth <= 0) {
            // Retry once more on next pulse
            Platform.runLater(() -> positionSpanBar(spanBar, spanContainer, event, start, end, weekStart, weekEnd, spanStartDate, spanEndDate));
            return;
        }

        // Time column offset (70px) + hgap (12px) between time col and first day col
        double timeColOffset = 70.0 + 12.0;

        double leftOffset;
        double spanWidth;

        if (spanStartDate.isBefore(weekStart)) {
            // Starts before this week – begin at day 0
            leftOffset = timeColOffset;
            long daysInWeek = java.time.temporal.ChronoUnit.DAYS.between(weekStart, spanEndDate.isAfter(weekEnd) ? weekEnd : spanEndDate) + 1;
            daysInWeek = Math.min(daysInWeek, 7);
            spanWidth = dayWidth * daysInWeek;

            if (!spanEndDate.isAfter(weekEnd) && spanEndDate.isAfter(weekStart)) {
                double partialAdj = calculatePartialDayAdjustment(end, dayWidth, true);
                spanWidth = (dayWidth * (daysInWeek - 1)) + partialAdj;
            }
        } else if (spanEndDate.isAfter(weekEnd)) {
            // Ends after this week
            long daysOffset = java.time.temporal.ChronoUnit.DAYS.between(weekStart, spanStartDate);
            long daysInWeek = 7 - daysOffset;

            leftOffset = timeColOffset + dayWidth * daysOffset;
            spanWidth = dayWidth * daysInWeek;

            if (spanStartDate.isAfter(weekStart)) {
                double partialAdj = calculatePartialDayAdjustment(start, dayWidth, false);
                spanWidth = (dayWidth * (daysInWeek - 1)) + partialAdj;
                leftOffset = timeColOffset + (dayWidth * daysOffset) + (dayWidth - partialAdj);
            }
        } else {
            // Entirely within the week
            long daysInSpan = java.time.temporal.ChronoUnit.DAYS.between(spanStartDate, spanEndDate) + 1;
            long daysOffset = java.time.temporal.ChronoUnit.DAYS.between(weekStart, spanStartDate);

            leftOffset = timeColOffset + dayWidth * daysOffset;
            spanWidth = dayWidth * daysInSpan;

            double startAdj = 0;
            double endAdj = 0;

            if (!start.toLocalTime().equals(LocalTime.MIN)) {
                startAdj = calculatePartialDayAdjustment(start, dayWidth, false);
                spanWidth = (dayWidth * (daysInSpan - 1)) + startAdj;
                leftOffset = timeColOffset + (dayWidth * daysOffset) + (dayWidth - startAdj);
            }

            if (end != null && !end.toLocalTime().equals(LocalTime.MAX) && !end.toLocalTime().equals(LocalTime.of(23, 59, 59))) {
                endAdj = calculatePartialDayAdjustment(end, dayWidth, true);
                spanWidth = (dayWidth * (daysInSpan - 1)) + endAdj;
            }

            if (daysInSpan == 1 && startAdj > 0 && endAdj > 0) {
                // Same day partial: just cover from start to end time
                double startFrac = (start.getHour() * 60.0 + start.getMinute()) / (24.0 * 60);
                double endFrac = (end.getHour() * 60.0 + end.getMinute()) / (24.0 * 60);
                leftOffset = timeColOffset + (dayWidth * daysOffset) + dayWidth * startFrac;
                spanWidth = dayWidth * (endFrac - startFrac);
            }
        }

        spanBar.setPrefWidth(Math.max(30, spanWidth));
        spanBar.setMaxWidth(Math.max(30, spanWidth));
        HBox.setMargin(spanBar, new Insets(0, 0, 0, leftOffset));
    }

    /**
     * Returns the actual rendered width of a single day pane.
     * Uses mondayPane's width; falls back to computing from container width.
     */
    private double getDayWidth() {
        if (mondayPane != null && mondayPane.getWidth() > 0) {
            return mondayPane.getWidth();
        }
        // Fallback: derive from spansContainer width minus time-col offset
        if (spansContainer != null && spansContainer.getWidth() > 0) {
            double timeColOffset = 70.0 + 12.0; // time col + hgap
            return (spansContainer.getWidth() - timeColOffset) / 7.0;
        }
        return 0;
    }

    /**
     * Calculate partial day adjustment for width
     * @param dateTime The date time of the partial boundary
     * @param fullDayWidth Width of a full day
     * @param isEndDay true for end day, false for start day
     * @return Adjusted width for partial day
     */
    private double calculatePartialDayAdjustment(LocalDateTime dateTime, double fullDayWidth, boolean isEndDay) {
        // Calculate percentage of the day covered
        long totalMinutesInDay = 24 * 60;
        long minutesIntoDay = dateTime.getHour() * 60 + dateTime.getMinute();

        double coveragePercentage;
        if (isEndDay) {
            // For end day, we want the portion from start of day to end time
            coveragePercentage = (double) minutesIntoDay / totalMinutesInDay;
        } else {
            // For start day, we want the portion from start time to end of day
            coveragePercentage = (double) (totalMinutesInDay - minutesIntoDay) / totalMinutesInDay;
        }

        // Minimum width for visibility (even a 1-hour span should be visible)
        double minWidth = 30;
        double calculatedWidth = fullDayWidth * coveragePercentage;

        return Math.max(minWidth, Math.min(calculatedWidth, fullDayWidth));
    }

    /**
     * Create the span bar with proper styling and content
     */
    private HBox createSpanBar(EventGetRequest event, LocalDateTime start, LocalDateTime end) {
        String color = getEventColor(event);

        HBox spanBar = new HBox(8);
        spanBar.getStyleClass().add("span-bar");
        spanBar.setAlignment(Pos.CENTER_LEFT);

        // Add left indicator for partial start
        boolean hasPartialStart = start != null && !start.toLocalTime().equals(LocalTime.MIN);
        boolean hasPartialEnd = end != null && !end.toLocalTime().equals(LocalTime.MAX);

        VBox contentBox = new VBox(4);
        contentBox.setAlignment(Pos.CENTER_LEFT);

        // Title
        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("span-bar-title");
        titleLabel.setWrapText(true);

        // Date range
        Label dateRangeLabel = new Label(formatSpanDateRange(start, end));
        dateRangeLabel.getStyleClass().add("span-bar-date");

        contentBox.getChildren().addAll(titleLabel, dateRangeLabel);

        // Add time indicators for partial days
        if (hasPartialStart || hasPartialEnd) {
            Label timeIndicator = new Label();
            timeIndicator.getStyleClass().add("span-bar-time");

            if (hasPartialStart && hasPartialEnd && start.toLocalDate().equals(end.toLocalDate())) {
                // Single day with both start and end times
                timeIndicator.setText(start.format(timeFormatter) + " - " + end.format(timeFormatter));
            } else if (hasPartialStart) {
                timeIndicator.setText("Starts: " + start.format(timeFormatter));
            } else if (hasPartialEnd) {
                timeIndicator.setText("Ends: " + end.format(timeFormatter));
            }

            contentBox.getChildren().add(timeIndicator);
        }

        spanBar.getChildren().add(contentBox);

        // Apply gradient for partial days
        String gradientStyle = "";
        if (hasPartialStart && hasPartialEnd) {
            // Gradient for both sides
            gradientStyle = "-fx-background-image: linear-gradient(to right, " +
                    "rgba(255,255,255,0.3) 0%, " +
                    color + " 15%, " +
                    color + " 85%, " +
                    "rgba(255,255,255,0.3) 100%);";
        } else if (hasPartialStart) {
            // Gradient only on left side
            gradientStyle = "-fx-background-image: linear-gradient(to right, " +
                    "rgba(255,255,255,0.3) 0%, " +
                    color + " 10%);";
        } else if (hasPartialEnd) {
            // Gradient only on right side
            gradientStyle = "-fx-background-image: linear-gradient(to right, " +
                    color + " 90%, " +
                    "rgba(255,255,255,0.3) 100%);";
        }

        spanBar.setStyle(
                "-fx-background-color: " + color + ";" +
                        gradientStyle +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );

        // Add hover effect
        spanBar.setOnMouseEntered(e -> {
            spanBar.setStyle(spanBar.getStyle() +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);" +
                    "-fx-scale-y: 1.02;");
        });

        spanBar.setOnMouseExited(e -> {
            spanBar.setStyle(spanBar.getStyle().replace(
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);" +
                            "-fx-scale-y: 1.02;", ""));
        });

        return spanBar;
    }

    /**
     * Format span date range for display
     */
    private String formatSpanDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        if (end == null) {
            return "From: " + start.format(dateFormatter);
        }

        if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(dateFormatter);
        }

        return start.format(dateFormatter) + " - " + end.format(dateFormatter);
    }

    /**
     * Render single day span - deferred to use actual pane width
     */
    private void renderSingleDaySpan(EventGetRequest event, LocalDateTime start) {
        LocalDate targetDate = start.toLocalDate();
        LocalDate weekStart = currentWeekStart;
        LocalDate weekEnd = currentWeekStart.plusDays(6);

        if (targetDate.isBefore(weekStart) || targetDate.isAfter(weekEnd)) {
            return;
        }

        LocalDateTime end = event.getEndDateTime() != null ?
                LocalDateTime.parse(event.getEndDateTime()) : null;

        HBox spanContainer = new HBox();
        spanContainer.getStyleClass().add("span-container");
        spanContainer.setPrefWidth(Double.MAX_VALUE);

        HBox spanBar = createSpanBar(event, start, end);
        spanContainer.getChildren().add(spanBar);
        spansContainer.getChildren().add(spanContainer);

        Platform.runLater(() -> {
            double dayWidth = getDayWidth();
            if (dayWidth <= 0) {
                Platform.runLater(() -> {
                    double dw = getDayWidth();
                    if (dw > 0) applySingleDaySpanPosition(spanBar, event, start, end, weekStart, dw, targetDate);
                });
                return;
            }
            applySingleDaySpanPosition(spanBar, event, start, end, weekStart, dayWidth, targetDate);
        });
    }

    private void applySingleDaySpanPosition(HBox spanBar, EventGetRequest event,
                                            LocalDateTime start, LocalDateTime end,
                                            LocalDate weekStart, double dayWidth,
                                            LocalDate targetDate) {
        double timeColOffset = 70.0 + 12.0;
        long daysOffset = java.time.temporal.ChronoUnit.DAYS.between(weekStart, targetDate);
        double leftOffset = timeColOffset + dayWidth * daysOffset;
        double spanWidth = dayWidth;

        if (end != null && !start.toLocalTime().equals(LocalTime.MIN)) {
            double startFrac = (start.getHour() * 60.0 + start.getMinute()) / (24.0 * 60);
            leftOffset = timeColOffset + (dayWidth * daysOffset) + (dayWidth * startFrac);
            if (!end.toLocalTime().equals(LocalTime.MAX) && !end.toLocalTime().equals(LocalTime.of(23, 59, 59))) {
                double endFrac = (end.getHour() * 60.0 + end.getMinute()) / (24.0 * 60);
                spanWidth = dayWidth * (endFrac - startFrac);
            } else {
                spanWidth = dayWidth * (1.0 - startFrac);
            }
        }

        spanBar.setPrefWidth(Math.max(30, spanWidth));
        spanBar.setMaxWidth(Math.max(30, spanWidth));
        HBox.setMargin(spanBar, new Insets(0, 0, 0, leftOffset));
    }


    private String getEventColor(EventGetRequest event) {
        if (event.getColor() != null && !event.getColor().isEmpty()) {
            return event.getColor();
        }
        String type = event.getType() != null ? event.getType().toLowerCase() : "slot";
        return DEFAULT_COLORS.getOrDefault(type, DEFAULT_COLORS.get("slot"));
    }

    private void renderSlotEvent(EventGetRequest event) {
        LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
        AnchorPane pane = getPaneForDate(start.toLocalDate());
        if (pane == null) return;

        double y = calculateY(start.toLocalTime());
        double height = SLOT_DEFAULT_HEIGHT;

        LocalDateTime end = null;
        if (event.getEndDateTime() != null && !event.getEndDateTime().isEmpty()) {
            end = LocalDateTime.parse(event.getEndDateTime());
            long mins = java.time.Duration.between(start, end).toMinutes();
            height = Math.max(SLOT_DEFAULT_HEIGHT, (mins / 60.0) * HOUR_HEIGHT);
        }

        String color = getEventColor(event);

        VBox card = new VBox(4);
        card.getStyleClass().add("slot-card");

        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("event-title");

        Label timeLabel = new Label(formatEventTime(start, end));
        timeLabel.getStyleClass().add("event-time");

        if (event.getGroup() != null) {
            Label groupLabel = new Label(event.getGroup().getName());
            groupLabel.getStyleClass().add("event-group");
            card.getChildren().add(groupLabel);
        }

        card.getChildren().addAll(titleLabel, timeLabel);

        card.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.2) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        card.setPrefWidth(120);
        card.setPrefHeight(height);

        AnchorPane.setTopAnchor(card, y);
        card.setOnMouseClicked(e -> openEventDetails(event));
        pane.getChildren().add(card);
    }


    private String formatSpanPeriod(EventGetRequest event) {
        LocalDateTime start = LocalDateTime.parse(event.getStartDateTime());
        LocalDateTime end = event.getEndDateTime() != null ? LocalDateTime.parse(event.getEndDateTime()) : null;

        if (end == null) {
            return "From: " + start.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } else {
            return start.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                    end.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }


    private String formatEventTime(LocalDateTime start, LocalDateTime end) {
        if (end == null) {
            return start.format(timeFormatter);
        } else if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(timeFormatter) + " - " + end.format(timeFormatter);
        } else {
            return start.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a")) + " - " +
                    end.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
        }
    }

    private AnchorPane getPaneForDate(LocalDate date) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(currentWeekStart, date);
        return switch ((int) daysBetween) {
            case 0 -> mondayPane;
            case 1 -> tuesdayPane;
            case 2 -> wednesdayPane;
            case 3 -> thursdayPane;
            case 4 -> fridayPane;
            case 5 -> saturdayPane;
            case 6 -> sundayPane;
            default -> null;
        };
    }

    private void openEventDetails(EventGetRequest event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/event-detail-view.fxml"));
            VBox root = loader.load();
            EventDetailController controller = loader.getController();
            controller.setEvent(event);
            controller.setOnRefreshCallback(this::refresh);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawTimeGrid(AnchorPane pane) {
        for (int i = 0; i <= TOTAL_VISIBLE_HOURS; i++) {
            Line line = new Line(0, i * HOUR_HEIGHT, 150, i * HOUR_HEIGHT);
            line.getStyleClass().add("day-pane-line");
            pane.getChildren().add(line);
        }
    }

    private double calculateY(LocalTime time) {
        double hourPart = time.getHour() - DAY_START_HOUR;
        double minutePart = time.getMinute() / 60.0;
        double yPosition = (hourPart + minutePart) * HOUR_HEIGHT;
        return Math.max(0, Math.min(yPosition, TOTAL_VISIBLE_HOURS * HOUR_HEIGHT - DEADLINE_MARKER_HEIGHT));
    }

    private boolean isEventInCurrentWeek(EventGetRequest e) {
        LocalDate date = LocalDateTime.parse(e.getStartDateTime()).toLocalDate();
        return !date.isBefore(currentWeekStart) && !date.isAfter(currentWeekStart.plusDays(6));
    }

    private String hexToRgba(String hex, double opacity) {
        if (hex == null || !hex.startsWith("#")) return "rgba(100,100,100,0.1)";
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, opacity);
    }

    private String formatNotificationTime(LocalDateTime time) {
        java.time.Duration duration = java.time.Duration.between(time, LocalDateTime.now());

        if (duration.toMinutes() < 1) {
            return "Just now";
        } else if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " minutes ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + " hours ago";
        } else if (duration.toDays() < 7) {
            return duration.toDays() + " days ago";
        } else {
            return time.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }

    @FXML
    public void handleNextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        refresh();
    }

    @FXML
    public void handlePreviousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        refresh();
    }

    @FXML
    public void handleBack() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    public void handleNewEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/planify/frontend/fxmls/add-event-view.fxml"));
            Parent root = loader.load();

            AddEventController controller = loader.getController();
            controller.setParent(this);

            Stage stage = new Stage();
            stage.setTitle("Add Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDashboard() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }
}