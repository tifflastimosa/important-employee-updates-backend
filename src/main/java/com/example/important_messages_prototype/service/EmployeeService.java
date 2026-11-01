package com.example.important_messages_prototype.service;

import java.util.List;

import com.example.important_messages_prototype.model.Employee;

public interface EmployeeService {

	/** All employees, optionally narrowed by department and/or store number (null or blank means no filter). */
	List<Employee> findAll(String department, String storeNumber);

	Employee findById(String id);

	Employee create(Employee employee);

	Employee update(String id, Employee changes);

	void delete(String id);
}
