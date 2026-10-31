package com.afterbooth.inventory.repository;

import com.afterbooth.inventory.domain.Organization;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
  List<Organization> findAllByOrderByIdAsc();
}
