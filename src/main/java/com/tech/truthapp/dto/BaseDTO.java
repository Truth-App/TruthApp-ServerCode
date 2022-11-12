package com.tech.truthapp.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class BaseDTO {

	@JsonDeserialize(converter = LongToLocalDateTimeConverter.class)
	private LocalDateTime createdAt;
	@JsonDeserialize(converter = LongToLocalDateTimeConverter.class)
	private LocalDateTime updatedAt;
	
	private String createdBy;
	private String lastModifiedBy;
	private Long version;
	
	public static class LongToLocalDateTimeConverter extends StdConverter<Long, LocalDateTime> {
		public LocalDateTime convert(final Long value) {
			return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
	}
}
