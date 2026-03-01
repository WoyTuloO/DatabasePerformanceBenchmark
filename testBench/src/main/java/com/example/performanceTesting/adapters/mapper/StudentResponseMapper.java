package com.example.performanceTesting.adapters.mapper;

import com.example.performanceTesting.adapters.dto.response.GetStudentByIdResponse;
import com.example.performanceTesting.adapters.dto.response.GetStudentsByDepartmentResponse;
import com.example.performanceTesting.domain.model.Student;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface StudentResponseMapper {

    static StudentResponseMapper get() {
        return Mappers.getMapper(StudentResponseMapper.class);
    }

    GetStudentByIdResponse getStudentById(Student student);

    default GetStudentsByDepartmentResponse getStudentsByDepartment(List<Student> students){
        List<GetStudentsByDepartmentResponse.StudentResponse> studentResponses = new ArrayList<>();
        for (Student student : students) {
            studentResponses.add(toStudentResponse(student));
        }
        return new GetStudentsByDepartmentResponse(studentResponses);
    }

    GetStudentsByDepartmentResponse.StudentResponse toStudentResponse(Student student);
}

