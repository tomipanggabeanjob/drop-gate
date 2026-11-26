package com.dropgate.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory/{dropId}")
    public InventorySnapshot getInventory(@PathVariable String dropId) {
        return inventoryService.getInventory(dropId);
    }

    @PostMapping("/inventory/holds")
    public InventoryHoldResponse createHold(@RequestBody InventoryHoldRequest request) {
        return inventoryService.placeHold(request);
    }

    @PostMapping("/drops/{dropId}/holds")
    public InventoryHoldResponse createDropHold(
            @PathVariable String dropId,
            @RequestBody InventoryHoldRequest request) {
        InventoryHoldRequest normalized = request.dropId() == null || request.dropId().isBlank()
                ? new InventoryHoldRequest(dropId, request.accountId(), request.quantity())
                : request;
        return inventoryService.placeHold(normalized);
    }
}
