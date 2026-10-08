package com.example.ordermanagement.controller;

import com.example.ordermanagement.BaseIntegrationTest;
import com.example.ordermanagement.dto.request.CustomerRequest;
import com.example.ordermanagement.dto.request.CustomerStatusRequest;
import com.example.ordermanagement.dto.response.CustomerResponse;
import com.example.ordermanagement.enums.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createCustomer_withValidRequest_returns201() {
        CustomerRequest request = buildCustomerRequest("integration@example.com");

        ResponseEntity<CustomerResponse> response =
                restTemplate.postForEntity("/api/customers", request, CustomerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("integration@example.com");
        assertThat(response.getBody().getCustomerNumber()).startsWith("CUST-");
        assertThat(response.getBody().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void createCustomer_withDuplicateEmail_returns409() {
        CustomerRequest request = buildCustomerRequest("duplicate@example.com");
        restTemplate.postForEntity("/api/customers", request, CustomerResponse.class);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/customers", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createCustomer_withMissingEmail_returns400() {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Test");
        request.setLastName("User");

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/customers", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getCustomerById_withExistingCustomer_returns200() {
        CustomerRequest request = buildCustomerRequest("getbyid@example.com");
        CustomerResponse created = restTemplate
                .postForEntity("/api/customers", request, CustomerResponse.class)
                .getBody();

        assertThat(created).isNotNull();

        ResponseEntity<CustomerResponse> response =
                restTemplate.getForEntity("/api/customers/" + created.getId(), CustomerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
    }

    @Test
    void getCustomerById_withNonExistingId_returns404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/customers/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCustomerStatus_toInactive_returns200() {
        CustomerRequest request = buildCustomerRequest("statusupdate@example.com");
        CustomerResponse created = restTemplate
                .postForEntity("/api/customers", request, CustomerResponse.class)
                .getBody();

        assertThat(created).isNotNull();

        CustomerStatusRequest statusRequest = new CustomerStatusRequest();
        statusRequest.setStatus(CustomerStatus.INACTIVE);

        ResponseEntity<CustomerResponse> response = restTemplate.exchange(
                "/api/customers/" + created.getId() + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(statusRequest),
                CustomerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    void listCustomers_returns200WithPagedResults() {
        restTemplate.postForEntity("/api/customers", buildCustomerRequest("list1@example.com"), CustomerResponse.class);
        restTemplate.postForEntity("/api/customers", buildCustomerRequest("list2@example.com"), CustomerResponse.class);

        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/customers?page=0&size=10", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
        assertThat(response.getBody()).contains("totalElements");
    }

    private CustomerRequest buildCustomerRequest(String email) {
        CustomerRequest request = new CustomerRequest();
        request.setFirstName("Integration");
        request.setLastName("Test");
        request.setEmail(email);
        request.setPhone("+1-555-9999");
        request.setCity("Test City");
        request.setCountry("USA");
        return request;
    }
}
