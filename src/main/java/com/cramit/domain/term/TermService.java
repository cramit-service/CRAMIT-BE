package com.cramit.domain.term;

import com.cramit.domain.term.dto.TermResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TermService {

	private final TermRepository termRepository;

	@Transactional(readOnly = true)
	public List<TermResponse> getTerms() {
		return termRepository.findAllByOrderByIdAsc().stream()
				.map(TermResponse::from)
				.toList();
	}

}
