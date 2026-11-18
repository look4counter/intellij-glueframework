# 작업 목록: Wire 추가 쓰기 스레드 보호

## 1단계: 준비

- [x] T001 specs/038-fix-wire-create-thread/spec.md의 명세·명확화 및 품질 점검을 완료한다.

## 2단계: 기반

- [x] T002 specs/038-fix-wire-create-thread/plan.md와 설계 문서를 작성하고 요구사항·작업 일관성을 분석한다.

## 3단계: 사용자 이야기 1

목표: 잠금 없는 연결 추가와 지연 취소를 독립 검증한다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelActivityCreationTest.kt에 실제 Wire 클릭·놓기 및 취소·원문 보존·실행 취소 회귀를 추가하고 수정 전 실패를 확인한다. FR-001~003, SC-001~002.
- [x] T004 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 createWire에 안전 문맥 예약·생명주기 가드·고정 문서 시점을 적용한다. FR-001~003.

## 4단계: 검증 및 마무리

- [x] T005 specs/038-fix-wire-create-thread/quickstart.md에 생성·연결·Service 회귀 결과를 기록하고 수렴 점검한다. SC-001~002.

## 의존성 및 구현 전략

T001 → T002 → T003 → T004 → T005 순서다. 사용자 이야기 1이 최소 완성 범위다. 같은 테스트 파일과 수정 전·후 검증에 의존하므로 병렬 실행 작업은 없다.



## 5단계: 사용자 이야기 2

- [x] T006 [US2] src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에 잠금 없는 세 provider 호출, 지연 중 선택·문서·모드·클립보드·읽기 전용·폐기 변경과 실행 취소 회귀를 추가하고 수정 전 오류를 확인한다. FR-004~006, SC-003.
- [x] T007 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 삭제·복사·붙여넣기를 안전 문맥으로 예약하고 최초 문서·선택 및 현재 상태를 검증한다. FR-004~006.

## 6단계: 후속 검증

- [x] T008 specs/038-fix-wire-create-thread/quickstart.md에 클립보드·Wire 생성·열기 회귀 및 수렴 결과를 기록한다. SC-003.

후속 의존성은 T006 → T007 → T008이며 기존 작업을 유지한다. 동일 소스와 수정 전 재현에 의존하므로 병렬 작업은 없다. 사용자 이야기 2를 독립 검증한다.


