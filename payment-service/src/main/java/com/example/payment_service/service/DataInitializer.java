package com.example.payment_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DataInitializer {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void init() {
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

    private void createNotifyFunction(String functionName, String channelName) {
        String sql = "CREATE OR REPLACE FUNCTION " + functionName + "() " +
                "RETURNS trigger AS $$ " +
                "BEGIN " +
                "PERFORM pg_notify('" + channelName + "', 'new_msg'); " +
                "RETURN NEW; " +
                "END; " +
                "$$ LANGUAGE plpgsql;";
        jdbcTemplate.execute(sql);
    }

    private void createInsertTrigger(String triggerName, String tableName, String functionName) {
        String sql = "DROP TRIGGER IF EXISTS " + triggerName + " ON " + tableName + ";" +
                "CREATE TRIGGER " + triggerName + " " +
                "AFTER INSERT ON " + tableName + " " +
                "FOR EACH STATEMENT " +
                "EXECUTE FUNCTION " + functionName + "();";
        jdbcTemplate.execute(sql);
    }

    private void createIndex(String indexName, String tableName) {
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS " + indexName + " ON " + tableName
                + " (transaction_id, event_type);";
        jdbcTemplate.execute(sql);
    }
}
