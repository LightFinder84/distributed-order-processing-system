package com.example.order_service;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class HibernateConfiguration {

    /**
     * Modify hibernate properties
     * Automatically called by spring, explicit call is not allowed.
     *
     * @param customObjectMapper object mapper from jackson
     * @return Hibernate properties object
     */
    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapper(final ObjectMapper customObjectMapper) {
        return properties -> properties.put(AvailableSettings.JSON_FORMAT_MAPPER,
                new JacksonJsonFormatMapper(customObjectMapper));
    }
}
