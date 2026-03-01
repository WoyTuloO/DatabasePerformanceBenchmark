package com.example.performanceTesting.domain.port;

import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import com.example.performanceTesting.domain.model.Student;

import java.util.List;

public interface StudentProvider {
    void save(CreateStudentCommand command);
    void saveAll(CreateMultipleStudentsCommand command);
    Student findById(GetStudentByIdQuery query);
    List<Student> findByDepartment(GetStudentsByDepartmentQuery query);
    void clean();
}
