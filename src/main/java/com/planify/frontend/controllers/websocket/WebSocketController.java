package com.planify.frontend.controllers.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.planify.frontend.controllers.notification.NotificationController;
import com.planify.frontend.models.auth.MemberInfo;
import com.planify.frontend.models.notification.NotificationResponse;
import com.planify.frontend.utils.LocalDateTimeAdapter;
import com.planify.frontend.utils.MemberInfoAdapter;
import com.planify.frontend.utils.UserSession;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class WebSocketController {

    private StompSession stompSession;
    private final WebSocketStompClient stompClient;
    private final String url = "ws://localhost:8000/wss";
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isConnecting = false;

    public WebSocketController() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        // Create a Gson instance that can handle your "createdAt" dates
        Gson gson = new GsonBuilder().
                registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).
                registerTypeAdapter(MemberInfo.class, new MemberInfoAdapter())
                .create();

        // Use the Gson version of the converter
        GsonMessageConverter converter = new GsonMessageConverter();
        converter.setGson(gson);

        this.stompClient.setMessageConverter(converter);
    }

    /**
     * Call this once when the user logs in or the app starts.
     * It will manage the connection forever.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndConnect, 0, 10, TimeUnit.SECONDS);
    }

    private void checkAndConnect() {
        if ((stompSession == null || !stompSession.isConnected()) && !isConnecting) {
            System.out.println("[WS] Connection lost or not started. Attempting to connect...");
            connect();
        }
    }

    private void connect() {
        isConnecting = true;
        String jwt = UserSession.getInstance().getToken();

        if (jwt == null) {
            System.out.println("[WS] No JWT found. Skipping connection.");
            isConnecting = false;
            return;
        }

        StompHeaders headers = new StompHeaders();
        headers.add("Authorization", "Bearer " + jwt);

        stompClient.connectAsync(url, (WebSocketHttpHeaders) null, headers, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(@NotNull StompSession session, @NotNull StompHeaders connectedHeaders) {
                stompSession = session;
                isConnecting = false;
                System.out.println("[WS] Connected successfully!");

                // Update UI status
                Platform.runLater(() -> NotificationController.updateStatus(true));

                subscribeToNotifications();
            }

            @Override
            public void handleException(@NotNull StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("[WS] Error: " + exception.getMessage());
                isConnecting = false;
            }

            @Override
            public void handleTransportError(@NotNull StompSession session, @NotNull Throwable exception) {
                System.err.println("[WS] Transport Error (Internet likely down)");
                isConnecting = false;
                Platform.runLater(() -> NotificationController.updateStatus(false));
            }
        });
    }

    private void subscribeToNotifications() {
        String email = UserSession.getInstance().getEmail();
        String topic = "/topic/notifications/" + email;

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            @NotNull
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return NotificationResponse.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                NotificationResponse notification = (NotificationResponse) payload;
                System.out.println("[WS] Received: " + notification.getMessage());

                // Push to UI
                Platform.runLater(() -> {
                    NotificationController.handleNewNotification(notification);
                });
            }
        });
        System.out.println("[WS] Subscribed to " + topic);
    }
}