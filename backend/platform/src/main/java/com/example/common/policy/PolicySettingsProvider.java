package com.example.common.policy;

/**
 * Provider abstraction that exposes the current policy toggle snapshot to consumers.
 */
public interface PolicySettingsProvider {

    /**
     * Returns the currently active toggle configuration loaded from the policy service.
     *
     * @return immutable snapshot
     */
    PolicyToggleSettings currentSettings();
}
