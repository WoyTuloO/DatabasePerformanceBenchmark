package com.example.performanceTesting.application.service;

import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import com.example.performanceTesting.domain.model.Student;
import com.example.performanceTesting.domain.port.StudentProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TestBenchService {

    @NonNull
    StudentProvider studentProvider;

    public void save(CreateStudentCommand command) {
        studentProvider.save(command);
    }

    public void saveAll(CreateMultipleStudentsCommand command) {
        studentProvider.saveAll(command);
    }

    public Student getById(GetStudentByIdQuery query) {
        return studentProvider.findById(query);
    }

    public List<Student> getByDept(GetStudentsByDepartmentQuery query) {
        return studentProvider.findByDepartment(query);
    }

    public void clean(){
        studentProvider.clean();
    }
}
