package com.example.dw.application.port;

import com.example.dw.application.dto.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;

/**
 * Port interface to interact with DW ingestion policy services.
 */
public interface DwIngestionPolicyPort {

    DwIngestionPolicyView currentPolicy();

    DwIngestionPolicyView updatePolicy(DwIngestionPolicyUpdateRequest request);
}
