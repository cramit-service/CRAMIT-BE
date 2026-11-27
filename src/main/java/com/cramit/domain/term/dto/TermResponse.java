package com.cramit.domain.term.dto;

import com.cramit.domain.term.Term;

public record TermResponse(
		Long termId,
		String title,
		String content,
		boolean required
) {
	public static TermResponse from(Term term) {
		return new TermResponse(term.getId(), term.getTitle(), term.getContent(), term.isRequired());
	}
}
