package com.example.performanceTesting.adapters.out.persistence;

import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import com.example.performanceTesting.domain.model.Course;
import com.example.performanceTesting.domain.model.Student;
import com.example.performanceTesting.domain.port.PostgresStudentProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class PostgresStudentAdapter implements PostgresStudentProvider {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_STUDENT = "INSERT INTO students (id, first_name, last_name, email, department, gpa, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT id, first_name, last_name, email, department, gpa, created_at FROM students WHERE id = ?";
    private static final String SELECT_BY_DEPARTMENT = "SELECT id, first_name, last_name, email, department, gpa, created_at FROM students WHERE department = ?";
    private static final String TRUNCATE_TABLE = "TRUNCATE TABLE students RESTART IDENTITY CASCADE";

    private static final String SELECT_COURSES_BY_STUDENT = "SELECT c.id, c.name, c.department, c.instructor_id FROM courses c JOIN enrollments e ON c.id = e.course_id WHERE e.student_id = ?";
    private static final String SELECT_COURSES_BY_INSTRUCTOR = "SELECT id, name, department, instructor_id FROM courses WHERE instructor_id = ?";


    private final RowMapper<Student> studentRowMapper = new RowMapper<Student>() {
        @Override
        public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Student(
                    (UUID) rs.getObject("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("department"),
                    rs.getDouble("gpa"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        }
    };

    @Override
    public void save(CreateStudentCommand command) {
        jdbcTemplate.update(INSERT_STUDENT,
                command.id(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.department(),
                command.gpa(),
                Timestamp.valueOf(command.createdAt())
        );
    }

    @Override
    public void saveAll(CreateMultipleStudentsCommand command) {
        List<CreateMultipleStudentsCommand.Student> students = command.studentsToCreate();

        jdbcTemplate.batchUpdate(
                INSERT_STUDENT,
                students,
                students.size(),
                (ps, s) -> {
                    ps.setObject(1, s.id());
                    ps.setString(2, s.firstName());
                    ps.setString(3, s.lastName());
                    ps.setString(4, s.email());
                    ps.setString(5, s.department());
                    ps.setDouble(6, s.gpa());
                    ps.setTimestamp(7, Timestamp.valueOf(s.createdAt()));
                }
        );
    }

    @Override
    public Student findById(GetStudentByIdQuery query) {
        try {
            return jdbcTemplate.queryForObject(SELECT_BY_ID, studentRowMapper, UUID.fromString(query.id()));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Student> findByDepartment(GetStudentsByDepartmentQuery query) {
        return jdbcTemplate.query(SELECT_BY_DEPARTMENT, studentRowMapper, query.name());
    }

    @Override
    public void clean() {
        jdbcTemplate.execute(TRUNCATE_TABLE);
    }

}
