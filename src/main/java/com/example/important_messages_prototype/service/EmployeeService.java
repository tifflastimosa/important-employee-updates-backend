package com.example.important_messages_prototype.service;

import java.util.List;

import com.example.important_messages_prototype.model.Employee;

public interface EmployeeService {

	List<Employee> findAll(String department);

	Employee findById(String id);

	Employee create(Employee employee);

	Employee update(String id, Employee changes);

	void delete(String id);
}
