package com.cramit.domain.lecture;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
