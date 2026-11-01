package com.example.important_messages_prototype.model;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Common fields shared by every MongoDB document. Timestamps are filled in by
 * Spring Data auditing (see MongoConfig).
 */
public abstract class BaseDocument {

	@Id
	private String id;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

	/**
	 * Two documents are equal when they are the same type and have the same id, so
	 * sets of documents (e.g. Set&lt;Employee&gt;) behave correctly. Unsaved documents
	 * (null id) are only equal to themselves.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return id != null && id.equals(((BaseDocument) o).id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + Objects.toString(id) + "}";
	}
}
