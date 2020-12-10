package com.github.eduramiba.javafx.auth0.rest;

import java.util.Map;

public interface ApiRESTClientHeadersProvider {
    Map<String, String> getExtraHeaders();
}
