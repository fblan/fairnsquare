package org.asymetrik.web.fairnsquare.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import io.restassured.RestAssured;
import io.vidocq.runtime.core.VidocqBootstrap;

/**
 * The extension behind {@link FairnsquareTest}. The first test class boots vidocq in-process, on a free port and a
 * fresh temporary data directory; a class whose settings differ ({@link FairnsquareTest#maxFileCount()}) stops it and
 * boots another. JUnit stops the last one at the end of the run, which deletes its data directory.
 */
public final class FairnsquareTestRuntime implements BeforeAllCallback, TestInstancePostProcessor {

    /** Signs the tests' CAPTCHA tokens ({@code CaptchaTokenTestHelper}). */
    public static final String CAPTCHA_SECRET = "change-me-in-test";

    /** SHA-256 of {@code test-admin-secret-fairnsquare}, the tests' admin password. */
    public static final String ADMIN_PASSWORD_HASH = "b19eca9b698d7465f938dca5e4ce7feb6f814af893b81fc30ea790164191fa76";

    private static final int DEFAULT_MAX_FILE_COUNT = 1000;
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create(FairnsquareTestRuntime.class);

    private static Runtime current;

    @Override
    public void beforeAll(ExtensionContext context) {
        FairnsquareTest settings = context.getRequiredTestClass().getAnnotation(FairnsquareTest.class);
        int maxFileCount = settings == null || settings.maxFileCount() == 0 ? DEFAULT_MAX_FILE_COUNT
                : settings.maxFileCount();
        synchronized (FairnsquareTestRuntime.class) {
            if (current != null && current.maxFileCount() != maxFileCount) {
                current.close();
                current = null;
            }
            if (current == null) {
                current = Runtime.start(maxFileCount);
                // Stops the last one at the end of the run.
                context.getRoot().getStore(NAMESPACE).computeIfAbsent(Stopper.class, key -> new Stopper(),
                        Stopper.class);
            }
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws IllegalAccessException {
        beforeAll(context);
        // Its superclasses' fields too, for test classes that share their fixtures.
        for (Class<?> type = testInstance.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Inject.class)) {
                    field.setAccessible(true);
                    field.set(testInstance, CDI.current().select(field.getType()).get());
                }
            }
        }
    }

    /** The application's data directory, for tests that look at the files. */
    public static Path dataDirectory() {
        return current.dataDirectory();
    }

    private static final class Stopper implements AutoCloseable {

        @Override
        public void close() {
            synchronized (FairnsquareTestRuntime.class) {
                if (current != null) {
                    current.close();
                    current = null;
                }
            }
        }
    }

    private record Runtime(VidocqBootstrap vidocq, int maxFileCount, Path dataDirectory) implements AutoCloseable {

        static Runtime start(int maxFileCount) {
            Path dataDirectory = createDataDirectory();
            int port = freePort();
            // System properties outrank vidocq.properties.
            System.setProperty("vidocq.http.port", Integer.toString(port));
            System.setProperty("fairnsquare.data.path", dataDirectory.toString());
            System.setProperty("fairnsquare.storage.max-file-count", Integer.toString(maxFileCount));
            System.setProperty("fairnsquare.storage.max-file-age-days", "30");
            System.setProperty("captcha.secret", CAPTCHA_SECRET);
            System.setProperty("admin.password-hash", ADMIN_PASSWORD_HASH);
            VidocqBootstrap vidocq = VidocqBootstrap.create().configure().start();
            RestAssured.port = port;
            return new Runtime(vidocq, maxFileCount, dataDirectory);
        }

        @Override
        public void close() {
            vidocq.shutdown();
            // FileSystemServiceTest empties it, the directory too.
            if (!Files.exists(dataDirectory)) {
                return;
            }
            try (Stream<Path> files = Files.walk(dataDirectory)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Path createDataDirectory() {
            try {
                return Files.createTempDirectory("fairnsquare-test-");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static int freePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
