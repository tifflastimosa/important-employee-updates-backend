package com.example.important_messages_prototype.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.important_messages_prototype.model.Employee;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

	Optional<Employee> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	Optional<Employee> findByEmployeeNumber(String employeeNumber);

	boolean existsByEmployeeNumber(String employeeNumber);

	List<Employee> findByDepartment(String department);

	List<Employee> findByDepartmentIn(Collection<String> departments);

	List<Employee> findByStoreNumber(String storeNumber);

	List<Employee> findByDepartmentAndStoreNumber(String department, String storeNumber);
}
