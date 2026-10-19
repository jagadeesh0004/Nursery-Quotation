package com.nursery.quotation.numbering;

import com.nursery.common.exception.BusinessException;
import com.nursery.pdf.profile.NurseryProfile;
import com.nursery.pdf.profile.NurseryProfileRepository;
import com.nursery.quotation.QuotationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

/**
 * Generates quotation numbers in the format: AAAABBBB (8 characters)
 *
 * - AAAA  = first letter of each word in nursery name (up to 4), padded with zeros
 * - BBBB  = zero-padded sequential number (4 digits, e.g., 0001, 0002, etc.)
 *
 * Examples:
 *   "Go Green Nursery Ltd" → GGNL0001, GGNL0002
 *   "Go Green Nursery" → GGN00001, GGN00002
 *   "Go Green" → GG000001, GG000002
 *   "Garden" → G0000001, G0000002
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

            //Check uniqueness per-user, not globally
            if (!quotationRepository.existsByUserIdAndQuotationNumber(userId, number)) {
                log.debug("Generated quotation number: {} for userId: {}", number, userId);
                return new GeneratedNumber(number, currentYear, nextSequence);
            }

            log.debug("Number {} already exists for user, trying next sequence...", number);
            attempt++;
        }

        // this should rarely happen now - only if 5 sequential numbers collide
        throw new BusinessException("Unable to generate a unique quotation number. Please try again.");
    }

    public record GeneratedNumber(String quotationNumber, int year, int sequence) {}
}
