package com.example.ordermanagement.controller;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.CustomerStatusRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.dto.response.PageResponse;
import com.example.ordermanagement.enums.CustomerStatus;
import com.example.ordermanagement.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management APIs")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/number/{customerNumber}")
    @Operation(summary = "Get customer by customer number")
    public ResponseEntity<CustomerResponse> getCustomerByNumber(@PathVariable String customerNumber) {
        return ResponseEntity.ok(customerService.getCustomerByNumber(customerNumber));
    }

    @GetMapping
    @Operation(summary = "List customers with optional filtering and pagination")
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(customerService.getCustomers(status, search, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer details")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update customer status")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @PathVariable Long id,
            @Valid @RequestBody CustomerStatusRequest request) {
        return ResponseEntity.ok(customerService.updateCustomerStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
