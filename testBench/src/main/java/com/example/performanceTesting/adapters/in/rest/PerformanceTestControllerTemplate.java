package com.example.performanceTesting.adapters.in.rest;

import com.example.performanceTesting.adapters.dto.request.CreateMultipleStudentsRequest;
import com.example.performanceTesting.adapters.dto.request.CreateStudentRequest;
import com.example.performanceTesting.adapters.mapper.StudentRequestMapper;
import com.example.performanceTesting.adapters.mapper.StudentResponseMapper;
import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import com.example.performanceTesting.application.service.TestBenchService;
import com.example.performanceTesting.domain.model.Student;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class PerformanceTestControllerTemplate {

    @NonNull
    private final TestBenchService service;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void save(@RequestBody @Nonnull CreateStudentRequest request) {
        CreateStudentCommand command = StudentRequestMapper.get().toCommand(request);
        service.save(command);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        GetStudentByIdQuery query = StudentRequestMapper.get().toStudentByIdQuery(id);
        Student byId = service.getById(query);
        if (Objects.isNull(byId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(StudentResponseMapper.get().getStudentById(byId));
    }

    @GetMapping("/dept/{name}")
    public ResponseEntity<?> getByDept(@PathVariable String name) {
        GetStudentsByDepartmentQuery query = StudentRequestMapper.get().toStudentsByDepartmentQuery(name);
        List<Student> byDept = service.getByDept(query);
        if (CollectionUtils.isEmpty(byDept)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(StudentResponseMapper.get().getStudentsByDepartment(byDept));
    }

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/clean")
    public void clean() {
        service.clean();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/bulk")
    public void saveAll(@RequestBody @Nonnull CreateMultipleStudentsRequest request) {
        CreateMultipleStudentsCommand command = StudentRequestMapper.get().toCommand(request);
        service.saveAll(command);
    }
}
