package com.cramit.domain.todo;

import com.cramit.domain.lecture.Lecture;
import com.cramit.domain.lecture.LectureRepository;
import com.cramit.domain.week.Week;
import com.cramit.domain.week.WeekRepository;
import com.cramit.global.config.JpaAuditingConfig;
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TodoService.class, JpaAuditingConfig.class})
@ActiveProfiles("test")
class TodoServiceTest {
    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final LocalDateTime WEEK_DATE = LocalDateTime.now();

    @Autowired
    private TodoService todoService;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private WeekRepository weekRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Test
    @DisplayName("weekId 없이 todo를 생성할 수 있다.")
    void createTodoWithoutWeek() {
        // given
        TodoCreateRequest request = new TodoCreateRequest(null, "교재 구매", null, null);

        // when
        TodoCreateResponse response = todoService.createTodo(request, MEMBER_ID);

        // then
        assertThat(response.todoId()).isNotNull();
        Todo todo = todoRepository.findById(response.todoId()).orElseThrow();
        assertThat(todo.getWeekId()).isNull();
        assertThat(todo.getTodoType()).isEqualTo(TodoType.USER);
        assertThat(todo.isCompleted()).isFalse();
        assertThat(todo.getSortOrder()).isZero();
    }

    @Test
    @DisplayName("weekId와 함께 todo를 생성할 수 있다.")
    void createTodoWithWeek() {
        // given
        Long weekId = saveWeek();
        TodoCreateRequest request = new TodoCreateRequest(weekId, "복습하기", LocalDateTime.now(), "메모");

        // when
        TodoCreateResponse response = todoService.createTodo(request, MEMBER_ID);

        // then
        Todo todo = todoRepository.findById(response.todoId()).orElseThrow();
        assertThat(todo.getWeekId()).isEqualTo(weekId);
    }

    @Test
    @DisplayName("존재하지 않는 weekId로 todo를 생성하면 예외가 발생한다.")
    void createTodoWithInvalidWeek() {
        TodoCreateRequest request = new TodoCreateRequest(999L, "복습하기", null, null);

        assertThatThrownBy(() -> todoService.createTodo(request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Test
    @DisplayName("todo 목록을 정렬 순서대로 조회한다.")
    void getTodosOrderedBySortOrder() {
        // given
        todoService.createTodo(new TodoCreateRequest(null, "A", null, null), MEMBER_ID);
        todoService.createTodo(new TodoCreateRequest(null, "B", null, null), MEMBER_ID);

        // when
        Page<TodoListResponse> response = todoService.getTodos(MEMBER_ID, null, null, PageRequest.of(0, 10));

        // then
        assertThat(response).hasSize(2);
    }

    @Test
    @DisplayName("weekId로 필터링해서 todo 목록을 조회한다.")
    void getTodosFilteredByWeek() {
        // given
        Long weekId = saveWeek();
        todoService.createTodo(new TodoCreateRequest(weekId, "복습", null, null), MEMBER_ID);
        todoService.createTodo(new TodoCreateRequest(null, "개인 할일", null, null), MEMBER_ID);

        // when
        Page<TodoListResponse> response = todoService.getTodos(MEMBER_ID, weekId, null, PageRequest.of(0, 10));

        // then
        assertThat(response).hasSize(1);
        assertThat(response.getContent().get(0).content()).isEqualTo("복습");
    }

    @Test
    @DisplayName("완료된 todo만 필터링해서 조회한다.")
    void getTodosFilteredByCompleted() {
        // given
        TodoCreateResponse created = todoService.createTodo(
                new TodoCreateRequest(null, "복습", null, null), MEMBER_ID);
        todoService.toggleTodo(created.todoId(), MEMBER_ID);
        todoService.createTodo(new TodoCreateRequest(null, "미완료", null, null), MEMBER_ID);

        // when
        Page<TodoListResponse> response = todoService.getTodos(MEMBER_ID, null, TodoFilterStatus.COMPLETED, PageRequest.of(0, 10));

        // then
        assertThat(response).hasSize(1);
        assertThat(response.getContent().get(0).content()).isEqualTo("복습");
    }

    @Test
    @DisplayName("todo를 수정할 수 있다.")
    void updateTodo() {
        // given
        TodoCreateResponse created = todoService.createTodo(
                new TodoCreateRequest(null, "원래 제목", null, null), MEMBER_ID);
        TodoUpdateRequest request = new TodoUpdateRequest(null, "수정된 제목", "새 메모", null);

        // when
        TodoUpdateResponse response = todoService.updateTodo(created.todoId(), request, MEMBER_ID);

        // then
        assertThat(response.todoId()).isEqualTo(created.todoId());
        Todo todo = todoRepository.findById(created.todoId()).orElseThrow();
        assertThat(todo.getContent()).isEqualTo("수정된 제목");
        assertThat(todo.getMemo()).isEqualTo("새 메모");
    }

    @Test
    @DisplayName("본인 소유가 아닌 todo를 수정하면 예외가 발생한다.")
    void updateTodoForbidden() {
        // given
        Todo todo = todoRepository.save(
                Todo.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .content("남의 할일")
                        .build()
        );
        TodoUpdateRequest request = new TodoUpdateRequest(null, "수정 시도", null, null);

        // when & then
        assertThatThrownBy(() -> todoService.updateTodo(todo.getTodoId(), request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_ACCESS_DENIED));
    }

    @Test
    @DisplayName("todo를 삭제하면 더 이상 조회되지 않는다.")
    void deleteTodo() {
        // given
        TodoCreateResponse created = todoService.createTodo(
                new TodoCreateRequest(null, "삭제될 할일", null, null), MEMBER_ID);

        // when
        todoService.deleteTodo(created.todoId(), MEMBER_ID);

        // then
        assertThat(todoRepository.findById(created.todoId())).isEmpty();
    }

    @Test
    @DisplayName("본인 소유가 아닌 todo를 삭제하면 예외가 발생한다.")
    void deleteTodoForbidden() {
        // given
        Todo todo = todoRepository.save(
                Todo.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .content("남의 할일")
                        .build()
        );

        // when & then
        assertThatThrownBy(() -> todoService.deleteTodo(todo.getTodoId(), MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_ACCESS_DENIED));
    }

    @Test
    @DisplayName("todo 완료 상태를 토글하면 완료/미완료가 전환된다.")
    void toggleTodo() {
        // given
        TodoCreateResponse created = todoService.createTodo(
                new TodoCreateRequest(null, "토글 테스트", null, null), MEMBER_ID);

        // when: 완료로 전환
        TodoToggleResponse completed = todoService.toggleTodo(created.todoId(), MEMBER_ID);

        // then
        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.completedAt()).isNotNull();

        // when: 다시 미완료로 전환
        TodoToggleResponse uncompleted = todoService.toggleTodo(created.todoId(), MEMBER_ID);

        // then
        assertThat(uncompleted.isCompleted()).isFalse();
        assertThat(uncompleted.completedAt()).isNull();
    }

    private Long saveWeek() {
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(MEMBER_ID)
                        .title("알고리즘")
                        .professorName("박지훈")
                        .build()
        ).getLectureId();

        return weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();
    }

    @Test
    @DisplayName("다른 회원 소유의 week로 todo를 생성하면 예외가 발생한다.")
    void createTodoWithOtherMembersWeek() {
        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .title("다른 사람 강의")
                        .professorName("교수님")
                        .build()
        ).getLectureId();

        Long weekId = weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();

        TodoCreateRequest request = new TodoCreateRequest(weekId, "몰래 붙이기", null, null);

        assertThatThrownBy(() -> todoService.createTodo(request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_ACCESS_DENIED));
    }

    @Test
    @DisplayName("다른 회원 소유의 week로 todo를 수정하면 예외가 발생한다.")
    void updateTodoWithOtherMembersWeek() {
        TodoCreateResponse created = todoService.createTodo(
                new TodoCreateRequest(null, "내 할일", null, null), MEMBER_ID);

        Long lectureId = lectureRepository.save(
                Lecture.builder()
                        .memberId(OTHER_MEMBER_ID)
                        .title("다른 사람 강의")
                        .professorName("교수님")
                        .build()
        ).getLectureId();

        Long weekId = weekRepository.save(
                Week.builder()
                        .lectureId(lectureId)
                        .title("1주차")
                        .weekDate(WEEK_DATE)
                        .build()
        ).getWeekId();

        TodoUpdateRequest request = new TodoUpdateRequest(weekId, "수정 시도", null, null);

        assertThatThrownBy(() -> todoService.updateTodo(created.todoId(), request, MEMBER_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TODO_ACCESS_DENIED));
    }
}

