package org.asymetrik.web.fairnsquare;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.asymetrik.web.fairnsquare.testing.FairnsquareTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The page and the web component scripts have no content hash in their names: the browser must ask the server again on
 * each load (a 304 when unchanged), or after a deploy it mixes old and new modules and a page stays blank (#178).
 */
@FairnsquareTest
class StaticCacheTest {

    @ParameterizedTest
    @ValueSource(strings = { "/", "/splits/someSplit/participants", "/main.js", "/components/icons.js",
            "/biz/participants/participants-page.js", "/infra/theme.css" })
    void filesAreRevalidated(String path) {
        given().get(path).then().statusCode(200).header("Cache-Control", is("no-cache"));
    }

    /** A shared link or a reload on any of the app's addresses gets the app's page, whose router shows the page. */
    @ParameterizedTest
    @ValueSource(strings = { "/", "/splits/someSplit", "/splits/someSplit/expenses", "/admin" })
    void appAddressesServeTheAppPage(String path) {
        given().get(path).then().statusCode(200).contentType(containsString("text/html"))
                .body(containsString("<script type=\"module\" src=\"/main.js\"></script>"));
    }
}
