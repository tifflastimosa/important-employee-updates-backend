package com.example.important_messages_prototype.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

class DeadlineCountdownTest {

	private final Instant now = Instant.parse("2026-11-01T12:00:00Z");

	@Test
	void breaksRemainingTimeIntoParts() {
		Instant deadline = now.plus(Duration.ofDays(2).plusHours(4).plusMinutes(13).plusSeconds(9));

		DeadlineCountdown c = DeadlineCountdown.between(now, deadline);

		assertThat(c.overdue()).isFalse();
		assertThat(c.secondsRemaining()).isEqualTo(2 * 86400 + 4 * 3600 + 13 * 60 + 9);
		assertThat(c.days()).isEqualTo(2);
		assertThat(c.hours()).isEqualTo(4);
		assertThat(c.minutes()).isEqualTo(13);
		assertThat(c.seconds()).isEqualTo(9);
		assertThat(c.display()).isEqualTo("2d 4h 13m left");
		assertThat(c.deadline()).isEqualTo(deadline);
		assertThat(c.asOf()).isEqualTo(now);
	}

	@Test
	void showsShorterFormatsCloseToTheDeadline() {
		assertThat(DeadlineCountdown.between(now, now.plus(Duration.ofHours(3).plusMinutes(5))).display())
				.isEqualTo("3h 5m left");
		assertThat(DeadlineCountdown.between(now, now.plus(Duration.ofMinutes(42))).display()).isEqualTo("42m left");
		assertThat(DeadlineCountdown.between(now, now.plusSeconds(30)).display()).isEqualTo("Due in less than a minute");
	}

	@Test
	void reportsOverdueWithNegativeSecondsAndPositiveParts() {
		DeadlineCountdown c = DeadlineCountdown.between(now, now.minus(Duration.ofHours(1).plusMinutes(5)));

		assertThat(c.overdue()).isTrue();
		assertThat(c.secondsRemaining()).isEqualTo(-(3600 + 5 * 60));
		assertThat(c.hours()).isEqualTo(1);
		assertThat(c.minutes()).isEqualTo(5);
		assertThat(c.display()).isEqualTo("Overdue by 1h 5m");
	}

	@Test
	void messageWithoutDeadlineHasNoCountdownAndIsNeverOverdue() {
		ImportantMessage noDeadline = new ImportantMessage("t", "c", Priority.NORMAL, null, Set.of());
		assertThat(noDeadline.countdownAt(now)).isNull();
		assertThat(noDeadline.isOverdueAt(now)).isFalse();

		ImportantMessage due = new ImportantMessage("t", "c", Priority.NORMAL, null, Set.of(), now.plusSeconds(60));
		assertThat(due.isOverdueAt(now)).isFalse();
		assertThat(due.isOverdueAt(now.plusSeconds(61))).isTrue();
		assertThat(due.countdownAt(now).secondsRemaining()).isEqualTo(60);
	}
}
