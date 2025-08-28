package com.example.common.repository;

import com.example.common.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmailOrUsername(String email, String username);
    List<UserEntity> findAll();
    boolean existsByEmail(String email);
    List<UserEntity> findByRole(String role);



    List<UserEntity> findByNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String name, String username);

    @Query("{'university': ?0, '_id': {$nin: ?1}}")
    List<UserEntity> findByUniversityAndIdNotIn(String university, List<String> excludeIds);

    // Alternative if university filtering isn't working:
    @Query("{'_id': {$nin: ?0}}")
    List<UserEntity> findByIdNotIn(List<String> excludeIds);

    List<UserEntity> findByNameContainingIgnoreCase(String name);

}