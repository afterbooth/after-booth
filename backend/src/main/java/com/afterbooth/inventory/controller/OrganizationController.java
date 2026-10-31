package com.afterbooth.inventory.controller;

import com.afterbooth.inventory.dto.OrganizationResponse;
import com.afterbooth.inventory.service.OrganizationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
class OrganizationController {
  private final OrganizationService organizations;

  OrganizationController(OrganizationService organizations) {
    this.organizations = organizations;
  }

  @GetMapping
  List<OrganizationResponse> listOrganizations() {
    return organizations.list();
  }
}
