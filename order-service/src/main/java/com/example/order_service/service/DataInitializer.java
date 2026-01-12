package com.example.order_service.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.order_service.model.Customer;
import com.example.order_service.model.Product;
import com.example.order_service.repository.CustomerRepository;
import com.example.order_service.repository.ProductRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class DataInitializer {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    @Transactional
    public void init() {
        // create product
        if (productRepository.count() == 0L && customerRepository.count() == 0L) {
            createCustomers();
            createProducts();
        }
        // create notify function
        createNotifyFunction("notify_inbox_insert", "inbox_channel");
        createNotifyFunction("notify_outbox_insert", "outbox_channel");
        // create insert trigger
        createInsertTrigger("trg_inbox_insert", "inbox", "notify_inbox_insert");
        createInsertTrigger("trg_outbox_insert", "outbox", "notify_outbox_insert");
        // create index
        createIndex("idx_unique_inbox_tx_type", "inbox");
        createIndex("idx_unique_outbox_tx_type", "outbox");
    }

    private void createCustomers() {
        Customer customer = new Customer("truong");
        customerRepository.save(customer);
    }

    private void createProducts() {
        Product product1 = new Product("product 1", new BigDecimal(100000));
        Product product2 = new Product("product 2", new BigDecimal(50000));
        productRepository.save(product1);
        productRepository.save(product2);
    }

    private void createNotifyFunction(String functionName, String channelName) {
        String sql = "CREATE OR REPLACE FUNCTION " + functionName + "() " +
            "RETURNS trigger AS $$ " +
            "BEGIN " +
            "PERFORM pg_notify('" + channelName + "', 'new_msg'); " +
            "RETURN NEW; " +
            "END; " +
            "$$ LANGUAGE plpgsql;"
        ;
        jdbcTemplate.execute(sql);
    }

    private void createInsertTrigger(String triggerName, String tableName, String functionName) {
        String sql = "DROP TRIGGER IF EXISTS " + triggerName + " ON " + tableName + ";" +
            "CREATE TRIGGER " + triggerName + " " +
            "AFTER INSERT ON " + tableName + " " +
            "FOR EACH STATEMENT " +
            "EXECUTE FUNCTION " + functionName + "();"
        ;
        jdbcTemplate.execute(sql);
    }

    private void createIndex(String indexName, String tableName) {
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (transaction_id, event_type);";
        jdbcTemplate.execute(sql);
    }
}
