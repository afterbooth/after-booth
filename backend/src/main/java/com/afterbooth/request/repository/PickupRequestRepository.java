package com.afterbooth.request.repository;

import com.afterbooth.request.domain.PickupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupRequestRepository extends JpaRepository<PickupRequest, Long> {}
