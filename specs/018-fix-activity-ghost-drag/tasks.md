# 작업 목록: Activity 미리보기 드래그

## 1단계: 준비

- [x] T001 specs/018-fix-activity-ghost-drag/spec.md 및 설계 문서를 검토하고 요구사항 연결을 확인한다.

## 2단계: 기반

- [x] T002 modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 기존 클립 및 입력 경로를 확인한다.

## 3단계: 사용자 이야기 1 — 미리보기 이동

독립 검증: 인접 마우스 이동은 부분 영역만 갱신하고 전체 렌더링과 동일한 결과를 낸다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 미리보기 부분 갱신과 잔상 회귀 검증을 추가한다.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 이전·현재 범위 부분 갱신 및 드래그 추적을 구현한다. FR-001, FR-002.

## 4단계: 사용자 이야기 2 — 놓기로 추가

독립 검증: 거리와 관계없이 정확한 위치에 한 번만 제출하고 외부·미니맵·취소는 무시한다.

- [x] T005 [US2] src/test/kotlin/com/look4counter/ModelEditorCreationTraceTest.kt에 드래그 추가 및 보호 조건을 검증한다.
- [x] T006 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 놓기 거리 제한을 제거하고 유효 영역과 중복 보호를 적용한다. FR-003, FR-004.

## 5단계: 통합 검증

- [x] T007 specs/018-fix-activity-ghost-drag/quickstart.md의 자동 검증을 실행하고 결과를 기록한다.
- [x] T008 build/distributions/ 플러그인 패키지를 생성하고 명세 대비 수렴을 확인한다.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 순서로 진행한다. 사용자 이야기 1을 최소 개선 단위로 검증한 뒤 이야기 2를 통합한다. 테스트 파일이 달라 T003과 T005는 독립 작성 가능하지만 이번 작업은 순차 진행한다.
