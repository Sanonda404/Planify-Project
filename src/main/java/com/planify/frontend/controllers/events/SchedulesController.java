package com.planify.frontend.controllers.events;

import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.SceneParent;
import com.planify.frontend.models.events.EventGetRequest;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.data.group.GroupEventDataManager;
import com.planify.frontend.utils.data.personal.EventDataManager;
import com.planify.frontend.utils.managers.NotificationManager;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SchedulesController extends SceneParent {

    public Button dashboardButton;

    @FXML private Label weekTitleLabel;
    @FXML private Label monDateLabel, tueDateLabel, wedDateLabel, thuDateLabel, friDateLabel, satDateLabel, sunDateLabel;

    @FXML private VBox groupFilterContainer, upcomingContainer;
    @FXML private AnchorPane headerLayer;
    @FXML private AnchorPane spansAnchor;
    @FXML private GridPane calendarGrid;
    @FXML private GridPane dayHeaderGrid;
    @FXML private AnchorPane calendarContentRoot;

    @FXML private AnchorPane mondayPane, tuesdayPane, wednesdayPane, thursdayPane, fridayPane, saturdayPane, sundayPane;

    @FXML private CheckBox personalFilterCheckbox;
    @FXML private CheckBox slotFilterCheckbox;
    @FXML private CheckBox spanFilterCheckbox;
    @FXML private CheckBox deadlineFilterCheckbox;
    @FXML private CheckBox markerFilterCheckbox;

    @FXML private Circle connectionStatusCircle;
    @FXML private Label connectionStatusLabel;
    @FXML public Button notifBtn;
    @FXML public VBox notifPanel;
    @FXML private ListView<NotificationResponse> notificationsList;
    @FXML private CheckBox mergedFilterCheckbox;

    private final List<EventGetRequest> allEvents = new ArrayList<>();
    private final Map<String, CheckBox> groupCheckBoxMap = new HashMap<>();

    private final List<EventGetRequest> pendingTimedEvents = new ArrayList<>();
    private final List<EventGetRequest> pendingSpanEvents = new ArrayList<>();


    private LocalDate currentWeekStart;
    private boolean filterListenersInitialized = false;
    private boolean layoutListenersInitialized = false;
    private boolean staticGridInitialized = false;

    private final PauseTransition renderDebounce = new PauseTransition(Duration.millis(120));
    private int widthRetryCount = 0;

    private static final double HOUR_HEIGHT = 60.0;
    private static final double DAY_START_HOUR = 0.0;
    private static final double TOTAL_VISIBLE_HOURS = 24.0;
    private static final double SLOT_DEFAULT_HEIGHT = 60.0;
    private static final double SMALL_TIMED_HEIGHT = 44.0;
    private static final double SLOT_TOP_OFFSET = 2.0;

    private static final double CARD_SPACING = 1.5;
    private static final double SLOT_SIDE_PADDING = 1.5;
    private static final double MIN_CARD_WIDTH = 8.0;

    private static final double SPAN_ROW_HEIGHT = 30.0;
    private static final double SPAN_ROW_GAP = 4.0;
    private static final double SPAN_TOP_PADDING = 2.0;
    private static final double SPAN_SIDE_PADDING = 2.0;

    private static final int MAX_VISIBLE_SPAN_ROWS = 3;
    private static final double MAX_SPAN_OVERLAY_HEIGHT =
            (SPAN_TOP_PADDING * 2) + (SPAN_ROW_HEIGHT * MAX_VISIBLE_SPAN_ROWS) + (SPAN_ROW_GAP * (MAX_VISIBLE_SPAN_ROWS - 1));
    private static final double HEADER_DAY_SECTION_HEIGHT = 86.0;
    private static final double HEADER_FIXED_HEIGHT = MAX_SPAN_OVERLAY_HEIGHT + HEADER_DAY_SECTION_HEIGHT;
    private static final double SPAN_START_SHIFT = 30.0;
    private final DateTimeFormatter apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter dayMonthFormatter = DateTimeFormatter.ofPattern("MMM dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    private boolean isRendering = false;
    private long lastRenderTime = 0;
    private static final long RENDER_COOLDOWN_MS = 500;

    private static final Map<String, String> DEFAULT_COLORS = Map.of(
            "slot", "#3c98fa",
            "span", "#00ba28",
            "deadline", "#ba0034",
            "marker", "#f59e0b"
    );

    private volatile boolean isHovering = false;

    // ─── TimedRenderItem ────────────────────────────────────────────────────────
    // overlaps() now uses pixel Y-range instead of datetime so that
    // markers/deadlines (which have a fake padded end-time) don't create false
    // overlaps with events that don't actually share screen space.
    private static class TimedRenderItem {
        EventGetRequest event;
        LocalDateTime start;
        LocalDateTime end;   // kept for sort order; NOT used for visual overlap
        double y;
        double height;
        int column = 0;
        String type;

        TimedRenderItem(EventGetRequest event, LocalDateTime start, LocalDateTime end,
                        double y, double height, String type) {
            this.event  = event;
            this.start  = start;
            this.end    = end;
            this.y      = y;
            this.height = height;
            this.type   = type;
        }

        /**
         * Two cards overlap visually when their pixel Y-ranges intersect.
         * A 1-px buffer is subtracted so cards that merely touch (share an edge)
         * are NOT considered overlapping — they can share a column.
         */
        boolean overlaps(TimedRenderItem other) {
            double thisTop    = this.y;
            double thisBottom = this.y + this.height - 1;
            double otherTop   = other.y;
            double otherBottom= other.y + other.height - 1;
            return thisTop <= otherBottom && otherTop <= thisBottom;
        }
    }

    // ─── SpanRenderItem ─────────────────────────────────────────────────────────
    private static class SpanRenderItem {
        EventGetRequest event;
        LocalDateTime start;
        LocalDateTime end;
        int startDayIndex;
        int endDayIndex;
        int row;

        SpanRenderItem(EventGetRequest event, LocalDateTime start, LocalDateTime end,
                       int startDayIndex, int endDayIndex) {
            this.event        = event;
            this.start        = start;
            this.end          = end;
            this.startDayIndex= startDayIndex;
            this.endDayIndex  = endDayIndex;
            this.row          = 0;
        }

        /** Two span bars overlap when their day-index ranges intersect. */
        boolean overlaps(SpanRenderItem other) {
            return !(this.endDayIndex < other.startDayIndex ||
                    other.endDayIndex < this.startDayIndex);
        }
    }

    // ─── DayColumnBounds ────────────────────────────────────────────────────────
    private static class DayColumnBounds {
        final double minX;
        final double maxX;

        DayColumnBounds(double minX, double maxX) {
            this.minX = minX;
            this.maxX = maxX;
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────────

    public void init() {
        NotificationController.setStatusControls(connectionStatusCircle, connectionStatusLabel);
    }

    @FXML
    public void initialize() {
        try {
            if (notificationsList != null) {
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
            }

            init();
            NotificationManager.setParent(this);
            currentWeekStart = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            setDefaultFilters();
            initFilterListeners();

            renderDebounce.setOnFinished(e -> performStableRender());

            Platform.runLater(() -> {
                try {
                    setupStableLayout();
                    applyGlobalClips();
                    setupFixedHeaderOverlay();
                    initStaticDayPanes();
                    refresh();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Day-pane helpers
    // ────────────────────────────────────────────────────────────────────────────

    private List<AnchorPane> getDayPanes() {
        List<AnchorPane> panes = new ArrayList<>();
        panes.add(mondayPane);
        panes.add(tuesdayPane);
        panes.add(wednesdayPane);
        panes.add(thursdayPane);
        panes.add(fridayPane);
        panes.add(saturdayPane);
        panes.add(sundayPane);
        return panes;
    }

    private AnchorPane getPaneByIndex(int dayIndex) {
        switch (dayIndex) {
            case 0: return mondayPane;
            case 1: return tuesdayPane;
            case 2: return wednesdayPane;
            case 3: return thursdayPane;
            case 4: return fridayPane;
            case 5: return saturdayPane;
            case 6: return sundayPane;
            default: return null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Layout setup
    // ────────────────────────────────────────────────────────────────────────────

    private void setupStableLayout() {
        if (layoutListenersInitialized) return;
        layoutListenersInitialized = true;

        if (calendarGrid != null) {
            calendarGrid.setMaxWidth(Double.MAX_VALUE);
        }
        if (spansAnchor != null) {
            spansAnchor.setManaged(false);
            spansAnchor.setMaxWidth(Double.MAX_VALUE);
        }

        // ONLY listen to window resize, not every layout change
        if (calendarContentRoot != null) {
            calendarContentRoot.widthProperty().addListener((obs, o, n) -> {
                if (Math.abs(n.doubleValue() - o.doubleValue()) > 20) {
                    scheduleRender();
                }
            });
        }

        if (dayHeaderGrid != null) {
            dayHeaderGrid.widthProperty().addListener((obs, o, n) -> {
                if (Math.abs(n.doubleValue() - o.doubleValue()) > 20) {
                    scheduleRender();
                }
            });
        }
    }

    private void scheduleRender() {
        if (isHovering) return; // Don't render while hovering

        long now = System.currentTimeMillis();
        if (now - lastRenderTime < RENDER_COOLDOWN_MS) return;

        lastRenderTime = now;
        Platform.runLater(this::performStableRender);
    }


    private void setupFixedHeaderOverlay() {
        if (headerLayer == null || dayHeaderGrid == null || spansAnchor == null) return;

        headerLayer.setMinHeight(HEADER_FIXED_HEIGHT);
        headerLayer.setPrefHeight(HEADER_FIXED_HEIGHT);
        headerLayer.setMaxHeight(HEADER_FIXED_HEIGHT);

        spansAnchor.setManaged(false);
        spansAnchor.setVisible(true);
        spansAnchor.toFront();

        spansAnchor.setMinHeight(MAX_SPAN_OVERLAY_HEIGHT);
        spansAnchor.setPrefHeight(MAX_SPAN_OVERLAY_HEIGHT);
        spansAnchor.setMaxHeight(MAX_SPAN_OVERLAY_HEIGHT);

        // Align spansAnchor to the same horizontal region as the day panes.
        // dayHeaderGrid is already constrained left=0/right=0 of headerLayer;
        // spansAnchor must match so scene-coordinate conversion stays at offset=0.
        AnchorPane.setTopAnchor(spansAnchor, 0.0);
        AnchorPane.setLeftAnchor(spansAnchor, 0.0);
        AnchorPane.setRightAnchor(spansAnchor, 0.0);

        AnchorPane.setTopAnchor(dayHeaderGrid, MAX_SPAN_OVERLAY_HEIGHT + 2.0);
        AnchorPane.setBottomAnchor(dayHeaderGrid, 0.0);
        AnchorPane.setLeftAnchor(dayHeaderGrid, 0.0);
        AnchorPane.setRightAnchor(dayHeaderGrid, 0.0);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(spansAnchor.widthProperty());
        clip.setHeight(MAX_SPAN_OVERLAY_HEIGHT);
        spansAnchor.setClip(clip);

        // Keep spansAnchor width in sync with dayHeaderGrid so span bars always
        // use the same coordinate origin as the day columns.
        dayHeaderGrid.widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 0) requestStableRender();
        });
        dayHeaderGrid.layoutXProperty().addListener((obs, o, n) -> requestStableRender());
    }

    private void applyGlobalClips() {
        for (AnchorPane pane : getDayPanes()) {
            if (pane != null) {
                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(pane.widthProperty());
                clip.heightProperty().bind(pane.heightProperty());
                pane.setClip(clip);
            }
        }
    }

    private void initStaticDayPanes() {
        if (staticGridInitialized) return;
        staticGridInitialized = true;

        for (AnchorPane pane : getDayPanes()) {
            if (pane != null) {
                pane.getChildren().clear();
                drawTimeGrid(pane);
                applyPaneClip(pane);
            }
        }

        if (spansAnchor != null) {
            spansAnchor.getChildren().clear();
            spansAnchor.setManaged(false);
            spansAnchor.setVisible(true);
            spansAnchor.setMinHeight(MAX_SPAN_OVERLAY_HEIGHT);
            spansAnchor.setPrefHeight(MAX_SPAN_OVERLAY_HEIGHT);
            spansAnchor.setMaxHeight(MAX_SPAN_OVERLAY_HEIGHT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Filters
    // ────────────────────────────────────────────────────────────────────────────

    private void setDefaultFilters() {
        if (personalFilterCheckbox != null) personalFilterCheckbox.setSelected(true);
        if (mergedFilterCheckbox != null) mergedFilterCheckbox.setSelected(false);
        if (slotFilterCheckbox     != null) slotFilterCheckbox.setSelected(true);
        if (spanFilterCheckbox     != null) spanFilterCheckbox.setSelected(true);
        if (deadlineFilterCheckbox != null) deadlineFilterCheckbox.setSelected(true);
        if (markerFilterCheckbox   != null) markerFilterCheckbox.setSelected(true);
    }

    private void initFilterListeners() {
        if (filterListenersInitialized) return;
        filterListenersInitialized = true;

        if (personalFilterCheckbox != null)
            personalFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
        if (mergedFilterCheckbox != null)
            mergedFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
        if (slotFilterCheckbox != null)
            slotFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
        if (spanFilterCheckbox != null)
            spanFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
        if (deadlineFilterCheckbox != null)
            deadlineFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
        if (markerFilterCheckbox != null)
            markerFilterCheckbox.selectedProperty().addListener((obs, o, n) -> refreshCalendar());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Event type helpers
    // ────────────────────────────────────────────────────────────────────────────

    private String normalizeEventType(EventGetRequest event) {
        if (event == null || event.getType() == null) return "slot";

        String raw     = event.getType().trim().toLowerCase();
        String compact = raw.replace('_', ' ').replace('-', ' ').trim();

        if (compact.equals("span")  || compact.equals("phase")
                || compact.equals("phase duration") || compact.equals("duration")
                || compact.equals("multi day")      || compact.equals("multiday")
                || compact.contains("span")         || compact.contains("phase")) {
            return "span";
        }
        if (compact.equals("deadline") || compact.contains("deadline")) return "deadline";
        if (compact.equals("marker")   || compact.contains("marker"))   return "marker";
        if (compact.equals("slot")     || compact.contains("slot"))     return "slot";

        return compact;
    }

    private boolean shouldRenderAsSpan(EventGetRequest event) {
        if (event == null) return false;

        String normalizedType = normalizeEventType(event);
        if ("span".equals(normalizedType)) return true;

        LocalDateTime start = safeParseDateTime(event.getStartDateTime());
        LocalDateTime end   = safeParseDateTime(event.getEndDateTime());

        if (start == null || end == null) return false;
        return end.toLocalDate().isAfter(start.toLocalDate());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Notifications toggle
    // ────────────────────────────────────────────────────────────────────────────

    @FXML
    private void toggleNotifications() {
        if (notifPanel == null) return;
        boolean isVisible = notifPanel.isManaged();
        notifPanel.setVisible(!isVisible);
        notifPanel.setManaged(!isVisible);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Refresh / data loading
    // ────────────────────────────────────────────────────────────────────────────

    public void refresh() {
        allEvents.clear();

        List<EventGetRequest> personalEvents = fetchLocalPersonalEvents();
        List<EventGetRequest> backendEvents  = fetchBackendEvents();

        if (personalEvents != null) allEvents.addAll(personalEvents);
        if (backendEvents  != null) allEvents.addAll(backendEvents);

        List<EventGetRequest> expandedEvents = new ArrayList<>();
        for (EventGetRequest event : allEvents) {
            expandedEvents.addAll(expandRecurringEvent(event));
        }

        allEvents.clear();
        allEvents.addAll(expandedEvents);

        updateWeekHeader();
        renderGroupFilters();
        refreshCalendar();
    }

    private List<EventGetRequest> fetchBackendEvents() {
        List<EventGetRequest> list = GroupEventDataManager.getAll();
        return list != null ? list : new ArrayList<>();
    }

    private List<EventGetRequest> fetchLocalPersonalEvents() {
        List<EventGetRequest> list = EventDataManager.getAll();
        return list != null ? list : new ArrayList<>();
    }

    private List<EventGetRequest> expandRecurringEvent(EventGetRequest event) {
        List<EventGetRequest> expandedEvents = new ArrayList<>();
        if (event == null) return expandedEvents;

        String repeatPattern = event.getRepeatPattern();
        if (repeatPattern == null
                || repeatPattern.equalsIgnoreCase("NO_REPEAT")
                || repeatPattern.isBlank()) {
            expandedEvents.add(event);
            return expandedEvents;
        }

        LocalDateTime start = safeParseDateTime(event.getStartDateTime());
        if (start == null) return expandedEvents;

        LocalDateTime  end         = safeParseDateTime(event.getEndDateTime());
        List<String>   excludedDays= event.getExcludedDays();
        LocalDateTime  currentStart= start;
        LocalDateTime  currentEnd  = end;

        while (currentStart.isBefore(LocalDateTime.now().plusMonths(3))) {
            if (shouldExcludeOccurrence(currentStart.toLocalDate(), excludedDays)) {
                currentStart = getNextOccurrence(currentStart, repeatPattern, event.getMonthlyDate());
                if (currentEnd != null) {
                    currentEnd = getNextOccurrence(currentEnd, repeatPattern, event.getMonthlyDate());
                }
                continue;
            }

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
                    "none",
                    null,
                    null,
                    event.getReminderMinutesBefore(),
                    event.getReminderType(),
                    event.getAttachmentUrl(),
                    event.getCreator(),
                    event.isEditingPermission()
            );

            expandedEvents.add(recurringEvent);

            currentStart = getNextOccurrence(currentStart, repeatPattern, event.getMonthlyDate());
            if (currentEnd != null) {
                currentEnd = getNextOccurrence(currentEnd, repeatPattern, event.getMonthlyDate());
            }
        }

        return expandedEvents;
    }

    private LocalDateTime getNextOccurrence(LocalDateTime current, String pattern, String monthlyDate) {
        switch (pattern.toLowerCase()) {
            case "daily":   return current.plusDays(1);
            case "weekly":  return current.plusWeeks(1);
            case "monthly":
                if (monthlyDate != null && !monthlyDate.isBlank()) {
                    try {
                        int dayOfMonth = Integer.parseInt(monthlyDate);
                        LocalDate nextDate = current.toLocalDate().plusMonths(1);
                        int validDay = Math.min(dayOfMonth, nextDate.lengthOfMonth());
                        return LocalDateTime.of(nextDate.withDayOfMonth(validDay), current.toLocalTime());
                    } catch (NumberFormatException e) {
                        return current.plusMonths(1);
                    }
                }
                return current.plusMonths(1);
            default: return current.plusDays(1);
        }
    }

    private boolean shouldExcludeOccurrence(LocalDate date, List<String> excludedDays) {
        if (excludedDays == null || excludedDays.isEmpty()) return false;
        String dayOfWeek = date.getDayOfWeek().toString().substring(0, 3);
        return excludedDays.contains(dayOfWeek);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Header & group filters
    // ────────────────────────────────────────────────────────────────────────────

    private void updateWeekHeader() {
        if (weekTitleLabel == null) return;

        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        weekTitleLabel.setText(
                currentWeekStart.format(dayMonthFormatter) + " - " + endOfWeek.format(dayMonthFormatter));

        Label[] dateLabels = {
                monDateLabel, tueDateLabel, wedDateLabel, thuDateLabel,
                friDateLabel, satDateLabel, sunDateLabel
        };
        for (int i = 0; i < 7; i++) {
            if (dateLabels[i] != null) {
                dateLabels[i].setText(String.valueOf(currentWeekStart.plusDays(i).getDayOfMonth()));
            }
        }
    }

    private void renderGroupFilters() {
        if (groupFilterContainer == null) return;

        groupFilterContainer.getChildren().clear();
        groupCheckBoxMap.clear();

        Set<String> groups = allEvents.stream()
                .filter(Objects::nonNull)
                .filter(e -> e.getGroup() != null && e.getGroup().getName() != null)
                .map(e -> e.getGroup().getName())
                .collect(Collectors.toCollection(TreeSet::new));

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

    // ────────────────────────────────────────────────────────────────────────────
    //  Calendar refresh / render pipeline
    // ────────────────────────────────────────────────────────────────────────────

    private void refreshCalendar() {
        List<EventGetRequest> filtered = allEvents.stream()
                .filter(Objects::nonNull)
                .filter(this::isEventInCurrentWeek)
                .filter(e -> {
                    boolean isMergedView = mergedFilterCheckbox != null && mergedFilterCheckbox.isSelected();

                    if (isMergedView) {
                        // MERGED VIEW: Show personal events + group events where mergeWithPersonal = true
                        boolean isPersonal = e.getGroup() == null;
                        boolean isMergedGroupEvent = e.getGroup() != null && e.isMergeWithPersonal();
                        return isPersonal || isMergedGroupEvent;
                    }else{
                        boolean isPersonal = e.getGroup() == null;
                        if (isPersonal) {
                            return personalFilterCheckbox == null || personalFilterCheckbox.isSelected();
                        } else {
                            String groupName = e.getGroup().getName();
                            CheckBox cb = groupCheckBoxMap.get(groupName);
                            return cb != null && cb.isSelected();
                        }
                    }
                })
                .filter(e -> {
                    String type = shouldRenderAsSpan(e) ? "span" : normalizeEventType(e);
                    switch (type) {
                        case "slot":     return slotFilterCheckbox     == null || slotFilterCheckbox.isSelected();
                        case "span":     return spanFilterCheckbox     == null || spanFilterCheckbox.isSelected();
                        case "deadline": return deadlineFilterCheckbox == null || deadlineFilterCheckbox.isSelected();
                        case "marker":   return markerFilterCheckbox   == null || markerFilterCheckbox.isSelected();
                        default: return true;
                    }
                })
                .collect(Collectors.toList());

        pendingTimedEvents.clear();
        pendingSpanEvents.clear();

        for (EventGetRequest event : filtered) {
            if (shouldRenderAsSpan(event)) {
                pendingSpanEvents.add(event);
            } else {
                pendingTimedEvents.add(event);
            }
        }

        updateUpcomingEvents(filtered);
        Platform.runLater(this::requestStableRender);
    }

    private void requestStableRender() {
        if (isHovering || isRendering) return;
        scheduleRender();
    }

    private void performStableRender() {
        if (isRendering) return;
        isRendering = true;

        try {
            forceLayoutNow();

            if (calendarGrid == null || dayHeaderGrid == null) return;
            if (calendarGrid.getWidth() <= 0 || dayHeaderGrid.getWidth() <= 0) return;

            if (!areWidthsReady()) {
                widthRetryCount++;
                if (widthRetryCount <= 10) renderDebounce.playFromStart();
                return;
            }

            widthRetryCount = 0;

            clearDynamicContent();
            redrawTimeGridsIfNeeded();
            renderTimedEventsWithCollisionLayout(new ArrayList<>(pendingTimedEvents));
            renderAllSpans(new ArrayList<>(pendingSpanEvents));

            // One-shot correction: re-draw spans on the very next layout pulse so
            // that any residual scene-coordinate drift (spansAnchor not yet at its
            // final position on the first frame) is corrected immediately.
            final List<EventGetRequest> spansSnapshot = new ArrayList<>(pendingSpanEvents);
            Platform.runLater(() -> renderAllSpans(spansSnapshot));
        } finally {
            isRendering = false;
        }
    }

    private void forceLayoutNow() {
        if (calendarContentRoot != null) { calendarContentRoot.applyCss(); calendarContentRoot.layout(); }
        if (dayHeaderGrid       != null) { dayHeaderGrid.applyCss();       dayHeaderGrid.layout();       }
        if (calendarGrid        != null) { calendarGrid.applyCss();        calendarGrid.layout();        }
        if (spansAnchor         != null) { spansAnchor.applyCss();         spansAnchor.layout();         }
        for (AnchorPane pane : getDayPanes()) {
            if (pane != null) { pane.applyCss(); pane.layout(); }
        }
    }

    private boolean areWidthsReady() {
        if (calendarGrid == null || dayHeaderGrid == null) return false;
        if (calendarGrid.getWidth() <= 0 || dayHeaderGrid.getWidth() <= 0) return false;
        for (AnchorPane pane : getDayPanes()) {
            if (pane == null) continue;
            if (pane.getWidth() <= 0) return false;
        }
        return true;
    }

    private void clearDynamicContent() {
        for (AnchorPane pane : getDayPanes()) {
            if (pane == null) continue;
            pane.getChildren().removeIf(node -> !(node instanceof Line));
        }

        if (spansAnchor != null) {
            spansAnchor.getChildren().clear();
            spansAnchor.setManaged(false);
            spansAnchor.setVisible(true);
            spansAnchor.setMinHeight(MAX_SPAN_OVERLAY_HEIGHT);
            spansAnchor.setPrefHeight(MAX_SPAN_OVERLAY_HEIGHT);
            spansAnchor.setMaxHeight(MAX_SPAN_OVERLAY_HEIGHT);
        }
    }

    private void redrawTimeGridsIfNeeded() {
        for (AnchorPane pane : getDayPanes()) {
            if (pane == null) continue;

            double expectedWidth = getSafePaneWidth(pane);
            boolean needRedraw = false;
            int lineCount = 0;

            for (Node node : pane.getChildren()) {
                if (node instanceof Line) {
                    lineCount++;
                    Line line = (Line) node;
                    if (Math.abs(line.getEndX() - expectedWidth) > 1.0) needRedraw = true;
                }
            }

            if (lineCount != ((int) TOTAL_VISIBLE_HOURS + 1)) needRedraw = true;

            if (needRedraw) {
                pane.getChildren().removeIf(node -> node instanceof Line);
                drawTimeGrid(pane);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Timed-event rendering  (slots + markers + deadlines, side-by-side)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Groups all timed events by day, computes each card's pixel Y-position and
     * height, then uses pixel-range collision detection to pack overlapping cards
     * into side-by-side columns without any overlay.
     *
     * Markers and deadlines are treated as point-in-time events visually
     * (SMALL_TIMED_HEIGHT px tall) but participate in the same column-assignment
     * algorithm as slots, so a slot at 9:00 and a marker at 9:00 will always sit
     * next to each other rather than on top of each other.
     */
    private void renderTimedEventsWithCollisionLayout(List<EventGetRequest> timedEvents) {
        // ── Step 1: build render items grouped by day ──────────────────────────
        Map<LocalDate, List<TimedRenderItem>> itemsByDay = new HashMap<>();

        for (EventGetRequest event : timedEvents) {
            if (shouldRenderAsSpan(event)) continue;

            LocalDateTime start = safeParseDateTime(event.getStartDateTime());
            if (start == null) continue;

            String type = normalizeEventType(event);
            LocalDateTime end;
            double height;

            if ("deadline".equals(type) || "marker".equals(type)) {
                end    = start.plusMinutes(30);
                height = SMALL_TIMED_HEIGHT;
            } else {
                end = safeParseDateTime(event.getEndDateTime());
                if (end == null || !end.isAfter(start)) end = start.plusHours(1);
                long mins = java.time.Duration.between(start, end).toMinutes();
                height = Math.max(SLOT_DEFAULT_HEIGHT, (mins / 60.0) * HOUR_HEIGHT);
            }

            AnchorPane pane = getPaneForDate(start.toLocalDate());
            if (pane == null) continue;

            double y = calculateY(start.toLocalTime(), height);
            itemsByDay.computeIfAbsent(start.toLocalDate(), d -> new ArrayList<>())
                    .add(new TimedRenderItem(event, start, end, y, height, type));
        }

        // ── Step 2: for each day, assign columns then render ───────────────────
        for (Map.Entry<LocalDate, List<TimedRenderItem>> entry : itemsByDay.entrySet()) {
            AnchorPane pane = getPaneForDate(entry.getKey());
            if (pane == null) continue;

            // Sort by Y (top), then tallest first, then title for stability
            List<TimedRenderItem> dayItems = entry.getValue().stream()
                    .sorted(Comparator
                            .comparingDouble((TimedRenderItem i) -> i.y)
                            .thenComparingDouble(i -> -i.height)
                            .thenComparing(i -> safeText(i.event.getTitle(), "")))
                    .collect(Collectors.toList());

            // ── Pass A: assign a column index to every item ────────────────────
            // Each item goes into the first column that has NO pixel-overlap with it.
            List<List<TimedRenderItem>> columns = new ArrayList<>();
            for (TimedRenderItem item : dayItems) {
                boolean placed = false;
                for (int c = 0; c < columns.size(); c++) {
                    boolean conflict = false;
                    for (TimedRenderItem seated : columns.get(c)) {
                        if (item.overlaps(seated)) { conflict = true; break; }
                    }
                    if (!conflict) {
                        columns.get(c).add(item);
                        item.column = c;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    item.column = columns.size();
                    List<TimedRenderItem> col = new ArrayList<>();
                    col.add(item);
                    columns.add(col);
                }
            }

            // ── Pass B: for every item find its "group width" ──────────────────
            // Two items are in the same visual group if they pixel-overlap (directly
            // or transitively).  Everyone in a group shares the SAME totalColumns
            // count so all cards in that group have equal width.
            //
            // Implementation: union-find by index in dayItems.
            int n = dayItems.size();
            int[] parent = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;

            // find with path compression
            // (declared as a local helper via index)
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (dayItems.get(i).overlaps(dayItems.get(j))) {
                        // union i and j
                        int ri = i, rj = j;
                        while (parent[ri] != ri) ri = parent[ri];
                        while (parent[rj] != rj) rj = parent[rj];
                        if (ri != rj) parent[ri] = rj;
                    }
                }
            }
            // flatten roots
            for (int i = 0; i < n; i++) {
                int r = i;
                while (parent[r] != r) r = parent[r];
                parent[i] = r;
            }

            // For each group (identified by root), find max column index used
            Map<Integer, Integer> groupMaxCol = new HashMap<>();
            for (int i = 0; i < n; i++) {
                int root = parent[i];
                groupMaxCol.merge(root, dayItems.get(i).column, Math::max);
            }

            // ── Pass C: place cards with group-aware equal widths ──────────────
            double paneWidth = getSafePaneWidth(pane);
            double available = paneWidth - (SLOT_SIDE_PADDING * 2);

            for (int i = 0; i < n; i++) {
                TimedRenderItem item = dayItems.get(i);
                int totalCols = groupMaxCol.get(parent[i]) + 1;

                double spacing   = totalCols > 1 ? Math.max(1.0, Math.min(CARD_SPACING, available / totalCols * 0.05)) : 0.0;
                double totalGap  = spacing * (totalCols - 1);
                double cardWidth = Math.max(MIN_CARD_WIDTH, (available - totalGap) / totalCols);
                double x         = SLOT_SIDE_PADDING + item.column * (cardWidth + spacing);

                VBox card = createCardForTimedItem(item);
                card.setPrefWidth(cardWidth);
                card.setMinWidth(cardWidth);
                card.setMaxWidth(cardWidth);
                card.setPrefHeight(item.height);
                card.setMinHeight(item.height);
                card.setMaxHeight(item.height);
                card.setLayoutX(x);
                card.setLayoutY(item.y);
                pane.getChildren().add(card);
            }
        }
    }

    private VBox createCardForTimedItem(TimedRenderItem item) {
        if ("deadline".equals(item.type)) {
            return createDeadlineMarkerCard(item.event, true, getEventColor(item.event));
        }
        if ("marker".equals(item.type)) {
            return createDeadlineMarkerCard(item.event, false, getEventColor(item.event));
        }
        return createSlotCard(item.event, item.start, item.end);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Smooth hover animation helper
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Attaches instant style-swap hover to a node (no scale/opacity animation).
     */
    private void addSmoothHover(Region node,
                                String baseStyle, String hoverStyle) {
        node.setOnMouseEntered(e -> node.setStyle(hoverStyle));
        node.setOnMouseExited(e  -> node.setStyle(baseStyle));
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Card builders
    // ────────────────────────────────────────────────────────────────────────────

    private VBox createSlotCard(EventGetRequest event, LocalDateTime start, LocalDateTime end) {
        String color = getEventColor(event);

        // Outer wrapper provides the colored left accent stripe + rounded corners
        VBox card = new VBox(0);
        card.getStyleClass().add("slot-card");
        card.setFillWidth(true);
        card.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.13) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-border-radius: 0 6 6 0;" +
                        "-fx-background-radius: 0 6 6 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);"
        );

        // Inner content pane with padding
        VBox inner = new VBox(3);
        inner.setPadding(new Insets(5, 7, 5, 8));
        inner.setFillWidth(true);

        Label titleLabel = new Label(safeText(event.getTitle(), "Untitled Event"));
        titleLabel.getStyleClass().add("event-title");
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-font-size: 11.5px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-wrap-text: true;"
        );

        Label timeLabel = new Label("🕐 " + formatEventTime(start, end));
        timeLabel.getStyleClass().add("event-time");
        timeLabel.setWrapText(false);
        timeLabel.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-text-fill: rgba(50,50,50,0.75);"
        );

        inner.getChildren().addAll(titleLabel, timeLabel);

        if (event.getGroup() != null && event.getGroup().getName() != null) {
            Label groupLabel = new Label("👥 " + event.getGroup().getName());
            groupLabel.getStyleClass().add("event-group");
            groupLabel.setWrapText(false);
            groupLabel.setStyle(
                    "-fx-font-size: 9.5px;" +
                            "-fx-text-fill: rgba(50,50,50,0.60);" +
                            "-fx-font-style: italic;"
            );
            inner.getChildren().add(groupLabel);
        }

        card.getChildren().add(inner);

        String baseStyle =
                "-fx-background-color: " + hexToRgba(color, 0.13) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-border-radius: 0 6 6 0;" +
                        "-fx-background-radius: 0 6 6 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 2);";
        String hoverStyle =
                "-fx-background-color: " + hexToRgba(color, 0.24) + ";" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-border-radius: 0 6 6 0;" +
                        "-fx-background-radius: 0 6 6 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 10, 0, 0, 3);";

        card.setStyle(baseStyle);
        addSmoothHover(card, baseStyle, hoverStyle);
        card.setOnMouseClicked(e -> openEventDetails(event));
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            // Delay resetting the flag to prevent rapid re-renders
            Platform.runLater(() -> isHovering = false);
        });
        return card;
    }

    private VBox createDeadlineMarkerCard(EventGetRequest event, boolean isDeadline, String color) {
        VBox card = new VBox(0);
        card.getStyleClass().add(isDeadline ? "deadline-card" : "marker-card");
        card.setFillWidth(true);

        // Colored top banner that fills full width
        HBox banner = new HBox(6);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(4, 8, 4, 8));
        banner.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 5 5 0 0;"
        );

        Label iconLabel = new Label(isDeadline ? "⏰" : "📍");
        iconLabel.setStyle("-fx-font-size: 11px;");

        Label typeLabel = new Label(isDeadline ? "DEADLINE" : "REMINDER");
        typeLabel.setStyle(
                "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-letter-spacing: 0.5px;"
        );
        banner.getChildren().addAll(iconLabel, typeLabel);

        // Body area
        VBox body = new VBox(3);
        body.setPadding(new Insets(5, 8, 5, 8));
        body.setStyle(
                "-fx-background-color: " + hexToRgba(color, 0.10) + ";" +
                        "-fx-background-radius: 0 0 5 5;"
        );

        Label titleLabel = new Label(safeText(event.getTitle(), "Untitled"));
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: rgba(30,30,30,0.90);"
        );

        LocalDateTime target = safeParseDateTime(event.getStartDateTime());
        Label timeLabel = new Label((isDeadline ? "Due: " : "At: ") +
                (target != null ? target.format(timeFormatter) : "--"));
        timeLabel.setStyle(
                "-fx-font-size: 10px;" +
                        "-fx-text-fill: rgba(50,50,50,0.70);"
        );
        timeLabel.setWrapText(false);

        body.getChildren().addAll(titleLabel, timeLabel);
        card.getChildren().addAll(banner, body);

        String dmBase =
                "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 5, 0, 0, 2);";
        String dmHover =
                "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 3);";

        card.setStyle(dmBase);
        addSmoothHover(card, dmBase, dmHover);
        card.setOnMouseClicked(e -> {
            System.out.println("Opening deadline card");
            openEventDetails(event);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(dmBase);
            // Delay resetting the flag to prevent rapid re-renders
            Platform.runLater(() -> isHovering = false);
        });
        return card;
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Span rendering  (header bar area, no overlaps)
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Renders multi-day span bars in the header area.
     * {@link #assignSpanRows} ensures that no two overlapping spans share a row,
     * so bars are always displayed side-by-side (stacked vertically) rather than
     * on top of each other.  Only the first MAX_VISIBLE_SPAN_ROWS rows are drawn.
     */
    private void renderAllSpans(List<EventGetRequest> spanEvents) {
        if (spansAnchor == null) return;

        spansAnchor.getChildren().clear();

        if (spanEvents.isEmpty()) return;

        List<SpanRenderItem> items   = new ArrayList<>();
        LocalDate            weekEnd = currentWeekStart.plusDays(6);

        for (EventGetRequest event : spanEvents) {
            if (!shouldRenderAsSpan(event)) continue;

            LocalDateTime start = safeParseDateTime(event.getStartDateTime());
            if (start == null) continue;

            LocalDateTime end = safeParseDateTime(event.getEndDateTime());
            if (end == null || end.isBefore(start)) end = start;

            // Clamp display range to the visible week
            LocalDate displayStart = start.toLocalDate().isBefore(currentWeekStart)
                    ? currentWeekStart : start.toLocalDate();
            LocalDate displayEnd   = end.toLocalDate().isAfter(weekEnd)
                    ? weekEnd : end.toLocalDate();

            if (displayEnd.isBefore(currentWeekStart) || displayStart.isAfter(weekEnd)) continue;

            int startDayIndex = (int) ChronoUnit.DAYS.between(currentWeekStart, displayStart);
            int endDayIndex   = (int) ChronoUnit.DAYS.between(currentWeekStart, displayEnd);

            items.add(new SpanRenderItem(event, start, end, startDayIndex, endDayIndex));
        }

        // Sort: leftmost first, widest first, then alphabetically
        items.sort(Comparator
                .comparingInt((SpanRenderItem s) -> s.startDayIndex)
                .thenComparingInt(s -> -(s.endDayIndex - s.startDayIndex))
                .thenComparing(s -> safeText(s.event.getTitle(), "")));

        // Assign non-overlapping rows
        assignSpanRows(items);

        Map<Integer, DayColumnBounds> dayColumns = getActualDayColumnBounds();
        if (dayColumns.isEmpty()) return;

        for (SpanRenderItem item : items) {
            if (item.row >= MAX_VISIBLE_SPAN_ROWS) continue;   // only render up to max rows

            DayColumnBounds startCol = dayColumns.get(item.startDayIndex);
            DayColumnBounds endCol   = dayColumns.get(item.endDayIndex);
            if (startCol == null || endCol == null) continue;

            double barLeft = startCol.minX + SPAN_SIDE_PADDING + SPAN_START_SHIFT+120;
            double barRight = endCol.maxX - SPAN_SIDE_PADDING;

            if (barRight <= barLeft) continue;

            double width = barRight - barLeft+90;

            double y        = SPAN_TOP_PADDING + item.row * (SPAN_ROW_HEIGHT + SPAN_ROW_GAP);

            HBox spanBar = createSpanBar(item.event, item.start, item.end,
                    item.startDayIndex, item.endDayIndex);
            spanBar.setLayoutX(barLeft);
            spanBar.setLayoutY(y);
            spanBar.setPrefWidth(width);
            spanBar.setMinWidth(width);
            spanBar.setMaxWidth(width);
            spanBar.setPrefHeight(SPAN_ROW_HEIGHT);
            spanBar.setMinHeight(SPAN_ROW_HEIGHT);
            spanBar.setMaxHeight(SPAN_ROW_HEIGHT);

            spansAnchor.getChildren().add(spanBar);
        }

        spansAnchor.setVisible(true);
        spansAnchor.toFront();
    }

    /**
     * Computes each day column's X-range in spansAnchor local coordinates.
     *
     * dayHeaderGrid and spansAnchor are siblings inside headerLayer, so the
     * only transform needed is dayHeaderGrid-local → headerLayer-local →
     * spansAnchor-local. This is immune to the cross-branch scene-coordinate
     * drift that happens when spansAnchor and the calendarGrid day panes live
     * in separate layout subtrees.
     *
     * The day header cells (column 0..6) inside dayHeaderGrid each correspond
     * to exactly one day.  We walk dayHeaderGrid's children, find the nodes at
     * column indices 0-6, and map their bounds into spansAnchor local space.
     *
     * Fallback: if dayHeaderGrid children are not yet laid out we accumulate
     * the raw day-pane widths — the debounce will fire a corrective re-render.
     */
    private Map<Integer, DayColumnBounds> getActualDayColumnBounds() {
        Map<Integer, DayColumnBounds> result = new HashMap<>();
        if (spansAnchor == null) return result;

        // ── Primary path: use dayHeaderGrid column cells ───────────────────────
        if (dayHeaderGrid != null && dayHeaderGrid.getWidth() > 0
                && spansAnchor.getScene() != null) {

            // Build a map from GridPane column-index → the first child node at that column
            Map<Integer, Node> colNodes = new HashMap<>();
            for (Node child : dayHeaderGrid.getChildren()) {
                Integer col = GridPane.getColumnIndex(child);
                if (col == null) col = 0;
                colNodes.putIfAbsent(col, child);
            }

            // If we found column cells, map each one into spansAnchor coords
            if (!colNodes.isEmpty()) {
                for (int i = 0; i < 7; i++) {
                    Node cell = colNodes.get(i);
                    if (cell == null) continue;

                    // cell local bounds → scene → spansAnchor local
                    Bounds  cellScene = cell.localToScene(cell.getBoundsInLocal());
                    Point2D lo = spansAnchor.sceneToLocal(cellScene.getMinX(), cellScene.getMinY());
                    Point2D hi = spansAnchor.sceneToLocal(cellScene.getMaxX(), cellScene.getMinY());
                    result.put(i, new DayColumnBounds(lo.getX(), hi.getX()));
                }
                if (result.size() == 7) return result;
                result.clear(); // partial result — fall through to fallback
            }
        }

        // ── Fallback path: accumulate day-pane widths (pre-layout only) ────────
        double cursor = 0;
        for (int i = 0; i < 7; i++) {
            AnchorPane pane = getPaneByIndex(i);
            double w = getSafePaneWidth(pane);
            result.put(i, new DayColumnBounds(cursor, cursor + w));
            cursor += w;
        }
        return result;
    }

    /**
     * Greedy row assignment for spans.  Each span is placed in the lowest-indexed
     * row that has no day-index conflict with any span already in that row.
     */
    private void assignSpanRows(List<SpanRenderItem> items) {
        List<List<SpanRenderItem>> rows = new ArrayList<>();

        for (SpanRenderItem item : items) {
            boolean placed = false;

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                boolean rowConflict = false;
                for (SpanRenderItem existing : rows.get(rowIndex)) {
                    if (item.overlaps(existing)) {
                        rowConflict = true;
                        break;
                    }
                }
                if (!rowConflict) {
                    item.row = rowIndex;
                    rows.get(rowIndex).add(item);
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                item.row = rows.size();
                List<SpanRenderItem> newRow = new ArrayList<>();
                newRow.add(item);
                rows.add(newRow);
            }
        }
    }

    private HBox createSpanBar(EventGetRequest event, LocalDateTime start, LocalDateTime end,
                               int startDayIndex, int endDayIndex) {
        String color = getEventColor(event);

        // Determine if this bar is clipped by the week boundary
        boolean continuesFromLeft  = startDayIndex == 0 && start.toLocalDate().isBefore(currentWeekStart);
        boolean continuesToRight   = endDayIndex   == 6 && end.toLocalDate().isAfter(currentWeekStart.plusDays(6));

        // Corner radii: flat on the side that continues beyond the week
        String radii = (continuesFromLeft ? "0" : "4") + " " +
                (continuesToRight  ? "0" : "4") + " " +
                (continuesToRight  ? "0" : "4") + " " +
                (continuesFromLeft ? "0" : "4");

        HBox spanBar = new HBox(5);
        spanBar.setAlignment(Pos.CENTER_LEFT);
        spanBar.getStyleClass().add("span-bar");
        spanBar.setPadding(new Insets(0, 8, 0, 8));

        // Left "◀" if continuing from a previous week
        if (continuesFromLeft) {
            Label leftArrow = new Label("◀");
            leftArrow.setStyle("-fx-font-size: 7px; -fx-text-fill: rgba(255,255,255,0.70);");
            spanBar.getChildren().add(leftArrow);
        }

        VBox content = new VBox(1);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLabel = new Label(safeText(event.getTitle(), "Untitled Span"));
        titleLabel.getStyleClass().add("span-bar-title");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setWrapText(false);
        titleLabel.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-text-overrun: ellipsis;"
        );

        String dateRange = formatSpanDateRange(start, end);
        if (dateRange != null && !dateRange.isBlank()) {
            Label dateLabel = new Label(dateRange);
            dateLabel.getStyleClass().add("span-bar-date");
            dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: rgba(255,255,255,0.82);");
            content.getChildren().addAll(titleLabel, dateLabel);
        } else {
            content.getChildren().add(titleLabel);
        }
        spanBar.getChildren().add(content);

        // Right "▶" if continuing into the next week
        if (continuesToRight) {
            Label rightArrow = new Label("▶");
            rightArrow.setStyle("-fx-font-size: 7px; -fx-text-fill: rgba(255,255,255,0.70);");
            spanBar.getChildren().add(rightArrow);
        }

        String baseStyle =
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + radii + ";" +
                        "-fx-border-radius: " + radii + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 4, 0, 0, 1);";

        String hoverStyle =
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: " + radii + ";" +
                        "-fx-border-radius: " + radii + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 0.88;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.32), 8, 0, 0, 2);";

        spanBar.setStyle(baseStyle);
        addSmoothHover(spanBar, baseStyle, hoverStyle);
        spanBar.setOnMouseClicked(e -> openEventDetails(event));
        spanBar.setOnMouseExited(e -> {
            spanBar.setStyle(baseStyle);
            // Delay resetting the flag to prevent rapid re-renders
            Platform.runLater(() -> isHovering = false);
        });
        return spanBar;
    }

    private String formatSpanDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd");
        if (start == null) return "";
        if (end == null) return start.format(dateFormatter);
        if (start.toLocalDate().equals(end.toLocalDate())) return start.format(dateFormatter);
        return start.format(dateFormatter) + " - " + end.format(dateFormatter);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Upcoming events sidebar
    // ────────────────────────────────────────────────────────────────────────────

    private void updateUpcomingEvents(List<EventGetRequest> events) {
        if (upcomingContainer == null) return;

        upcomingContainer.getChildren().clear();

        List<EventGetRequest> upcoming = events.stream()
                .filter(Objects::nonNull)
                .filter(e -> {
                    LocalDateTime start = safeParseDateTime(e.getStartDateTime());
                    return start != null && start.isAfter(LocalDateTime.now().minusMinutes(1));
                })
                .sorted(Comparator.comparing(e -> safeParseDateTime(e.getStartDateTime())))
                .limit(5)
                .collect(Collectors.toList());

        if (upcoming.isEmpty()) {
            Label label = new Label("No upcoming events");
            label.getStyleClass().add("no-upcoming-label");
            upcomingContainer.getChildren().add(label);
            return;
        }

        for (EventGetRequest event : upcoming) {
            String evColor = getEventColor(event);
            String evType  = shouldRenderAsSpan(event) ? "span" : normalizeEventType(event);
            String typeIcon = "slot".equals(evType) ? "🗓" :
                    "span".equals(evType) ? "📅" :
                            "deadline".equals(evType) ? "⏰" : "📍";

            VBox box = new VBox(4);
            box.setPadding(new Insets(8, 10, 8, 12));
            box.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.80);" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: " + evColor + ";" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 3, 0, 0, 1);"
            );

            HBox titleRow = new HBox(5);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            Label iconLbl = new Label(typeIcon);
            iconLbl.setStyle("-fx-font-size: 11px;");
            Label title = new Label(safeText(event.getTitle(), "Untitled Event"));
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 11.5px; -fx-font-weight: bold; -fx-text-fill: rgba(20,20,20,0.88);");
            titleRow.getChildren().addAll(iconLbl, title);
            box.getChildren().add(titleRow);

            LocalDateTime start = safeParseDateTime(event.getStartDateTime());
            Label time = new Label(start != null
                    ? start.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"))
                    : "--");
            time.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(80,80,80,0.75);");

            box.getChildren().add(time);
            box.setOnMouseClicked(e -> openEventDetails(event));

            upcomingContainer.getChildren().add(box);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Utility helpers
    // ────────────────────────────────────────────────────────────────────────────

    private void applyPaneClip(AnchorPane pane) {
        if (pane == null) return;
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
    }

    private double getSafePaneWidth(AnchorPane pane) {
        if (pane == null) return 110;
        double width = pane.getWidth();
        if (width <= 0) width = pane.prefWidth(-1);
        if (width <= 0) width = pane.getMinWidth();
        if (width <= 0) width = 110;
        return width;
    }

    private String getEventColor(EventGetRequest event) {
        String type = normalizeEventType(event);
        if (shouldRenderAsSpan(event)) type = "span";

        if (event != null && event.getColor() != null && !event.getColor().isBlank()) {
            return event.getColor();
        }
        return DEFAULT_COLORS.getOrDefault(type, DEFAULT_COLORS.get("slot"));
    }

    private void drawTimeGrid(AnchorPane pane) {
        if (pane == null) return;
        double width = getSafePaneWidth(pane);
        for (int i = 0; i <= TOTAL_VISIBLE_HOURS; i++) {
            Line line = new Line(0, i * HOUR_HEIGHT + SLOT_TOP_OFFSET,
                    width, i * HOUR_HEIGHT + SLOT_TOP_OFFSET);
            line.getStyleClass().add("day-pane-line");
            pane.getChildren().add(line);
        }
    }

    private double calculateY(LocalTime time, double cardHeight) {
        double hourPart   = time.getHour()   - DAY_START_HOUR;
        double minutePart = time.getMinute() / 60.0;
        double y          = (hourPart + minutePart) * HOUR_HEIGHT + SLOT_TOP_OFFSET;
        double maxY       = (TOTAL_VISIBLE_HOURS * HOUR_HEIGHT) - cardHeight;
        return Math.max(SLOT_TOP_OFFSET, Math.min(y, maxY));
    }

    private String formatEventTime(LocalDateTime start, LocalDateTime end) {
        if (start == null) return "--";
        if (end   == null) return start.format(timeFormatter);
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return start.format(timeFormatter) + " - " + end.format(timeFormatter);
        }
        return start.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"))
                + " - "
                + end.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
    }

    private boolean isEventInCurrentWeek(EventGetRequest e) {
        LocalDateTime startDateTime = safeParseDateTime(e.getStartDateTime());
        if (startDateTime == null) return false;

        LocalDate weekStart = currentWeekStart;
        LocalDate weekEnd   = currentWeekStart.plusDays(6);
        LocalDate startDate = startDateTime.toLocalDate();

        LocalDateTime endDateTime = safeParseDateTime(e.getEndDateTime());
        LocalDate endDate = endDateTime != null ? endDateTime.toLocalDate() : startDate;

        return !endDate.isBefore(weekStart) && !startDate.isAfter(weekEnd);
    }

    private AnchorPane getPaneForDate(LocalDate date) {
        if (date == null) return null;
        long daysBetween = ChronoUnit.DAYS.between(currentWeekStart, date);
        switch ((int) daysBetween) {
            case 0: return mondayPane;
            case 1: return tuesdayPane;
            case 2: return wednesdayPane;
            case 3: return thursdayPane;
            case 4: return fridayPane;
            case 5: return saturdayPane;
            case 6: return sundayPane;
            default: return null;
        }
    }

    private LocalDateTime safeParseDateTime(String value) {
        try {
            if (value == null || value.isBlank()) return null;
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            try {
                String normalized = value.trim().replace(' ', 'T');
                if (normalized.endsWith("Z")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                int plusIndex = normalized.indexOf('+');
                if (plusIndex > 0) normalized = normalized.substring(0, plusIndex);
                return LocalDateTime.parse(normalized);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String safeText(String text, String fallback) {
        return (text == null || text.isBlank()) ? fallback : text;
    }

    private String hexToRgba(String hex, double opacity) {
        try {
            if (hex == null || !hex.startsWith("#") || hex.length() < 7) {
                return "rgba(100,100,100,0.10)";
            }
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            return String.format("rgba(%d,%d,%d,%.2f)", r, g, b, opacity);
        } catch (Exception e) {
            return "rgba(100,100,100,0.10)";
        }
    }

    private void openEventDetails(EventGetRequest event) {
        System.out.println("Opening details");
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/planify/frontend/fxmls/event-detail-view.fxml"));
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

    // ────────────────────────────────────────────────────────────────────────────
    //  FXML action handlers  (names unchanged)
    // ────────────────────────────────────────────────────────────────────────────

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
    public void handleDashboard() {
        SceneManager.switchScene("dashboard-view.fxml", "Dashboard");
    }

    @FXML
    public void handleNewEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/planify/frontend/fxmls/add-event-view.fxml"));
            Parent root = loader.load();

            AddEventController controller = loader.getController();
            controller.setParent(this);

            Stage stage = new Stage();
            stage.setTitle("Add Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            Platform.runLater(() -> {
                refresh();
                requestStableRender();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
