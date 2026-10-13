package com.framework.repository;

import com.framework.model.AdminMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMenuRepository extends JpaRepository<AdminMenuItem, Long> {

    List<AdminMenuItem> findAllByOrderByOrderAsc();
}
