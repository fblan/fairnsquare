package org.asymetrik.web.fairnsquare.infrastructure.web;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Enables SPA (history-mode) routing: all non-API paths are rerouted to /, where the Web Bundler serves the index page.
 * Required because sv-router uses HTML5 history routing.
 */
@ApplicationScoped
public class WebRouter {

    void onStart(@Observes StartupEvent event, Router router) {
        router.get("/*").order(Integer.MAX_VALUE - 1).handler(ctx -> {
            String path = ctx.request().path();
            if (!path.startsWith("/api") && !path.startsWith("/q") && !path.startsWith("/static")) {
                ctx.reroute("/");
            } else {
                ctx.next();
            }
        });
    }
}
