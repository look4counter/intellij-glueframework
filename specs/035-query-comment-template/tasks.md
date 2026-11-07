# 작업 목록: 쿼리 주석 템플릿

## 1단계: 준비

- [x] T001 명세·계획·계약 및 기존 문서 변경 경로를 확인하고 .gitignore의 Gradle 산출물 제외를 확인한다. 대상: specs/035-query-comment-template/, .gitignore.

## 2단계: 기반

- [x] T002 queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryCommentSettings.kt에 기본 예시와 복사 기반 영속 상태를 구현한다. FR-001.
- [x] T003 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryCommentTemplate.kt에 단일 패스 변수·날짜 치환, 미지원 보존, 줄바꿈 및 빈 템플릿 처리를 구현한다. FR-003, FR-004, FR-005, FR-006.

## 3단계: 사용자 이야기 1 — 설정

독립 검증: 변경 감지·초기화·적용·화면 재생성·직렬화 복원.

- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryEditorConfigurable.kt에 여러 줄 입력과 안내 및 설정 수명주기를 연결한다. FR-001, FR-007.
- [x] T005 [US1] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에 설정 저장·복원 및 적용 전 초안 격리 검증을 추가하고 실행한다. SC-002.

## 4단계: 사용자 이야기 2 — 삽입

독립 검증: 최신 쿼리 문맥의 머리말 삽입·본문 보존·Source 동기화·실행 취소·비활성 상태.

- [x] T006 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 아래로 오른쪽 버튼 및 최신 편집 반영, 지연 선택 보호, 편집 가능성 검사를 연결한다. FR-002, FR-005, FR-006.
- [x] T007 [US2] src/test/kotlin/com/look4counter/QueryCommentTemplateTest.kt와 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에서 치환·경계 조건·배치·Source·실행 취소를 검증한다. FR-002~FR-006, SC-001, SC-003.

## 5단계: 마무리

- [x] T008 관련 편집기 및 설정 회귀 검증 후 specs/035-query-comment-template/quickstart.md에 결과를 기록하고 명세·계획·작업과 구현의 수렴을 확인한다. SC-001~SC-003.

## 의존성과 실행 전략

T001 → T002·T003 → T004 → T005 → T006 → T007 → T008 순으로 실행한다. 기반 이후 설정 이야기와 치환 단위 검증은 파일이 달라 병행 가능하지만 이번에는 순차 수행한다. 먼저 설정 기능을 완성하고 삽입 기능을 연결한다. 두 사용자 이야기를 모두 완료해야 사용자 요청이 충족된다.

## 6단계: 안내 문구 변경

- [x] T009 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryEditorConfigurable.kt의 안내 문구를 FR-007의 한 줄로 교체하고 문자열 대조 및 차이 검사 후 수렴을 확인한다.

T009는 기존 작업 완료 후 수행한다. 변경 파일이 하나이므로 병행 작업은 없다.

## 7단계: 설정 하위 항목 분리

- [x] T010 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/GlueQueryToolConfigurable.kt와 GlueQueryDatabaseConfigurable.kt로 기존 설정 UI와 독립 수명주기를 분리하고 GlueQueryEditorConfigurable.kt는 상위 안내로 바꾼다. FR-008, FR-009.
- [x] T011 [US1] queryeditor/src/main/resources/META-INF/queryeditor.xml에 Tool·Database의 부모 ID와 표시 이름을 등록하고 settings/GlueFrameworkConfigurable.kt의 안내 경로를 갱신한다. FR-008.
- [x] T012 [US1] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에서 실제 설정 트리·페이지 구성·독립 적용·기존 값 유지 및 데이터베이스 회귀를 검증하고 QuerySqlEditorTest.kt의 메타데이터 도구 검증을 Database로 갱신한 뒤 quickstart.md에 기록한다. FR-008, FR-009, SC-002.

T010 → T011 → T012 순서로 진행한다. 두 화면 분리 후 설정 검증을 수행하며 같은 파일 변경이므로 순차 실행한다.

## 8단계: 기본 상위 설정 화면

- [x] T013 [US1] queryeditor/src/main/resources/META-INF/queryeditor.xml 및 modeleditor/src/main/resources/META-INF/modeleditor.xml에 기본 그룹과 groupId를 등록하고 queryeditor/src/main/resources/messages/GlueFrameworkBundle.properties에 상단 설명을 추가한다. 사용하지 않는 settings/GlueFrameworkConfigurable.kt를 제거한다. FR-010.
- [x] T014 [US1] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt에서 실제 그룹·설명·하위 항목 및 기본 화면 위임을 확인하고 기존 설정 회귀와 빌드 검증을 수행한 뒤 quickstart.md에 결과를 기록한다. FR-010, FR-008, FR-009.

T013 → T014 순서로 진행한다. 플랫폼 기본 화면과 링크를 재사용하며 별도 링크 구현 작업은 필요하지 않다.

## 9단계: 기본 템플릿과 변수 설명

- [x] T015 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryCommentSettings.kt의 기본 예시를 QueryId 표기로 바꾸고 GlueQueryToolConfigurable.kt의 안내에 변수별 의미를 표시한다. 문자열 대조와 차이 검사 후 quickstart.md에 기록하고 수렴을 확인한다. FR-007, FR-011.

T015는 기존 작업 완료 후 수행한다. 문자열 변경으로 새 테스트는 작성하지 않는다.

## 10단계: 쿼리 그룹 링크와 ResultType

- [x] T016 [US1] queryeditor/src/main/resources/META-INF/queryeditor.xml 및 messages/GlueFrameworkBundle.properties에서 GlueQueryEditor 기본 그룹과 Tool·Database 연결 및 설명을 등록하고 settings/GlueQueryEditorConfigurable.kt를 제거한다. FR-012.
- [x] T017 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/settings/QueryCommentSettings.kt의 기본 설명 변수를 달러 표기로 바꾸고 GlueQueryToolConfigurable.kt의 안내에 달러 표기 변수 네 개와 설명을 표시한다. FR-007, FR-011, FR-013.
- [x] T018 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryCommentTemplate.kt에 ResultType 치환을 추가하고 QueryDesignEditor.kt에서 최신 ResultType을 전달한다. FR-013.
- [x] T019 [US1] [US2] src/test/kotlin/com/look4counter/DataSourceSettingsTest.kt, QueryCommentTemplateTest.kt, QueryDesignEditorTest.kt에서 중첩 기본 그룹·기본 예시·ResultType 빈값 및 최신 편집 반영을 검증하고 빌드·문서 기록과 수렴을 완료한다. FR-007, FR-011~FR-013.

T016 → T017 → T018 → T019 순서로 실행한다. 기존 저장 서비스와 샤프 문법은 유지한다.
