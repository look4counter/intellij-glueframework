# 작업 목록: 쿼리 탭 위치 동기화

## 1단계: 준비

- [x] T001 specs/019-sync-query-editor-tabs/spec.md와 설계 문서를 검토해 요구사항 및 헌장 검사를 완료한다.

## 2단계: 기반

- [x] T002 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryTabNavigation.kt에 문서별 공유 키와 안전 실행 처리를 추가한다. FR-004, FR-005.

## 3단계: 사용자 이야기 1 — Source 커서 이동

독립 검증: 선택한 두 번째 query 및 삭제된 선택의 첫 query 대체 위치를 확인한다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/QueryTabNavigationTest.kt에 Source 커서·수정 직후 이동·읽기 전용 검증을 추가한다.
- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 선택 포인터 게시를 추가한다. FR-001.
- [x] T005 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySourceEditorProvider.kt에 로딩 후 커서·스크롤 이동 및 첫 query 대체를 구현한다. FR-001, FR-003.

## 4단계: 사용자 이야기 2 — Design 쿼리 선택

독립 검증: 속성·본문·닫는 태그·공백의 커서에 대응하는 목록 선택을 확인한다.

- [x] T006 [US2] src/test/kotlin/com/look4counter/QueryTabNavigationTest.kt에 범위 경계·첫 query 대체·중복 및 빈 ID·폐기·오류 XML 검증을 추가한다.
- [x] T007 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QuerySourceEditorProvider.kt에 비활성화 커서 위치 게시를 구현한다. FR-002.
- [x] T008 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에 최신 커서의 query 선택과 목록 스크롤 및 외부 탐색 우선순위를 적용한다. FR-002, FR-003, FR-004, FR-005.

## 5단계: 통합 검증

- [x] T009 specs/019-sync-query-editor-tabs/quickstart.md의 자동 검증 및 build/distributions/ 설치 패키지 생성을 수행하고 결과를 기록한다.

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 순서로 진행한다. 이야기 1의 Source 이동을 최소 단위로 검증하고 이야기 2를 통합한다. 두 이야기는 동일 파일과 테스트 기반을 공유하므로 순차 진행한다. 검증 대상 클래스별 실행은 독립적이지만 이번에는 하나의 Gradle 실행으로 묶는다.
