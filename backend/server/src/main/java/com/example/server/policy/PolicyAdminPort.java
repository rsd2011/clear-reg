package com.example.server.policy;

import com.example.admin.policy.dto.PolicyUpdateRequest;
import com.example.admin.policy.dto.PolicyView;
import com.example.admin.policy.dto.PolicyYamlRequest;

/**
 * Port interface exposing policy administration operations required by the server module.
 */
public interface PolicyAdminPort {

    PolicyView currentPolicy();

    PolicyView updateToggles(PolicyUpdateRequest request);

    PolicyView updateFromYaml(PolicyYamlRequest request);
}
