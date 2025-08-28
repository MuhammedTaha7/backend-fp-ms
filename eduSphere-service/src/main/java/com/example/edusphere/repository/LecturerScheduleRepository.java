package com.example.edusphere.repository;

import com.example.edusphere.entity.LecturerSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LecturerScheduleRepository extends MongoRepository<LecturerSchedule, String> {
    List<LecturerSchedule> findByLecturerId(String lecturerId);
}