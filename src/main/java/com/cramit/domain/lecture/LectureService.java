package com.cramit.domain.lecture;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;

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
    public LectureListResponse getLectures(Long memberId) {
        List<Lecture> myLectures = lectureRepository.findByMemberId(memberId);

        List<LectureListResponse.LectureSummary> mySummaries = myLectures.stream()
                .map(lecture -> new LectureListResponse.LectureSummary(
                        lecture.getLectureId(),
                        lecture.getTitle(),
                        lecture.getProfessorName(),
                        0, // TODO: Week 엔티티 완성되면 실제 주차 개수로 교체
                        null, // 내 강의는 ownerNickname 없음
                        null // TODO: Exam 엔티티 완성되면 가장 가까운 시험 계산 로직 추가
                )).toList();

        // TODO: MemberLecture 엔티티/Repository 완성되면 공유 강의 목록 조회 로직 추가
        List<LectureListResponse.LectureSummary> sharedSummaries = Collections.emptyList();

        return new  LectureListResponse(mySummaries, sharedSummaries);
    }

    @Transactional(readOnly = true)
    public LectureDetailResponse getLectureDetail(Long lectureId, Long memberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(()->new IllegalArgumentException("강의를 찾을 수 없습니다.")); //전역 예외 생기면 여기만 교체

        return new LectureDetailResponse(
                lecture.getLectureId(),
                lecture.getTitle(),
                lecture.getProfessorName(),
                lecture.isOwnedBy(memberId),
                null, //Todo: Member 엔티티 완성되면 소유자 닉네임 조회
                1, //Todo: MemberLecture 완성되면 참여 인원 수 조회
                lecture.getCreatedAt().toString()
        );
    }

    @Transactional
    public LectureUpdateResponse updateLecture(LectureUpdateRequest request, Long lectureId, Long currentMemberId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        if (!lecture.isOwnedBy(currentMemberId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        if (!lecture.isOwnedBy(currentMemberId)) {
            throw new IllegalStateException("접근 권한이 없습니다.");
        }

        lectureRepository.deleteById(lectureId);
    }
}
