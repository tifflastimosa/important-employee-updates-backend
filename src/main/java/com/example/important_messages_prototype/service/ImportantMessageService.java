package com.example.important_messages_prototype.service;

import java.util.List;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
import com.example.important_messages_prototype.model.DeadlineCountdown;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;

public interface ImportantMessageService {

	List<ImportantMessage> findAll(Priority priority);

	ImportantMessage findById(String id);

	/** Messages the employee can see: company-wide ones plus those aimed at their department. */
	List<ImportantMessage> findForEmployee(String employeeId);

	/** Messages the employee still needs to sign/acknowledge: soonest deadline first, then ones without a deadline. */
	List<ImportantMessage> findPendingForEmployee(String employeeId);

	/** Who has and has not signed/acknowledged the message. */
	AcknowledgementStatus getAcknowledgementStatus(String messageId);

	/** Countdown to the message's deadline. Throws if the message has no deadline. */
	DeadlineCountdown getCountdown(String messageId);

	/** Messages past their deadline that still have employees who have not signed, most overdue first. */
	List<ImportantMessage> findOverdue();

	/** Messages with a deadline in the next {@code days} days, soonest first. */
	List<ImportantMessage> findDueWithin(int days);

	ImportantMessage create(ImportantMessage message);

	ImportantMessage update(String id, ImportantMessage changes);

	void delete(String id);

	ImportantMessage acknowledge(String messageId, String employeeId);
}
