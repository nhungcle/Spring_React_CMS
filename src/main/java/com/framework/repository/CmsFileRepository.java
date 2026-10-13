package com.framework.repository;

import com.framework.model.CmsFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CmsFileRepository extends JpaRepository<CmsFile, Long> {
}
