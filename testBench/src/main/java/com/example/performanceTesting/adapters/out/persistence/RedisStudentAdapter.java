package com.example.performanceTesting.adapters.out.persistence;

import com.example.performanceTesting.application.command.CreateMultipleStudentsCommand;
import com.example.performanceTesting.application.command.CreateStudentCommand;
import com.example.performanceTesting.application.query.GetStudentByIdQuery;
import com.example.performanceTesting.application.query.GetStudentsByDepartmentQuery;
import com.example.performanceTesting.domain.model.Course;
import com.example.performanceTesting.domain.model.Student;
import com.example.performanceTesting.domain.port.RedisStudentProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class RedisStudentAdapter implements RedisStudentProvider {

    private final RedisTemplate<String, Student> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String STUDENT_KEY_PREFIX = "student:";
    private static final String STUDENTS_BY_DEPT_KEY_PREFIX = "students:dept:";

    @Override
    public void save(CreateStudentCommand command) {
        Student student = new Student(
                command.id(),
                command.firstName(),
                command.lastName(),
                command.email(),
                command.department(),
                command.gpa(),
                command.createdAt()
        );
        redisTemplate.opsForValue().set(STUDENT_KEY_PREFIX + command.id(), student);
        String deptKey = STUDENTS_BY_DEPT_KEY_PREFIX + command.department();
        stringRedisTemplate.opsForSet()
                .add(deptKey, command.id().toString());
    }

    @Override
    public void saveAll(CreateMultipleStudentsCommand command) {
        List<CreateMultipleStudentsCommand.Student> students = command.studentsToCreate();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (CreateMultipleStudentsCommand.Student s : students) {

                byte[] key = ((RedisSerializer<String>) redisTemplate.getKeySerializer())
                        .serialize(STUDENT_KEY_PREFIX + s.id());

                byte[] value = ((RedisSerializer<Student>) redisTemplate.getValueSerializer())
                        .serialize(new Student(
                                s.id(),
                                s.firstName(),
                                s.lastName(),
                                s.email(),
                                s.department(),
                                s.gpa(),
                                s.createdAt()
                        ));

                connection.set(key, value);

                byte[] deptKey = ((RedisSerializer<String>) stringRedisTemplate.getKeySerializer())
                        .serialize(STUDENTS_BY_DEPT_KEY_PREFIX + s.department());
                byte[] idValue = ((RedisSerializer<String>) stringRedisTemplate.getValueSerializer())
                        .serialize(s.id().toString());

                connection.sAdd(deptKey, idValue);
            }
            return null;
        });
    }

    @Override
    public Student findById(GetStudentByIdQuery query) {
        try {
            return redisTemplate.opsForValue().get(STUDENT_KEY_PREFIX + query.id());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Student> findByDepartment(GetStudentsByDepartmentQuery query) {

        String deptKey = STUDENTS_BY_DEPT_KEY_PREFIX + query.name();
        Set<String> ids = stringRedisTemplate.opsForSet().members(deptKey);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<String> keys = ids.stream()
                .map(id -> STUDENT_KEY_PREFIX + id)
                .toList();

        List<Student> students = redisTemplate.opsForValue().multiGet(keys);
        return students != null ? students : new ArrayList<>();
    }

    @Override
    public void clean() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

}

