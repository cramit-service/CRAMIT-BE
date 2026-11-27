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
import com.cramit.global.exception.BusinessException;
import com.cramit.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTodoService {

    static final int MAX_TODOS_PER_DAY = 3;
    private static final int MAX_CONTENT_LENGTH = 255;
    private static final LocalTime DUE_TIME = LocalTime.of(23, 59);

    private final TodoRepository todoRepository;
    private final ExamRepository examRepository;
    private final LectureRepository lectureRepository;
    private final WeekRepository weekRepository;
    private final GeminiTodoClient geminiTodoClient;
    private final TransactionTemplate transactionTemplate;

    /**
     * 같은 시험의 이전 AI TODO 중 완료하지 않은 것은 지우고 새 계획으로 바꾼다.(완료한 것, 직접 만들거나 수정한 TODO, 같은 강의의 다른 시험 계획은 유지)
     */
    public List<TodoListResponse> generate(Long examId, Long memberId) {
        LocalDate today = LocalDate.now();
        PlanContext context = transactionTemplate.execute(status -> loadContext(examId, memberId, today));

        List<GeminiTodoItem> items = geminiTodoClient.generate(
                AiTodoPrompts.SYSTEM,
                AiTodoPrompts.userInput(context.exam(), today, context.summaryWeeks(), context.remainingTodos()));

        List<Todo> todos = toTodos(items, context, memberId, today);
        if (todos.isEmpty()) {
            throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
        }

        return transactionTemplate.execute(status -> {
            Exam lockedExam = examRepository.findByIdForUpdate(examId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));
            if (!lockedExam.getExamDate().equals(context.exam().getExamDate())
                    || !lockedExam.getLectureId().equals(context.exam().getLectureId())) {
                throw new BusinessException(ErrorCode.TODO_GENERATION_FAILED);
            }

            todoRepository.deleteIncompleteAiTodos(memberId, examId);
            return todoRepository.saveAll(todos).stream()
                    .map(todo -> TodoListResponse.of(todo, context.lecture()))
                    .toList();
        });
    }

    private PlanContext loadContext(Long examId, Long memberId, LocalDate today) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        Lecture lecture = lectureRepository.findById(exam.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.LECTURE_ACCESS_DENIED);
        }

        if (exam.getExamDate().isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_DDAY);
        }

        List<Week> lectureWeeks = weekRepository.findByLectureIdOrderByWeekDateDesc(lecture.getLectureId());

        // 시험일까지 진행된 주차 중 1차 요약본이 있는 주차만 계획의 근거로 쓴다.
        List<Week> summaryWeeks = lectureWeeks.stream()
                .filter(week -> !week.getWeekDate().isAfter(exam.getExamDate()))
                .filter(week -> week.getFirstSummaryMd() != null && !week.getFirstSummaryMd().isBlank())
                .sorted(Comparator.comparing(Week::getWeekDate))
                .toList();

        if (summaryWeeks.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_CONTEXT);
        }

        List<Todo> remainingTodos = todoRepository.findRemainingTodos(memberId, examId);
        return new PlanContext(exam, lecture, summaryWeeks, remainingTodos);
    }

    private List<Todo> toTodos(List<GeminiTodoItem> items, PlanContext context, Long memberId, LocalDate today) {
        Set<Long> allowedWeekIds = context.summaryWeeks().stream()
                .map(Week::getWeekId)
                .collect(Collectors.toSet());
        LocalDate examDate = context.exam().getExamDate();

        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (Todo remaining : context.remainingTodos()) {
            if (remaining.getDueDate() != null) {
                countByDate.merge(remaining.getDueDate().toLocalDate(), 1, Integer::sum);
            }
        }

        List<Todo> todos = new ArrayList<>();

        for (GeminiTodoItem item : items) {

            if (item == null || item.weekId() == null || !allowedWeekIds.contains(item.weekId())
                    || item.content() == null || item.content().isBlank()) {
                continue;
            }

            LocalDate dueDate = parseDate(item.dueDate());
            if (dueDate == null) {
                continue;
            }
            dueDate = dueDate.isBefore(today) ? today : dueDate.isAfter(examDate) ? examDate : dueDate;

            int countOfDay = countByDate.getOrDefault(dueDate, 0);
            if (countOfDay >= MAX_TODOS_PER_DAY) {
                continue;
            }
            countByDate.put(dueDate, countOfDay + 1);

            todos.add(Todo.builder()
                    .memberId(memberId)
                    .weekId(item.weekId())
                    .examId(context.exam().getExamId())
                    .content(truncate(item.content().strip()))
                    .memo(item.memo() != null && !item.memo().isBlank() ? item.memo().strip() : null)
                    .dueDate(dueDate.atTime(DUE_TIME))
                    .todoType(TodoType.AI)
                    .build());
        }
        return todos;
    }

    private LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String truncate(String content) {
        return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
    }

    private record PlanContext(Exam exam, Lecture lecture, List<Week> summaryWeeks, List<Todo> remainingTodos) {}
}
