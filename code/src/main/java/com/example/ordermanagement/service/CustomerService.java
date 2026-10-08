package com.example.ordermanagement.service;

import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.CustomerStatusRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.dto.response.PageResponse;
import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.enums.CustomerStatus;
import com.example.ordermanagement.exception.BusinessValidationException;
import com.example.ordermanagement.exception.CustomerNotFoundException;
import com.example.ordermanagement.exception.DuplicateResourceException;
import com.example.ordermanagement.mapper.CustomerMapper;
import com.example.ordermanagement.repository.CustomerRepository;
import com.example.ordermanagement.repository.CustomerSpecifications;
import com.example.ordermanagement.util.NumberGenerator;
import com.example.ordermanagement.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("firstName", "lastName", "email", "customerNumber", "status", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final NumberGenerator numberGenerator;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("A customer with email '" + request.getEmail() + "' already exists");
        }

        Customer customer = new Customer();
        customer.setCustomerNumber(numberGenerator.generateCustomerNumber());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: {} ({})", saved.getCustomerNumber(), saved.getEmail());
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return customerMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByNumber(String customerNumber) {
        Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new CustomerNotFoundException(customerNumber));
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(CustomerStatus status, String search, Pageable pageable) {
        PageableValidator.validateSort(pageable, SORTABLE_FIELDS);
        Page<Customer> page = customerRepository.findAll(CustomerSpecifications.withFilters(status, search), pageable);
        return PageResponse.from(page, customerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findById(id);

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail())
                && customerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new DuplicateResourceException("A customer with email '" + request.getEmail() + "' already exists");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());

        Customer saved = customerRepository.save(customer);
        log.info("Customer updated: {}", saved.getCustomerNumber());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, CustomerStatusRequest request) {
        Customer customer = findById(id);
        CustomerStatus previous = customer.getStatus();
        customer.setStatus(request.getStatus());

        Customer saved = customerRepository.save(customer);
        log.info("Customer {} status changed: {} -> {}", saved.getCustomerNumber(), previous, saved.getStatus());
        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = findById(id);
        customerRepository.delete(customer);
        log.info("Customer deleted: {}", customer.getCustomerNumber());
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    public void validateCustomerIsActive(Customer customer) {
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessValidationException(
                    "Customer '" + customer.getCustomerNumber() + "' is not active (status: " + customer.getStatus() + ")");
        }
    }

}
