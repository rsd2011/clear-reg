package com.example.dw.application.readmodel;

import java.util.Optional;

public interface PermissionMenuReadModelPort {

    boolean isEnabled();

    Optional<PermissionMenuReadModel> load(String principalId);

    PermissionMenuReadModel rebuild(String principalId);

    void evict(String principalId);
}
