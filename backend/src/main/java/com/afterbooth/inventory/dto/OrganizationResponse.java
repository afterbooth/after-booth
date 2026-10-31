package com.afterbooth.inventory.dto;

import com.afterbooth.inventory.domain.Organization;

public record OrganizationResponse(Long id, String name) {
  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(organization.id(), organization.name());
  }
}
