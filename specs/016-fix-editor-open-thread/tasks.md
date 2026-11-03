# 작업 목록: 편집기 열기 스레드 오류

## 단계 1: 준비

- [x] T001 specs/016-fix-editor-open-thread/spec.md의 요구사항 명세와 명확화 및 품질 점검을 완료한다.
- [x] T002 specs/016-fix-editor-open-thread/plan.md와 research.md에 생성·탐색·선택·갱신 경로의 조사 및 설계를 기록한다. (FR-001)

## 단계 2: 기반 검증

- [x] T003 src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt에 실제 쓰기 의도 잠금 해제 도우미와 재현 테스트를 추가하고 실패를 확인한다. (FR-001, SC-001)

## 단계 3: 사용자 이야기 1

- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에서 ID·검색 위치 탐색의 안전한 예약, 마지막 요청 보존, 폐기·무효 파일 취소를 구현한다. 생성 시 결함이 확인되면 QueryResultTypeEditor.kt도 수정한다. (FR-002, FR-003)
- [x] T005 [US1] src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt에서 최신 원문·연속 이동·폐기·원문 보존·안전하지 않은 모달을 검증한다. (FR-002, FR-003, FR-005, SC-002)

## 단계 4: 사용자 이야기 2

- [x] T006 [US2] src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt에서 모델 및 서비스 직접 편집 모드의 잠금 없는 생성·선택 통지·초기 표시를 검증하고 ModelDesignEditorProvider.kt의 결함이 확인되면 수정한다. (FR-004, FR-005)

## 단계 5: 통합 검증

- [x] T007 관련 쿼리·모델 테스트와 컴파일을 실행하고 specs/016-fix-editor-open-thread/quickstart.md에 결과와 한계를 기록한다. (SC-001, SC-002, SC-003)
- [x] T008 speckit-converge로 명세·계획·작업 대비 코드를 점검하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 순서로 실행한다. 두 이야기의 테스트는 같은 파일을 사용하므로 순차 편집한다. 개별 테스트 명령은 파일 변경이 없을 때 독립 실행 가능하다. 사용자 이야기 1을 먼저 검증한 다음 모델 회귀와 통합 검증으로 확장한다.

- [x] T009 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditorProvider.kt와 QuerySourceEditorProvider.kt, modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelSourceEditorProvider.kt의 생성 문맥을 보호하고 EditorOpenThreadTest.kt에서 검증한다. (FR-001)
- [x] T010 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryNavigation.kt의 두 open 진입점을 안전한 문맥으로 예약하고 EditorOpenThreadTest.kt에서 검증한다. (FR-001, FR-002)


## 단계 6: 수렴 보완

- [x] T011 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceFiles.kt의 모델 참조 파일 열기를 안전한 문맥으로 예약하고 src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt에서 검증한다. FR-001의 파일 열기 경로 보호가 부분 구현되어 남은 작업이다.
- [x] T012 [US1] src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt에서 지연 탐색 전 파일 무효화와 읽기 전용 탐색을 검증한다. FR-003과 FR-005의 경계 검증을 보완한다.


## 단계 7: 사용자 이야기 3 — 파일 열기 호환성

- [x] T013 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditorProvider.kt와 QuerySourceEditorProvider.kt의 생성 래퍼를 동일 잠금의 Application 호출로 교체한다. (FR-006)
- [x] T014 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt와 ModelSourceEditorProvider.kt의 생성 래퍼를 동일 잠금의 Application 호출로 교체한다. (FR-006)
- [x] T015 [US3] src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt와 EditorMultiViewTest.kt를 실행하고 네 제공자의 바이트코드 참조 및 build/distributions의 배포본을 검증한다. specs/016-fix-editor-open-thread/quickstart.md에 결과와 실제 IDE 검증 한계를 기록한다. (SC-004)

의존성: T013 → T014 → T015. 두 모듈 수정은 파일이 달라 독립 실행 가능하다. 최소 전달 단위는 네 제공자 교체 전체이며 기존 잠금 없는 열기 검증을 유지한다.
