package com.nursery.invoice.numbering;

import com.nursery.common.exception.BusinessException;
import com.nursery.invoice.InvoiceRepository;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNumberingService {

    private final InvoiceRepository invoiceRepository;
    private final NurseryProfileRepository profileRepository;

    public GeneratedNumber generate(UUID userId) {
        int currentYear = Year.now().getValue();

        NurseryProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "Nursery profile not set up. Please complete your profile before finalizing invoices."));

        int baseSequence = invoiceRepository.findMaxSequenceForUserAndYear(userId, currentYear);
        String initials = profile.getInitials() != null && !profile.getInitials().isBlank()
                ? profile.getInitials().toUpperCase()
                : "0000";

        int attempt = 0;
        while (attempt < 5) {
            int nextSequence = baseSequence + 1 + attempt;
            String number = String.format("INV-%s%04d", initials, nextSequence);
            if (!invoiceRepository.existsByUserIdAndInvoiceNumber(userId, number)) {
                log.debug("Generated invoice number: {} for userId: {}", number, userId);
                return new GeneratedNumber(number, currentYear, nextSequence);
            }
            log.debug("Invoice number {} already exists for userId={}, trying next sequence...", number, userId);
            attempt++;
        }

        throw new BusinessException("Unable to generate a unique invoice number. Please try again.");
    }

    public record GeneratedNumber(String invoiceNumber, int year, int sequence) {}
}
