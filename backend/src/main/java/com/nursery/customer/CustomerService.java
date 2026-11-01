package com.nursery.customer;

import com.nursery.common.exception.ResourceNotFoundException;
import com.nursery.config.SecurityUtils;
import com.nursery.customer.dto.CustomerDtos.*;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final InvoiceRepository invoiceRepository;
    private final NurseryProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<CustomerSummary> list(UUID userId, String search) {
        List<Customer> customers = search == null || search.isBlank()
                ? customerRepository.findForUser(userId)
                : customerRepository.findForUserWithSearch(userId, search.trim());
        return customers
                .stream().map(c -> new CustomerSummary(c.getId(), c.getName(), c.getPhone(), c.getAddress(),
                        quotationRepository.countByCustomerIdAndUserId(c.getId(), userId),
                        invoiceRepository.countByCustomerIdAndUserId(c.getId(), userId), c.getUpdatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDetail get(UUID userId, Long id) {
        Customer c = owned(userId, id);
        NurseryProfile profile = profileRepository.findByUserId(userId).orElse(null);
        return new CustomerDetail(c.getId(), c.getName(), c.getPhone(), c.getAddress(), c.getCreatedAt(), c.getUpdatedAt(),
                quotationRepository.findByCustomerIdAndUserId(c.getId(), userId).stream()
                        .map(q -> new CustomerDocument(q.getId(), q.getQuotationNumber() == null ? "QTN-" + q.getId() : q.getQuotationNumber(), q.getStatus().name(), q.getCustomerName(), q.getGrandTotal(), q.getCreatedAt())).toList(),
                invoiceRepository.findByCustomerIdAndUserId(c.getId(), userId).stream()
                        .map(i -> new CustomerDocument(i.getId(), formatInvoiceNumber(profile, i.getId(), i.getSequence(), i.getYear()), i.getStatus().name(), i.getCustomerName(), i.getGrandTotal(), i.getCreatedAt())).toList());
    }

    @Transactional
    public CustomerSummary update(UUID userId, Long id, CustomerRequest request) {
        Customer c = owned(userId, id);
        c.setName(request.name().trim());
        c.setPhone(normalize(request.phone()));
        c.setAddress(normalize(request.address()));
        Customer saved = customerRepository.save(c);
        return new CustomerSummary(saved.getId(), saved.getName(), saved.getPhone(), saved.getAddress(),
                quotationRepository.countByCustomerIdAndUserId(id, userId),
                invoiceRepository.countByCustomerIdAndUserId(id, userId), saved.getUpdatedAt());
    }

    @Transactional
    public Customer findOrCreate(UUID userId, String name, String phone, String address) {
        String normalizedName = name == null ? null : name.trim();
        if (normalizedName == null || normalizedName.isBlank()) return null;
        String normalizedPhone = normalize(phone);
        String normalizedAddress = normalize(address);
        return customerRepository.findByUserIdAndNameIgnoreCase(userId, normalizedName).stream()
                .filter(customer -> sameIgnoreCase(customer.getPhone(), normalizedPhone)
                        && sameIgnoreCase(customer.getAddress(), normalizedAddress))
                .findFirst()
                .orElseGet(() -> customerRepository.save(Customer.builder().userId(userId).name(normalizedName)
                        .phone(normalizedPhone).address(normalizedAddress).build()));
    }

    private Customer owned(UUID userId, Long id) {
        return customerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    public static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean sameIgnoreCase(String left, String right) {
        return left == null ? right == null : left.equalsIgnoreCase(right);
    }

    private String formatInvoiceNumber(NurseryProfile profile, Long id, Integer sequence, Integer year) {
        if (sequence == null || year == null) return "INV-" + id;
        String initials = profile != null && profile.getInitials() != null && !profile.getInitials().isBlank()
                ? profile.getInitials().toUpperCase() : "INV";
        return String.format("INV-%s%04d", initials, sequence);
    }
}
