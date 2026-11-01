package com.example.important_messages_prototype.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.important_messages_prototype.model.AcknowledgementStatus;
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
