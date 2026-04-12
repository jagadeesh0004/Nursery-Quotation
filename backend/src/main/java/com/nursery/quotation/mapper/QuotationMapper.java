package com.nursery.quotation.mapper;

import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import com.nursery.quotation.dto.QuotationDtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuotationMapper {

    @Mapping(target = "items", source = "items")
    QuotationResponse toResponse(Quotation quotation);

    QuotationSummaryResponse toSummaryResponse(Quotation quotation);

    QuotationItemResponse toItemResponse(QuotationItem item);
}
