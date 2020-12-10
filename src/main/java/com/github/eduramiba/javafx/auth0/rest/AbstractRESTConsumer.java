package com.github.eduramiba.javafx.auth0.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.eduramiba.javafx.auth0.utils.Utils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.CheckedSupplier;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRESTConsumer implements ApiRESTConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRESTConsumer.class);

    public static final MediaType JSON = MediaType.parse("application/json");
    public static final RetryPolicy<Object> DEFAULT_RETRY_POLICY = new RetryPolicy<>()
            .withMaxRetries(3)
            .withDelay(1, 2, ChronoUnit.SECONDS)
            .handleIf((Throwable ex) -> {
                if (ex instanceof ApiRESTException) {
                    //Do not retry some status codes:
                    final int statusCode = ((ApiRESTException) ex).getStatus();
                    if ((statusCode >= 300 && statusCode < 400)
                            || statusCode == HttpURLConnection.HTTP_BAD_REQUEST
                            || statusCode == HttpURLConnection.HTTP_NOT_FOUND
                            || statusCode == HttpURLConnection.HTTP_FORBIDDEN
                            || statusCode == HttpURLConnection.HTTP_UNAUTHORIZED
                            || statusCode == HttpURLConnection.HTTP_BAD_METHOD
                            || statusCode == HttpURLConnection.HTTP_NOT_ACCEPTABLE
                            || statusCode == HttpURLConnection.HTTP_UNSUPPORTED_TYPE) {
                        return false;
                    }
                }

                return true;
            })
            .onFailedAttempt(event -> {
                LOG.warn("Error doing request, will retry", event.getLastFailure());
            })
            .onFailure(event -> {
                LOG.error("Irrecoverable error doing request", event.getFailure());
            });

    protected final URI baseURI;
    private final OkHttpClient.Builder clientBuilder;
    private OkHttpClient client;

    private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 30_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;
    private static final int DEFAULT_WRITE_TIMEOUT_MILLIS = 30_000;

    private final Set<ApiRESTClientHeadersProvider> headersProviders = new HashSet<>();

    private RetryPolicy<Object> retryPolicy;

    public AbstractRESTConsumer(final URI baseURI, final ClientAuthenticationCredentials apiKeyAuthorization) {
        this.baseURI = baseURI;

        clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new HeadersInterceptor(apiKeyAuthorization))
                .connectTimeout(DEFAULT_CONNECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        retryPolicy = DEFAULT_RETRY_POLICY;
    }

    @Override
    public void setTimeoutMillis(int timeoutMilliseconds) {
        setConnectionTimeoutMillis(timeoutMilliseconds);
        setReadTimeoutMillis(timeoutMilliseconds);
        setWriteTimeoutMillis(timeoutMilliseconds);
    }

    @Override
    public void setConnectionTimeoutMillis(int timeoutMilliseconds) {
        clientBuilder.connectTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setReadTimeoutMillis(int timeoutMilliseconds) {
        clientBuilder.readTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setWriteTimeoutMillis(int timeoutMilliseconds) {
        clientBuilder.writeTimeout(timeoutMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void addRESTClientHeadersProvider(ApiRESTClientHeadersProvider headersProvider) {
        if (headersProvider != null) {
            headersProviders.add(headersProvider);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setRetryPolicy(RetryPolicy<?> retryPolicy) {
        this.retryPolicy = (RetryPolicy) retryPolicy;
    }

    protected <T> T withRetry(final CheckedSupplier<T> supplier) {
        return Failsafe.with(retryPolicy)
                .get(supplier);
    }

    protected <T> T get(String uri, Class<T> clazz) throws IOException {
        return get(URI.create(uri), clazz);
    }

    protected <T> T get(String uri, TypeReference<T> type) throws IOException {
        return get(URI.create(uri), type);
    }

    protected <T> T get(URI uri, Class<T> clazz) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP GET to {}", uri);
            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .get()
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                return parseResponseBody(response, clazz);
            } catch (IOException e) {
                throw new ApiRESTIOException((IOException) e);
            } finally {
                LOG.debug("[TIMING] HTTP GET to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T parseResponseBody(final Response response, final Class<T> clazz) throws IOException {
        return parseResponseBody(response, Unchecked.function(in -> Utils.parseJSON(in, clazz)));
    }

    protected <T> T parseResponseBody(final Response response, final TypeReference<T> type) throws IOException {
        return parseResponseBody(response, Unchecked.function(in -> Utils.parseJSON(in, type)));
    }

    protected <T> T parseResponseBody(final Response response, final Function<InputStream, T> parseFunction) throws IOException {
        if (response == null) {
            return null;
        }

        final String requestMethod = response.request().method();
        final boolean isOperation = Seq.of("GET", "HEAD", "OPTIONS")
                .noneMatch(requestMethod::equalsIgnoreCase);

        final int status = response.code();
        if (status == HttpURLConnection.HTTP_NO_CONTENT) {
            return null;
        }

        if (status == HttpURLConnection.HTTP_NOT_FOUND) {
            if (isOperation) {
                throw ApiRESTException.from(response);
            } else {
                return null;
            }
        }

        validateResponse(response);

        final ResponseBody body = response.body();

        if (body == null) {
            return null;
        }

        final Optional<BufferedInputStream> inputStream = getResponseDataInputStream(response);

        return inputStream.map(Unchecked.function(in -> {
            final MediaType contentType = body.contentType();
            if (contentType == null || !contentType.subtype().toLowerCase().contains("json")) {
                throw new IllegalStateException(
                        "Expected JSON response media type but got " + contentType + ". Content:\n"
                        + IOUtils.toString(in, StandardCharsets.UTF_8)
                );
            }

            return parseFunction.apply(in);
        })).orElse(null);
    }

    protected void validateResponse(final Response response) throws ApiRESTException, IOException {
        if (!response.isSuccessful()) {
            if (response.code() == HttpURLConnection.HTTP_NOT_IMPLEMENTED) {
                final ResponseBody body = response.body();

                final String message = body != null ? Unchecked.supplier(() -> body.string()).get() : "NO MESSAGE";
                throw new UnsupportedOperationException(String.format("Unsupported request to %s. Message: %s", response.request().url().toString(), message));
            }

            throw ApiRESTException.from(response);
        }
    }

    protected <T> T get(URI uri, TypeReference<T> type) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP GET to {}", uri);
            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .get()
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                return parseResponseBody(response, type);
            } catch (IOException e) {
                throw new ApiRESTIOException((IOException) e);
            } finally {
                LOG.debug("[TIMING] HTTP GET to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected Response post(String uri, Object data) throws IOException {
        return AbstractRESTConsumer.this.post(URI.create(uri), data);
    }

    protected Response post(URI uri, Object data) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP POST: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .post(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try {
                return getClient().newCall(request).execute();
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP POST to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T post(String uri, Object data, Class<T> responseClass) throws IOException {
        return post(URI.create(uri), data, responseClass);
    }

    protected <T> T post(String uri, Object data, TypeReference<T> responseType) throws IOException {
        return post(URI.create(uri), data, responseType);
    }

    protected <T> T post(URI uri, Object data, TypeReference<T> responseType) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP POST: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .post(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                LOG.debug("Response = {}", response);

                return parseResponseBody(response, responseType);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP POST to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T post(URI uri, Object data, Class<T> responseClass) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP POST: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .post(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                LOG.debug("Response = {}", response);

                return parseResponseBody(response, responseClass);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP POST to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected Response put(String uri) throws IOException {
        return put(uri, null);
    }

    protected Response put(String uri, Object data) throws IOException {
        return AbstractRESTConsumer.this.put(URI.create(uri), data);
    }

    protected Response put(URI uri) throws IOException {
        return put(uri, null);
    }

    protected Response put(URI uri, Object data) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP PUT: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .put(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();

            try {
                return getClient().newCall(request).execute();
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP POST to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T put(String uri, Object data, Class<T> responseClass) throws IOException {
        return put(URI.create(uri), data, responseClass);
    }

    protected <T> T put(String uri, Object data, TypeReference<T> responseType) throws IOException {
        return put(URI.create(uri), data, responseType);
    }

    protected <T> T put(URI uri, Object data, TypeReference<T> responseType) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP PUT: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .put(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                LOG.debug("Response = {}", response);

                return parseResponseBody(response, responseType);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP PUT to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T put(URI uri, Object data, Class<T> responseClass) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP PUT: {} = {}", uri, Utils.toJSON(data));

            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .put(
                            RequestBody.create(Utils.toJSON(data), JSON)
                    )
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                LOG.debug("Response = {}", response);

                return parseResponseBody(response, responseClass);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e);
                } else if (e.getCause() instanceof IOException) {
                    throw new ApiRESTIOException((IOException) e.getCause());
                } else {
                    throw e;
                }
            } finally {
                LOG.debug("[TIMING] HTTP PUT to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected Response delete(String uri) throws IOException {
        return delete(URI.create(uri));
    }

    protected Response delete(URI uri) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP DELETE to {}", uri);
            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .delete()
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try {
                return getClient().newCall(request).execute();
            } catch (IOException e) {
                throw new ApiRESTIOException((IOException) e);
            } finally {
                LOG.debug("[TIMING] HTTP DELETE to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected <T> T delete(URI uri, Class<T> responseClass) throws IOException {
        return withRetry(() -> {
            LOG.debug("HTTP DELETE to {}", uri);
            final Request request = new Request.Builder()
                    .url(uri.toString())
                    .delete()
                    .addHeader("Accept", JSON.toString())
                    .build();

            final long t0 = System.nanoTime();
            try (Response response = getClient().newCall(request).execute()) {
                return parseResponseBody(response, responseClass);
            } catch (IOException e) {
                throw new ApiRESTIOException((IOException) e);
            } finally {
                LOG.debug("[TIMING] HTTP DELETE to {}, {} ns", uri, System.nanoTime() - t0);
            }
        });
    }

    protected synchronized OkHttpClient getClient() {
        if (client == null) {
            client = clientBuilder.build();
        }

        return client;
    }

    @Override
    public synchronized void close() {
        client = null;
    }

    public static Optional<BufferedInputStream> getResponseDataInputStream(Response response) throws IOException {
        final ResponseBody body = response.body();

        if (body == null) {
            return Optional.empty();
        }

        final BufferedInputStream inputStream = new BufferedInputStream(body.byteStream());
        inputStream.mark(1);
        if (inputStream.read() == -1) {
            return Optional.empty();
        }
        inputStream.reset();

        if (inputStream.available() > 0) {
            return Optional.of(inputStream);
        }

        return Optional.empty();
    }

    private class HeadersInterceptor implements Interceptor {

        private final ClientAuthenticationCredentials credentials;

        public HeadersInterceptor(ClientAuthenticationCredentials apiKeyAuthorization) {
            this.credentials = apiKeyAuthorization;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            final Request.Builder newRequestBuilder = chain.request().newBuilder();

            headersProviders.stream()
                    .map(p -> p.getExtraHeaders())
                    .filter(Objects::nonNull)
                    .flatMap(p -> p.entrySet().stream())
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .forEach(entry -> {
                        newRequestBuilder.header(entry.getKey(), entry.getValue());
                    });

            newRequestBuilder.header("User-Agent", "JavaFX Auth0 Login Example");

            LOG.debug("Api key authorization settings: {}", credentials);
            if (credentials != null
                    && !StringUtils.isBlank(credentials.getRequestHeader())
                    && !StringUtils.isBlank(credentials.getSecret())) {
                LOG.debug("Using credentials: {}", credentials);

                newRequestBuilder.header(credentials.getRequestHeader().trim(), credentials.getSecret().trim());
            }

            return chain.proceed(newRequestBuilder.build());
        }
    }

}
