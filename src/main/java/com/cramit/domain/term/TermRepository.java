package com.cramit.domain.term;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

	List<Term> findByRequiredTrue();

	List<Term> findAllByOrderByIdAsc();

}
