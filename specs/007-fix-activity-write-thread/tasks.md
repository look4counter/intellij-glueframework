# 작업 목록: Activity 편집 스레드 오류

입력: [명세](spec.md), [계획](plan.md), [조사](research.md)

## 단계 1: 준비

- [x] T001 specs/007-fix-activity-write-thread/research.md의 SDK 호출 체인과 원인을 확인한다.

## 단계 2: 기반 확인

- [x] T002 specs/007-fix-activity-write-thread/checklists/requirements.md 및 명세·계획·작업의 일관성을 확인한다.

## 단계 3: 사용자 이야기 1

독립 검증: 잠금 없는 EDT에서 캔버스 더블클릭과 Diagram 속성 요청이 오류 없이 처리되는지 확인한다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 잠금 없는 편집 진입·확인·취소·실행 취소 회귀 검증을 추가하고 수정 전 실패를 확인한다. FR-001, FR-002, FR-003.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 편집 요청의 안전 문맥 예약과 refresh 잠금 가드를 적용한다. FR-001, FR-003.
- [x] T005 [US1] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt의 전체 속성 편집 검증을 실행한다. FR-002, SC-001, SC-002.

## 단계 4: 마무리

- [x] T006 specs/007-fix-activity-write-thread/quickstart.md에 검증 결과와 한계를 기록하고 배포 ZIP을 생성한다.

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 → T006 순서로 실행한다. 사용자 이야기 1이 전체 최소 제공 범위다. 같은 테스트 파일과 구현에 의존하므로 병렬 구현은 하지 않는다. 독립적인 SDK 조사만 계획 단계에서 병렬 수행했다.
