package com.middle.api.repository;

import com.middle.api.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FindingRepository extends JpaRepository<Finding, Long> {
    List<Finding> findByMediaId(Long mediaId);
}