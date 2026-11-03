# 작업 목록: 자바 문자열 쿼리 탐색

## 단계 1: 준비

- [x] T001 specs/017-java-query-navigation/spec.md의 명세·명확화·품질 점검을 완료한다.
- [x] T002 specs/017-java-query-navigation/plan.md와 research.md에 기존 색인·탐색 조사 및 읽기 전용 참조 설계를 기록한다. (FR-001, FR-004)

## 단계 2: 재현

- [x] T003 src/test/kotlin/com/look4counter/JavaQueryNavigationTest.kt에서 기본 선언 이동의 정확 일치 테스트를 작성하고 구현 전 실패를 확인한다. (FR-001)

## 단계 3: 사용자 이야기 1

- [x] T004 queryeditor/src/main/kotlin/com/look4counter/queryeditor/JavaQueryReferenceContributor.kt와 queryeditor/src/main/resources/META-INF/queryeditor.xml에 자바 문자열의 소프트 다중 참조와 등록을 구현한다. (FR-001, FR-002, FR-003, FR-004)
- [x] T005 src/test/kotlin/com/look4counter/JavaQueryNavigationTest.kt에서 중복, 음성 사례, 이스케이프, 텍스트 블록, 변경·삭제, 읽기 액션과 편집기 이동을 검증한다. (FR-002, FR-003, FR-004, FR-005, SC-001, SC-002)

## 단계 4: 통합 검증

- [x] T006 기존 쿼리 색인·탐색·스레드 회귀 테스트와 컴파일을 실행하고 specs/017-java-query-navigation/quickstart.md에 결과를 기록한다. (SC-003)
- [x] T007 speckit-converge로 명세·계획·작업과 코드를 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 순서로 실행한다. 최소 정확 일치 이동을 먼저 구현하고 중복·경계 사례를 순차 검증한다. 테스트는 같은 파일을 수정하므로 순차 작성한다. 기존 색인과 탐색 회귀 테스트는 서로 독립 실행 가능하다.


