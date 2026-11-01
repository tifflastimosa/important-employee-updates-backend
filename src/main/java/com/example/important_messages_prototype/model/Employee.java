package com.example.important_messages_prototype.model;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** An employee who can receive (and, depending on role, publish) important messages. */
@Document(collection = "employees")
public class Employee extends BaseDocument {

	/** Company-issued employee number. Unique across employees. */
	@NotBlank
	private String employeeNumber;

	@NotBlank
	private String firstName;

	@NotBlank
	private String lastName;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String department;

	private String jobTitle;

	@NotNull
	private Role role = Role.EMPLOYEE;

	/** Whether the employee has agreed to sign documents electronically. Defaults to false. */
	private boolean agreedToElectronicSignature;

	public Employee() {
	}

	public Employee(String employeeNumber, String firstName, String lastName, String email, String department,
			String jobTitle, Role role) {
		this.employeeNumber = employeeNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.department = department;
		this.jobTitle = jobTitle;
		setRole(role);
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public boolean canPublishMessages() {
		return role != null && role.canPublishMessages();
	}

	/** Copies the editable fields from another employee (used for updates). */
	public void updateFrom(Employee other) {
		this.employeeNumber = other.employeeNumber;
		this.firstName = other.firstName;
		this.lastName = other.lastName;
		this.email = other.email;
		this.department = other.department;
		this.jobTitle = other.jobTitle;
		setRole(other.role);
		this.agreedToElectronicSignature = other.agreedToElectronicSignature;
	}

	public String getEmployeeNumber() { return employeeNumber; }
	public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

	public String getFirstName() { return firstName; }
	public void setFirstName(String firstName) { this.firstName = firstName; }

	public String getLastName() { return lastName; }
	public void setLastName(String lastName) { this.lastName = lastName; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getDepartment() { return department; }
	public void setDepartment(String department) { this.department = department; }

	public String getJobTitle() { return jobTitle; }
	public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

	public Role getRole() { return role; }
	public void setRole(Role role) { this.role = role == null ? Role.EMPLOYEE : role; }

	public boolean isAgreedToElectronicSignature() { return agreedToElectronicSignature; }
	public void setAgreedToElectronicSignature(boolean agreedToElectronicSignature) {
		this.agreedToElectronicSignature = agreedToElectronicSignature;
	}
}
