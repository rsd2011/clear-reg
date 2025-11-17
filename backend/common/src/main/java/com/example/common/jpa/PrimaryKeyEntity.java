package com.example.common.jpa;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Persistable;
import org.springframework.data.util.ProxyUtils;

import com.github.f4b6a3.ulid.UlidCreator;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

@MappedSuperclass
public abstract class PrimaryKeyEntity implements Persistable<UUID>, Serializable {

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id = UlidCreator.getMonotonicUlid().toUuid();

    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (ProxyUtils.getUserClass(this) != ProxyUtils.getUserClass(other)) {
            return false;
        }
        PrimaryKeyEntity that = (PrimaryKeyEntity) other;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
