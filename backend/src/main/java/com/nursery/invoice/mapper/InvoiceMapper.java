package com.nursery.invoice.mapper;

import com.nursery.invoice.Invoice;
import com.nursery.invoice.InvoiceItem;
import com.nursery.invoice.dto.InvoiceDtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "nurseryName", ignore = true)
    @Mapping(target = "nurseryAddress", ignore = true)
    @Mapping(target = "nurseryPhone", ignore = true)
    @Mapping(target = "nurseryOwnerName", ignore = true)
    @Mapping(target = "items", source = "items")
    InvoiceResponse toResponse(Invoice invoice);

    @Mapping(target = "invoiceNumber", ignore = true)
    InvoiceSummaryResponse toSummaryResponse(Invoice invoice);

    InvoiceItemResponse toItemResponse(InvoiceItem item);

    List<InvoiceItemResponse> toItemResponses(List<InvoiceItem> items);
}
