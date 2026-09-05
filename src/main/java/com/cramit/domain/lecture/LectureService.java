package com.cramit.domain.lecture;

import com.cramit.domain.lecture.dto.LectureCreateRequest;
import com.cramit.domain.lecture.dto.LectureCreateResponse;
import com.cramit.domain.lecture.dto.LectureDetailResponse;
import com.cramit.domain.lecture.dto.LectureUpdateRequest;
import com.cramit.domain.lecture.dto.LectureUpdateResponse;
import com.cramit.domain.lecture.dto.MyLectureItem;
import com.cramit.domain.lecture.dto.SharedLectureItem;
import com.cramit.domain.member.Member;
import com.cramit.domain.member.MemberRepository;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;
    private final MemberRepository memberRepository;

    private final LecturePptRepository lecturePptRepository;

    private final LectureAudioRepository  lectureAudioRepository;

    private final WeekRepository weekRepository;

    @Transactional
    public LectureCreateResponse createLecture(LectureCreateRequest request, Long memberId) {
        Lecture lecture = Lecture.builder()
                .memberId(memberId)
                .title(request.title())
                .professorName(request.professorName())
                .build();

        lectureRepository.save(lecture);

        return new LectureCreateResponse(lecture.getLectureId(), lecture.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<MyLectureItem> getMyLectures(Long memberId) {
        List<Lecture> myLectures = lectureRepository.findByMemberId(memberId);
        return myLectures.stream()
                .map(MyLectureItem::from)
                .toList();
    }

    @Transactional(readOnly = true)
    // TODO: MemberLecture 도메인 완성되면 실제 공유 강의 조회 로직 구현
    public List<SharedLectureItem> getSharedLectures(Long memberId) {
        return Collections.emptyList();
    }

    @Transactional(readOnly = true)
    public LectureDetailResponse getLectureDetail(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        // TODO: MemberLecture 도메인 완성되면 공유받은 회원도 접근 가능하도록 조건 추가
        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        Member owner = memberRepository.findById(lecture.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        return LectureDetailResponse.from(
                lecture,
                memberId,
                owner.getNickname(),
                1 // TODO: MemberLecture 완성되면 참여 인원 수 조회
        );
    }

    @Transactional
    public LectureUpdateResponse updateLecture(LectureUpdateRequest request, Long lectureId, Long currentMemberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(currentMemberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        String professorName = request.professorName() != null
                ? request.professorName()
                : lecture.getProfessorName();

        lecture.update(request.title(), professorName); // TODO: Exam 엔티티 완성되면 request.examDate() 반영

        return new  LectureUpdateResponse(lecture.getLectureId());
    }

    @Transactional
    public void deleteLecture(Long lectureId, Long currentMemberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        if (!lecture.isOwnedBy(currentMemberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        List<Week> weeks = weekRepository.findByLectureIdOrderByWeekDateDesc(lectureId);
        List<Long> weekIds = weeks.stream().map(Week::getWeekId).toList();

        if (!weekIds.isEmpty()) {
            lecturePptRepository.deleteAllByWeekIdIn(weekIds);
            lectureAudioRepository.deleteAllByWeekIdIn(weekIds);
            // TODO: script, summary, todo, chatBotSession 등 도메인 완성되면 여기 추가
        }
        weekRepository.deleteAll(weeks);

        lectureRepository.delete(lecture);
    }
}
