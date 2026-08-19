package com.example.payheurebackend.mapper;

import com.example.payheurebackend.domain.Employee;
import com.example.payheurebackend.dto.EmployeeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "deleted", expression = "java(employee.isDeleted())")
    EmployeeResponse toResponse(Employee employee);
}
