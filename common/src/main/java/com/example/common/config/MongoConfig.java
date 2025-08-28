package com.example.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new StringToLocalTimeConverter());
        converters.add(new LocalTimeToStringConverter());
        return new MongoCustomConversions(converters);
    }

    // This converter reads a String from MongoDB and converts it to a LocalTime object
    static class StringToLocalTimeConverter implements Converter<String, LocalTime> {
        @Override
        public LocalTime convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }
            return LocalTime.parse(source, DateTimeFormatter.ISO_LOCAL_TIME);
        }
    }

    // This converter writes a LocalTime object as a String to MongoDB
    static class LocalTimeToStringConverter implements Converter<LocalTime, String> {
        @Override
        public String convert(LocalTime source) {
            if (source == null) {
                return null;
            }
            return source.format(DateTimeFormatter.ISO_LOCAL_TIME);
        }
    }
}