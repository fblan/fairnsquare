package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageStats;
import org.junit.jupiter.api.Test;

class StorageStatsTest {

    // -------------------------------------------------------------------------
    // formattedUsedSize unit tests
    // -------------------------------------------------------------------------

    @Test
    void shouldFormatUsedSizeInKo() {
        StorageStats stats = new StorageStats(512, 1024, 1);
        assertThat(stats.formattedUsedSize()).isEqualTo("0.50Ko");
    }

    @Test
    void shouldFormatUsedSizeInMo() {
        long twoMb = 2L * 1024 * 1024;
        StorageStats stats = new StorageStats(twoMb, twoMb * 10, 1);
        assertThat(stats.formattedUsedSize()).isEqualTo("2.00Mo");
    }

    @Test
    void shouldFormatUsedSizeInGo() {
        long twoGb = 2L * 1024 * 1024 * 1024;
        StorageStats stats = new StorageStats(twoGb, twoGb * 10, 1);
        assertThat(stats.formattedUsedSize()).isEqualTo("2.00Go");
    }

    // -------------------------------------------------------------------------
    // remainingPercent edge case
    // -------------------------------------------------------------------------

    @Test
    void shouldReturn100PercentRemainingWhenMaxIsZero() {
        StorageStats stats = new StorageStats(0, 0, 0);
        assertThat(stats.remainingPercent()).isEqualTo(100.0);
    }

    @Test
    void shouldReturnCorrectRemainingPercent() {
        StorageStats stats = new StorageStats(512, 1024, 1);
        assertThat(stats.remainingPercent()).isEqualTo(50.0);
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    void shouldIncludeAllFieldsInToString() {
        long twoMb = 2L * 1024 * 1024;
        StorageStats stats = new StorageStats(twoMb, twoMb * 10, 3);

        String result = stats.toString();

        assertThat(result).contains("usedSize=2.00Mo");
        assertThat(result).contains("remainingMo=");
        assertThat(result).contains("remainingPct=");
        assertThat(result).contains("fileCount=3");
    }
}
