package com.example.important_messages_prototype.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Who has and has not signed/acknowledged an important message. */
public record AcknowledgementStatus(
		String messageId,
		String title,
		int totalRecipients,
		int acknowledgedCount,
		int notAcknowledgedCount,
		boolean fullyAcknowledged,
		List<EmployeeSummary> acknowledged,
		List<EmployeeSummary> notAcknowledged) {

	public static AcknowledgementStatus of(ImportantMessage message) {
		List<EmployeeSummary> acknowledged = summarize(message.getAcknowledgedBy());
		List<EmployeeSummary> notAcknowledged = summarize(message.getNotAcknowledgedBy());
		return new AcknowledgementStatus(
				message.getId(),
				message.getTitle(),
				acknowledged.size() + notAcknowledged.size(),
				acknowledged.size(),
				notAcknowledged.size(),
				notAcknowledged.isEmpty(),
				acknowledged,
				notAcknowledged);
	}

	private static List<EmployeeSummary> summarize(Collection<Employee> employees) {
		return employees.stream()
				.map(EmployeeSummary::from)
				.sorted(Comparator.comparing(EmployeeSummary::fullName, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}
}
