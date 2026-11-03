# 작업 목록: 비정상 Wire 제외 저장

## 단계 1: 준비

- [x] T001 specs/008-skip-invalid-export-wires/research.md에서 저장 경로와 비정상 유형을 확인한다.

## 단계 2: 기반 확인

- [x] T002 specs/008-skip-invalid-export-wires/의 명세·계획·작업과 품질 점검을 대조한다.

## 단계 3: 사용자 이야기 1

독립 검증은 비정상 연결이 섞인 모델의 Service 생성과 원문 보존이다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt에 비정상 Wire·시작점·자동 저장 회귀 검증을 추가한다. FR-001~FR-004.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlExporter.kt에서 유효 Wire만 초기 연결과 전이로 출력한다. FR-001~FR-003.
- [x] T005 [US1] src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt의 정상 출력과 연결 외 오류 보호를 포함한 전체 검증을 실행한다. FR-004, SC-001, SC-002.

## 단계 4: 마무리

- [x] T006 specs/008-skip-invalid-export-wires/quickstart.md에 결과와 검증 한계를 기록하고 구현과 명세의 차이를 점검한다.

## 의존성과 전략

T001 → T002 → T003 → T004 → T005 → T006 순서로 진행한다. 사용자 이야기 1이 최소 제공 범위다. 파일과 검증 의존성 때문에 병렬 구현은 하지 않는다. 계획 단계의 독립 조사만 연구 에이전트로 수행했다.
