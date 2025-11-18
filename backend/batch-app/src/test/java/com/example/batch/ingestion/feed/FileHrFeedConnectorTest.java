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

import com.example.batch.ingestion.HrFileStorageService;
import com.example.hr.dto.HrFeedType;
import com.example.hr.dto.HrFileDescriptor;

@ExtendWith(MockitoExtension.class)
class FileHrFeedConnectorTest {

    @Mock
    private HrFileStorageService storageService;

    @Test
    void givenPendingFile_whenNextFeed_thenRegistersDescriptor() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), HrFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willReturn("payload");
        FileHrFeedConnector connector = new FileHrFeedConnector(storageService);

        Optional<HrFeed> feed = connector.nextFeed();

        assertThat(feed).isPresent();
        connector.onSuccess(feed.get());
        verify(storageService).markProcessed(descriptor, true);
    }

    @Test
    void givenReadFailure_whenNextFeed_thenMarkFailedOnce() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), HrFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willThrow(new IOException("broken"));
        FileHrFeedConnector connector = new FileHrFeedConnector(storageService);

        assertThat(connector.nextFeed()).isEmpty();
        verify(storageService).markProcessed(descriptor, false);
    }

    @Test
    void givenFailureAfterFeed_whenOnFailure_thenMarkProcessed() throws IOException {
        HrFileDescriptor descriptor = new HrFileDescriptor("employees.csv", LocalDate.now(), 1,
                java.nio.file.Path.of("/tmp/employees.csv"), HrFeedType.EMPLOYEE);
        given(storageService.nextPendingFile()).willReturn(Optional.of(descriptor));
        given(storageService.readPayload(descriptor)).willReturn("payload");
        FileHrFeedConnector connector = new FileHrFeedConnector(storageService);

        HrFeed feed = connector.nextFeed().orElseThrow();
        connector.onFailure(feed, new IllegalStateException("error"));
        verify(storageService).markProcessed(descriptor, false);
    }
}
