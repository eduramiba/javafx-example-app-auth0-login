package com.github.eduramiba.javafx.auth0.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.jooq.lambda.Unchecked;

public class Utils {

    protected static final ObjectMapper JSON_PARSER = new ObjectMapper();
    protected static final ObjectMapper JSON_WRITER_PRETTY_PRINT = new ObjectMapper();
    protected static final ObjectMapper JSON_WRITER = new ObjectMapper();

    static {
        configureJSONReader(JSON_PARSER);
        configureJSONWriter(JSON_WRITER);
        configureJSONWriter(JSON_WRITER_PRETTY_PRINT);

        JSON_WRITER_PRETTY_PRINT.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public static void configureJSONReader(ObjectMapper mapper) {
        mapper
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }

    public static void configureJSONWriter(ObjectMapper mapper) {
        mapper
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                //.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }

    public static void configureJSONReaderAndWriter(ObjectMapper mapper) {
        mapper
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                //.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
    }

    public static <T> T parseJSON(String json, Class<T> clazz) throws IOException {
        if (json == null) {
            return null;
        }

        return JSON_PARSER.readValue(json, clazz);
    }

    public static <T> T parseJSON(File file, TypeReference<T> type) throws IOException {
        return JSON_PARSER.readValue(file, type);
    }

    public static <T> T parseJSON(File file, Class<T> clazz) throws IOException {
        return JSON_PARSER.readValue(file, clazz);
    }

    public static <T> T parseJSON(InputStream is, TypeReference<T> type) throws IOException {
        return JSON_PARSER.readValue(is, type);
    }

    public static <T> T parseJSON(InputStream is, Class<T> clazz) throws IOException {
        return JSON_PARSER.readValue(is, clazz);
    }

    public static <T> T parseJSON(String json, TypeReference<T> type) throws IOException {
        if (json == null) {
            return null;
        }

        return JSON_PARSER.readValue(json, type);
    }

    public static String toJSONPrettyPrint(Object value) {
        try {
            return JSON_WRITER_PRETTY_PRINT.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            Unchecked.throwChecked(ex);
            return null;
        }
    }

    public static String toJSON(Object value) {
        try {
            return JSON_WRITER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            Unchecked.throwChecked(ex);
            return null;
        }
    }

}
