package com.cramit.domain.subscription;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

	Optional<Plan> findByType(PlanType type);

}
