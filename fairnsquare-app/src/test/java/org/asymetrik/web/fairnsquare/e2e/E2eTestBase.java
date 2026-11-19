package org.asymetrik.web.fairnsquare.e2e;

import static io.restassured.RestAssured.given;

import java.net.URL;
import java.nio.file.Path;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;

import io.quarkiverse.playwright.InjectPlaywright;
import io.quarkiverse.playwright.WithPlaywright;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.http.ContentType;

/**
 * Base class of the end-to-end tests: they drive the real frontend in a headless Chromium against the running app.
 * <p>
 * Each test gets a fresh browser context, so localStorage never leaks from one test to another. By default the context
 * starts with a valid CAPTCHA token in localStorage, so the tests do not have to solve the CAPTCHA.
 * <p>
 * When a test fails, a Playwright trace (screenshots, DOM snapshots, network) is saved to
 * {@code target/playwright-traces/<class>.<test>.zip}; open it with {@code npx playwright show-trace <file>} or on
 * trace.playwright.dev.
 * <p>
 * Tests locate elements by role, label or text — never by CSS classes or framework markup — so that they keep passing
 * when the frontend moves from Svelte to web components.
 */
@WithPlaywright
@QuarkusTestResource(TempStorageTestResource.class)
abstract class E2eTestBase {

    /** Must match the key used by the frontend captcha store. */
    private static final String CAPTCHA_TOKEN_KEY = "fairnsquare_captchaToken";

    @InjectPlaywright
    Browser browser;

    @TestHTTPResource("/")
    URL root;

    protected BrowserContext context;
    protected Page page;

    /** Runs before {@code @AfterEach}, while the browser context is still open. */
    @RegisterExtension
    final AfterTestExecutionCallback saveTraceOnFailure = extensionContext -> {
        if (extensionContext.getExecutionException().isPresent()) {
            String method = extensionContext.getRequiredTestMethod().getName();
            String display = extensionContext.getDisplayName();
            String name = extensionContext.getRequiredTestClass().getSimpleName() + "." + method
                    + (display.startsWith(method) ? "" : "-" + display.replaceAll("[^A-Za-z0-9]+", "_"));
            context.tracing()
                    .stop(new Tracing.StopOptions().setPath(Path.of("target", "playwright-traces", name + ".zip")));
        }
    };

    @BeforeEach
    void openPage() {
        context = browser.newContext(new Browser.NewContextOptions().setBaseURL(root.toString()));
        if (withCaptchaToken()) {
            context.addInitScript("localStorage.setItem('%s', '%s')".formatted(CAPTCHA_TOKEN_KEY,
                    CaptchaTokenTestHelper.generateToken()));
        }
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    /** Override to start the browser without a CAPTCHA token. */
    protected boolean withCaptchaToken() {
        return true;
    }

    /** The button with this exact accessible name. */
    protected Locator button(String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    /** The button with this exact accessible name inside the given element. */
    protected static Locator button(Locator scope, String name) {
        return scope.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
    }

    /** The form field with this exact label. */
    protected Locator field(String label) {
        return page.getByLabel(label, new Page.GetByLabelOptions().setExact(true));
    }

    /** The form field with this exact label inside the given element. */
    protected static Locator field(Locator scope, String label) {
        return scope.getByLabel(label, new Locator.GetByLabelOptions().setExact(true));
    }

    /** The element whose whole text is exactly this. */
    protected Locator text(String text) {
        return page.getByText(text, new Page.GetByTextOptions().setExact(true));
    }

    /** The open dialog with this exact accessible name. */
    protected Locator dialog(String name) {
        return page.getByRole(AriaRole.DIALOG, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    /** The toast notification showing this text. */
    protected Locator toast(String text) {
        return page.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions().setHasText(text));
    }

    /** Creates a split through the API and returns its id. */
    protected static String createSplit(String name) {
        return given().header("X-Captcha-Token", CaptchaTokenTestHelper.generateToken()).contentType(ContentType.JSON)
                .body("{\"name\": \"%s\"}".formatted(name)).post("/api/splits").then().statusCode(201).extract()
                .path("id");
    }

    /** Adds a participant through the API and returns its id. */
    protected static String addParticipant(String splitId, String name, int nights) {
        return given().contentType(ContentType.JSON)
                .body("{\"name\": \"%s\", \"nights\": %d, \"share\": 1}".formatted(name, nights))
                .post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract().path("id");
    }

    /** Adds a BY_NIGHT expense through the API and returns its id. */
    protected static String addExpense(String splitId, String description, String amount, String payerId) {
        return given().contentType(ContentType.JSON).body("""
                {"amount": %s, "description": "%s", "payerId": "%s"}
                """.formatted(amount, description, payerId)).post("/api/splits/" + splitId + "/expenses/by-night")
                .then().statusCode(201).extract().path("id");
    }
}
