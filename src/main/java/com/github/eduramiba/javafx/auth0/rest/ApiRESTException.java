package com.github.eduramiba.javafx.auth0.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduramiba.javafx.auth0.utils.Utils;
import static com.github.eduramiba.javafx.auth0.rest.AbstractRESTConsumer.getResponseDataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiRESTException extends UserFriendlyIOException {

    private static final Logger LOG = LoggerFactory.getLogger(ApiRESTException.class);

    private final transient String requestUri;
    private final transient String requestMethod;
    private final transient String responseBody;
    private final transient AppServerErrorMessagesResponse errorResponse;
    private final transient int status;

    public ApiRESTException(String requestUri, String requestMethod, String responseBody, AppServerErrorMessagesResponse errorResponse, int status) {
        this.requestUri = requestUri;
        this.requestMethod = requestMethod;
        this.responseBody = StringUtils.stripToNull(responseBody);
        this.errorResponse = errorResponse;
        this.status = status;
    }

    @Override
    public String getFriendlyMessage() {
        String message;
        switch (status) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_BAD_METHOD:
            case HttpURLConnection.HTTP_NOT_ACCEPTABLE:
            case HttpURLConnection.HTTP_UNSUPPORTED_TYPE:
                message = "Could not process the Application server request correctly. The request is badly formatted, please make sure that all your data and files are formatted correctly. If they are, please report a bug to xxx@yyy.com";
                break;
            case HttpURLConnection.HTTP_CONFLICT:
                message = "Could not process the Application server request correctly. Please make sure you are doing something that is possible in Application server, the server reponse code indicates some conflict or incompatible action was detected.";
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                message = "Could not process the Application server request correctly. Please make sure that you have the right permissions and that you have logged in to Application server";
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                message = "Could not process the Application server request correctly. Application server requested an element that does not exist, please make sure the identifier is correct and the element has not been deleted";
                break;
            case HttpURLConnection.HTTP_UNAVAILABLE:
                message = "Could not process the Application server request correctly. The service is down, please try again later";
                break;
            default:
                if (status >= 500) {
                    message = "Could not process the Application server request correctly. An unknown problem happened, this is probably a bug in Application server or the API is not available, please report it to Application server Team. HTTP Status: " + status;
                } else {
                    message = "Could not process the Application server request correctly. Please make sure that you have the right permissions and that you have logged in to Application server, also check your network is working correctly, no problems with proxies, etc.";
                }
        }

        if (errorResponse != null && errorResponse.getMessages() != null) {
            final String extraMessage = StringUtils.join(errorResponse.getMessages(), "; ");

            message += ". HTTP Status code = " + status + ". The server indicated the following cause of the problem: " + extraMessage;
        } else {
            message += ". HTTP Status code = " + status;

            if (responseBody != null) {
                message += ". Body = " + responseBody;
            }
        }

        if (errorResponse != null && errorResponse.getErrorCode() != null) {
            message += "Error code = " + errorResponse.getErrorCode();
        }

        return message;
    }

    @Override
    public String getUniqueCode() {
        return "REST-API-STATUS-" + status;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return getFriendlyMessage();
    }

    public AppServerErrorMessagesResponse getErrorResponse() {
        return errorResponse;
    }

    @Override
    public Object getSource() {
        String sourceIdHelpText = requestUri + " [" + requestMethod + "]";
        if (responseBody != null) {
            sourceIdHelpText += ": " + responseBody;
        }

        return sourceIdHelpText;
    }

    public static class AppServerErrorMessagesResponse {

        private final List<String> messages;
        private final String errorCode;
        private final Map<String, Object> errorDescriptionData;

        public AppServerErrorMessagesResponse(List<Object> messages, String errorCode, Map<String, Object> errorDescriptionData) {
            this.errorCode = errorCode;
            this.errorDescriptionData = errorDescriptionData != null ? errorDescriptionData : Collections.emptyMap();
            if (messages != null) {
                this.messages = messages.stream()
                        .filter(Objects::nonNull)
                        .map(Objects::toString)
                        .collect(Collectors.toUnmodifiableList());
            } else {
                this.messages = Collections.emptyList();
            }
        }

        public List<String> getMessages() {
            return messages;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public Map<String, Object> getErrorDescriptionData() {
            return errorDescriptionData;
        }

        @JsonCreator
        public static AppServerErrorMessagesResponse parse(
                @JsonProperty("errorCode") String errorCode,
                @JsonProperty("errorDescriptionData") Map<String, Object> errorDescriptionData,
                @JsonProperty("messages") List<Object> messages,
                @JsonProperty("message") List<Object> message
        ) {

            if (messages != null) {
                return new AppServerErrorMessagesResponse(messages, errorCode, errorDescriptionData);
            } else if (message != null) {
                return new AppServerErrorMessagesResponse(message, errorCode, errorDescriptionData);
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return Utils.toJSONPrettyPrint(this);
        }
    }

    public static ApiRESTException from(final Response response) throws IOException {
        final Optional<BufferedInputStream> inputStream = getResponseDataInputStream(response);

        final Optional<String> responseBody = inputStream.map(in -> {
            try {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return null;
            }
        });

        return new ApiRESTException(
                response.request().url().toString(),
                response.request().method(),
                responseBody.orElse(null),
                responseBody.map(ApiRESTException::errorMessages).orElse(null),
                response.code()
        );
    }

    private static AppServerErrorMessagesResponse errorMessages(final String responseBody) {
        try {
            return Utils.parseJSON(responseBody, AppServerErrorMessagesResponse.class);
        } catch (Exception ex) {
            LOG.warn("Error parsing messages response", ex);
            return null;
        }
    }
}
