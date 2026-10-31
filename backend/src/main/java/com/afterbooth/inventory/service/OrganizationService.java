package com.afterbooth.inventory.service;

import com.afterbooth.inventory.dto.OrganizationResponse;
import com.afterbooth.inventory.repository.OrganizationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
  private final OrganizationRepository organizations;

  OrganizationService(OrganizationRepository organizations) {
    this.organizations = organizations;
  }

  @Transactional(readOnly = true)
  public List<OrganizationResponse> list() {
    return organizations.findAllByOrderByIdAsc().stream().map(OrganizationResponse::from).toList();
  }
}
