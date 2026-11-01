package com.example.important_messages_prototype.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
import com.example.important_messages_prototype.model.DeadlineCountdown;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;
import com.example.important_messages_prototype.service.ImportantMessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class ImportantMessageController {

	private final ImportantMessageService messageService;

	public ImportantMessageController(ImportantMessageService messageService) {
		this.messageService = messageService;
	}

	@GetMapping
	public List<ImportantMessage> list(@RequestParam(required = false) Priority priority) {
		return messageService.findAll(priority);
	}

	/** Messages past their deadline that still have employees who have not signed (for managers). */
	@GetMapping("/overdue")
	public List<ImportantMessage> overdue() {
		return messageService.findOverdue();
	}

	/** Messages with a deadline in the next N days (default 7), soonest first. */
	@GetMapping("/due-soon")
	public List<ImportantMessage> dueSoon(@RequestParam(defaultValue = "7") int days) {
		return messageService.findDueWithin(days);
	}

	@GetMapping("/{id}")
	public ImportantMessage get(@PathVariable String id) {
		return messageService.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ImportantMessage create(@Valid @RequestBody ImportantMessage message) {
		return messageService.create(message);
	}

	@PutMapping("/{id}")
	public ImportantMessage update(@PathVariable String id, @Valid @RequestBody ImportantMessage message) {
		return messageService.update(id, message);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable String id) {
		messageService.delete(id);
	}

	/** Time left until the message's deadline. Small response, suitable for polling. */
	@GetMapping("/{id}/countdown")
	public DeadlineCountdown countdown(@PathVariable String id) {
		return messageService.getCountdown(id);
	}

	/** Who has and has not signed/acknowledged the message. */
	@GetMapping("/{id}/acknowledgements")
	public AcknowledgementStatus acknowledgementStatus(@PathVariable String id) {
		return messageService.getAcknowledgementStatus(id);
	}

	/** Marks the message as read by the given employee. */
	@PostMapping("/{id}/acknowledgements/{employeeId}")
	public ImportantMessage acknowledge(@PathVariable String id, @PathVariable String employeeId) {
		return messageService.acknowledge(id, employeeId);
	}
}
