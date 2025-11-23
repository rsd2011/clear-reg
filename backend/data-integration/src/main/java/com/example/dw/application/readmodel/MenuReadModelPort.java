package com.example.dw.application.readmodel;

import java.util.Optional;

public interface MenuReadModelPort {

    boolean isEnabled();

    Optional<MenuReadModel> load();

    MenuReadModel rebuild();

    void evict();
}
