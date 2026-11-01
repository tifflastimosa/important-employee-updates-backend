package com.example.important_messages_prototype.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.important_messages_prototype.model.Employee;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.service.EmployeeService;
import com.example.important_messages_prototype.service.ImportantMessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeService employeeService;
	private final ImportantMessageService messageService;

	public EmployeeController(EmployeeService employeeService, ImportantMessageService messageService) {
		this.employeeService = employeeService;
		this.messageService = messageService;
	}

	@GetMapping
	public List<Employee> list(@RequestParam(required = false) String department,
			@RequestParam(required = false) String storeNumber) {
		return employeeService.findAll(department, storeNumber);
	}

	@GetMapping("/{id}")
	public Employee get(@PathVariable String id) {
		return employeeService.findById(id);
	}

	/** The important messages this employee should see. */
	@GetMapping("/{id}/messages")
	public List<ImportantMessage> messagesFor(@PathVariable String id) {
		return messageService.findForEmployee(id);
	}

	/** The important messages this employee still needs to sign/acknowledge. */
	@GetMapping("/{id}/messages/pending")
	public List<ImportantMessage> pendingMessagesFor(@PathVariable String id) {
		return messageService.findPendingForEmployee(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Employee create(@Valid @RequestBody Employee employee) {
		return employeeService.create(employee);
	}

	@PutMapping("/{id}")
	public Employee update(@PathVariable String id, @Valid @RequestBody Employee employee) {
		return employeeService.update(id, employee);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String id) {
		employeeService.delete(id);
	}
}
