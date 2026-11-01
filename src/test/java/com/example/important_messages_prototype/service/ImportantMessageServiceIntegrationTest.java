package com.example.important_messages_prototype.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
import com.example.important_messages_prototype.model.Employee;
import com.example.important_messages_prototype.model.EmployeeSummary;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;
import com.example.important_messages_prototype.model.Role;
import com.example.important_messages_prototype.repository.EmployeeRepository;
import com.example.important_messages_prototype.repository.ImportantMessageRepository;

/**
 * Runs against the MongoDB configured in application.properties.
 * Uses unique department names and emails per run and deletes everything it creates.
 */
@SpringBootTest
class ImportantMessageServiceIntegrationTest {

	@Autowired private EmployeeService employeeService;
	@Autowired private ImportantMessageService messageService;
	@Autowired private EmployeeRepository employeeRepository;
	@Autowired private ImportantMessageRepository messageRepository;

	private final String tag = UUID.randomUUID().toString().substring(0, 8);
	private final String engineering = "Engineering-" + tag;
	private final String sales = "Sales-" + tag;
	private final String store1 = "S1-" + tag;
	private final String store2 = "S2-" + tag;
	private final List<String> employeeIds = new ArrayList<>();
	private final List<String> messageIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		messageRepository.deleteAllById(messageIds);
		employeeRepository.deleteAllById(employeeIds);
	}

	private Employee newEmployee(String first, String department, Role role) {
		return newEmployee(first, department, store1, role);
	}

	private Employee newEmployee(String first, String department, String storeNumber, Role role) {
		Employee e = employeeService.create(new Employee(first.toUpperCase() + "-" + tag, first, "Test",
				first.toLowerCase() + "-" + tag + "@example.com", department, storeNumber, "Tester", role));
		employeeIds.add(e.getId());
		return e;
	}

	private ImportantMessage newMessage(String title, Employee author, Set<String> departments) {
		ImportantMessage m = messageService.create(new ImportantMessage(title, "Body of " + title, Priority.HIGH,
				author, departments));
		messageIds.add(m.getId());
		return m;
	}

	@Test
	void createsEmployeeAndRejectsDuplicateEmail() {
		Employee alice = newEmployee("Alice", engineering, Role.MANAGER);

		assertThat(alice.getId()).isNotBlank();
		assertThat(alice.getCreatedAt()).isNotNull();
		assertThat(employeeService.findById(alice.getId()).getFullName()).isEqualTo("Alice Test");

		Employee duplicate = new Employee("OTHER-" + tag, "Other", "Person", alice.getEmail().toUpperCase(), sales,
				store1, null, Role.EMPLOYEE);
		assertThatThrownBy(() -> employeeService.create(duplicate)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsDuplicateEmployeeNumber() {
		Employee alice = newEmployee("Alice", engineering, Role.EMPLOYEE);

		Employee sameNumber = new Employee(alice.getEmployeeNumber(), "Other", "Person", "other-" + tag + "@example.com",
				sales, store1, null, Role.EMPLOYEE);
		assertThatThrownBy(() -> employeeService.create(sameNumber))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Employee number");
	}

	@Test
	void electronicSignatureAgreementDefaultsToFalseAndCanBeUpdated() {
		Employee alice = newEmployee("Alice", engineering, Role.EMPLOYEE);
		assertThat(employeeService.findById(alice.getId()).isAgreedToElectronicSignature()).isFalse();

		Employee changes = employeeService.findById(alice.getId());
		changes.setAgreedToElectronicSignature(true);
		employeeService.update(alice.getId(), changes);

		assertThat(employeeService.findById(alice.getId()).isAgreedToElectronicSignature()).isTrue();
	}

	@Test
	void storeNumberIsSavedUpdatedAndFilterable() {
		Employee alice = newEmployee("Alice", engineering, store1, Role.EMPLOYEE);
		Employee carol = newEmployee("Carol", sales, store1, Role.EMPLOYEE);
		Employee dave = newEmployee("Dave", engineering, store2, Role.EMPLOYEE);

		assertThat(employeeService.findById(alice.getId()).getStoreNumber()).isEqualTo(store1);

		assertThat(employeeService.findAll(null, store1)).containsExactlyInAnyOrder(alice, carol);
		assertThat(employeeService.findAll(engineering, store1)).containsExactly(alice);
		assertThat(employeeService.findAll(engineering, null)).containsExactlyInAnyOrder(alice, dave);
		assertThat(employeeService.findAll(engineering, " ")).containsExactlyInAnyOrder(alice, dave);

		Employee changes = employeeService.findById(alice.getId());
		changes.setStoreNumber(store2);
		employeeService.update(alice.getId(), changes);
		assertThat(employeeService.findAll(null, store2)).containsExactlyInAnyOrder(alice, dave);
	}

	@Test
	void onlyManagersAndAdminsCanPublish() {
		Employee bob = newEmployee("Bob", sales, Role.EMPLOYEE);

		assertThatThrownBy(() -> newMessage("Not allowed", bob, Set.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("cannot publish");
	}

	@Test
	void employeesSeeCompanyWideAndTheirDepartmentsMessages() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee engineer = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee seller = newEmployee("Dave", sales, Role.EMPLOYEE);

		ImportantMessage everyone = newMessage("Everyone " + tag, manager, Set.of());
		ImportantMessage engOnly = newMessage("Eng only " + tag, manager, Set.of(engineering));

		ImportantMessage reloaded = messageService.findById(everyone.getId());
		assertThat(reloaded.getAuthor().getId()).isEqualTo(manager.getId());
		assertThat(reloaded.getAuthor().getFullName()).isEqualTo(manager.getFullName());
		assertThat(messageRepository.findByAuthorOrderByCreatedAtDesc(manager))
				.extracting(ImportantMessage::getId).containsExactly(engOnly.getId(), everyone.getId());

		List<String> engineerSees = messageService.findForEmployee(engineer.getId()).stream()
				.map(ImportantMessage::getId).toList();
		List<String> sellerSees = messageService.findForEmployee(seller.getId()).stream()
				.map(ImportantMessage::getId).toList();

		assertThat(engineerSees).contains(everyone.getId(), engOnly.getId());
		assertThat(sellerSees).contains(everyone.getId()).doesNotContain(engOnly.getId());
	}

	@Test
	void newMessageListsEveryRecipientExceptTheAuthorAsNotAcknowledged() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee engineer1 = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee engineer2 = newEmployee("Erin", engineering, Role.EMPLOYEE);
		Employee seller = newEmployee("Dave", sales, Role.EMPLOYEE);

		ImportantMessage engOnly = newMessage("Eng only " + tag, manager, Set.of(engineering));
		ImportantMessage everyone = newMessage("Everyone " + tag, manager, Set.of());

		ImportantMessage reloadedEng = messageService.findById(engOnly.getId());
		assertThat(reloadedEng.getAcknowledgedBy()).isEmpty();
		assertThat(reloadedEng.getNotAcknowledgedBy()).containsExactlyInAnyOrder(engineer1, engineer2);
		assertThat(reloadedEng.getNotAcknowledgedBy())
				.extracting(Employee::getFullName).contains("Carol Test", "Erin Test");

		// Company-wide: everyone in the database except the author (other real employees may exist too).
		assertThat(messageService.findById(everyone.getId()).getNotAcknowledgedBy())
				.contains(engineer1, engineer2, seller)
				.doesNotContain(manager);
	}

	@Test
	void acknowledgingMovesEmployeeFromNotAcknowledgedToAcknowledged() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee engineer1 = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee engineer2 = newEmployee("Erin", engineering, Role.EMPLOYEE);
		Employee seller = newEmployee("Dave", sales, Role.EMPLOYEE);
		ImportantMessage engOnly = newMessage("Eng only " + tag, manager, Set.of(engineering));

		messageService.acknowledge(engOnly.getId(), engineer1.getId());
		ImportantMessage again = messageService.acknowledge(engOnly.getId(), engineer1.getId());

		assertThat(again.getAcknowledgedBy()).containsExactly(engineer1);
		assertThat(again.getNotAcknowledgedBy()).containsExactly(engineer2);

		ImportantMessage reloaded = messageService.findById(engOnly.getId());
		assertThat(reloaded.isAcknowledgedBy(engineer1)).isTrue();
		assertThat(reloaded.getAcknowledgedBy()).extracting(Employee::getFullName).containsExactly("Carol Test");
		assertThat(reloaded.isFullyAcknowledged()).isFalse();

		messageService.acknowledge(engOnly.getId(), engineer2.getId());
		assertThat(messageService.findById(engOnly.getId()).isFullyAcknowledged()).isTrue();

		assertThatThrownBy(() -> messageService.acknowledge(engOnly.getId(), seller.getId()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void changingTargetDepartmentsRecalculatesWhoStillNeedsToSign() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee engineer = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee seller = newEmployee("Dave", sales, Role.EMPLOYEE);
		ImportantMessage message = newMessage("Eng only " + tag, manager, Set.of(engineering));
		messageService.acknowledge(message.getId(), engineer.getId());

		ImportantMessage changes = new ImportantMessage(message.getTitle(), message.getContent(), message.getPriority(),
				manager, Set.of(engineering, sales));
		ImportantMessage updated = messageService.update(message.getId(), changes);

		assertThat(updated.getAcknowledgedBy()).containsExactly(engineer);
		assertThat(updated.getNotAcknowledgedBy()).containsExactly(seller);
	}

	@Test
	void acknowledgementStatusListsWhoHasAndHasNotSigned() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee carol = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee erin = newEmployee("Erin", engineering, Role.EMPLOYEE);
		Employee bob = newEmployee("Bob", engineering, Role.EMPLOYEE);
		ImportantMessage message = newMessage("Policy " + tag, manager, Set.of(engineering));
		messageService.acknowledge(message.getId(), carol.getId());

		AcknowledgementStatus status = messageService.getAcknowledgementStatus(message.getId());

		assertThat(status.messageId()).isEqualTo(message.getId());
		assertThat(status.totalRecipients()).isEqualTo(3);
		assertThat(status.acknowledgedCount()).isEqualTo(1);
		assertThat(status.notAcknowledgedCount()).isEqualTo(2);
		assertThat(status.fullyAcknowledged()).isFalse();
		assertThat(status.acknowledged()).extracting(EmployeeSummary::id).containsExactly(carol.getId());
		assertThat(status.notAcknowledged()).extracting(EmployeeSummary::fullName)
				.containsExactly("Bob Test", "Erin Test");
		assertThat(status.notAcknowledged()).extracting(EmployeeSummary::employeeNumber)
				.containsExactly(bob.getEmployeeNumber(), erin.getEmployeeNumber());
		assertThat(status.notAcknowledged()).extracting(EmployeeSummary::storeNumber).containsOnly(store1);
	}

	@Test
	void pendingMessagesOnlyIncludeOnesTheEmployeeHasNotSigned() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee carol = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Employee dave = newEmployee("Dave", sales, Role.EMPLOYEE);
		ImportantMessage first = newMessage("First " + tag, manager, Set.of(engineering));
		ImportantMessage second = newMessage("Second " + tag, manager, Set.of(engineering));
		ImportantMessage salesOnly = newMessage("Sales " + tag, manager, Set.of(sales));

		assertThat(messageService.findPendingForEmployee(carol.getId()))
				.extracting(ImportantMessage::getId).containsExactly(second.getId(), first.getId());

		messageService.acknowledge(first.getId(), carol.getId());

		assertThat(messageService.findPendingForEmployee(carol.getId()))
				.extracting(ImportantMessage::getId).containsExactly(second.getId());
		assertThat(messageService.findPendingForEmployee(dave.getId()))
				.extracting(ImportantMessage::getId).containsExactly(salesOnly.getId());
		assertThat(messageService.findPendingForEmployee(manager.getId())).isEmpty();
		assertThatThrownBy(() -> messageService.findPendingForEmployee("does-not-exist"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private ImportantMessage newMessageDue(String title, Employee author, Set<String> departments, Instant deadline) {
		ImportantMessage m = messageService.create(new ImportantMessage(title, "Body of " + title, Priority.HIGH,
				author, departments, deadline));
		messageIds.add(m.getId());
		return m;
	}

	@Test
	void deadlineIsSavedAndCountdownIsReturned() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		newEmployee("Carol", engineering, Role.EMPLOYEE);
		Instant deadline = Instant.now().plus(Duration.ofDays(3)).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

		ImportantMessage message = newMessageDue("Due " + tag, manager, Set.of(engineering), deadline);

		ImportantMessage reloaded = messageService.findById(message.getId());
		assertThat(reloaded.getDeadline()).isEqualTo(deadline);
		assertThat(reloaded.getCountdown().overdue()).isFalse();
		assertThat(reloaded.getCountdown().days()).isEqualTo(2); // a few ms less than 3 full days
		assertThat(messageService.getCountdown(message.getId()).deadline()).isEqualTo(deadline);
		assertThat(messageService.getAcknowledgementStatus(message.getId()).countdown()).isNotNull();

		ImportantMessage noDeadline = newMessage("No deadline " + tag, manager, Set.of(engineering));
		assertThat(messageService.findById(noDeadline.getId()).getCountdown()).isNull();
		assertThatThrownBy(() -> messageService.getCountdown(noDeadline.getId()))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void deadlineMustBeInTheFutureWhenSetOrChanged() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Instant past = Instant.now().minus(Duration.ofHours(1));

		assertThatThrownBy(() -> newMessageDue("Past " + tag, manager, Set.of(engineering), past))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("future");

		ImportantMessage message = newMessageDue("Future " + tag, manager, Set.of(engineering),
				Instant.now().plus(Duration.ofDays(1)));
		ImportantMessage changes = messageService.findById(message.getId());
		changes.setDeadline(past);
		assertThatThrownBy(() -> messageService.update(message.getId(), changes))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void editingAnOverdueMessageWithoutChangingItsDeadlineIsAllowed() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		newEmployee("Carol", engineering, Role.EMPLOYEE);
		ImportantMessage message = newMessageDue("Overdue " + tag, manager, Set.of(engineering),
				Instant.now().plus(Duration.ofDays(1)));
		// Simulate time passing by moving the stored deadline into the past directly.
		ImportantMessage stored = messageRepository.findById(message.getId()).orElseThrow();
		stored.setDeadline(Instant.now().minus(Duration.ofHours(2)));
		messageRepository.save(stored);

		ImportantMessage changes = messageService.findById(message.getId());
		changes.setContent("Clarified wording");
		ImportantMessage updated = messageService.update(message.getId(), changes);

		assertThat(updated.getContent()).isEqualTo("Clarified wording");
		assertThat(updated.getCountdown().overdue()).isTrue();
	}

	@Test
	void pendingMessagesAreOrderedBySoonestDeadlineThenNoDeadline() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee carol = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Instant now = Instant.now();
		ImportantMessage none = newMessage("None " + tag, manager, Set.of(engineering));
		ImportantMessage later = newMessageDue("Later " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(5)));
		ImportantMessage sooner = newMessageDue("Sooner " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(1)));

		assertThat(messageService.findPendingForEmployee(carol.getId()))
				.extracting(ImportantMessage::getId)
				.containsExactly(sooner.getId(), later.getId(), none.getId());
	}

	@Test
	void overdueAndDueSoonQueries() {
		Employee manager = newEmployee("Alice", engineering, Role.MANAGER);
		Employee carol = newEmployee("Carol", engineering, Role.EMPLOYEE);
		Instant now = Instant.now();
		ImportantMessage overdue = newMessageDue("Overdue " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(1)));
		ImportantMessage overdueButSigned = newMessageDue("Signed " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(1)));
		ImportantMessage dueTomorrow = newMessageDue("Tomorrow " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(1)));
		ImportantMessage dueNextMonth = newMessageDue("Next month " + tag, manager, Set.of(engineering), now.plus(Duration.ofDays(30)));
		messageService.acknowledge(overdueButSigned.getId(), carol.getId());
		for (ImportantMessage m : List.of(overdue, overdueButSigned)) {
			ImportantMessage stored = messageRepository.findById(m.getId()).orElseThrow();
			stored.setDeadline(now.minus(Duration.ofHours(3)));
			messageRepository.save(stored);
		}

		assertThat(messageService.findOverdue()).extracting(ImportantMessage::getId)
				.contains(overdue.getId())
				.doesNotContain(overdueButSigned.getId(), dueTomorrow.getId(), dueNextMonth.getId());
		assertThat(messageService.findDueWithin(7)).extracting(ImportantMessage::getId)
				.contains(dueTomorrow.getId())
				.doesNotContain(overdue.getId(), dueNextMonth.getId());
		assertThatThrownBy(() -> messageService.findDueWithin(0)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void missingDocumentsRaiseNotFound() {
		assertThatThrownBy(() -> employeeService.findById("does-not-exist"))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> messageService.findById("does-not-exist"))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
