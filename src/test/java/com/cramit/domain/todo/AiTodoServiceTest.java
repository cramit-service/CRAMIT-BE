package com.cramit.domain.todo;

import com.cramit.domain.ai.GeminiTodoClient;
import com.cramit.domain.ai.dto.GeminiTodoItem;
import com.cramit.domain.exam.entity.Exam;
import com.cramit.domain.exam.repository.ExamRepository;
import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.todo.dto.TodoListResponse;
import com.cramit.domain.todo.enums.TodoType;
import com.cramit.domain.week.entity.Week;
import com.cramit.domain.week.repository.WeekRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import({AiTodoService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class AiTodoServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final LocalDate TODAY = LocalDate.now();

    @MockBean
    private GeminiTodoClient geminiTodoClient;

    @Autowired
    private AiTodoService aiTodoService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private WeekRepository weekRepository;

    @Test
    @DisplayName("요약본이 있는 주차를 근거로 AI TODO를 생성해 저장한다.")
    void generate() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(weekId, "정렬 알고리즘 정리", TODAY.plusDays(1).toString(), "시간복잡도 위주로")
        ));

        // when
        List<TodoListResponse> response = aiTodoService.generate(examId, MEMBER_ID);

        // then
        assertThat(response).hasSize(1);
        TodoListResponse todo = response.get(0);
        assertThat(todo.todoType()).isEqualTo(TodoType.AI);
        assertThat(todo.weekId()).isEqualTo(weekId);
        assertThat(todo.content()).isEqualTo("정렬 알고리즘 정리");
        assertThat(todo.memo()).isEqualTo("시간복잡도 위주로");
        assertThat(todo.dueDate().toLocalDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(todoRepository.findById(todo.todoId()).orElseThrow().getExamId()).isEqualTo(examId);
        verify(geminiTodoClient).generate(eq(AiTodoPrompts.SYSTEM), contains("weekId=" + weekId));
    }

    @Test
    @DisplayName("AI 응답 중 없는 주차·빈 내용·잘못된 날짜는 버리고, 범위를 벗어난 날짜는 오늘~시험일로 맞추며 하루 3개까지만 저장한다.")
    void generateValidatesAiResponse() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        LocalDate examDate = TODAY.plusDays(5);
        Long examId = saveExam(lectureId, examDate);
        String tomorrow = TODAY.plusDays(1).toString();
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(999L, "없는 주차", tomorrow, null),
                new GeminiTodoItem(weekId, "  ", tomorrow, null),
                new GeminiTodoItem(weekId, "잘못된 날짜", "내일", null),
                new GeminiTodoItem(weekId, "지난 날짜", TODAY.minusDays(3).toString(), null),
                new GeminiTodoItem(weekId, "시험 이후", examDate.plusDays(3).toString(), null),
                new GeminiTodoItem(weekId, "내일 1", tomorrow, null),
                new GeminiTodoItem(weekId, "내일 2", tomorrow, null),
                new GeminiTodoItem(weekId, "내일 3", tomorrow, null),
                new GeminiTodoItem(weekId, "내일 4", tomorrow, null)
        ));

        // when
        List<TodoListResponse> response = aiTodoService.generate(examId, MEMBER_ID);

        // then
        assertThat(response).extracting(TodoListResponse::content)
                .containsExactly("지난 날짜", "시험 이후", "내일 1", "내일 2", "내일 3");
        assertThat(response.get(0).dueDate().toLocalDate()).isEqualTo(TODAY);
        assertThat(response.get(1).dueDate().toLocalDate()).isEqualTo(examDate);
    }

    @Test
    @DisplayName("다시 생성하면 같은 시험의 미완료 AI TODO만 바뀌고, 완료한 AI TODO·직접 만든 TODO·다른 시험의 계획은 유지된다.")
    void generateReplacesIncompleteAiTodos() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        Long finalExamId = saveExam(lectureId, TODAY.plusDays(40));
        Todo oldAi = saveTodo(weekId, examId, "이전 AI 계획", TodoType.AI);
        Todo doneAi = saveTodo(weekId, examId, "완료한 AI 계획", TodoType.AI);
        doneAi.toggleComplete();
        Todo userTodo = saveTodo(weekId, null, "직접 만든 할 일", TodoType.USER);
        Todo finalExamAi = saveTodo(weekId, finalExamId, "기말 AI 계획", TodoType.AI);
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(weekId, "새 AI 계획", TODAY.plusDays(1).toString(), null)
        ));

        // when
        aiTodoService.generate(examId, MEMBER_ID);

        // then
        assertThat(todoRepository.findByMemberIdOrderByTodoIdDesc(MEMBER_ID))
                .extracting(Todo::getContent)
                .containsExactlyInAnyOrder("완료한 AI 계획", "직접 만든 할 일", "기말 AI 계획", "새 AI 계획");
        assertThat(todoRepository.findById(oldAi.getTodoId())).isEmpty();
        assertThat(todoRepository.findById(userTodo.getTodoId())).isPresent();
        assertThat(todoRepository.findById(finalExamAi.getTodoId())).isPresent();
    }

    @Test
    @DisplayName("AI 응답에 null 항목이 섞여 있으면 건너뛰고 나머지를 저장한다.")
    void generateSkipsNullItem() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(Arrays.asList(
                null,
                new GeminiTodoItem(weekId, "정렬 정리", TODAY.plusDays(1).toString(), null)
        ));

        // when
        List<TodoListResponse> response = aiTodoService.generate(examId, MEMBER_ID);

        // then
        assertThat(response).extracting(TodoListResponse::content).containsExactly("정렬 정리");
    }

    @Test
    @DisplayName("시험 날짜가 지났으면 INVALID_DDAY를 던진다.")
    void generatePastExam() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        saveWeek(lectureId, TODAY.minusDays(14), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.minusDays(1));

        // when & then
        assertThatThrownBy(() -> aiTodoService.generate(examId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DDAY));
        verify(geminiTodoClient, never()).generate(anyString(), anyString());
    }

    @Test
    @DisplayName("시험일까지 1차 요약본이 있는 주차가 없으면 NO_CONTEXT를 던진다.")
    void generateWithoutSummary() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        saveWeek(lectureId, TODAY.minusDays(7), null);
        saveWeek(lectureId, TODAY.plusDays(10), "## 시험 이후 주차");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));

        // when & then
        assertThatThrownBy(() -> aiTodoService.generate(examId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NO_CONTEXT));
    }

    @Test
    @DisplayName("본인 강의의 시험이 아니면 LECTURE_ACCESS_DENIED를 던진다.")
    void generateForbidden() {
        // given
        Long lectureId = saveLecture(OTHER_MEMBER_ID);
        saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));

        // when & then
        assertThatThrownBy(() -> aiTodoService.generate(examId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LECTURE_ACCESS_DENIED));
    }

    @Test
    @DisplayName("AI 응답에 저장할 수 있는 할 일이 하나도 없으면 TODO_GENERATION_FAILED를 던지고 기존 TODO는 지우지 않는다.")
    void generateNoValidTodo() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        saveTodo(weekId, examId, "이전 AI 계획", TodoType.AI);
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(999L, "없는 주차", TODAY.plusDays(1).toString(), null)
        ));

        // when & then
        assertThatThrownBy(() -> aiTodoService.generate(examId, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_GENERATION_FAILED));
        assertThat(todoRepository.findByMemberIdOrderByTodoIdDesc(MEMBER_ID))
                .extracting(Todo::getContent)
                .containsExactly("이전 AI 계획");
    }

    @Test
    @DisplayName("다시 생성하면 남아 있는 같은 시험의 TODO(완료한 것, 직접 만들거나 수정한 것)를 프롬프트에 넣는다.")
    void generateSendsRemainingTodos() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        Todo doneAi = saveTodo(weekId, examId, "버블 정렬 정리", TodoType.AI);
        doneAi.toggleComplete();
        saveTodo(weekId, examId, "내가 고친 퀵 정렬 계획", TodoType.USER);
        saveTodo(weekId, examId, "지워질 AI 계획", TodoType.AI);
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(weekId, "병합 정렬 정리", TODAY.plusDays(1).toString(), null)
        ));

        // when
        aiTodoService.generate(examId, MEMBER_ID);

        // then
        verify(geminiTodoClient).generate(eq(AiTodoPrompts.SYSTEM), argThat(input ->
                input.contains("[완료] " + TODAY.plusDays(2) + " | 버블 정렬 정리")
                        && input.contains("[예정] " + TODAY.plusDays(2) + " | 내가 고친 퀵 정렬 계획")
                        && !input.contains("지워질 AI 계획")));
    }

    @Test
    @DisplayName("하루 최대 개수는 남아 있는 같은 시험의 TODO까지 합쳐서 세고, 다른 시험의 TODO는 세지 않는다.")
    void generateCountsRemainingTodosPerDay() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(5));
        Long otherExamId = saveExam(lectureId, TODAY.plusDays(40));
        LocalDate day = TODAY.plusDays(2);
        saveTodo(weekId, examId, "직접 만든 할 일 1", TodoType.USER);
        saveTodo(weekId, examId, "직접 만든 할 일 2", TodoType.USER);
        saveTodo(weekId, otherExamId, "기말 계획", TodoType.USER);
        given(geminiTodoClient.generate(anyString(), anyString())).willReturn(List.of(
                new GeminiTodoItem(weekId, "새 계획 1", day.toString(), null),
                new GeminiTodoItem(weekId, "새 계획 2", day.toString(), null),
                new GeminiTodoItem(weekId, "다음 날 계획", day.plusDays(1).toString(), null)
        ));

        // when
        List<TodoListResponse> response = aiTodoService.generate(examId, MEMBER_ID);

        // then
        assertThat(response).extracting(TodoListResponse::content)
                .containsExactly("새 계획 1", "다음 날 계획");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("AI가 계획을 만드는 동안 시험일이 바뀌면 저장하지 않고 실패한다.")
    void generateFailsWhenExamChangedDuringGeneration() {
        // given
        Long lectureId = saveLecture(MEMBER_ID);
        Long weekId = saveWeek(lectureId, TODAY.minusDays(7), "## 정렬");
        Long examId = saveExam(lectureId, TODAY.plusDays(10));
        given(geminiTodoClient.generate(anyString(), anyString())).willAnswer(invocation -> {
            Exam exam = examRepository.findById(examId).orElseThrow();
            exam.update(exam.getLectureId(), exam.getTitle(), TODAY.plusDays(2), exam.getMemo());
            examRepository.save(exam);
            return List.of(new GeminiTodoItem(weekId, "정렬 정리", TODAY.plusDays(8).toString(), null));
        });

        try {
            // when & then
            assertThatThrownBy(() -> aiTodoService.generate(examId, MEMBER_ID))
                    .isInstanceOfSatisfying(BusinessException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_GENERATION_FAILED));
            assertThat(todoRepository.findByMemberIdOrderByTodoIdDesc(MEMBER_ID)).isEmpty();
        } finally {
            todoRepository.deleteAll();
            examRepository.deleteAll();
            weekRepository.deleteAll();
            lectureRepository.deleteAll();
        }
    }

    private Long saveLecture(Long memberId) {
        return lectureRepository.save(Lecture.builder()
                .memberId(memberId)
                .title("알고리즘")
                .professorName("박지훈")
                .colorIndex(1)
                .build()).getLectureId();
    }

    private Long saveWeek(Long lectureId, LocalDate weekDate, String firstSummaryMd) {
        Week week = Week.builder()
                .lectureId(lectureId)
                .title(weekDate + " 주차")
                .weekDate(weekDate)
                .build();
        if (firstSummaryMd != null) {
            week.updateFirstSummary(firstSummaryMd);
        }
        return weekRepository.save(week).getWeekId();
    }

    private Long saveExam(Long lectureId, LocalDate examDate) {
        return examRepository.save(Exam.builder()
                .lectureId(lectureId)
                .title("중간고사")
                .examDate(examDate)
                .build()).getExamId();
    }

    private Todo saveTodo(Long weekId, Long examId, String content, TodoType todoType) {
        return todoRepository.save(Todo.builder()
                .memberId(MEMBER_ID)
                .weekId(weekId)
                .examId(examId)
                .content(content)
                .dueDate(TODAY.plusDays(2).atTime(23, 59))
                .todoType(todoType)
                .build());
    }
}
