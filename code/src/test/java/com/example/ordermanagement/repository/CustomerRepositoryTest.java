package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Customer;
import com.example.ordermanagement.enums.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        customerRepository.save(buildCustomer("CUST-T001", "Alice", "Smith",  "alice@example.com",  CustomerStatus.ACTIVE));
        customerRepository.save(buildCustomer("CUST-T002", "Bob",   "Jones",  "bob@example.com",    CustomerStatus.ACTIVE));
        customerRepository.save(buildCustomer("CUST-T003", "Carol", "Wilson", "carol@example.com",  CustomerStatus.INACTIVE));
        customerRepository.save(buildCustomer("CUST-T004", "Dave",  "Brown",  "dave@example.com",   CustomerStatus.BLOCKED));
    }

    @Test
    void findByCustomerNumber_returnsCorrectCustomer() {
        Optional<Customer> result = customerRepository.findByCustomerNumber("CUST-T002");

        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("Bob");
    }

    @Test
    void existsByEmail_withExistingEmail_returnsTrue() {
        assertThat(customerRepository.existsByEmailIgnoreCase("carol@example.com")).isTrue();
    }

    @Test
    void existsByEmail_isCaseInsensitive() {
        assertThat(customerRepository.existsByEmailIgnoreCase("CAROL@Example.com")).isTrue();
    }

    @Test
    void existsByEmail_withNonExistingEmail_returnsFalse() {
        assertThat(customerRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void filter_byStatus_returnsOnlyActiveCustomers() {
        Page<Customer> result = customerRepository.findAll(
                CustomerSpecifications.withFilters(CustomerStatus.ACTIVE, null), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(c -> c.getStatus() == CustomerStatus.ACTIVE);
    }

    @Test
    void filter_withNameSearch_returnsMatchingCustomers() {
        Page<Customer> result = customerRepository.findAll(
                CustomerSpecifications.withFilters(CustomerStatus.ACTIVE, "alice"), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void filter_withNullStatus_returnsAllMatchingSearch() {
        Page<Customer> result = customerRepository.findAll(
                CustomerSpecifications.withFilters(null, "example.com"), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void filter_withNullStatusAndNullSearch_returnsAll() {
        Page<Customer> result = customerRepository.findAll(
                CustomerSpecifications.withFilters(null, null), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    private Customer buildCustomer(String number, String first, String last,
                                   String email, CustomerStatus status) {
        Customer c = new Customer();
        c.setCustomerNumber(number);
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setStatus(status);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }
}
