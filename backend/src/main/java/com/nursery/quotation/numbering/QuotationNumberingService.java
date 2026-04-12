package com.nursery.quotation.numbering;

import com.nursery.common.exception.BusinessException;
import com.nursery.profile.NurseryProfile;
import com.nursery.profile.NurseryProfileRepository;
import com.nursery.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

/**
 * Generates quotation numbers in the format: ABCDXXXX (8 characters)
 *
 * - ABCD  = first letter of each word in nursery name (uppercase)
 * - XXXX  = zero-padded sequential number (4 digits, e.g., 0001, 0002, etc.)
 *
 * Example: SUNS0001 (Sri Uma Nursery Suppliers, sequence 1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotationNumberingService {

    private final QuotationRepository quotationRepository;
    private final NurseryProfileRepository profileRepository;

    public GeneratedNumber generate(UUID userId) {
        int currentYear = Year.now().getValue();

        NurseryProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        "Nursery profile not set up. Please complete your profile before finalizing quotations."));

        int baseSequence = quotationRepository.findMaxSequenceForUserAndYear(userId, currentYear);
        String initials = profile.getInitials().toUpperCase();

        int attempt = 0;
        while (attempt < 5) {
            int nextSequence = baseSequence + 1 + attempt;
            String number = String.format("%s%04d", initials, nextSequence);

            if (!quotationRepository.existsByQuotationNumber(number)) {
                log.debug("Generated quotation number: {} for userId: {}", number, userId);
                return new GeneratedNumber(number, currentYear, nextSequence);
            }

            attempt++;
        }

        throw new BusinessException("Unable to generate a unique quotation number. Please try again.");
    }

    public record GeneratedNumber(String quotationNumber, int year, int sequence) {}
}
