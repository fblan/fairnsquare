package org.asymetrik.web.fairnsquare;

import io.vidocq.runtime.core.VidocqBootstrap;

/** Starts the application: vidocq's CDI container, the web server and the REST resources. */
public final class FairnsquareApp {

    public static void main(String[] args) {
        VidocqBootstrap.create().configure().start().awaitShutdown();
    }
}
