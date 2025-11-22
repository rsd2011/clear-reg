package com.example.batch.ingestion.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;

class DataFeedConnectorTest {

    @Test
    @DisplayName("기본 onSuccess/onFailure 구현은 예외를 던지지 않는다")
    void defaultHooksAreNoOp() {
        DataFeedConnector connector = new SimpleConnector();
        DataFeed feed = sampleFeed();

        connector.onSuccess(feed);
        connector.onFailure(feed, new RuntimeException("boom"));

        assertThat(connector.name()).isEqualTo("test-source");
    }

    @Test
    @DisplayName("nextFeed가 비었을 때 Optional.empty를 반환한다")
    void nextFeedOptionalEmpty() {
        DataFeedConnector connector = new SimpleConnector();
        assertThat(connector.nextFeed()).isEmpty();
    }

    @Test
    @DisplayName("readNext가 예외를 던지면 onFailure가 호출된다")
    void nextFeedFailureTriggersOnFailure() {
        FailingConnector connector = new FailingConnector();

        assertThat(connector.nextFeed()).isEmpty();
        assertThat(connector.failureCount).isEqualTo(1);
    }

    private DataFeed sampleFeed() {
        return new DataFeed("id", DataFeedType.EMPLOYEE, LocalDate.now(), 1, "payload", "src", Map.of());
    }

    private static class SimpleConnector implements DataFeedConnector {
        @Override
        public Optional<DataFeed> nextFeed() {
            return Optional.empty();
        }

        @Override
        public String name() {
            return "test-source";
        }
    }

    private static class FailingConnector implements DataFeedConnector {
        int failureCount = 0;

        @Override
        public Optional<DataFeed> nextFeed() {
            try {
                throw new RuntimeException("repo down");
            } catch (Exception ex) {
                onFailure(null, ex);
                return Optional.empty();
            }
        }

        @Override
        public void onFailure(DataFeed feed, Exception ex) {
            failureCount++;
        }

        @Override
        public String name() {
            return "failing-source";
        }
    }
}
