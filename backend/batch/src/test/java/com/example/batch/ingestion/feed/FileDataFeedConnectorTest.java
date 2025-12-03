package com.example.batch.ingestion.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.batch.ingestion.DwFileStorageService;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrFileDescriptor;

@ExtendWith(MockitoExtension.class)
class FileDataFeedConnectorTest {

    @Mock
    private DwFileStorageService storageService;

    @Test
    void givenPendingFile_whenNextFeed_thenRegistersDescriptor() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), DataFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willReturn("payload");
        FileDataFeedConnector connector = new FileDataFeedConnector(storageService);

        Optional<DataFeed> feed = connector.nextFeed();

        assertThat(feed).isPresent();
        connector.onSuccess(feed.get());
        verify(storageService).markProcessed(descriptor, true);
    }

    @Test
    void givenReadFailure_whenNextFeed_thenMarkFailedOnce() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), DataFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willThrow(new IOException("broken"));
        FileDataFeedConnector connector = new FileDataFeedConnector(storageService);

        assertThat(connector.nextFeed()).isEmpty();
        verify(storageService).markProcessed(descriptor, false);
    }

    @Test
    void givenFailureAfterFeed_whenOnFailure_thenMarkProcessed() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), DataFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willReturn("payload");
        FileDataFeedConnector connector = new FileDataFeedConnector(storageService);

        DataFeed feed = connector.nextFeed().orElseThrow();
        connector.onFailure(feed, new IllegalStateException("error"));
        verify(storageService).markProcessed(descriptor, false);
    }

    @Test
    void onSuccessWithUnknownFeedDoesNotCallStorage() {
        FileDataFeedConnector connector = new FileDataFeedConnector(storageService);
        DataFeed feed = new DataFeed("missing", DataFeedType.EMPLOYEE, LocalDate.now(), 1, "p", "FILE", java.util.Map.of());

        connector.onSuccess(feed);

        org.mockito.Mockito.verify(storageService, org.mockito.Mockito.never()).markProcessed(org.mockito.Mockito.any(), org.mockito.Mockito.anyBoolean());
    }

    @Test
    void givenNoPendingFile_whenNextFeed_thenEmptyAndNoMark() {
        given(storageService.nextPendingFile()).willReturn(Optional.empty());
        FileDataFeedConnector connector = new FileDataFeedConnector(storageService);

        assertThat(connector.nextFeed()).isEmpty();
    }
}
