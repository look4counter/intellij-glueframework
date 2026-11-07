# 작업 목록: Query 목록 아이콘 편집

## 단계 1: 준비

- [x] T001 specs/010-query-list-actions/research.md의 아이콘·PSI·스레드 설계를 확인한다.

## 단계 2: 기반 확인

- [x] T002 specs/010-query-list-actions/의 명세·계획·작업 및 품질 점검을 대조한다.

## 단계 3: 사용자 이야기 1

- [x] T003 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryCreationDialog.kt와 META-INF/queryeditor.xml에 입력 팝업·검증·서비스를 구현한다. FR-002.
- [x] T004 [US1] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt와 QueryDesignEditor.kt에 추가 명령과 안전한 아이콘 실행을 구현한다. FR-001, FR-002, FR-004.

## 단계 4: 사용자 이야기 2

- [x] T005 [US2] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryMapDocumentModel.kt와 QueryDesignEditor.kt에 삭제·복제·이동과 선택·경계 상태를 구현한다. FR-003~FR-005.
- [x] T006 [US2] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 두 사용자 이야기의 실제 버튼·입력 검증·실행 취소·보존·스레드 회귀 검증을 추가하고 기존 SQL 테스트와 함께 실행한다. SC-001, SC-002.

## 단계 5: 마무리

- [x] T007 specs/010-query-list-actions/quickstart.md에 결과와 한계를 기록하고 ZIP 및 명세와 구현의 차이를 점검한다.

## 의존성과 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 순서다. 추가가 최소 제공 범위이고 선택 관리를 이어서 완성한다. 동일 파일을 편집하므로 병렬 구현은 하지 않는다. 두 이야기는 각각 추가와 관리 버튼으로 독립 검증한다.

## 단계 6: 버튼 크기 보완

- [x] T008 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에서 버튼 크기를 배율 적용 24×24로 줄이고 기본 여백을 제거한다. FR-006.
- [x] T009 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 아이콘·렌더링·버튼 동작 검증을 실행하고 표시 크기를 확인한다. FR-001, FR-003, FR-006.
- [x] T010 specs/010-query-list-actions/quickstart.md에 검증 결과를 기록하고 설치용 ZIP 및 명세와 구현의 일치를 확인한다.

추가 의존성: T008 → T009 → T010 순서다.

## 단계 7: 32×32 패딩 조정

- [x] T011 queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt에서 아이콘을 유지하고 4 단위 margin과 배율 적용 32×32 버튼 크기를 설정한다. FR-006.
- [x] T012 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt의 기존 렌더링 높이를 조정하고 아이콘·버튼 동작 검증 및 이미지 확인을 수행한다. FR-001, FR-003, FR-006.
- [x] T013 specs/010-query-list-actions/quickstart.md에 검증 결과를 기록하고 ZIP과 명세·구현 일치를 확인한다.

추가 의존성: T011 → T012 → T013 순서다.

## 단계 8: 쿼리 ID 기본 빠른 검색

- [x] T014 [US3] src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 실제 목록의 플랫폼 Speed Search 등록·ID 검색·선택 동기화·문서 비변경·ID 갱신·빈 목록 회귀를 작성한다. FR-007~FR-009.
- [x] T015 [US3] queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt의 쿼리 JBList에 ListSpeedSearch.installOn을 연결하고 기존 renderer·선택·입력 처리를 유지한다. FR-007~FR-009.
- [x] T016 관련 QueryEditor 회귀를 실행하고 specs/010-query-list-actions/quickstart.md에 결과를 기록한 뒤 speckit-converge로 이번 범위를 대조한다. FR-007~FR-009.

T014 → T015 → T016 순서로 수행한다. 실제 목록을 기준으로 독립 검증하며 다른 입력 컴포넌트에는 검색을 등록하지 않는다.
