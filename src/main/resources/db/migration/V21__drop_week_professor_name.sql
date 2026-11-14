-- week.professor_name는 lecture.professor_name과 중복이라 사용하지 않는다 (#25).
ALTER TABLE week DROP COLUMN professor_name;
