package com.example.important_messages_prototype.service;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
import com.example.important_messages_prototype.model.Employee;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;
import com.example.important_messages_prototype.repository.EmployeeRepository;
import com.example.important_messages_prototype.repository.ImportantMessageRepository;

@Service
public class ImportantMessageServiceImpl implements ImportantMessageService {

	private final ImportantMessageRepository messageRepository;
	private final EmployeeService employeeService;
	private final EmployeeRepository employeeRepository;

	public ImportantMessageServiceImpl(ImportantMessageRepository messageRepository, EmployeeService employeeService,
			EmployeeRepository employeeRepository) {
		this.messageRepository = messageRepository;
		this.employeeService = employeeService;
		this.employeeRepository = employeeRepository;
	}

	/** Everyone the message is addressed to, except its author. */
	private List<Employee> recipientsOf(ImportantMessage message) {
		List<Employee> candidates = message.isCompanyWide()
				? employeeRepository.findAll()
				: employeeRepository.findByDepartmentIn(message.getTargetDepartments());
		return candidates.stream()
				.filter(e -> !e.equals(message.getAuthor()))
				.toList();
	}

	@Override
	public List<ImportantMessage> findAll(Priority priority) {
		return priority == null
				? messageRepository.findAllByOrderByCreatedAtDesc()
				: messageRepository.findByPriorityOrderByCreatedAtDesc(priority);
	}

	@Override
	public ImportantMessage findById(String id) {
		return messageRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Important message", id));
	}

	@Override
	public List<ImportantMessage> findForEmployee(String employeeId) {
		Employee employee = employeeService.findById(employeeId);
		return messageRepository.findVisibleToDepartment(employee.getDepartment());
	}

	@Override
	public List<ImportantMessage> findPendingForEmployee(String employeeId) {
		Employee employee = employeeService.findById(employeeId);
		return messageRepository.findNotAcknowledgedBy(new ObjectId(employee.getId()));
	}

	@Override
	public AcknowledgementStatus getAcknowledgementStatus(String messageId) {
		return AcknowledgementStatus.of(findById(messageId));
	}

	@Override
	public ImportantMessage create(ImportantMessage message) {
		if (message.getAuthor() == null || message.getAuthor().getId() == null) {
			throw new BusinessRuleException("author.id is required");
		}
		Employee author = employeeService.findById(message.getAuthor().getId());
		if (!author.canPublishMessages()) {
			throw new BusinessRuleException(author.getFullName() + " (" + author.getRole() + ") cannot publish messages");
		}
		message.setId(null);
		message.setAuthor(author);
		message.setAcknowledgedBy(null);
		message.assignRecipients(recipientsOf(message));
		return messageRepository.save(message);
	}

	@Override
	public ImportantMessage update(String id, ImportantMessage changes) {
		ImportantMessage existing = findById(id);
		existing.updateFrom(changes);
		existing.assignRecipients(recipientsOf(existing));
		return messageRepository.save(existing);
	}

	@Override
	public void delete(String id) {
		messageRepository.delete(findById(id));
	}

	@Override
	public ImportantMessage acknowledge(String messageId, String employeeId) {
		ImportantMessage message = findById(messageId);
		Employee employee = employeeService.findById(employeeId);
		if (!message.isRecipient(employee)) {
			throw new BusinessRuleException("This message is not addressed to " + employee.getFullName());
		}
		if (message.acknowledge(employee)) {
			message = messageRepository.save(message);
		}
		return message;
	}
}
