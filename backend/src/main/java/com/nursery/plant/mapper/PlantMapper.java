package com.nursery.plant.mapper;

import com.nursery.plant.Dimension;
import com.nursery.plant.Plant;
import com.nursery.plant.dto.PlantDtos.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlantMapper {

    @Mapping(target = "dimensions", source = "dimensions")
    PlantResponse toResponse(Plant plant);

    @Mapping(target = "dimension", source = "dimension")
    DimensionResponse toDimensionResponse(Dimension dimension);
}
