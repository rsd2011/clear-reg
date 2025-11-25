package com.example.dwgateway.dw;

import com.example.dw.application.policy.DwIngestionPolicyUpdateRequest;
import com.example.dw.application.policy.DwIngestionPolicyView;

/**
 * Port interface to interact with DW ingestion policy services.
 */
public interface DwIngestionPolicyPort {

    DwIngestionPolicyView currentPolicy();

    DwIngestionPolicyView updatePolicy(DwIngestionPolicyUpdateRequest request);
}
