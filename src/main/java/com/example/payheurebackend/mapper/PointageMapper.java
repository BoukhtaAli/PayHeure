package com.example.payheurebackend.mapper;

import com.example.payheurebackend.domain.Pointage;
import com.example.payheurebackend.dto.PointageResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PointageMapper {

    PointageResponse toResponse(Pointage pointage);

    List<PointageResponse> toResponseList(List<Pointage> pointages);
}
