# 작업 목록: Query 목록 전환 오류

## 단계 1: 준비

- [x] T001 specs/009-fix-query-selection-thread/research.md의 목록 전환 호출 경로를 확인한다.

## 단계 2: 기반 확인

- [x] T002 specs/009-fix-query-selection-thread/의 명세·계획·작업과 품질 점검을 대조한다.

## 단계 3: 사용자 이야기 1

독립 검증은 잠금 없는 실제 목록 이벤트의 전환과 편집 대상 보존이다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 잠금 없는 전환·대기 편집·연속 선택·폐기·쓰기 불가 문맥 검증을 추가하고 수정 전 실패를 확인한다. FR-001~FR-003.
- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt의 선택 전환 전체를 안전 문맥에서 실행하고 대상을 유지한다. FR-001~FR-003.
- [x] T005 [US1] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt와 QuerySqlEditorTest.kt 전체를 실행한다. FR-004, SC-001, SC-002.

## 단계 4: 마무리

- [x] T006 specs/009-fix-query-selection-thread/quickstart.md에 결과·한계를 기록하고 두 변경을 포함한 설치용 ZIP을 생성한다.

## 의존성과 전략

T001 → T002 → T003 → T004 → T005 → T006 순서로 수행한다. 사용자 이야기 1이 최소 제공 범위다. 같은 파일과 검증 의존성 때문에 병렬 구현은 하지 않는다. 독립 원인 조사는 계획 단계에서 연구 에이전트가 수행했다.
