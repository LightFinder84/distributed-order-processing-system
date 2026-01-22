package com.example.order_service.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.order_service.model.Customer;
import com.example.order_service.model.Product;
import com.example.order_service.repository.CustomerRepository;
import com.example.order_service.repository.ProductRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

/**
 * Service for initializing sample data and database infrastructure.
 *
 * <p>
 * This service runs automatically on application startup to initialize the
 * database with sample data and create necessary database objects for event
 * publishing and processing.
 * </p>
 *
 * <p>
 * The initialization process includes:
 * </p>
 * <ul>
 * <li>Creating sample customers and products if the database is empty</li>
 * <li>Creating PostgreSQL NOTIFY functions for real-time event
 * notifications</li>
 * <li>Creating database triggers to automatically notify about inbox and outbox
 * changes</li>
 * <li>Creating unique indexes on transaction ID and event type for inbox and
 * outbox tables</li>
 * </ul>
 *
 * <p>
 * The NOTIFY functions and triggers enable asynchronous event processing by
 * notifying the application when new events arrive in the inbox or when events
 * need to be published from the outbox.
 * </p>
 *
 * @author Order Service Team
 * @version 1.0
 */
@Service
public class DataInitializer {

    /**
     * Repository for Customer entities.
     */
    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Repository for Product entities.
     */
    @Autowired
    private ProductRepository productRepository;

    /**
     * JDBC template for executing raw SQL statements.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Sample product price used for initial product data.
     */
    private final BigDecimal samplePrize = new BigDecimal(100000);

    /**
     * Alternative sample product price used for second initial product.
     */
    private final BigDecimal samplePrize2 = new BigDecimal(100000);

    /**
     * Initializes the database with sample data and infrastructure.
     *
     * <p>
     * This method is called automatically after the bean is constructed using
     * the {@link PostConstruct} annotation. It performs all database initialization
     * operations including sample data creation and trigger/function setup.
     * </p>
     */
    @PostConstruct
    @Transactional
    public final void init() {
        // create product
        if (productRepository.count() == 0L && customerRepository.count() == 0L) {
            createCustomers();
            createProducts();
        }
        // create notify function
        createNotifyFunction("inbox");
        createNotifyFunction("outbox");
        // create insert trigger
        createInsertTrigger("inbox");
        createInsertTrigger("outbox");
        // create index
        createIndex("inbox");
        createIndex("outbox");
    }

    /**
     * Creates sample customer records in the database.
     *
     * <p>
     * This method initializes a sample customer named "truong" which can be
     * used for testing and demonstration purposes.
     * </p>
     */
    private void createCustomers() {
        Customer customer = new Customer("truong");
        customerRepository.save(customer);
    }

    /**
     * Creates sample product records in the database.
     *
     * <p>
     * This method initializes two sample products with predefined prices,
     * which can be used for testing and demonstration purposes in order processing.
     * </p>
     */
    private void createProducts() {
        Product product1 = new Product("product 1", samplePrize);
        Product product2 = new Product("product 2", samplePrize2);
        productRepository.save(product1);
        productRepository.save(product2);
    }

    /**
     * Creates a PostgreSQL NOTIFY function for event notifications.
     *
     * <p>
     * This method creates database functions that trigger notifications on the
     * specified
     * channel when new records are inserted into the inbox or outbox tables. The
     * functions
     * use PostgreSQL's pg_notify to send asynchronous notifications to listening
     * clients.
     * </p>
     *
     * @param pattern the table type ("inbox" or "outbox") to determine which NOTIFY
     *                function to create
     */
    private void createNotifyFunction(final String pattern) {
        // private void createNotifyFunction(final String functionName, final String
        // channelName) {
        String sql = null;
        if ("inbox".equals(pattern)) {
            sql = "CREATE OR REPLACE FUNCTION "
                    + "notify_inbox_insert()"
                    + "RETURNS trigger AS $$ "
                    + "BEGIN "
                    + "PERFORM pg_notify('"
                    + "inbox_channel"
                    + "', 'new_msg'); "
                    + "RETURN NEW; "
                    + "END; "
                    + "$$ LANGUAGE plpgsql;";
        } else {
            sql = "CREATE OR REPLACE FUNCTION "
                    + "notify_outbox_insert()"
                    + "RETURNS trigger AS $$ "
                    + "BEGIN "
                    + "PERFORM pg_notify('"
                    + "outbox_channel"
                    + "', 'new_msg'); "
                    + "RETURN NEW; "
                    + "END; "
                    + "$$ LANGUAGE plpgsql;";
        }
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a trigger for automatic event notifications on table inserts.
     *
     * <p>
     * This method creates database triggers that fire after INSERT statements on
     * the specified
     * table, executing the corresponding NOTIFY function to alert the application
     * about new events.
     * This enables real-time event processing for the inbox/outbox pattern
     * implementation.
     * </p>
     *
     * @param tableName the table on which to attach the trigger ("inbox" or
     *                  "outbox")
     */
    private void createInsertTrigger(final String tableName) {
        String sql = "";
        if ("inbox".equals(tableName)) {
            sql = "DROP TRIGGER IF EXISTS trg_inbox_insert ON inbox;"
                    + "CREATE TRIGGER trg_inbox_insert AFTER INSERT ON inbox "
                    + "FOR EACH STATEMENT EXECUTE FUNCTION notify_inbox_insert();";
        } else {
            sql = "DROP TRIGGER IF EXISTS trg_outbox_insert ON outbox;"
                    + "CREATE TRIGGER trg_outbox_insert AFTER INSERT ON outbox "
                    + "FOR EACH STATEMENT EXECUTE FUNCTION notify_outbox_insert();";
        }

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Creates a unique index on the specified table.
     *
     * <p>
     * This method creates a unique index on transaction_id and event_type columns
     * to ensure that each event for a given transaction is processed only once,
     * preventing duplicate event processing in the inbox and outbox patterns.
     * </p>
     *
     * @param tableName the table on which to create the index
     */
    private void createIndex(final String tableName) {
        String sql = null;

        if ("inbox".equals(tableName)) {
            sql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_inbox_tx_type "
                + "ON inbox (transaction_id, event_type);";
        } else {
            sql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_outbox_tx_type "
                + "ON outbox (transaction_id, event_type);";
        }

        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                PreparedStatement statement = conn.prepareStatement(sql);) {
            statement.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
