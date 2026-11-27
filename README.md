<p align="center">
  <img width="220" height="220" alt="크래밋파비콘" src="https://github.com/user-attachments/assets/bb978ee9-d59c-41c8-957d-ca645d5e525f" />


</p>

<div>

## ⚡️ CRAMIT

> **"CRAM IT: Cram more in less time." <br> 더 적은 시간에, 더 많은 것을 머릿속에 채우다.**
 
</div>

<br>

## 📌 Cramit은 이런 서비스예요

> 대학생들은 강의자료, 강의 녹음, 필기, 개인 메모 등을 여러 플랫폼에 나누어 관리하며 시험 기간마다 다시 정리해야 하는 불편함을 겪는다. Cramit은 강의자료와 강의 녹음을 AI로 분석하여 자동 요약을 제공하고, 사용자가 학습하며 추가한 메모와 학습 포인트까지 함께 활용하는 통합 학습 환경을 제공하기 위해 개발되었다.

### 🔥 강의자료와 강의 녹음을 하나로

> PDF와 STT를 페이지 단위로 자동 매핑해, 원하는 페이지만 골라 다시 들을 수 있습니다

강의자료(PDF)를 업로드하고 강의를 녹음하면, 녹음 종료 즉시 STT가 자동 생성되고 강의자료 페이지와 STT 구간이 자동으로 매핑된다. '해당 페이지 수업 듣기' 버튼 하나로 필요한 구간만 빠르게 재청취할 수 있다.

### 🔥 AI 요약본 생성

> 강의자료(PDF)와 STT를 함께 분석하여 Markdown 형태의 요약본을 생성합니다

'요약본 생성' 버튼 클릭 시 AI가 강의자료와 STT를 종합 분석하여 페이지별 핵심 내용을 정리한 요약본을 생성한다. 교수가 강조한 내용은 별도로 표시된다.

### 🔥 메모와 학습 포인트

> 학습 중 남긴 메모와 학습 포인트가 챗봇·학습 계획에도 반영됩니다

요약본에서 원하는 텍스트를 드래그해 메모를 남기거나 학습 포인트를 생성할 수 있다. '학습 내용 적용' 버튼을 누르면 이 기록들이 프로젝트에 반영되어, 이후 AI 챗봇 답변과 학습 계획 생성에도 함께 활용된다.

### 🔥 AI가 도와주는 시험 대비 학습 플랜

> 시험 일정을 등록하면 AI가 날짜별 학습 계획(TODO)을 자동으로 만들어줍니다

시험 일정과 D-Day를 등록하면, 프로젝트의 학습 진도와 내용을 분석해 AI가 날짜별 학습 계획을 자동 생성한다. TODO는 AI 생성 외에 직접 추가·수정도 가능하다.

<br>

## 👤 CRAMIT의 구성원을 소개합니다!

| 역할 | 담당 |
|:---|:---|
| 🖥️ Frontend | 박태현 · 강준우 |
| ⌨️ Backend | 배서윤 · 엄정인 |
| 🎨 Design | 배서윤 |

<br>

## 기술 스택

| 구분 | 기술 | 용도 |
| --- | --- | --- |
| Language | ![Java](https://img.shields.io/badge/Java%2017-000000?style=for-the-badge&logo=openjdk&logoColor=white) | Backend Application |
| Framework | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) | REST API Server |
| Security | ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![JWT](https://img.shields.io/badge/JJWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) | Authentication & Authorization |
| ORM | ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white) | ORM |
| Query Builder | ![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?style=for-the-badge) | Dynamic Query |
| Database | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) | AWS RDS · pgvector(벡터 검색) |
| Cache | ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white) | 리프레시 토큰 저장 |
| Migration | ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white) | Schema Version Management |
| AI | ![Gemini](https://img.shields.io/badge/Gemini%20API-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white) | AI Features (챗봇·요약·Todo·임베딩) |
| STT | ![Azure Speech](https://img.shields.io/badge/Azure%20Speech-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white) | Speech-to-Text |
| Storage | ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white) ![Cloudflare R2](https://img.shields.io/badge/Cloudflare%20R2-F38020?style=for-the-badge&logo=cloudflare&logoColor=white) | File Storage |
| Async | ![Spring Async](https://img.shields.io/badge/Spring%20Async-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Resilience4j](https://img.shields.io/badge/Resilience4j-FF6600?style=for-the-badge) | Async Processing & Fault Tolerance |
| API Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black) | API Documentation |
| Testing | ![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white) ![Mockito](https://img.shields.io/badge/Mockito-78A641?style=for-the-badge) | Unit & Integration Test |
| Build & Deploy | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white) | Build & Deployment |

<br>

## 아키텍처

```mermaid
flowchart TB
    subgraph FE["Frontend (별도 레포, React/Next.js)"]
        UI["강의 업로드 · 학습 화면 · 챗봇 UI"]
    end

    FE -->|HTTPS REST| API

    subgraph BE["CRAMIT-BE (Spring Boot 3.x)"]
        direction TB
        API["Controller Layer<br/>REST API · Swagger<br/>Spring Security(JWT) 인증·인가"]

        subgraph DOMAIN["Domain Layer"]
            direction LR
            D1["Lecture / Week<br/>(강의·주차·PPT·음성)"]
            D2["Chat / Summary / Todo<br/>(AI 챗봇·요약·학습계획)"]
            D3["Member / Subscription<br/>(회원·구독·약관)"]
        end

        API --> DOMAIN

        ASYNC["비동기 처리<br/>STT 완료 이벤트 → 요약 생성 트리거<br/>pgvector 임베딩 백필 스케줄러"]
        DOMAIN -.이벤트.-> ASYNC

        RESILIENCE["Resilience4j<br/>Circuit Breaker · Retry<br/>(외부 API 장애 격리)"]
        DOMAIN --> RESILIENCE
    end

    RESILIENCE --> KAKAO["Kakao / Google OAuth2"]
    RESILIENCE --> GEMINI["Google Gemini API<br/>(챗봇·요약·Todo·임베딩)"]
    RESILIENCE --> AZURE_STT["Azure Speech<br/>(STT)"]

    DOMAIN --> DB[("PostgreSQL + pgvector<br/>(Flyway 마이그레이션)")]
    DOMAIN --> REDIS[("Redis<br/>(세션/캐시)")]
    DOMAIN --> S3[("AWS S3 / Cloudflare R2<br/>(PPT·음성 파일)")]
```

<br>


## 핵심 처리 흐름

```mermaid
sequenceDiagram
    actor U as 사용자 (FE)
    participant API as CRAMIT-BE
    participant S3 as S3/R2
    participant DB as PostgreSQL(+pgvector)
    participant STT as Azure Speech
    participant AI as Gemini API

    Note over U,API: 1. 강의자료 업로드
    U->>API: POST /api/files/presigned-url (PPT/음성)
    API->>S3: presigned PUT URL 발급
    API-->>U: uploadUrl, fileUrl
    U->>S3: 파일 직접 PUT 업로드

    U->>API: POST /lectures/{id}/weeks (ppt·audio fileUrl)
    API->>S3: PPT 매직바이트·용량 검증 후<br/>불변 키로 서버사이드 복사
    API->>DB: Week/LecturePpt/LectureAudio 저장<br/>(sttStatus=PENDING)
    API-->>U: 201 Created

    Note over API,STT: 2. STT 비동기 처리 (커밋 후 트리거)
    API->>STT: 음성 파일 전사 요청
    STT-->>API: 전사 텍스트
    API->>DB: Script/ScriptDetail 저장<br/>(sttStatus=COMPLETED)

    Note over API,AI: 3. STT 완료 후 자동 연계
    API->>AI: 1차 요약본 생성 요청
    AI-->>API: 요약 결과
    API->>DB: Summary 저장

    Note over API,DB: 4. 주기적 임베딩 백필 (10분 간격)
    API->>AI: 페이지 텍스트 임베딩 요청
    AI-->>API: 임베딩 벡터(768차원)
    API->>DB: script_embedding 저장

    Note over U,AI: 5. AI 챗봇 질의 (RAG)
    U->>API: POST /chat (질문)
    API->>DB: pgvector 유사도 검색 + 키워드 검색
    DB-->>API: 관련 페이지 컨텍스트
    API->>AI: 컨텍스트 + 질문으로 답변 생성 요청
    AI-->>API: 답변 + 근거 페이지
    API-->>U: 답변 반환
```

<br>

## ⚙️ 실행 환경

- **JDK 버전**: `17`
- **빌드 도구**: Gradle
- **여는 방법**
  ```bash
  git clone https://github.com/cramit-service/cramit-be.git
  cd cramit-be
  ```

  `src/main/resources/application-local.yml`에 아래 정보를 입력합니다.

  - Database
  - JWT Secret
  - Kakao OAuth
  - Google OAuth
  - Gemini API Key
  - AWS Credentials

  > `application-local.yml`은 `.gitignore`에 포함되어 있습니다.

- **실행**
  
  ```bash
  ./gradlew bootRun
  ```

<br>

## 🔗 Git Convention

### 1️⃣ Git Flow

```
  develop ← 작업 브랜치
```

- `main branch` : 배포 브랜치
- `develop branch` : 개발 브랜치, feature 브랜치가 merge됨
- `feature / chore / refactor branch` : 개발 브랜치

  <br/>

### 2️⃣ Flow

- 이슈 생성
- 이슈 번호에 맞게 `develop` 브랜치에서 새로운 브랜치 생성
- 작업 완료 후 커밋 컨벤션에 맞게 커밋
- PR 생성
- 코드 리뷰 후 `develop` 브랜치로 병합
  - 최소 1명 승인 후 `develop` 브랜치로 머지

  <br/>

### 3️⃣ Branch Naming Convention

- **구조**: `브랜치종류/#이슈번호-기능설명`
- **구분자**: 하이픈(`-`) 사용

| Prefix     | 설명               |
| ---------- | ------------------ |
| `init`     | 초기 프로젝트 세팅 |
| `feat`     | 새로운 기능 추가   |
| `fix`      | 버그 수정          |
| `docs`     | 문서 변경          |
| `style`    | UI 수정            |
| `refactor` | 코드 리팩토링      |
| `test`     | 테스트 코드 관련   |
| `chore`    | 설정 파일 변경     |
| `hotfix`   | 긴급한 버그 수정   |
| `deploy`   | 배포 관련 작업     |

  <br/>

### 4️⃣ Commit Mess
  - `init: 프로젝트 초기 세팅 (#1)`
  - `feat: 로그인 API 구현 (#2)`

| Prefix     | 설명               |
| ---------- | ------------------ |
| `init`     | 초기 프로젝트 세팅 |
| `feat`     | 새로운 기능 추가   |
| `fix`      | 버그 수정          |
| `docs`     | 문서 변경          |
| `style`    | UI 수정            |
| `refactor` | 코드 리팩토링      |
| `test`     | 테스트 코드 관련   |
| `chore`    | 설정 파일 변경     |
| `hotfix`   | 긴급한 버그 수정   |
| `deploy`   | 배포 관련 작업     |

  <br/>

### 5️⃣ API Response Status Code Convention

| 상황 | HTTP Status | 설명 |
|---|---|---|
| 생성 (POST) | 201 Created | 새 리소스 생성 성공 |
| 조회 (GET) | 200 OK | 조회 성공 |
| 수정 (PATCH) | 200 OK | 수정 성공, 수정된 데이터 반환 |
| 삭제 (DELETE) | 204 No Content | 삭제 성공, 반환 데이터 없음 |
<div>

<br>

## 📂 프로젝트 구조

```
📦 cramit-be
├─ 📁 .github
│  ├─ 📁 ISSUE_TEMPLATE
│  │  ├─ 📄 feat.md              # [FEAT] 이슈 템플릿
│  │  ├─ 📄 chore.md             # [CHORE] 이슈 템플릿
│  │  └─ 📄 bug.md               # [BUG] 이슈 템플릿
│  ├─ 📁 workflows
│  │  ├─ 📄 ci.yml               # 테스트/보안 스캔 CI
│  │  └─ 📄 cd.yml               # develop 머지 시 운영 서버 자동 배포(CD)
│  └─ 📄 pull_request_template.md
├─ 📄 .coderabbit.yaml           # CodeRabbit 리뷰 언어/설정
├─ 📄 .gitattributes             # 줄바꿈(EOL) 통일 설정
├─ 📄 .gitignore
├─ 📄 .dockerignore
├─ 📄 .env.example               # 로컬 docker-compose용 환경변수 템플릿
├─ 📄 .env.prod.example     템플릿
├─ 📄 Dockerfile
├─ 📄 docker-compose.yml   vector)·Redis 컨테이너
├─ 📄 docker-compose.prod.yml    # 운영 배포용(app·nginx·certbot)
├─ 📁 docker
│  ├─ 📁 nginx                   # 운영 리버스 프록시 설정
│  └─ 📁 certbot
├─ 📁 gradle/wrapper
├─ 📄 gradlew / gradlew.bat
├─ 📁 src
│  ├─ 📁 main
│  │  ├─ 📁 java/com/cramit
│  │  │  ├─ 📄 CramitApplication.java
│  │  │  ├─ 📁 global
│  │  │  │  ├─ 📁 config    Querydsl, Resilience 등설정
│  │  │  │  ├─ 📁 exception        # 전역 예외처리, 에러코드 enum
│  │  │  │  ├─ 📁 security
│  │  │  │  ├─ 📁 common           # 공통 응답 포맷, 유틸
│  │  │  │  ├─ 📁 async            # 비동기 처리 공통 설정
│  │  │  │  ├─ 📁 external 통 모듈(타임아웃 분류 등)
│  │  │  │  ├─ 📁 storage          # S3/R2 파일 저장소 설정
│  │  │  │  └─ 📁 logging          # 요청 추적(traceId) 로깅
│  │  │  └─ 📁 domain
│  │  │     ├─ 📁 member           # 회원가입 / 로그인 / 프로필
│  │  │     ├─ 📁 lecture          # 강의 CRUD
│  │  │     ├─ 📁 week     음성 자료
│  │  │     ├─ 📁 file             # 파일 업로드 URL 발급·검증(S3)
│  │  │     ├─ 📁 stt      hisper)
│  │  │     ├─ 📁 todo             # TODO (수동 생성 + AI 생성)
│  │  │     ├─ 📁 chat
│  │  │     ├─ 📁 summary          # AI 요약본 생성·조회
│  │  │     ├─ 📁 learning 습 메모
│  │  │     ├─ 📁 share     게시판
│  │  │     ├─ 📁 exam
│  │  │     ├─ 📁 subscript
│  │  │     ├─ 📁 term
│  │  │     └─ 📁 ai       임베딩 공통 모듈
│  │  └─ 📁 resources
│  │     ├─ 📄 application.
│  │     ├─ 📄 application-re, 개인 환경변수)
│  │     ├─ 📄 application-
│  │     ├─ 📄 application-
│  │     ├─ 📄 application-
│  │     └─ 📁 db/migration 이그레이션 스크립트
(V1~V23)
│  └─ 📁 test
├─ 📄 build.gradle
└─ 📄 settings.gradle
```

</div>
