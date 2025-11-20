package com.example.dw.application.job;

public interface DwIngestionJobQueue {

    void enqueue(DwIngestionJob job);
}
