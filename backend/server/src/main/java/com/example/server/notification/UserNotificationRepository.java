package com.example.server.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByRecipientUsernameOrderByCreatedAtDesc(String recipientUsername);

    Optional<UserNotification> findByIdAndRecipientUsername(UUID id, String recipientUsername);
}
