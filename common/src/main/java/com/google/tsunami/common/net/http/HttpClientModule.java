/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.tsunami.common.net.http;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.time.Duration;
import javax.inject.Singleton;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

/** Guice module for installing {@link HttpClient} library. */
public final class HttpClientModule extends AbstractModule {
  // Maximum number of idle connections to each to keep in the pool.
  private final int connectionPoolMaxIdle;
  // Duration to keep the connection alive in the pool before closing it.
  private final Duration connectionPoolKeepAliveDuration;
  // Maximum number of requests to execute concurrently.
  private final int maxRequests;
  // Maximum number of requests for each host (URL's host name) to execute concurrently.
  private final int maxRequestsPerHost;
  // Whether or not to follow redirect from server.
  private final boolean followRedirects;

  public HttpClientModule(Builder builder) {
    checkNotNull(builder);
    this.connectionPoolMaxIdle = builder.connectionPoolMaxIdle;
    this.connectionPoolKeepAliveDuration = builder.connectionPoolKeepAliveDuration;
    this.maxRequests = builder.maxRequests;
    this.maxRequestsPerHost = builder.maxRequestsPerHost;
    this.followRedirects = builder.followRedirects;
  }

  @Provides
  @Singleton
  ConnectionPool provideConnectionPool() {
    return new ConnectionPool(
        connectionPoolMaxIdle, connectionPoolKeepAliveDuration.toMillis(), MILLISECONDS);
  }

  @Provides
  @Singleton
  Dispatcher provideDispatcher() {
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(maxRequests);
    dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
    return dispatcher;
  }

  // Missing features:
  // 1. Custom cookie handler.
  // 2. Connection / read / write timeout.
  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient(ConnectionPool connectionPool, Dispatcher dispatcher) {
    return new OkHttpClient.Builder()
        .connectionPool(connectionPool)
        .dispatcher(dispatcher)
        .followRedirects(followRedirects)
        .build();
  }

  /** Builder for {@link HttpClientModule}. */
  public static final class Builder {
    private static final int DEFAULT_CONNECTION_POOL_MAX_IDLE = 5;
    private static final Duration DEFAULT_CONNECTION_POOL_KEEP_ALIVE_DURATION =
        Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_REQUESTS = 64;
    private static final int DEFAULT_MAX_REQUESTS_PER_HOST = 5;
    private static final boolean DEFAULT_FOLLOW_REDIRECTS = true;

    private int connectionPoolMaxIdle = DEFAULT_CONNECTION_POOL_MAX_IDLE;
    private Duration connectionPoolKeepAliveDuration = DEFAULT_CONNECTION_POOL_KEEP_ALIVE_DURATION;
    private int maxRequests = DEFAULT_MAX_REQUESTS;
    private int maxRequestsPerHost = DEFAULT_MAX_REQUESTS_PER_HOST;
    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    /** Sets the maximum number of idle connections to each to keep in the pool. */
    public Builder setConnectionPoolMaxIdle(int maxIdle) {
      checkArgument(maxIdle > 0);
      this.connectionPoolMaxIdle = maxIdle;
      return this;
    }

    /** Sets the duration to keep the connection alive in the connection pool before closing it. */
    public Builder setConnectionPoolKeepAliveDuration(Duration keepAliveDuration) {
      checkNotNull(keepAliveDuration);
      checkArgument(!keepAliveDuration.isNegative());
      this.connectionPoolKeepAliveDuration = keepAliveDuration;
      return this;
    }

    /** Sets the maximum number of requests to execute concurrently. */
    public Builder setMaxRequests(int maxRequests) {
      checkArgument(maxRequests > 0);
      this.maxRequests = maxRequests;
      return this;
    }

    /**
     * Sets the maximum number of requests for each host (URL's host name) to execute concurrently.
     */
    public Builder setMaxRequestsPerHost(int maxRequestsPerHost) {
      checkArgument(maxRequestsPerHost > 0);
      this.maxRequestsPerHost = maxRequestsPerHost;
      return this;
    }

    /**
     * Sets whether or not to follow redirect from server. If unset, by default redirects will be
     * followed.
     */
    public Builder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public HttpClientModule build() {
      return new HttpClientModule(this);
    }
  }
}
