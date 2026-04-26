package org.asymetrik.web.fairnsquare.admin;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.asymetrik.web.fairnsquare.admin.api.internal.AdminTokenFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AdminTokenFilter — token validation, constant-time comparison, and unconfigured-hash scenarios.
 */
class AdminTokenFilterTest {

    private static final String PASSWORD = "password";
    private static final String PASSWORD_HASH = sha256Hex(PASSWORD);

    private AdminTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminTokenFilter(PASSWORD_HASH);
    }

    @Test
    void validToken_doesNotAbort() throws Exception {
        StubContext ctx = new StubContext(PASSWORD);

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNull();
    }

    @Test
    void wrongToken_abortsWith401() throws Exception {
        StubContext ctx = new StubContext("wrong-password");

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNotNull();
        assertThat(ctx.abortedResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void missingToken_abortsWith401() throws Exception {
        StubContext ctx = new StubContext(null);

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNotNull();
        assertThat(ctx.abortedResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void uppercaseStoredHash_stillValidates() throws Exception {
        // constantTimeHashEquals lowercases stored hash before parsing — accepts both cases
        filter = new AdminTokenFilter(PASSWORD_HASH.toUpperCase());
        StubContext ctx = new StubContext(PASSWORD);

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNull();
    }

    @Test
    void unconfiguredHash_abortsWith503() throws Exception {
        filter = new AdminTokenFilter("");
        StubContext ctx = new StubContext(PASSWORD);

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNotNull();
        assertThat(ctx.abortedResponse.getStatus()).isEqualTo(503);
    }

    @Test
    void invalidHashFormat_abortsWith401() throws Exception {
        // HexFormat.parseHex throws on garbage — constantTimeHashEquals catches and returns false → 401
        filter = new AdminTokenFilter("not-valid-hex!!");
        StubContext ctx = new StubContext(PASSWORD);

        filter.filter(ctx);

        assertThat(ctx.abortedResponse).isNotNull();
        assertThat(ctx.abortedResponse.getStatus()).isEqualTo(401);
    }

    // --- Helpers ---

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Minimal ContainerRequestContext stub — only getHeaderString and abortWith are implemented. */
    private static class StubContext implements ContainerRequestContext {

        private final String token;
        Response abortedResponse = null;

        StubContext(String token) {
            this.token = token;
        }

        @Override
        public String getHeaderString(String name) {
            return AdminTokenFilter.ADMIN_TOKEN_HEADER.equals(name) ? token : null;
        }

        @Override
        public void abortWith(Response response) {
            this.abortedResponse = response;
        }

        @Override
        public Object getProperty(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<String> getPropertyNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setProperty(String name, Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeProperty(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UriInfo getUriInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRequestUri(URI requestUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRequestUri(URI baseUri, URI requestUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Request getRequest() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMethod() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMethod(String method) {
            throw new UnsupportedOperationException();
        }

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getDate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locale getLanguage() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLength() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MediaType getMediaType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MediaType> getAcceptableMediaTypes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Locale> getAcceptableLanguages() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Cookie> getCookies() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasEntity() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getEntityStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setEntityStream(InputStream input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SecurityContext getSecurityContext() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSecurityContext(SecurityContext context) {
            throw new UnsupportedOperationException();
        }
    }
}
