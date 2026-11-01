package com.example.important_messages_prototype.service;

/** Thrown when a requested document does not exist. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resource, String id) {
		super(resource + " not found: " + id);
	}
}
