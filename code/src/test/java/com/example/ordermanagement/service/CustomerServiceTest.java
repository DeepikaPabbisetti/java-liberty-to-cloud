package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.CustomerStatusRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.enums.CustomerStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.CustomerNotFoundException;
import com.example.ordermanagement.exception.DuplicateResourceException;
import com.example.ordermanagement.mapper.CustomerMapper;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.util.NumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerMapper customerMapper;
    @Mock private NumberGenerator numberGenerator;

    @InjectMocks
    private CustomerService customerService;

    private Customer activeCustomer;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        activeCustomer = new Customer();
        activeCustomer.setId(1L);
        activeCustomer.setCustomerNumber("CUST-1001");
        activeCustomer.setFirstName("James");
        activeCustomer.setLastName("Anderson");
        activeCustomer.setEmail("james.anderson@example.com");
        activeCustomer.setStatus(CustomerStatus.ACTIVE);
        activeCustomer.setCreatedAt(LocalDateTime.now());
        activeCustomer.setUpdatedAt(LocalDateTime.now());

        customerResponse = CustomerResponse.builder()
                .id(1L)
                .customerNumber("CUST-1001")
                .firstName("James")
                .lastName("Anderson")
                .email("james.anderson@example.com")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    @Test
    void createCustomer_withValidRequest_returnsCreatedCustomer() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("James");
        request.setLastName("Anderson");
        request.setEmail("james.anderson@example.com");

        when(customerRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(numberGenerator.generateCustomerNumber()).thenReturn("CUST-1001");
        when(customerRepository.save(any(Customer.class))).thenReturn(activeCustomer);
        when(customerMapper.toResponse(activeCustomer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.createCustomer(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("james.anderson@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_withDuplicateEmail_throwsDuplicateResourceException() {
        CustomerRequest request = new CustomerRequest();
        request.setEmail("james.anderson@example.com");

        when(customerRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("james.anderson@example.com");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getCustomerById_withExistingId_returnsCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(customerMapper.toResponse(activeCustomer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.getCustomerById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getCustomerById_withNonExistingId_throwsCustomerNotFoundException() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateCustomerStatus_changesStatusSuccessfully() {
        CustomerStatusRequest request = new CustomerStatusRequest();
        request.setStatus(CustomerStatus.INACTIVE);

        Customer updatedCustomer = new Customer();
        updatedCustomer.setId(1L);
        updatedCustomer.setCustomerNumber("CUST-1001");
        updatedCustomer.setEmail("james.anderson@example.com");
        updatedCustomer.setStatus(CustomerStatus.INACTIVE);
        updatedCustomer.setCreatedAt(LocalDateTime.now());
        updatedCustomer.setUpdatedAt(LocalDateTime.now());

        CustomerResponse inactiveResponse = CustomerResponse.builder()
                .id(1L)
                .status(CustomerStatus.INACTIVE)
                .build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);
        when(customerMapper.toResponse(updatedCustomer)).thenReturn(inactiveResponse);

        CustomerResponse result = customerService.updateCustomerStatus(1L, request);

        assertThat(result.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    void validateCustomerIsActive_withActiveCustomer_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> customerService.validateCustomerIsActive(activeCustomer));
    }

    @Test
    void validateCustomerIsActive_withInactiveCustomer_throwsBusinessValidationException() {
        activeCustomer.setStatus(CustomerStatus.INACTIVE);

        assertThatThrownBy(() -> customerService.validateCustomerIsActive(activeCustomer))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void validateCustomerIsActive_withBlockedCustomer_throwsBusinessValidationException() {
        activeCustomer.setStatus(CustomerStatus.BLOCKED);

        assertThatThrownBy(() -> customerService.validateCustomerIsActive(activeCustomer))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void deleteCustomer_withExistingCustomer_deletesSuccessfully() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(activeCustomer));

        customerService.deleteCustomer(1L);

        verify(customerRepository).delete(activeCustomer);
    }
}
