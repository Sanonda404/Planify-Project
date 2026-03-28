module com.planify.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires spring.messaging;
    requires spring.websocket;

    opens com.planify.frontend to javafx.fxml;
    exports com.planify.frontend;
    requires java.net.http;
    requires org.json;
    requires com.google.gson;
    requires javafx.graphics;
    requires java.desktop;
    requires annotations;
    requires com.fasterxml.jackson.databind;
    requires spring.amqp;
    requires javafx.media;
    requires org.jspecify;

    opens com.planify.frontend.controllers to javafx.fxml;
    opens com.planify.frontend.controllers.group to javafx.fxml;
    opens com.planify.frontend.controllers.Request to javafx.fxml;
    opens com.planify.frontend.models to javafx.fxml;
    opens com.planify.frontend.models.group to com.google.gson;
    opens com.planify.frontend.models.project to com.google.gson;
    opens com.planify.frontend.models.resources to com.google.gson;
    opens com.planify.frontend.controllers.task to javafx.fxml;
    opens com.planify.frontend.controllers.project to com.google.gson, javafx.fxml;
    opens com.planify.frontend.controllers.bot to com.google.gson, javafx.fxml;
    opens com.planify.frontend.models.tasks to com.google.gson;
    opens com.planify.frontend.controllers.events to javafx.fxml;
    opens com.planify.frontend.models.auth to com.google.gson;
    opens com.planify.frontend.models.milestone to com.google.gson;
    opens com.planify.frontend.models.events to com.google.gson;
    opens com.planify.frontend.controllers.auth to javafx.fxml;
    opens com.planify.frontend.controllers.resources to javafx.fxml;
    opens com.planify.frontend.models.notification to com.google.gson;
    opens com.planify.frontend.utils.managers to javafx.fxml;
    opens com.planify.frontend.utils.services to javafx.fxml;
}