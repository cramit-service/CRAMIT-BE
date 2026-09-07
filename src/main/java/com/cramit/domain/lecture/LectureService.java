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
import com.cramit.domain.share.MemberLecture;
import com.cramit.domain.share.MemberLectureRepository;
import com.cramit.domain.week.repository.LectureAudioRepository;
import com.cramit.domain.week.repository.LecturePptRepository;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;
    private final MemberRepository memberRepository;
    private final LecturePptRepository lecturePptRepository;
    private final LectureAudioRepository  lectureAudioRepository;
    private final WeekRepository weekRepository;
    private final MemberLectureRepository memberLectureRepository;

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
                .map(lecture -> {
                    int weekCount = weekRepository.findByLectureIdOrderByWeekDateDesc(lecture.getLectureId()).size();
                    return MyLectureItem.from(lecture, weekCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SharedLectureItem> getSharedLectures(Long memberId) {
        List<MemberLecture> sharedRelations = memberLectureRepository.findByMemberId(memberId);

        return sharedRelations.stream()
                .map(ml -> {
                    Lecture lecture = lectureRepository.findById(ml.getLectureId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
                    Member owner = memberRepository.findById(lecture.getMemberId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
                    int weekCount = weekRepository.findByLectureIdOrderByWeekDateDesc(lecture.getLectureId()).size();

                    return SharedLectureItem.of(lecture, weekCount, owner.getNickname());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LectureDetailResponse getLectureDetail(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        boolean canAccess = lecture.isOwnedBy(memberId)
                || memberLectureRepository.existsByLectureIdAndMemberId(lectureId, memberId);

        if (!canAccess) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        Member owner = memberRepository.findById(lecture.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        int memberCount = memberLectureRepository.findByLectureId(lectureId).size() + 1; // 공유받은 멤버 수 + 생성자 본인

        return LectureDetailResponse.from(
                lecture,
                memberId,
                owner.getNickname(),
                memberCount
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
