package com.example.edusphere.repository;

import com.example.edusphere.entity.File;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends MongoRepository<File, String> {

    @Query("{$or: ["
            + "{'accessType': 'public'},"
            + "{'accessType': 'students'},"
            + "{'accessType': 'lecturers'},"
            + "{'accessType': 'course'},"
            + "{'accessType': 'personal'}" // Add the new personal access type
            + "]}")
    List<File> findFilesForBaseFiltering();

    List<File> findAllByOrderByUploadDateDesc();
}