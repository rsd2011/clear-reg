package com.example.server.policy;

import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.example.policy.dto.PolicyYamlRequest;

/**
 * Port interface exposing policy administration operations required by the server module.
 */
public interface PolicyAdminPort {

    PolicyView currentPolicy();

    PolicyView updateToggles(PolicyUpdateRequest request);

    PolicyView updateFromYaml(PolicyYamlRequest request);
}
