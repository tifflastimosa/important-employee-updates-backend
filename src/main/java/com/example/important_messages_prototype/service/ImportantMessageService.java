package com.example.important_messages_prototype.service;

import java.util.List;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;

public interface ImportantMessageService {

	List<ImportantMessage> findAll(Priority priority);

	ImportantMessage findById(String id);

	/** Messages the employee can see: company-wide ones plus those aimed at their department. */
	List<ImportantMessage> findForEmployee(String employeeId);

	/** Messages the employee still needs to sign/acknowledge, newest first. */
	List<ImportantMessage> findPendingForEmployee(String employeeId);

	/** Who has and has not signed/acknowledged the message. */
	AcknowledgementStatus getAcknowledgementStatus(String messageId);

	ImportantMessage create(ImportantMessage message);

	ImportantMessage update(String id, ImportantMessage changes);

	void delete(String id);

	ImportantMessage acknowledge(String messageId, String employeeId);
}
