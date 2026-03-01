package com.example.performanceTesting.adapters.mapper;

import com.example.performanceTesting.adapters.dto.request.CreateMultipleStudentsRequest;
import com.example.performanceTesting.adapters.dto.request.CreateStudentRequest;
import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StudentRequestMapper {

    static StudentRequestMapper get() {
        return Mappers.getMapper(StudentRequestMapper.class);
    }

    CreateStudentCommand toCommand(CreateStudentRequest request);

    GetStudentByIdQuery toStudentByIdQuery(String id);

    GetStudentsByDepartmentQuery toStudentsByDepartmentQuery(String name);

    CreateMultipleStudentsCommand toCommand(CreateMultipleStudentsRequest request);
}

