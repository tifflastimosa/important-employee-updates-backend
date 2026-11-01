package com.example.important_messages_prototype.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	private final List<String> employeeIds = new ArrayList<>();
	private final List<String> messageIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		messageRepository.deleteAllById(messageIds);
		employeeRepository.deleteAllById(employeeIds);
	}

	private Employee newEmployee(String first, String department, Role role) {
		Employee e = employeeService.create(new Employee(first.toUpperCase() + "-" + tag, first, "Test",
				first.toLowerCase() + "-" + tag + "@example.com", department, "Tester", role));
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
				null, Role.EMPLOYEE);
		assertThatThrownBy(() -> employeeService.create(duplicate)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void rejectsDuplicateEmployeeNumber() {
		Employee alice = newEmployee("Alice", engineering, Role.EMPLOYEE);

		Employee sameNumber = new Employee(alice.getEmployeeNumber(), "Other", "Person", "other-" + tag + "@example.com",
				sales, null, Role.EMPLOYEE);
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

	@Test
	void missingDocumentsRaiseNotFound() {
		assertThatThrownBy(() -> employeeService.findById("does-not-exist"))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> messageService.findById("does-not-exist"))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
