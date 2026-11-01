package com.example.important_messages_prototype.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A message published to employees. If targetDepartments is empty the message is
 * company-wide; otherwise only employees in those departments see it.
 *
 * Every recipient starts in notAcknowledgedBy and moves to acknowledgedBy when they
 * sign/acknowledge the message.
 */
@Document(collection = "important_messages")
public class ImportantMessage extends BaseDocument {

	@NotBlank
	private String title;

	@NotBlank
	private String content;

	@NotNull
	private Priority priority = Priority.NORMAL;

	/**
	 * The employee who published the message. Stored in MongoDB as a reference to the
	 * employee's document (only the id is saved) and loaded automatically when read.
	 */
	@NotNull
	@DocumentReference
	private Employee author;

	private Set<String> targetDepartments = new HashSet<>();

	/** Recipients who have signed/acknowledged the message. Stored as references to employee documents. */
	@DocumentReference
	private Set<Employee> acknowledgedBy = new HashSet<>();

	/** Recipients who have not yet signed/acknowledged the message. Stored as references to employee documents. */
	@DocumentReference
	private Set<Employee> notAcknowledgedBy = new HashSet<>();

	public ImportantMessage() {
	}

	public ImportantMessage(String title, String content, Priority priority, Employee author, Set<String> targetDepartments) {
		this.title = title;
		this.content = content;
		setPriority(priority);
		this.author = author;
		setTargetDepartments(targetDepartments);
	}

	public boolean isCompanyWide() {
		return targetDepartments.isEmpty();
	}

	public boolean isVisibleTo(Employee employee) {
		return isCompanyWide() || targetDepartments.contains(employee.getDepartment());
	}

	/** True if the employee is required to sign this message (whether or not they have yet). */
	public boolean isRecipient(Employee employee) {
		return acknowledgedBy.contains(employee) || notAcknowledgedBy.contains(employee);
	}

	/**
	 * Sets who must sign the message. Anyone who already signed stays in acknowledgedBy;
	 * everyone else in the given list goes into notAcknowledgedBy.
	 */
	public void assignRecipients(Collection<Employee> recipients) {
		notAcknowledgedBy = new HashSet<>(recipients);
		notAcknowledgedBy.removeAll(acknowledgedBy);
	}

	/**
	 * Moves the employee from notAcknowledgedBy to acknowledgedBy.
	 * Returns false if they had already signed.
	 *
	 * @throws IllegalArgumentException if the employee is not a recipient of this message
	 */
	public boolean acknowledge(Employee employee) {
		if (acknowledgedBy.contains(employee)) {
			return false;
		}
		if (!notAcknowledgedBy.remove(employee)) {
			throw new IllegalArgumentException("Employee " + employee.getId() + " is not a recipient of this message");
		}
		return acknowledgedBy.add(employee);
	}

	public boolean isAcknowledgedBy(Employee employee) {
		return acknowledgedBy.contains(employee);
	}

	public boolean isFullyAcknowledged() {
		return notAcknowledgedBy.isEmpty();
	}

	/** Copies the editable fields from another message. Author and acknowledgements are kept. */
	public void updateFrom(ImportantMessage other) {
		this.title = other.title;
		this.content = other.content;
		setPriority(other.priority);
		setTargetDepartments(other.targetDepartments);
	}

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public Priority getPriority() { return priority; }
	public void setPriority(Priority priority) { this.priority = priority == null ? Priority.NORMAL : priority; }

	public Employee getAuthor() { return author; }
	public void setAuthor(Employee author) { this.author = author; }

	public Set<String> getTargetDepartments() { return targetDepartments; }
	public void setTargetDepartments(Set<String> targetDepartments) {
		this.targetDepartments = targetDepartments == null ? new HashSet<>() : new HashSet<>(targetDepartments);
	}

	public Set<Employee> getAcknowledgedBy() { return acknowledgedBy; }
	public void setAcknowledgedBy(Set<Employee> acknowledgedBy) {
		this.acknowledgedBy = acknowledgedBy == null ? new HashSet<>() : new HashSet<>(acknowledgedBy);
	}

	public Set<Employee> getNotAcknowledgedBy() { return notAcknowledgedBy; }
	public void setNotAcknowledgedBy(Set<Employee> notAcknowledgedBy) {
		this.notAcknowledgedBy = notAcknowledgedBy == null ? new HashSet<>() : new HashSet<>(notAcknowledgedBy);
	}
}
