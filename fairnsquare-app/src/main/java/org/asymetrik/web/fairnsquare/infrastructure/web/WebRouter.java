package org.asymetrik.web.fairnsquare.infrastructure.web;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Serves the app's page for its addresses (/splits/&lt;id&gt;/expenses, /admin...), so a shared link or a reload opens
 * the right page: every GET that no static file answers, outside the API and /q, is rerouted to /, which serves
 * index.html. Its router (main.js) then shows the page for the address.
 */
@ApplicationScoped
public class WebRouter {

    void onStart(@Observes StartupEvent event, Router router) {
        router.get("/*").order(Integer.MAX_VALUE - 1).handler(ctx -> {
            String path = ctx.request().path();
            if (!path.startsWith("/api") && !path.startsWith("/q")) {
                ctx.reroute("/");
            } else {
                ctx.next();
            }
        });
    }
}
