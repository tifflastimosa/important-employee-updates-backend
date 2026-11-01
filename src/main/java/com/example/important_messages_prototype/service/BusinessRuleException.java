package com.example.important_messages_prototype.service;

/** Thrown when a request is well-formed but breaks a business rule. Mapped to HTTP 409. */
public class BusinessRuleException extends RuntimeException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
