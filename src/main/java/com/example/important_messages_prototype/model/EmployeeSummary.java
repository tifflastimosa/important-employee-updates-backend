package com.example.important_messages_prototype.model;

/** A short view of an employee for lists, without contact details or audit fields. */
public record EmployeeSummary(String id, String employeeNumber, String fullName, String department,
		String storeNumber) {

	public static EmployeeSummary from(Employee employee) {
		return new EmployeeSummary(employee.getId(), employee.getEmployeeNumber(), employee.getFullName(),
				employee.getDepartment(), employee.getStoreNumber());
	}
}
