package com.example.edusphere.service;

import com.example.edusphere.entity.Department;
import com.example.edusphere.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    Department save(Department department);
}

