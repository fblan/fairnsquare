package org.asymetrik.web.fairnsquare;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

/**
 * The page and the web component scripts have no content hash in their names: the browser must ask the server again on
 * each load (a 304 when unchanged), or after a deploy it mixes old and new modules and a page stays blank (#178). Only
 * the bundle, whose names carry a hash, may be kept.
 */
@QuarkusTest
class StaticCacheTest {

    @ParameterizedTest
    @ValueSource(strings = { "/", "/splits/someSplit/participants", "/components/icons.js",
            "/biz/participants/participants-page.js", "/infra/theme.css" })
    void unhashedFilesAreRevalidated(String path) {
        given().get(path).then().statusCode(200).header("Cache-Control", is("no-cache"));
    }

    @Test
    void theHashedBundleIsKept() {
        String index = given().get("/").then().statusCode(200).extract().asString();
        Matcher bundle = Pattern.compile("src=\"(/static/bundle/[^\"]+\\.js)\"").matcher(index);
        assertThat(bundle.find()).isTrue();

        // In prod it is "public, immutable, max-age=86400"; in tests the web bundler sends no cache header.
        String cacheControl = given().get(bundle.group(1)).then().statusCode(200).extract().header("Cache-Control");
        assertThat(cacheControl).isNotEqualTo("no-cache");
    }
}
