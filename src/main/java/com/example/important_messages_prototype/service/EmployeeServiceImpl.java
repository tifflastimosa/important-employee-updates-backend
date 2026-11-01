package com.example.important_messages_prototype.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.important_messages_prototype.model.Employee;
import com.example.important_messages_prototype.repository.EmployeeRepository;

@Service
public class EmployeeServiceImpl implements EmployeeService {

	private final EmployeeRepository employeeRepository;

	public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	@Override
	public List<Employee> findAll(String department) {
		return department == null || department.isBlank()
				? employeeRepository.findAll()
				: employeeRepository.findByDepartment(department);
	}

	@Override
	public Employee findById(String id) {
		return employeeRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Employee", id));
	}

	@Override
	public Employee create(Employee employee) {
		if (employeeRepository.existsByEmailIgnoreCase(employee.getEmail())) {
			throw new BusinessRuleException("An employee with email " + employee.getEmail() + " already exists");
		}
		if (employeeRepository.existsByEmployeeNumber(employee.getEmployeeNumber())) {
			throw new BusinessRuleException("Employee number " + employee.getEmployeeNumber() + " is already in use");
		}
		employee.setId(null);
		return employeeRepository.save(employee);
	}

	@Override
	public Employee update(String id, Employee changes) {
		Employee existing = findById(id);
		employeeRepository.findByEmailIgnoreCase(changes.getEmail())
				.filter(other -> !other.getId().equals(id))
				.ifPresent(other -> {
					throw new BusinessRuleException("An employee with email " + changes.getEmail() + " already exists");
				});
		employeeRepository.findByEmployeeNumber(changes.getEmployeeNumber())
				.filter(other -> !other.getId().equals(id))
				.ifPresent(other -> {
					throw new BusinessRuleException("Employee number " + changes.getEmployeeNumber() + " is already in use");
				});
		existing.updateFrom(changes);
		return employeeRepository.save(existing);
	}

	@Override
	public void delete(String id) {
		employeeRepository.delete(findById(id));
	}
}
