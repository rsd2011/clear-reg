package com.example.dw.application.export;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingExportFailureNotifier implements ExportFailureNotifier {
    @Override
    public void notify(ExportFailureEvent event) {
        log.warn("export failed: type={}, file={}, count={}, result={}",
                event.getExportType(), event.getFileName(), event.getRecordCount(), event.getResultCode());
    }
}
