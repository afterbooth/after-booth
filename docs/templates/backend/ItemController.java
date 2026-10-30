package com.afterbooth.inventory.controller;

import com.afterbooth.inventory.dto.ItemResponse;
import com.afterbooth.inventory.dto.RegisterItemRequest;
import com.afterbooth.inventory.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TEMPLATE: controller. HTTP only: bind, validate, call one service method, return a DTO.
// Paths and shapes must match contract/openapi.yaml.
@RestController
@RequestMapping("/api/organizations/{organizationId}/items")
class ItemController {

  private final ItemService itemService;

  ItemController(ItemService itemService) {
    this.itemService = itemService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ItemResponse register(
      @PathVariable Long organizationId, @Valid @RequestBody RegisterItemRequest request) {
    return itemService.register(organizationId, request);
  }

  @GetMapping("/{itemId}")
  ItemResponse get(@PathVariable Long itemId) {
    return itemService.get(itemId);
  }
}
