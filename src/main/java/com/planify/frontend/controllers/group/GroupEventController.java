package com.planify.frontend.controllers.group;

import com.planify.frontend.controllers.events.AddEventController;
import com.planify.frontend.models.group.EventSummary;
import com.planify.frontend.utils.managers.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GroupEventController {

    @FXML private GridPane eventsGrid;
    private GroupDetailsController parent;
    private String groupUuid;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a");

    @FXML
    private void initialize() {
        // Initialization
    }

    public void setParent(GroupDetailsController parent) {
        this.parent = parent;
    }

    public void setGroupUuid(String uuid) {
        this.groupUuid = uuid;
    }

    public void setEvents(List<EventSummary> events) {
        eventsGrid.getChildren().clear();
        int row = 0, col = 0;
        for (EventSummary event : events) {
            VBox card = createEventCard(event);
            eventsGrid.add(card, col, row);
            GridPane.setMargin(card, new Insets(4));

            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }

        // Show empty state if no events
        if (events.isEmpty()) {
            VBox emptyState = createEmptyState();
            eventsGrid.add(emptyState, 0, 0, 2, 1);
        }
    }

    private VBox createEventCard(EventSummary event) {
        VBox card = new VBox(16);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("event-card");
        card.setPadding(new Insets(24));

        // Top row: icon + badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(pickIcon(event.getType()));
        iconLabel.getStyleClass().add("event-icon");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label badgeLabel = new Label(event.getType().toUpperCase());
        badgeLabel.getStyleClass().add(pickBadgeClass(event.getType()));

        topRow.getChildren().addAll(iconLabel, spacer1, badgeLabel);

        // Middle: title + description
        VBox infoBox = new VBox(8);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("event-title");

        Label descLabel = new Label(event.getDescription());
        descLabel.getStyleClass().add("event-desc");
        descLabel.setWrapText(true);

        infoBox.getChildren().addAll(titleLabel, descLabel);

        // Bottom: date + view button
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        String dateText = formatEventDate(event);
        Label dateLabel = new Label(dateText);
        dateLabel.getStyleClass().add("event-date");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button viewBtn = new Button("View Details →");
        viewBtn.getStyleClass().add("event-view-btn");
        viewBtn.setOnAction(e -> openEventDetails(event));

        bottomRow.getChildren().addAll(dateLabel, spacer2, viewBtn);

        card.getChildren().addAll(topRow, infoBox, bottomRow);
        return card;
    }

    private String formatEventDate(EventSummary event) {
        if (event.getStartDateTime() == null) return "Date TBD";

        StringBuilder sb = new StringBuilder("🗓️ ");
        sb.append(event.getStartDateTime());

        if (event.getEndDateTime() != null && !event.getEndDateTime().equals(event.getStartDateTime())) {
            sb.append(" → ").append(event.getEndDateTime());
        }

        return sb.toString();
    }

    private String pickIcon(String type) {
        return switch (type.toLowerCase()) {
            case "slot" -> "📅";
            case "deadline" -> "⏰";
            case "meeting" -> "🤝";
            case "workshop" -> "🔧";
            case "dinner" -> "🍽️";
            default -> "📌";
        };
    }

    private String pickBadgeClass(String type) {
        return switch (type.toLowerCase()) {
            case "slot" -> "event-badge-slot";
            case "deadline" -> "event-badge-deadline";
            case "meeting" -> "event-badge-meeting";
            default -> "event-badge-slot";
        };
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(60));
        emptyState.getStyleClass().add("empty-state");

        Label iconLabel = new Label("📅");
        iconLabel.getStyleClass().add("empty-icon");

        Label textLabel = new Label("No events scheduled");
        textLabel.getStyleClass().add("empty-text");

        Label subLabel = new Label("Create an event to get started");
        subLabel.getStyleClass().add("empty-text");
        subLabel.setStyle("-fx-font-size: 12px;");

        emptyState.getChildren().addAll(iconLabel, textLabel, subLabel);
        return emptyState;
    }

    @FXML
    private void openAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/com/planify/frontend/fxmls/add-event-view.fxml"));
            Parent root = loader.load();

            AddEventController controller = loader.getController();
            controller.setGroupUuid(groupUuid);
            controller.setParent(parent);

            Stage stage = new Stage();
            stage.setTitle("Add Event");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openEventDetails(EventSummary event) {
        // TODO: Open event details dialog
        System.out.println("Opening event: " + event.getTitle());
    }
}