package org.asymetrik.web.fairnsquare.testing;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;

/**
 * One headless Chromium for the whole run, started by the first end-to-end test and closed when the JVM exits.
 * Playwright downloads it to {@code ~/.cache/ms-playwright} the first time.
 */
public final class SharedBrowser {

    private static Browser browser;

    private SharedBrowser() {
    }

    public static synchronized Browser chromium() {
        if (browser == null) {
            Playwright playwright = Playwright.create();
            browser = playwright.chromium().launch();
            Runtime.getRuntime().addShutdownHook(new Thread(playwright::close));
        }
        return browser;
    }
}
