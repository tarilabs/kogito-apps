/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.task.console;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class VertxRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxRouter.class);

    @Inject
    @ConfigProperty(name = "kogito.dataindex.http.url", defaultValue = "http://localhost:8180")
    String dataIndexHttpURL;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.tenant-enabled", defaultValue = "false")
    String authEnabled;

    @Inject
    @ConfigProperty(name = "kogito.task.active.states.list", defaultValue = "Ready,Reserved")
    String activeTaskStates;

    @Inject
    @ConfigProperty(name = "kogito.task.states.list", defaultValue = "Ready,Reserved,Completed,Aborted,Skipped")
    String allTaskStates;

    @ConfigProperty(name = "kogito.test.user-system.enabled", defaultValue = "false")
    String testUserSystemEnabled;


    @Inject
    Vertx vertx;

    private String resource;

    @PostConstruct
    public void init() {
        resource = vertx.fileSystem()
                .readFileBlocking("META-INF/resources/index.html")
                .toString(UTF_8)
                .replace("__DATA_INDEX_ENDPOINT__", "\"" + dataIndexHttpURL + "/graphql\"")
                .replace("__KOGITO_AUTH_ENABLED__", authEnabled)
                .replace("__KOGITO_TASK_ACTIVE_STATES_LIST__", "\"" + activeTaskStates + "\"")
                .replace("__KOGITO_TASK_STATES_LIST__", "\"" + allTaskStates + "\"")
                .replace("__KOGITO_TEST_USER_SYSTEM_ENABLED__", testUserSystemEnabled);
    }

    void setupRouter(@Observes Router router) {
        router.route("/").handler(ctx -> ctx.response().putHeader("location", "/TaskInbox/").setStatusCode(302).end());
        router.route("/TaskInbox").handler(this::handle);
        router.route("/TaskDetails*").handler(this::handle);
        router.route().handler(StaticHandler.create());
    }

    public void handle(RoutingContext context) {
        try {
            context.response()
                    .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .putHeader(HttpHeaders.CONTENT_TYPE, "text/html;charset=utf8")
                    .end(resource);
        } catch (Exception ex) {
            LOGGER.error("Error handling index.html", ex);
            context.fail(500, ex);
        }
    }
}
