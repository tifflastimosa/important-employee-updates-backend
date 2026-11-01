package com.example.important_messages_prototype.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.important_messages_prototype.model.Employee;
import com.example.important_messages_prototype.model.ImportantMessage;
import com.example.important_messages_prototype.model.Priority;

public interface ImportantMessageRepository extends MongoRepository<ImportantMessage, String> {

	List<ImportantMessage> findAllByOrderByCreatedAtDesc();

	List<ImportantMessage> findByPriorityOrderByCreatedAtDesc(Priority priority);

	List<ImportantMessage> findByAuthorOrderByCreatedAtDesc(Employee author);

	/** Company-wide messages (no target departments) plus messages aimed at the given department. */
	@Query(value = "{ '$or': [ { 'targetDepartments': { '$size': 0 } }, { 'targetDepartments': ?0 } ] }",
			sort = "{ 'createdAt': -1 }")
	List<ImportantMessage> findVisibleToDepartment(String department);

	/** Messages where the employee is still in notAcknowledgedBy (stored as an array of employee ObjectIds). */
	@Query(value = "{ 'notAcknowledgedBy': ?0 }", sort = "{ 'createdAt': -1 }")
	List<ImportantMessage> findNotAcknowledgedBy(ObjectId employeeId);
}
