package com.example.important_messages_prototype.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Time left until a message's deadline, calculated at {@code asOf} (the server's current time).
 *
 * secondsRemaining is negative once the deadline has passed. days/hours/minutes/seconds are the
 * parts of the remaining (or overdue) time, always zero or positive, so a frontend can show
 * "2d 4h 13m" directly or tick down live from secondsRemaining.
 */
public record DeadlineCountdown(
		Instant deadline,
		Instant asOf,
		long secondsRemaining,
		boolean overdue,
		long days,
		long hours,
		long minutes,
		long seconds,
		String display) {

	public static DeadlineCountdown between(Instant now, Instant deadline) {
		Duration remaining = Duration.between(now, deadline);
		boolean overdue = remaining.isNegative();
		Duration abs = remaining.abs();
		long days = abs.toDays();
		long hours = abs.toHoursPart();
		long minutes = abs.toMinutesPart();
		long seconds = abs.toSecondsPart();
		return new DeadlineCountdown(deadline, now, remaining.toSeconds(), overdue, days, hours, minutes, seconds,
				display(overdue, days, hours, minutes));
	}

	private static String display(boolean overdue, long days, long hours, long minutes) {
		String amount;
		if (days > 0) {
			amount = days + "d " + hours + "h " + minutes + "m";
		} else if (hours > 0) {
			amount = hours + "h " + minutes + "m";
		} else if (minutes > 0) {
			amount = minutes + "m";
		} else {
			return overdue ? "Overdue by less than a minute" : "Due in less than a minute";
		}
		return overdue ? "Overdue by " + amount : amount + " left";
	}
}
