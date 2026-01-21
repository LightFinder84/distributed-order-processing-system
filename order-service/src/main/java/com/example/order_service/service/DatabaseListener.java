package com.example.order_service.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for listening to database events via PostgreSQL LISTEN/NOTIFY.
 *
 * <p>This service establishes persistent connections to the PostgreSQL database
 * and listens for NOTIFY events from the inbox and outbox tables. When changes
 * occur in these tables, the database sends notifications through dedicated
 * channels, triggering event processing.</p>
 *
 * <p>The service manages two separate listener threads:</p>
 * <ul>
 *   <li>Inbox Listener - Listens for incoming events from other services and
 *       triggers event processing through {@link EventProcessor}</li>
 *   <li>Outbox Listener - Listens for outgoing events to be published and
 *       triggers event publishing through {@link EventProducer}</li>
 * </ul>
 *
 * <p>Both listeners are resilient to database connection failures and will
 * automatically attempt to reconnect after a configurable period.</p>
 *
 * <p>This implementation uses PostgreSQL's built-in LISTEN/NOTIFY mechanism
 * for efficient, near real-time event processing without polling the database.</p>
 *
 * @see EventProcessor
 * @see EventProducer
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class DatabaseListener {

    /**
     * PostgreSQL database connection URL from Spring configuration.
     */
    @Value("${spring.datasource.url}")
    private String url;

    /**
     * Database username for establishing connections.
     */
    @Value("${spring.datasource.username}")
    private String user;

    /**
     * Database password for establishing connections.
     */
    @Value("${spring.datasource.password}")
    private String password;

    /**
     * Service for processing events received from the inbox.
     */
    @Autowired
    private EventProcessor eventProcessor;

    /**
     * Service for producing and publishing events to the outbox.
     */
    @Autowired
    private EventProducer eventProducer;

    /**
     * Executor service for managing listener threads and processing tasks.
     * Configured with a fixed thread pool of 2 threads for concurrent event handling.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    /**
     * Timeout in milliseconds for waiting on database notifications.
     */
    private static final int DATABASE_CONNECTION_TIMEOUT = 5000;

    /**
     * Delay in milliseconds before attempting to reconnect after a connection failure.
     */
    private static final int CONNECTION_RETRY_PERIOD = 5000;

    /**
     * Initializes the database listeners.
     *
     * <p>This method is called automatically after the bean is constructed.
     * It starts both the inbox and outbox listener threads which continuously
     * monitor their respective database channels.</p>
     *
     * @throws Exception if there is an error during initialization
     */
    @PostConstruct
    public final void init() throws Exception {
        startInboxListener();
        startOutboxListener();
    }

    /**
     * Starts the outbox listener thread.
     *
     * <p>This method creates and starts a daemon thread that continuously listens
     * to the "outbox_channel" PostgreSQL channel. When the database sends notifications,
     * it triggers the event producer to process and publish outbox events.</p>
     *
     * <p>The listener maintains a persistent connection to the database and handles
     * connection failures by attempting to reconnect after the configured delay.</p>
     */
    private void startOutboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN outbox_channel");
                    }

                    eventProducer.processOutbox();

                    // get notification
                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConn.getNotifications(DATABASE_CONNECTION_TIMEOUT);
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(eventProducer::processOutbox);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Database connection dropped, retry in 5 seconds... " + e.getMessage());
                    try {
                        Thread.sleep(CONNECTION_RETRY_PERIOD);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Starts the inbox listener thread.
     *
     * <p>This method creates and starts a daemon thread that continuously listens
     * to the "inbox_channel" PostgreSQL channel. When the database sends notifications,
     * it triggers the event processor to process incoming events.</p>
     *
     * <p>The listener maintains a persistent connection to the database and handles
     * connection failures by attempting to reconnect after the configured delay.</p>
     */
    private void startInboxListener() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN inbox_channel");
                    }

                    eventProcessor.processEvents();

                    while (!Thread.currentThread().isInterrupted()) {
                        PGNotification[] notifications = pgConn.getNotifications(DATABASE_CONNECTION_TIMEOUT);
                        if (notifications != null && notifications.length > 0) {
                            executorService.submit(eventProcessor::processEvents);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Mất kết nối DB Listener, đang thử lại sau 5 giây... " + e.getMessage());
                    try {
                        Thread.sleep(CONNECTION_RETRY_PERIOD);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
