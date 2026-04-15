package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.StorageStats;
import org.junit.jupiter.api.Test;

class StorageStatsTest {

    @Test
    void shouldReturnCorrectRemainingFileCount() {
        StorageStats stats = new StorageStats(3000, 5000, 0, 0);
        assertThat(stats.remainingFileCount()).isEqualTo(2000);
    }

    @Test
    void shouldReturnZeroRemainingWhenAtLimit() {
        StorageStats stats = new StorageStats(5000, 5000, 0, 0);
        assertThat(stats.remainingFileCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnZeroRemainingWhenOverLimit() {
        StorageStats stats = new StorageStats(5001, 5000, 0, 0);
        assertThat(stats.remainingFileCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnCorrectUsedPercent() {
        StorageStats stats = new StorageStats(2500, 5000, 0, 0);
        assertThat(stats.usedPercent()).isEqualTo(50.0);
    }

    @Test
    void shouldReturn100PercentWhenMaxIsZero() {
        StorageStats stats = new StorageStats(0, 0, 0, 0);
        assertThat(stats.usedPercent()).isEqualTo(100.0);
    }

    @Test
    void shouldIncludeAllFieldsInToString() {
        StorageStats stats = new StorageStats(1000, 5000, 512_000, 2_621_440_000L);
        String result = stats.toString();
        assertThat(result).contains("fileCount=1000");
        assertThat(result).contains("maxFileCount=5000");
        assertThat(result).contains("usedPct=");
        assertThat(result).contains("usedBytes=512000");
        assertThat(result).contains("maxTotalBytes=");
    }
}
