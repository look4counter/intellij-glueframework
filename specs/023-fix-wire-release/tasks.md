# 실행 작업: Wire 놓음 입력 보완

## 1단계: 준비

- [x] T001 specs/023-fix-wire-release/spec.md와 설계 산출물의 요구사항·범위·경계 조건을 검토한다.

## 2단계: 기반 검증

- [x] T002 [US1] src/test/kotlin/com/look4counter/ModelDiagramWireCreationTest.kt에 놓음 기반 선택과 무효 입력·중복·좌표 회귀 검증을 추가한다. FR-001~004, SC-001~003.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 공통 Wire 선택 함수, 누름 상태 수명과 놓음 확정·후속 클릭 제외를 구현한다. FR-001~004.

## 4단계: 검증과 마무리

- [x] T004 새 테스트와 기존 관련 회귀 검증을 실행하고 specs/023-fix-wire-release/quickstart.md에 결과와 빌드 제약을 기록한다.
- [x] T005 specs/023-fix-wire-release/spec.md·plan.md·tasks.md와 코드를 대조하여 수렴 여부를 확인한다.

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 순서로 진행한다. 단일 캔버스 입력 경로라 병렬 작업은 없다. 사용자 이야기 1이 최소 범위이며, 독립 검증 기준은 클릭 없는 시작·끝 선택 1회와 후속 클릭의 중복 부재이다.
