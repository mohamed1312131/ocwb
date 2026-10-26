package com.omnicare.repository;

import com.omnicare.model.SharedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedDocumentRepository extends JpaRepository<SharedDocument, Long> {
    Optional<SharedDocument> findByShareToken(String shareToken);
    List<SharedDocument> findAllByOrderByCreatedAtDesc();
}
