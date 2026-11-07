# 실행 작업: 저장 생명주기 안정화

## 1단계: 준비

- [x] T001 specs/024-stabilize-save-lifecycle/spec.md와 설계·명확화·품질 점검을 검토한다.

## 2단계: 기반

- [x] T002 specs/024-stabilize-save-lifecycle/plan.md와 tasks.md의 요구사항 대응을 speckit-analyze로 확인한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt에 미저장·동일 내용 미저장·읽기 전용 보호와 정상 재시도 검증을 추가하고 수정 전 실패를 확인한다. FR-001~003, SC-001.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceSaveListener.kt에 출력 문서 상태 보호 조건을 추가한다. FR-001~003.

## 4단계: 사용자 이야기 2

- [x] T005 [US2] src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt에 탭 재선택·Commit·저장·재열기와 Source 충돌·Rollback 회귀 검증을 추가한다. FR-004~005, SC-002.

## 5단계: 검증과 마무리

- [x] T006 관련 자동 테스트와 Gradle 빌드를 시도하고 specs/024-stabilize-save-lifecycle/quickstart.md에 명령·결과·수동 검증 한계를 기록한다. SC-001~003.
- [x] T007 specs/024-stabilize-save-lifecycle/spec.md·plan.md·tasks.md와 구현을 speckit-converge로 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T005→T006→T007 순서로 진행한다. US1을 최소 안전성 개선으로 먼저 완료하고 US2를 검증한다. US1과 US2의 테스트 파일은 독립적이지만 이번 작업은 순차 실행한다. 실제 IDE 조작은 수동 검증 범위로 별도 표시한다.
