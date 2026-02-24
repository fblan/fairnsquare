package org.asymetrik.web.fairnsquare.sharedkernel.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for {@link LogInterceptor}. Uses a {@link LoggedTestService} test bean to verify logging behavior.
 */
@QuarkusTest
class LogInterceptorTest {

    @Inject
    LoggedTestService service;

    private final CapturingHandler logHandler = new CapturingHandler();

    @BeforeEach
    void setUp() {
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(LoggedTestService.class.getName());
        julLogger.addHandler(logHandler);
        logHandler.clear();
    }

    @AfterEach
    void tearDown() {
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(LoggedTestService.class.getName());
        julLogger.removeHandler(logHandler);
    }

    @Test
    void shouldLogMethodNameAndResultOnSuccess() {
        service.greet("Alice");

        assertThat(logHandler.getMessages()).anyMatch(msg -> msg.contains("method=greet")
                && msg.contains("result=Hello, Alice") && msg.contains("duration="));
    }

    @Test
    void shouldLogTaggedParameters() {
        service.greet("Bob");

        assertThat(logHandler.getMessages()).anyMatch(msg -> msg.contains("name=Bob"));
    }

    @Test
    void shouldLogMultipleTags() {
        service.multiTag("alpha", "beta");

        assertThat(logHandler.getMessages())
                .anyMatch(msg -> msg.contains("first=alpha") && msg.contains("second=beta"));
    }

    @Test
    void shouldLogWithoutTagsWhenNonePresent() {
        service.noTags("ignored");

        assertThat(logHandler.getMessages())
                .anyMatch(msg -> msg.contains("method=noTags") && msg.contains("result=null"));
    }

    @Test
    void shouldLogErrorOnException() {
        assertThatThrownBy(() -> service.failing("fail-123")).isInstanceOf(IllegalStateException.class);

        assertThat(logHandler.getMessages())
                .anyMatch(msg -> msg.contains("method=failing") && msg.contains("id=fail-123")
                        && msg.contains("error=test failure for fail-123") && msg.contains("duration="));
    }

    @Test
    void shouldLogAtInfoLevelOnSuccess() {
        service.greet("Charlie");

        assertThat(logHandler.getRecords()).anyMatch(record -> record.getLevel().intValue() == Level.INFO.intValue()
                && record.getMessage().contains("method=greet"));
    }

    @Test
    void shouldLogAtSevereLevelOnError() {
        assertThatThrownBy(() -> service.failing("err")).isInstanceOf(IllegalStateException.class);

        assertThat(logHandler.getRecords()).anyMatch(record -> record.getLevel().intValue() == Level.SEVERE.intValue()
                && record.getMessage().contains("method=failing"));
    }

    @Test
    void shouldUnwrapPresentOptionalInLog() {
        service.findPresent("abc");

        assertThat(logHandler.getMessages())
                .anyMatch(msg -> msg.contains("method=findPresent") && msg.contains("result=found-abc"));
    }

    @Test
    void shouldLogEmptyForAbsentOptional() {
        service.findAbsent("xyz");

        assertThat(logHandler.getMessages())
                .anyMatch(msg -> msg.contains("method=findAbsent") && msg.contains("result=empty"));
    }

    @Test
    void shouldIncludeDurationInLog() {
        service.greet("Delta");

        assertThat(logHandler.getMessages()).anyMatch(msg -> msg.matches(".*duration=\\d+ms.*"));
    }

    /**
     * Simple JUL handler that captures log records in memory.
     */
    private static class CapturingHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        void clear() {
            records.clear();
        }

        List<LogRecord> getRecords() {
            return List.copyOf(records);
        }

        List<String> getMessages() {
            return records.stream().map(LogRecord::getMessage).toList();
        }
    }
}
