CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    department VARCHAR(100),
    gpa DECIMAL(3, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_students_email ON students(email);
CREATE INDEX IF NOT EXISTS idx_students_dept ON students(department);

CREATE TABLE IF NOT EXISTS instructors (
                                           id UUID PRIMARY KEY,
                                           first_name VARCHAR(100) NOT NULL,
                                           last_name VARCHAR(100) NOT NULL,
                                           email VARCHAR(255) UNIQUE NOT NULL,
                                           department VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS courses (
                                       id UUID PRIMARY KEY,
                                       name VARCHAR(255) NOT NULL,
                                       department VARCHAR(100),
                                       instructor_id UUID REFERENCES instructors(id)
);

CREATE TABLE IF NOT EXISTS enrollments (
                                           student_id UUID REFERENCES students(id),
                                           course_id UUID REFERENCES courses(id),
                                           grade DECIMAL(3,2),
                                           PRIMARY KEY (student_id, course_id)
);


CREATE INDEX IF NOT EXISTS idx_courses_dept ON courses(department);
CREATE INDEX IF NOT EXISTS idx_enrollments_student ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course ON enrollments(course_id);
