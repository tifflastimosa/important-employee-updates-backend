package com.example.important_messages_prototype.model;

/** What an employee is allowed to do with important messages. */
public enum Role {
	EMPLOYEE,
	MANAGER,
	ADMIN;

	/** Managers and admins can publish important messages. */
	public boolean canPublishMessages() {
		return this != EMPLOYEE;
	}
}
