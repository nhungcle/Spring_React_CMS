package com.framework.repository;

import com.framework.model.CmsTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CmsTemplateRepository extends JpaRepository<CmsTemplate, Long> {
    List<CmsTemplate> findByIsActiveTrue();
}
