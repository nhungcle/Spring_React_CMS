package com.framework.repository;

import com.framework.model.MainMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MainMenuRepository extends JpaRepository<MainMenuItem, Long> {

    List<MainMenuItem> findAllByOrderByOrderAsc();
}
