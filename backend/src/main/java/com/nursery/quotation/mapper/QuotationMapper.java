package com.nursery.quotation.mapper;

import com.nursery.quotation.Quotation;
import com.nursery.quotation.QuotationItem;
import com.nursery.quotation.dto.QuotationDtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuotationMapper {

    @Mapping(target = "items", source = "items")
    @Mapping(target = "nurseryName", ignore = true)
    @Mapping(target = "nurseryAddress", ignore = true)
    @Mapping(target = "nurseryPhone", ignore = true)
    @Mapping(target = "nurseryOwnerName", ignore = true)
    QuotationResponse toResponse(Quotation quotation);

    QuotationSummaryResponse toSummaryResponse(Quotation quotation);

    QuotationItemResponse toItemResponse(QuotationItem item);
}
