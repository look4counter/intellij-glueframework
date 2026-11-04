# 작업 목록: Orthogonal Wire 자석

## 1단계: 준비

- [x] T001 specs/021-orthogonal-wire-magnet/spec.md 및 설계 문서의 요구사항·헌장 준수를 검토한다.

## 2단계: 사용자 이야기 1 — 설정과 버튼

독립 검증: Snap 옆 버튼 순서와 기본값·독립성·설정 복원·열린 편집기 공유를 확인한다.

- [x] T002 [US1] src/test/kotlin/com/look4counter/OrthogonalWireSnapTest.kt에 설정과 도구 모음 회귀 테스트를 작성한다.
- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelSnapSettings.kt에 독립 상태와 알림을 추가한다. FR-002.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt와 ModelDesignEditorProvider.kt에 Orthogonal 토글과 상태 전파를 연결한다. FR-001, FR-002.

## 3단계: 사용자 이야기 2 — 자석 드래그

독립 검증: 6px 진입·10px 이탈과 줌·Alt 및 실제 드래그의 미리보기·확정 좌표를 확인한다.

- [x] T005 [US2] src/test/kotlin/com/look4counter/OrthogonalWireSnapTest.kt에 축별 임계·줌·드래그·취소·저장·실행 취소 검증을 작성한다.
- [x] T006 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/OrthogonalWireSnap.kt에 원본 인접 좌표 기준의 자석 상태와 보정을 구현한다. FR-003, FR-004.
- [x] T007 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 미리보기·놓기·Alt·토글 변경·제스처 초기화를 연결한다. FR-003, FR-004, FR-005.

## 4단계: 통합 검증

- [x] T008 specs/021-orthogonal-wire-magnet/quickstart.md의 회귀 검증과 build/distributions/ 설치 패키지 생성을 수행하고 결과를 기록한다.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 순서로 진행한다. 버튼과 설정을 먼저 연결한 후 드래그 동작을 통합한다. 같은 회귀 테스트 파일과 캔버스를 공유하므로 순차 진행한다. 설정 및 기하 검증은 독립 실행 가능하다.
