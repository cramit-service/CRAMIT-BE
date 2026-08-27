package com.cramit.domain.member;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

}
