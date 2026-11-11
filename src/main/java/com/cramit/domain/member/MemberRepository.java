package com.cramit.domain.member;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

	boolean existsByNicknameAndDeletedFalse(String nickname);

	boolean existsByNicknameAndDeletedFalseAndIdNot(String nickname, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Member m where m.id = :id")
	Optional<Member> findByIdForUpdate(Long id);

}
