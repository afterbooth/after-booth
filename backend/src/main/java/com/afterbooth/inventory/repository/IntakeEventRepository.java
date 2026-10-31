package com.afterbooth.inventory.repository;

import com.afterbooth.inventory.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeEventRepository extends JpaRepository<Event, Long> {}
