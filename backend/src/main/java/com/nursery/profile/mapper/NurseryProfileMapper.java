package com.nursery.profile.mapper;

import com.nursery.profile.NurseryProfile;
import com.nursery.profile.dto.ProfileDtos.ProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NurseryProfileMapper {

    ProfileResponse toResponse(NurseryProfile profile);
}
