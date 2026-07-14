<p align="center">
  <img width="220" height="220" alt="크래밋파비콘" src="https://github.com/user-attachments/assets/bb978ee9-d59c-41c8-957d-ca645d5e525f" />


</p>

<div>

## ⚡️ CRAMIT

> **AI 기반 스마트 학습 어시스턴트**

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

## ⚙️ 기술 스택

| 구분 | 기술 | 용도 |
| --- | --- | --- |
| Language | ![Java](https://img.shields.io/badge/Java%2017-000000?style=for-the-badge&logo=openjdk&logoColor=white) | Backend Application |
| Framework | ![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) | REST API Server |
| Security | ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![JWT](https://img.shields.io/badge/JJWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white) | Authentication & Authorization |
| ORM | ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white) | ORM |
| Query Builder | ![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?style=for-the-badge) | Dynamic Query |
| Database | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white) | AWS RDS |
| Migration | ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white) | Schema Version Management |
| AI | ![Gemini](https://img.shields.io/badge/Gemini%20API-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white) | AI Features |
| STT | ![Google Cloud](https://img.shields.io/badge/Speech--to--Text-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white) | Speech-to-Text |
| Storage | ![AWS S3](https://img.shields.io/badge/AWS%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white) | File Storage |
| Async | ![Spring Async](https://img.shields.io/badge/Spring%20Async-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Resilience4j](https://img.shields.io/badge/Resilience4j-FF6600?style=for-the-badge) | Async Processing & Fault Tolerance |
| API Docs | ![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black) | API Documentation |
| Testing | ![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white) ![Mockito](https://img.shields.io/badge/Mockito-78A641?style=for-the-badge) | Unit & Integration Test |
| Build & Deploy | ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) ![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![AWS EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white) | Build & Deployment |

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

<div>

## 📂 프로젝트 구조

```
📦 cramit-be
├─ 📁 .github
│  ├─ 📁 ISSUE_TEMPLATE
│  │  ├─ 📄 feat.md              # [FEAT] 이슈 템플릿
│  │  ├─ 📄 chore.md             # [CHORE] 이슈 템플릿
│  │  └─ 📄 bug.md               # [BUG] 이슈 템플릿
│  └─ 📄 pull_request_template.md
├─ 📄 .coderabbit.yaml           # CodeRabbit 리뷰 언어/설정
├─ 📄 .gitattributes             # 줄바꿈(EOL) 통일 설정
├─ 📄 .gitignore
├─ 📁 gradle/wrapper
├─ 📁 src
│  ├─ 📁 main
│  │  ├─ 📁 java/com/cramit
│  │  │  ├─ 📄 CramitApplication.java
│  │  │  ├─ 📁 global
│  │  │  │  ├─ 📁 config           # Security, Swagger, Async 등 설정
│  │  │  │  ├─ 📁 exception        # 전역 예외처리, 에러코드 enum
│  │  │  │  ├─ 📁 security         # JWT, OAuth2 관련
│  │  │  │  └─ 📁 common           # 공통 응답 포맷, 유틸
│  │  │  └─ 📁 domain
│  │  │     ├─ 📁 user             # 회원가입 / 로그인 / 프로필
│  │  │     ├─ 📁 project          # 프로젝트 CRUD
│  │  │     ├─ 📁 lecture          # PDF 업로드 / 녹음 / STT / 페이지 매핑
│  │  │     ├─ 📁 summary          # AI 요약본 생성·조회
│  │  │     ├─ 📁 learning         # 학습 포인트 / 메모 / 학습 내용 적용
│  │  │     ├─ 📁 share            # 프로젝트 공유
│  │  │     ├─ 📁 chat             # AI 챗봇
│  │  │     ├─ 📁 todo             # TODO (AI 생성 + 수동)
│  │  │     ├─ 📁 exam             # 시험 일정
│  │  │     └─ 📁 ai               # Gemini 프롬프트 공통 모듈
│  │  └─ 📁 resources
│  │     ├─ 📄 application.yml
│  │     ├─ 📄 application-local.yml   # (gitignore, 개인 환경변수)
│  │     └─ 📁 db/migration            # Flyway 마이그레이션 스크립트
│  └─ 📁 test
├─ 📄 build.gradle
├─ 📄 settings.gradle
└─ 📄 Dockerfile
```

</div>
