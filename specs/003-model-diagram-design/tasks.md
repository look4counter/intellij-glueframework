# 작업 목록: 모델 다이어그램 표시

## 1단계: 준비

- [x] T001 specs/003-model-diagram-design/spec.md의 State 끝점 명확화 및 산출물 일관성을 확인한다.

## 2단계: 공통 기반

- [x] T002 modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagram.kt에 XML PSI 파서와 선언 순서 연결 모델을 구현한다. (FR-001, FR-002, FR-005)

## 3단계: 사용자 이야기 1

- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 도형·이름 렌더링과 스크롤 범위를 구현한다. (FR-001)

독립 검증: 샘플의 노드 8개, Activity 높이 30 및 State 크기 20을 확인한다.

## 4단계: 사용자 이야기 2

- [x] T004 [US2] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 종류별 좌표, 선언 인덱스, 누락 끝점, 다중 꺾임점과 weight 무시 검증을 작성한다.
- [x] T005 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt에 사용자 정의 좌표 보정과 보간을 구현하고 캔버스에 선을 연결한다. (FR-002~FR-005)

독립 검증: 가로·세로 겹침과 사분면별 수치 검증, 샘플 연결선 12개를 확인한다.

## 5단계: 사용자 이야기 3

- [x] T006 [US3] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 공유 문서 기반 캔버스와 변경·오류·해제 처리를 연결한다. (FR-006)

독립 검증: Source 미저장 변경이 반영되고 원문과 weight가 보존된다.

## 6단계: 검증

- [x] T007 src/test/kotlin/com/look4counter/ModelDiagramTest.kt와 EditorMultiViewTest.kt를 실행하고 샘플 렌더링 이미지 및 명세 대비 잔여 작업을 점검한다.

## 의존성 및 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 → T007 순서다. 파서 검토와 독립 기하 테스트 사례 검토는 병렬로 가능하지만 공유 파일 구현은 순차 수행한다. 노드 표시를 최소 범위로 검증한 다음 연결선과 Source 동기화를 완성한다.

## 7단계: 표시 개선

- [x] T008 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt와 ModelDiagramCanvas.kt에 중앙 라벨, IDE 배경, 회색 Wire 및 흰색 Activity와 최종 원 확대를 적용한다. (FR-007~FR-009)

## 8단계: 탐색

- [x] T009 [US5] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 빈 공간 판별과 드래그 화면 이동을 구현한다. (FR-010)

## 9단계: 검증

- [x] T010 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 중앙 라벨·색상·드래그를 검증하고 밝은/어두운 샘플 이미지를 확인한 뒤 converge 점검한다.

T008 → T009 → T010 순서로 수행한다. 라벨 좌표 사례와 탐색 사례 검토는 독립적이나 같은 캔버스 구현은 순차로 진행한다.

## 10단계: 선택 및 탐색 입력 개선

- [x] T011 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 라벨 높이를 3px 줄이고 세로 정렬 및 Child 글자 GRAY_145를 적용한다. (FR-008, FR-011)
- [x] T012 [US5] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 우측 드래그를 위치에 무관한 탐색으로 변경한다. (FR-010)
- [x] T013 [US6] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 클릭·Shift 추가 선택, 완전 포함 범위 Box와 8방향 선택 핸들을 구현한다. (FR-012~FR-014)
- [x] T014 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 선택·탐색·라벨 높이·핸들 픽셀 및 XML 보존을 검증하고 샘플 이미지와 converge를 확인한다.

T011 → T012 → T013 → T014 순서로 같은 캔버스 변경을 수행한다. 좌표 사례 및 픽셀 사례 검토는 독립적으로 가능하다.

## 11단계: Wire 선택과 Child 이동

- [x] T015 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 라벨 1px 이동, 7/5px Child 핸들 및 비활성 테두리, FinalState 원 (1,1) 이동을 적용한다. (FR-009, FR-014, FR-015)
- [x] T016 [US7] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 Wire·조건 라벨 선택과 끝점 7/5px·꺾임점 3/1px 핸들을 구현한다. (FR-016, FR-017)
- [x] T017 [US7] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt와 ModelDesignEditorProvider.kt에 Child 이동 미리보기 및 XML location 편집·Undo·읽기 전용 처리를 구현한다. (FR-018)
- [x] T018 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 Hit 임계치, 핸들 픽셀과 이동·Source·Undo·저장을 검증하고 이미지 및 converge 점검을 수행한다.

T015 → T016 → T017 → T018 순서로 수행한다. XML 편집은 선택·탐색과 구분하고 드래그 release마다 한 번만 반영한다.

## 12단계: 앞쪽 표시 및 너비 편집

- [x] T019 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 라벨 하단 1px 및 추가 글자 이동, FinalState 지름 15를 적용한다. (FR-009, FR-015)
- [x] T020 [US8] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 마지막 선택 앞쪽 렌더링·Hit, Wire 6/4px와 중점 4/2px 핸들, Navigation 좌표를 구현한다. (FR-017, FR-019, FR-020)
- [x] T021 [US8] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt와 ModelDesignEditorProvider.kt에 Activity 좌우 너비 조절과 XML size·location·Undo 연동을 구현한다. (FR-021)
- [x] T022 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 픽셀·겹침·좌표 표시·좌우 조절·최소 너비·저장·Undo·읽기 전용을 검증하고 샘플 이미지 및 converge를 점검한다.

T019 → T020 → T021 → T022 순서다. 표시 상태는 XML 순서를 바꾸지 않으며 너비 편집은 기존 이동 편집 경로를 확장한다.

## 13단계: 화면 안정화와 Wire 끝점 미리보기

- [x] T023 [US4] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 Child 6/4px 핸들과 투명한 FinalState 외곽을 적용한다. (FR-009, FR-014)
- [x] T024 [US9] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 화면 원점을 고정하고 캔버스 크기 축소를 방지하여 편집 시 다른 요소·Navigation·스크롤 위치를 유지한다. (FR-023)
- [x] T025 [US9] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 4px Wire Hit와 핸들 hover 커서 및 끝점 드래그 임시 경로를 구현한다. (FR-016, FR-022, FR-024)
- [x] T026 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 핸들 픽셀·커서·4/5px Hit·음수 이동 화면 고정·양 끝점 드래그를 검증하고 이미지와 converge를 점검한다.

T023 → T024 → T025 → T026 순서로 수행한다. 끝점 드래그 미리보기는 XML 편집 콜백과 분리한다.

## 14단계: 다중 편집과 Wire 편집 저장

- [x] T027 [US10] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt와 ModelDesignEditorProvider.kt에 다중 선택 유지·일괄 이동 및 Activity 일괄 너비 조절을 구현한다. (FR-025, FR-028)
- [x] T028 [US10] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt와 ModelDiagramCanvas.kt에 6px 핸들 Hit, 꺾임점 이동 및 중점 삽입을 구현한다. (FR-026, FR-027)
- [x] T029 [US10] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt와 ModelDesignEditorProvider.kt에 끝점 재연결·잘못된 대상 원복 및 Wire XML 편집·Undo 보호를 구현한다. (FR-024, FR-028)
- [x] T030 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 일괄 편집, 핸들 경계, 꺾임점 저장, 재연결·원복·Undo·읽기 전용 검증을 추가하고 이미지 및 converge를 확인한다.

T027 → T028 → T029 → T030 순서다. 편집은 release 한 번당 하나의 명령으로 묶으며 화면 원점과 Navigation 이동량은 유지한다.

## 15단계: 가상 State 끝점 미리보기

- [x] T031 [US11] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt에 가상 State 중심의 끝점 미리보기와 꺾임점 절대 위치 보존 계산을 구현한다. (FR-029, FR-030)
- [x] T032 [US11] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 자유 끝점 좌표 대체를 가상 State 경로 계산으로 변경한다. 실제 Child 재연결은 유지한다. (FR-024, FR-029)
- [x] T033 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 양방향 종류별 보정, 꺾임점·Navigation 유지 및 재연결·원복을 검증하고 converge를 점검한다.

T031 → T032 → T033 순서로 수행한다. 기존 연결 규칙을 재사용하고 XML 저장 형식은 변경하지 않는다.

## 16단계: 끝점 이동 시 꺾임점 비례 이동

- [x] T034 [US11] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt에서 가상 State 끝점의 꺾임점 재인코딩을 제거하고 저장된 상대 벡터로 비율 계산을 수행한다. (FR-030)
- [x] T035 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 단일·다중 꺾임점의 시작·끝 비례 이동 및 XML·weight 보존과 원복 검증을 반영한다.
- [x] T036 specs/003-model-diagram-design/quickstart.md와 설계 문서를 갱신하고 관련 회귀 테스트 및 converge를 확인한다.

T034 → T035 → T036 순서다. 꺾임점 삽입 편집의 위치 보존 규칙과 끝점 이동의 상대 벡터 보존 규칙을 구분한다.

## 17단계: Activity 직결 연결 대칭 보정

- [x] T037 [US12] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt에 세로 유효 구간 8px 여백 및 가까운 변 쌍 선택을 적용한다. 수직 우선과 대각선 보정을 유지한다. (FR-004, FR-031)
- [x] T038 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 수직·수평 구간 제한, 방향·가까운 변과 좁은 구간·대각선 보정을 검증한다.
- [x] T039 specs/003-model-diagram-design/quickstart.md를 갱신하고 관련 회귀 테스트 및 converge를 확인한다.

T037 → T038 → T039 순서로 진행한다. 꺾임점 있는 연결과 State 연결의 회귀도 함께 검증한다.

## 18단계: 작은 핸들 분할·병합 임계치

- [x] T040 [US13] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt와 ModelDiagramCanvas.kt에서 원래 직선으로부터 5px 거리로 중점 분할·병합을 판정한다. (FR-027, FR-032)
- [x] T041 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 양방향 경계, 반복 전환, Navigation 및 무편집 release·저장·Undo 검증을 추가한다. (FR-033)
- [x] T042 specs/003-model-diagram-design/quickstart.md와 설계 문서를 갱신하고 관련 회귀 테스트 및 converge를 확인한다.

T040 → T041 → T042 순서로 진행한다. 기존 꺾임점은 유지하고 이번 드래그에서 만든 임시 꺾임점만 병합한다.

## 19단계: 기존 bendpoint 핸들 병합

- [x] T043 [US14] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt와 ModelDiagramCanvas.kt에 기존 점의 제거 후보 기반 5px 병합·복원을 구현한다. (FR-034)
- [x] T044 [US14] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 삭제 인덱스로 정확한 bendpoint만 삭제하는 XML·Undo 처리를 구현한다. (FR-035)
- [x] T045 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 경계·여러 점·반복 병합·XML·Undo·속성 보존을 검증하고 quickstart 및 converge를 확인한다.

T043 → T044 → T045 순서다. 드래그한 점만 병합하고 남은 점의 위치와 태그를 유지한다.

## 20단계: Activity 직결 끝점 드래그 대칭 수정

- [x] T046 [US15] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 Activity 직결의 양 끝 드래그가 마우스 좌표를 공통 firstRelativePoint 기준으로 반영하도록 수정한다. (FR-036)
- [x] T047 src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 수직·수평의 양 끝 드래그·clamp·Navigation 및 Source·저장·Undo/Redo 검증을 추가한다. (FR-037)
- [x] T048 specs/003-model-diagram-design/quickstart.md와 데이터 모델을 갱신하고 회귀 테스트 및 converge를 확인한다.

T046 → T047 → T048 순서다. XML 형식과 기존 State·꺾임점 경로 규칙은 유지한다.

## 21단계: Child 관통 방지 병합

- [x] T049 [US16] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 내부 관통·경계 접촉·연결 Child·두 핸들 회귀 검증을 추가한다. (FR-038, FR-039)
- [x] T050 [US16] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt에 도형 내부 교차 조건을 추가하고 ModelDiagramCanvas.kt에서 전체 Child를 전달한다. (FR-038, FR-039)
- [x] T051 [US16] 관련 테스트를 실행하고 명세·계획·작업과 구현의 수렴을 확인한다.

T049 → T050 → T051 순서로 진행한다.

## 22단계: 드래그 인접 선분 관통 검사 정정

- [x] T052 [US17] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 A–C 비관통/A–B 또는 B–C 관통 및 반대 사례를 기존점·중점 양쪽으로 검증한다. (FR-040, FR-041)
- [x] T053 [US17] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt에서 이동한 B의 인접 두 선분을 검사하도록 변경한다. (FR-040, FR-041)
- [x] T054 [US17] 모델 다이어그램 테스트와 speckit 수렴 검토를 수행하고 관련 문서를 갱신한다.

T052 → T053 → T054 순서로 수행한다.

## 23단계: 연결 Child만 관통 검사

- [x] T055 [US18] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 비연결 Child를 무시하는 기존점·중점 Canvas 드래그와 연결 Child 관통 차단을 검증한다. (FR-042, FR-043)
- [x] T056 [US18] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt와 ModelDiagramCanvas.kt에서 전체 nodes 인수를 제거하고 start·end만 검사한다. (FR-042)
- [x] T057 [US18] 관련 테스트를 실행하고 설계 문서 갱신 및 수렴 검토를 수행한다. (FR-043)

T055 → T056 → T057 순서로 진행한다. 21단계의 전체 Child 전달 작업은 이번 단계에서 연결 Child 한정으로 대체한다.

## 24단계: 작은 핸들로 공통 접점 이동

- [x] T058 [US19] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 양 축 이동·양 경계·축 외 이동·단일점 교집합과 Canvas Source·저장·Undo/Redo·Navigation 검증을 추가하고 기존 분할 테스트를 State 연결로 유지한다. (FR-044~FR-046)
- [x] T059 [US19] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt에서 Activity 직결 중점 드래그의 공통 좌표를 유효 구간으로 제한한다. (FR-044, FR-045)
- [x] T060 [US19] 관련 회귀 테스트를 실행하고 설계 문서·수동 검증 절차를 갱신하며 수렴을 점검한다. (FR-046)

T058 → T059 → T060 순서로 수행한다.

## 25단계: 공통 이동 범위 밖의 분할

- [x] T061 [US20] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 양축·양경계의 안쪽 직결과 바깥 분할, 단일점 교집합, 반복 전환 및 Navigation·저장·Undo/Redo를 검증한다. (FR-047, FR-048)
- [x] T062 [US20] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt의 Activity 직결 중점 처리를 범위 밖 bendpoint 삽입으로 변경한다. (FR-047)
- [x] T063 [US20] 관련 테스트·설계 문서·수동 검증 절차와 구현의 수렴을 확인한다. (FR-048)

T061 → T062 → T063 순서로 진행한다. 24단계의 범위 밖 경계 고정은 이번 분할 규칙으로 대체한다.

## 26단계: 직결 복원의 관통 검사 유지

- [x] T064 [US21] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 범위 안 start·end 관통 분할과 안전한 복원, 반복 전환 및 저장·Undo/Redo를 수평·수직으로 검증한다. (FR-049, FR-050)
- [x] T065 [US21] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt의 직결 공통 이동 분기에 기존 canMergeAt 검사를 적용한다. (FR-049)
- [x] T066 [US21] 관련 테스트를 실행하고 문서와 구현의 수렴을 확인한다. (FR-050)

T064 → T065 → T066 순서로 진행한다.

## 27단계: 드래그 취소

- [x] T067 [US22] src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에 Child 이동·너비·Wire 끝점·중점·bendpoint·Navigation·범위 선택의 Esc 취소와 release 무편집 회귀 검증을 추가한다. (FR-051, FR-052)
- [x] T068 [US22] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 제스처 상태 관리·원본 복원·Esc action과 Source refresh의 취소 처리를 구현한다. (FR-051, FR-052)

## 28단계: 선택 Child 복사·붙여넣기

- [x] T069 [US23] src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에 선택 필터·내부 Wire·ID·인덱스·속성·좌표·반복 붙여넣기·다른 문서·Undo/Redo와 비활성 조건을 검증한다. (FR-053~FR-058)
- [x] T070 [US23] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramClipboard.kt에 불변 XML 스냅샷과 로컬 클립보드 형식을 구현한다. (FR-053, FR-056, FR-058)
- [x] T071 [US23] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 복사·붙여넣기 provider와 단일 XML 명령, ID·Wire 참조·좌표 재매핑을 구현한다. (FR-053~FR-058)
- [x] T072 [US23] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 UiDataProvider와 화면 오프셋·붙여넣기 선택 연계를 구현한다. (FR-054, FR-057, FR-058)
- [x] T073 관련 플랫폼 회귀 테스트와 수렴 점검을 수행하고 research.md·data-model.md·quickstart.md 및 contracts 문서를 갱신한다. (FR-051~FR-058)

의존 순서: T067 → T068 → T069 → T070 → T071 → T072 → T073. 기존 Source·Undo 보호 경로를 재사용한다.

## 29단계: 선택 삭제와 연결 참조 정리

- [x] T074 [US24] src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에 Wire·Child·다중 선택 삭제, 삭제 후 선언 인덱스 재매핑, 미지원 Child 참조, 속성 보존, Undo/Redo·저장 및 비활성 조건을 검증한다. (FR-059~FR-063)
- [x] T075 [US24] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 DeleteProvider 데이터와 Delete 키 액션 및 선택 해제를 구현한다. (FR-059, FR-062, FR-063)
- [x] T076 [US24] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 삭제 집합·Wire 인덱스 재매핑·단일 PSI 삭제 명령과 보호 조건을 구현한다. (FR-060~FR-063)
- [x] T077 관련 모델·클립보드·Design/Source 통합 회귀 테스트를 실행하고 설계 문서·수동 검증 절차 갱신과 수렴 점검을 수행한다. (FR-059~FR-063)

T074 → T075 → T076 → T077 순서로 수행한다. 기존 선택 구조·Undo·Source 동기화 경로를 유지한다.

## 30단계: 조건·Activity 속성 편집

- [x] T078 [US25] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 Wire·라벨 더블클릭 확인/취소·무변경·Source 보호·Undo/Redo를 검증한다. (FR-064, FR-065)
- [x] T079 [US26] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 name·테이블·description 초기값과 수정·행 추가/삭제·검증·XML 속성/줄바꿈 보존 및 확인/취소를 검증한다. (FR-066~FR-068)
- [x] T080 [US25] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramDialogs.kt에 편집 데이터·프로젝트 서비스·DialogWrapper와 Activity 입력 패널을 구현하고 META-INF/modeleditor.xml에 서비스를 등록한다. (FR-064, FR-066)
- [x] T081 [US25] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 더블클릭 콜백을 연결하고 ModelDesignEditorProvider.kt에 원자적 속성 편집·동시 Source 보호를 구현한다. DiagramXmlText.kt와 ModelDiagram.kt에서 XML 사용자 문자열·숫자 엔티티를 저장·복원한다. (FR-064~FR-068)

## 31단계: Child 추가 툴바와 ghost

- [x] T082 [US27] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 종류 기억·토글·ghost·연속 생성·ID/namespace·Navigation·Esc·읽기 전용·Undo/Redo를 검증한다. (FR-069~FR-072)
- [x] T083 [US27] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 Select 박스·추가 토글·종류 기억 및 단일 Child 생성 명령을 구현한다. (FR-069, FR-070, FR-072)
- [x] T084 [US27] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 생성 모드·반투명 ghost·클릭 생성·Esc/Navigation과 기존 액션 충돌 방지를 구현한다. (FR-070~FR-072)
- [x] T085 문서·계약·수동 검증 절차를 갱신하고 관련 회귀 테스트·수렴 점검으로 남은 작업을 완료한다. (FR-064~FR-072)

T078 → T079 → T080 → T081 → T082 → T083 → T084 → T085 순서로 수행한다.

## 32단계: 편집 창 UI·행 복사·클래스 열기

- [x] T086 [US28] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 Transition/Activity 배치·여러 행 복사/붙여넣기·TSV·sourceIndex 분리·확인 전 XML 무변경을 검증한다. (FR-073~FR-076)
- [x] T087 [US29] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 현재 class 입력·실제 클래스 파일 열기·빈 값/미발견 안내·XML 무변경을 검증한다. (FR-077, FR-078)
- [x] T088 [US28] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramDialogs.kt와 PropertyRowsClipboard.kt에 UI 배치·테이블 클립보드 provider를 구현한다. (FR-073~FR-076)
- [x] T089 [US29] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ActivityClassNavigation.kt와 modeleditor/build.gradle.kts에 Java 클래스 탐색·열기 및 패널 연결을 구현한다. (FR-077, FR-078)

## 33단계: 추가 버튼 통합과 ghost 동일 스타일

- [x] T090 [US30] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationButton.kt와 ModelDesignEditorProvider.kt에서 별도 Select를 통합 토글 드롭다운으로 교체한다. (FR-079)
- [x] T091 [US30] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 ghost를 실제 drawNode와 동일하게 그리고 src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에서 토글·종류·픽셀 일치를 검증한다. (FR-079, FR-080)
- [x] T092 관련 회귀 테스트·이미지 배치 확인·설계 문서·수동 절차 및 수렴 점검을 완료한다. (FR-073~FR-080)

T086 → T087 → T088 → T089 → T090 → T091 → T092 순서로 수행한다. 기존 확인/취소·Source·Undo 및 생성 좌표 규칙은 유지한다.

## 34단계: IntelliJ 표준 생성 툴바

- [x] T093 [US31] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에서 표준 ActionToolbar·SplitButtonAction·ToggleAction 구성과 기존 생성·Esc·비활성 검증을 갱신한다. (FR-081, FR-082)
- [x] T094 [US31] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt와 ModelDesignEditorProvider.kt로 표준 액션 툴바를 연결하고 ChildCreationButton.kt를 제거한다. (FR-081, FR-082)

## 35단계: Activity 수평 접점과 State 외곽 편집

- [x] T095 [US32] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 Activity 좌우 y 이동·여백 경계·실제/가상 State·대칭 연결을 검증한다. (FR-083, FR-084)
- [x] T096 [US33] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 State 둘레 투영·기본 외곽·중심 클릭·끝점 드래그·저장·Undo/Redo·State 이동 접점 보존을 검증하고 기존 중심 기대값을 갱신한다. (FR-085~FR-087)
- [x] T097 [US32] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt에 Activity y 반영·State 접점 투영·가상 State 선호 초기화를 구현한다. (FR-083~FR-087)
- [x] T098 [US33] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt와 ModelDesignEditorProvider.kt에 State 이동의 접점 좌표 보존을 구현하고 DiagramEdits.kt의 관통 판정을 외곽 접점에 맞게 정리한다. (FR-087)
- [x] T099 관련 모델·클립보드·속성·Design/Source 통합 회귀 검증 및 설계 문서·수동 절차·수렴 점검을 완료한다. (FR-081~FR-087)

T093 → T094 → T095 → T096 → T097 → T098 → T099 순서로 수행한다. 앞선 UI·클립보드·삭제·ghost 개선은 유지한다.

## 36단계: 수직 접점과 미니맵

- [x] T100 [US34] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 위/아래·시작/끝·State/가상 State/bendpoint의 8px x 제한과 좁은 Activity를 검증한다. (FR-088, FR-089)
- [x] T101 [US34] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramGeometry.kt의 수직 접점 x를 유효 범위로 제한한다. (FR-088, FR-089)
- [x] T102 [US35] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에 내용 투영·음수 좌표·뷰포트·Navigation/스크롤 고정·편집 갱신·상호작용 보호·XML 무변경을 검증한다. (FR-090~FR-093)
- [x] T103 [US35] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt와 ModelDiagramCanvas.kt에 반투명 미니맵·뷰포트·스크롤 리스너·왼쪽 클릭 보호를 구현한다. (FR-090~FR-093)
- [x] T104 specs/003-model-diagram-design/alignment-review.md에 이동/너비 조절 가이드·release 스냅의 가능성과 후속 설계 선택을 검토한다. 스냅 코드는 구현하지 않는다.
- [x] T105 관련 회귀 테스트·미니맵 이미지 확인·설계 문서·수동 검증 절차와 수렴 점검을 완료한다. (FR-088~FR-093)

T100 → T101 → T102 → T103 → T104 → T105 순서로 진행한다. 정렬 보조선/스냅은 검토 문서만 작성한다.

## 37단계: 실시간 정렬과 후속 편집 개선

- [x] T106 [US36] src/test/kotlin/com/look4counter/ModelDiagramAlignmentTest.kt에서 임계치·이동·다중 선택·너비·Alt·취소·release를 검증한다. (FR-094~FR-096)
- [x] T107 [US36] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramAlignment.kt와 ModelDiagramCanvas.kt에 실시간 스냅·가이드·상태 정리를 구현한다. (FR-094~FR-096)
- [x] T108 [US37] DiagramMiniMap.kt와 ModelDiagramCanvas.kt에서 제목을 제거하고 배율을 0.7배로 바꾸며 ModelDiagramMiniMapTest.kt에서 검증한다. (FR-097)
- [x] T109 [US38] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 클래스 참조 입력·일반 행 분리·선택 값 전달 검증을 추가한다. (FR-098, FR-099)
- [x] T110 [US38] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ActivityClassCellEditor.kt와 ModelDiagramDialogs.kt에 class 행 전용 자동완성 입력을 연결한다. (FR-098, FR-099)
- [x] T111 관련 회귀 테스트와 quickstart.md·alignment-review.md를 갱신하고 speckit-converge로 잔여 작업을 확인한다. (FR-094~FR-099)

T106 → T107 → T108 → T109 → T110 → T111 순서로 수행한다.

## 38단계: 같은 간격 보조선과 이동 스냅

- [x] T112 [US39] src/test/kotlin/com/look4counter/ModelDiagramAlignmentTest.kt에 가로/세로 사이·앞·뒤·상이한 크기·음수·겹침·인접성·정렬 경쟁과 임계치 검증을 추가한다. (FR-100~FR-102)
- [x] T113 [US39] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramAlignment.kt에 축별 간격 후보 및 정렬과의 우선순위·거리 가이드 계산을 구현한다. (FR-100~FR-102)
- [x] T114 [US39] ModelDiagramCanvas.kt의 보조선 렌더링에 간격 치수선·끝 눈금·화살표·거리 라벨을 구현한다. (FR-102, FR-103)
- [x] T115 [US39] ModelDiagramAlignmentTest.kt에 다중 선택·Alt·release·취소·Source/읽기 전용·Navigation·저장/Undo와 렌더링을 검증하고 기존 모델 회귀를 실행한다. (FR-103)
- [x] T116 specs/003-model-diagram-design의 설계·계약·검증 문서와 작업 상태를 갱신하고 speckit-converge로 잔여 작업을 확인한다. (FR-100~FR-103)

T112 → T113 → T114 → T115 → T116 순서로 수행한다.

## 39단계: 메모리 Query ID 인덱스와 sql-key 자동완성

- [x] T117 [US40] src/test/kotlin/com/look4counter/QueryIdIndexTest.kt에서 최초 수집·파일/미저장 변경·추가/삭제/rename·XML 필터·후보 분리·복구를 검증한다. (FR-104, FR-105)
- [x] T118 [US40] queryeditor/src/main/kotlin/com/look4counter/queryeditor/queryindex/QueryIdIndexService.kt와 QueryIdIndexStartupActivity.kt 및 queryeditor.xml에 메모리 서비스·백그라운드 최초 수집·증분 리스너를 구현한다. (FR-104, FR-105)
- [x] T119 [US40] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt에 sql-key 전용 입력·접두사 후보·실제 선택·ID만 삽입·확인 전 원본 유지·변경 후 후보를 검증한다. (FR-106, FR-107)
- [x] T120 [US40] modeleditor/build.gradle.kts·QueryIdCellEditor.kt·ModelDiagramDialogs.kt에 메모리 스냅샷 기반 Query ID 자동완성을 연결한다. (FR-106, FR-107)
- [x] T121 관련 모델·QueryEditor 회귀와 문서·계약·검증 절차를 갱신하고 speckit-converge로 잔여 작업을 확인한다. (FR-104~FR-107)

T112~T116의 기존 작업과 이어서 T117 → T118 → T119 → T120 → T121 순서로 수행한다.

## 40단계: 미니맵 현재 화면 크기와 Navigation 자동 축소

- [x] T122 [US41] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에 실제 화면 크기/비율·작은 내용·큰 Navigation·스크롤/리사이즈·음수 좌표의 사각형 전체 포함을 검증한다. (FR-108~FR-110)
- [x] T123 [US41] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt에 내용과 현재 화면의 합집합 투영을 구현한다. (FR-108, FR-109)
- [x] T124 [US41] ModelDiagramCanvas.kt에서 viewport 전체를 그리고 실제 렌더링과 기존 편집 회귀·XML 무변경을 검증한다. (FR-108~FR-110)
- [x] T125 specs/003-model-diagram-design의 설계·계약·검증 절차를 갱신하고 speckit-converge로 남은 작업을 확인한다. (FR-108~FR-110)

T122 → T123 → T124 → T125 순서로 수행한다.

## 41단계: 미니맵 기본 배율 복원

- [x] T126 [US42] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt의 기대 배율을 기본 맞춤 배율로 갱신한다. (FR-111)
- [x] T127 [US42] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt에서 0.7 추가 축소를 제거하고 기존 미니맵 회귀를 실행한다. (FR-111)
- [x] T128 설계·계약·검증 문서를 기본 배율로 갱신하고 speckit-converge로 잔여 작업을 점검한다. (FR-111)

T126 → T127 → T128 순서로 수행한다.

## 42단계: 탐색·설정·화면 조작 확장

- [x] T129 [US43] src/test/kotlin/com/look4counter/QueryNavigationTest.kt에 ID·offset 탐색·Find in Files 계약·누락·XML 무변경 검증을 추가한다. (FR-112, FR-113)
- [x] T130 [US43] queryeditor의 QueryNavigation.kt·QueryEditorNavigation.kt와 QueryDesignEditor.kt에 공통 탐색 인터페이스 및 NavigatableFileEditor를 구현한다. (FR-112, FR-113)
- [x] T131 [US44] ModelEditorFeaturesTest.kt와 ModelDiagram.kt·ModelDiagramCanvas.kt에 루트 property 파싱·고정 표시를 구현·검증한다. (FR-115)
- [x] T132 [US44] ModelEditorSettingsTest.kt에 프리셋 복제·Apply/Reset·색상·서비스 경로 검증을 추가한다. (FR-116, FR-117)
- [x] T133 [US44] modeleditor의 ModelEditorSettings.kt·GlueModelEditorConfigurable.kt와 modeleditor.xml에 설정/디렉터리 picker·프리셋 목록/색상 편집을 구현한다. (FR-116, FR-117)
- [x] T134 [US43] ServiceFiles.kt·ServiceNameCellEditor.kt·ModelDiagramDialogs.kt에 service-name 후보·Open Query/Service를 연결하고 속성 편집 테스트를 갱신한다. (FR-112, FR-114)
- [x] T135 [US45] ModelEditorFeaturesTest.kt와 WireMagnet.kt·ChildCreationToolbar.kt·ModelDiagramCanvas.kt에 토글·중심/30px 스냅·제한 범위를 구현·검증한다. (FR-118)
- [x] T136 [US45] ModelDesignEditorProvider.kt·ModelDiagramCanvas.kt에 스크롤바 제거·휠 Navigation·하늘색 Range box를 구현하고 검증한다. (FR-119, FR-120)
- [x] T137 [US45] ModelDiagramCanvas.kt와 ModelDiagramMiniMapTest.kt에 미니맵 click Navigation·초기 최소 맞춤 및 기존 클릭 보호를 구현·검증한다. (FR-121)
- [x] T138 기존 모델/Query 회귀와 렌더링·설계/계약/수동 검증 문서 및 speckit-converge 점검을 완료한다. (FR-112~FR-121)

T129 → T130 → T131 → T132 → T133 → T134 → T135 → T136 → T137 → T138 순서로 수행한다. 같은 파일의 작업은 순차 통합한다.

## 43단계: 루트 편집과 후속 조작 개선

- [x] T139 [US46] ModelEditorFeaturesTest.kt·ModelDiagramPropertiesTest.kt에 description 제외/표시·루트 편집 UI/확인/취소/Undo/Child 보존 검증을 추가한다. (FR-122, FR-123)
- [x] T140 [US46] ModelDiagramDialogs.kt·ChildCreationToolbar.kt·ModelDesignEditorProvider.kt·ModelDiagramCanvas.kt에 루트 속성 편집과 HUD 형식을 구현한다. (FR-122, FR-123)
- [x] T141 [US47] ModelEditorFeaturesTest.kt와 WireMagnet.kt·ModelDiagramCanvas.kt에 중간점/꺾임점 자석·경계·off 검증 및 구현을 추가한다. (FR-124)
- [x] T142 [US47] ModelDiagramMiniMapTest.kt·ModelEditorFeaturesTest.kt·ModelDiagramCanvas.kt에 고정 입력 투영 기반 미니맵 드래그/release/Esc를 구현·검증한다. (FR-125)
- [x] T143 [US48] ModelEditorSettingsTest.kt에 프로젝트 상대 경로·외부 거부·서비스 접미사·실제 자동완성 선택을 검증한다. (FR-126~FR-128)
- [x] T144 [US48] ServiceFiles.kt·ServiceNameCellEditor.kt에 프로젝트 범위 경로 통합·파일명 변환·표준 자동 팝업 입력을 구현한다. (FR-126~FR-128)
- [x] T145 [US48] GlueModelEditorConfigurable.kt·ModelDiagramDialogs.kt에 NoScroll·프로젝트 폴더 picker·작은 설정 배치와 compact 패널을 구현하고 렌더링 검증한다. (FR-128, FR-129)
- [x] T146 관련 모델/Query 회귀와 설계·계약·수동 검증 문서를 갱신하고 speckit-converge로 남은 작업을 확인한다. (FR-122~FR-129)

T139 → T140 → T141 → T142 → T143 → T144 → T145 → T146 순서로 수행한다.

## 44단계: 프리셋과 Wire 추가 확장

- [x] T147 [US49] ServiceFiles.kt와 ModelEditorSettingsTest.kt에서 설정 밖/미설정 프로젝트 파일·접미사·중복 검색을 구현·검증한다. (FR-131)
- [x] T148 [US49] icons/magnet.svg와 magnet_dark.svg를 IntelliJ Settings 팔레트와 맞춘다. (FR-130)
- [x] T149 [US50] ModelDiagramPropertiesTest.kt·ModelEditorFeaturesTest.kt에 프리셋 선택·속성 생성·색상·설정 갱신 검증을 추가한다. (FR-132, FR-133)
- [x] T150 [US50] ModelEditorSettings.kt·ModelDiagram.kt·ChildCreationToolbar.kt·ModelDiagramCanvas.kt·ModelDesignEditorProvider.kt에 동적 메뉴·프리셋 생성·class 색상을 구현한다. (FR-132, FR-133)
- [x] T151 [US51] ModelDiagramPropertiesTest.kt와 ModelEditorFeaturesTest.kt에 Wire 토글·미리보기·참조 저장·Undo/Redo·취소·보호 검증을 추가한다. (FR-134~FR-136)
- [x] T152 [US51] ChildCreationToolbar.kt·ModelDiagramCanvas.kt·ModelDesignEditorProvider.kt에 Wire 생성과 배타적 모드·단일 XML 명령을 구현한다. (FR-134~FR-136)
- [x] T153 관련 모델/Query 회귀·렌더링·설계 및 계약 문서를 갱신하고 speckit-converge로 잔여 작업을 점검한다. (FR-130~FR-136)

T147 → T148 → T149 → T150 → T151 → T152 → T153 순서로 수행한다.
## 45단계: 생성과 표시 개선

- [x] T154 [US52] modeleditor의 ServiceFiles.kt에서 Design 우선 열기를 구현하고 src/test/kotlin/com/look4counter/ModelEditorNavigationTest.kt에서 실제 에디터와 기록용 관리자로 탭 선택 요청을 확인한다. (FR-137)
- [x] T155 [US52] ChildCreationToolbar.kt에서 메뉴 선택 자동 활성화를 구현하고 ModelDiagramPropertiesTest.kt의 메뉴 시나리오를 갱신한다. (FR-138)
- [x] T156 [US52] ModelEditorSettings.kt·GlueModelEditorConfigurable.kt·ModelDesignEditorProvider.kt·ModelDiagramCanvas.kt에 Foreground를 추가하고 ModelEditorSettingsTest.kt와 ModelDiagramPropertiesTest.kt에서 저장·색상 반영을 검증한다. (FR-139)
- [x] T157 [US52] ModelDiagramCanvas.kt의 빈 화면 Wire 자석 꺾임과 미리보기 스타일을 수정하고 ModelEditorFeaturesTest.kt에서 실제 목표 경로와 비교한다. (FR-140)
- [x] T158 specs/003-model-diagram-design/quickstart.md에 결과를 기록하고 모델·Query 회귀 및 최종 수렴 점검을 수행한다. (FR-137~FR-140)

T154 → T155 → T156 → T157 → T158 순서로 수행한다. 독립 검증 기준은 Design 선택·메뉴 즉시 ghost·설정 저장 및 name 색상·직선 미리보기다.

## 46단계: 텍스트 툴바와 확대

- [x] T159 [US53] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt에 선택 텍스트·메뉴 닫기·명칭과 Wire 텍스트를 구현한다. (FR-141, FR-142)
- [x] T160 [US53] modeleditor의 ModelDiagramCanvas.kt·DiagramMiniMap.kt·DiagramClipboard.kt·ModelDesignEditorProvider.kt에 배율 변환을 통합하고 ZoomControls.kt의 버튼 묶음을 연결한다. (FR-143, FR-144)
- [x] T161 [US53] src/test/kotlin/com/look4counter/ModelDiagramZoomTest.kt와 ModelDiagramPropertiesTest.kt·ModelDiagramClipboardTest.kt에서 텍스트·배율·편집·미니맵·화면 붙여넣기를 검증한다. (FR-141~FR-144)

## 47단계: 저장 시 서비스 변환

- [x] T162 [US54] modeleditor의 ModelServiceXmlExporter.kt에 초기 연결·Activity·transition·transaction·property 변환과 오류 검증을 구현한다. (FR-146, FR-147)
- [x] T163 [US54] modeleditor의 ModelServiceSaveListener.kt와 resources/META-INF/modeleditor.xml에 저장 이벤트·프로젝트 경로·출력 갱신·오류 알림을 연결한다. (FR-145, FR-147)
- [x] T164 [US54] src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt와 테스트 리소스에 사용자 예시·false transaction·특수문자·불완전 그래프·저장/재저장·열린 출력 동기화를 검증한다. (FR-145~FR-147)
- [x] T165 specs/003-model-diagram-design/quickstart.md에 결과를 기록하고 회귀·최종 수렴 점검을 완료한다. (FR-141~FR-147)

T159 → T160 → T161 → T162 → T163 → T164 → T165로 수행한다. UI와 변환은 독립 검증할 수 있으나 현재 작업은 순차 통합한다.

## 48단계: 안전한 Design 갱신

- [x] T166 [US55] src/test/kotlin/com/look4counter/ModelEditorRefreshTest.kt에서 쓰기 불가 문맥의 생성·선택·최신 문서 반영·폐기 보호를 재현한다. (FR-148, FR-149)
- [x] T167 [US55] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 중복 병합·명시적 비모달 예약·쓰기 안전성 검사와 생명주기 보호를 구현한다. (FR-148, FR-149)
- [x] T168 specs/003-model-diagram-design/quickstart.md에 회귀 결과를 기록하고 명세·구현 최종 수렴을 점검한다. (FR-148, FR-149)

T166 → T167 → T168 순서로 진행한다. 재현 테스트는 기존 버전에서 TransactionGuard 오류를 내고 수정 버전에서 안전한 예약과 최신 상태 표시를 확인해야 한다.

## 49단계: 예약 갱신 중 생성 요청 보존

- [x] T169 [US56] src/test/kotlin/com/look4counter/ModelEditorRefreshTest.kt에 탭 선택 직후·연속 생성·Source 변경·오류 보호·쓰기 불가 생성 검사를 추가한다. (FR-150, FR-151)
- [x] T170 [US56] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 예약 상태 차단을 제거하고 최신 문서 검증 및 안전한 생성 재시도를 구현한다. (FR-150, FR-151)
- [x] T171 specs/003-model-diagram-design/quickstart.md에 회귀 결과를 기록하고 구현·명세 최종 수렴을 확인한다. (FR-150, FR-151)

T169 → T170 → T171 순서로 수행한다.

## 50단계: 배율 선택 목록

- [x] T172 [US57] modeleditor의 ZoomControls.kt·ModelDiagramCanvas.kt를 20~150% Select와 10% 간격으로 변경한다. (FR-152)
- [x] T173 [US57] src/test/kotlin/com/look4counter/ModelDiagramZoomTest.kt·ModelDiagramClipboardTest.kt·ModelDiagramPropertiesTest.kt를 새 배율·선택 UI에 맞게 갱신하고 검증한다. (FR-152)
- [x] T174 specs/003-model-diagram-design/quickstart.md를 갱신하고 생성 클릭 수정과 함께 회귀·최종 수렴 점검을 수행한다. (FR-150~FR-152)

T169~T171의 생성 수정 이후 T172 → T173 → T174를 통합 검증한다.

## 51단계: 생성 요청 큐

- [x] T175 [US58] src/test/kotlin/com/look4counter/ModelEditorRefreshTest.kt에 메뉴 직후·FIFO 위치/ID·모드 취소/재활성화·폐기와 요청 프리셋을 검증한다. (FR-153)
- [x] T176 [US58] modeleditor의 ModelDesignEditorProvider.kt에 요청 큐와 비모달 처리를 구현하고 ModelDiagramCanvas.kt에 모드 세대를 추가한다. (FR-153)
- [x] T177 specs/003-model-diagram-design/quickstart.md에 재현·회귀 결과를 기록하고 최종 수렴 점검을 수행한다. (FR-153)

T175 → T176 → T177 순서로 수행한다.

## 52단계: 생성 클릭 진단 기록

- [x] T178 [US59] modeleditor의 ChildCreationTrace.kt·ModelDiagramCanvas.kt에 원시/Canvas 마우스 관찰과 입력 식별자·무시 이유를 추가한다. (FR-154, FR-155)
- [x] T179 [US59] ChildCreationToolbar.kt·ModelDesignEditorProvider.kt에 선택/팝업/모드·큐 요청 ID·등록/취소/실행/완료·문서 상태 기록을 연결한다. (FR-154, FR-155)
- [x] T180 [US59] src/test/kotlin/com/look4counter/ModelEditorCreationTraceTest.kt에 거부 사유·로그 상관관계·큐 취소·기존 동작 보존을 검증하고 quickstart.md에 수집 방법을 기록한다. (FR-154, FR-155)
- [x] T181 specs/003-model-diagram-design의 문서와 구현을 최종 점검하고 관련 회귀 및 수렴 검사를 완료한다. (FR-154, FR-155)

T178 → T179 → T180 → T181 순서로 수행한다.

## Phase 53: 미세 이동 클릭 생성 보정

- [x] T182 [US60] ModelEditorRefreshTest.kt·ModelEditorCreationTraceTest.kt에 MOUSE_CLICKED 없이 1px PRESS-RELEASE 생성, 큰 이동 드래그 거부, 후속 click 중복 방지, 모드 취소를 검증하는 회귀 테스트를 추가한다. (FR-156~FR-158)
- [x] T183 [US60] ModelDiagramCanvas.kt에 Child 생성 PRESS-RELEASE 제스처 상태와 화면 이동 임계치 판정을 추가하고, 유효 release에서만 생성 요청을 한 번 제출하도록 수정한다. (FR-156, FR-157)
- [x] T184 [US60] ModelDesignEditorProvider.kt에서 Child 생성 전용 ArrayDeque·NON_MODAL drain을 제거하고 기존 쓰기 명령을 즉시 호출하도록 단순화하되, 문서 갱신 예약과 유효성 검사를 보존한다. (FR-158)
- [x] T185 [US60] ChildCreationTrace.kt 및 관련 진단 로그를 새 PRESS-RELEASE 제출·중복 무시·드래그 거부 사유에 맞게 갱신한다. (FR-156~FR-158)
- [ ] T186 [US60] 전체 관련 테스트와 speckit-converge로 미세 이동 생성 보정의 명세·계획·구현 일치 여부를 확인한다. (FR-156~FR-158)


## 54단계: Wire 조건과 동반 이동

- [x] T187 [US61] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagram.kt와 ModelDiagramCanvas.kt, ModelDesignEditorProvider.kt에 기본 조건과 InitialState 라벨 제외를 적용한다. (FR-159, FR-160)
- [x] T188 [US62] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramEdits.kt의 동반 이동에 절대 기준점 이동과 크기 변경 제외를 적용한다. (FR-161, FR-162)
- [x] T189 [US61] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에 조건 기본값·표시 정책과 경로 평행이동 회귀를 추가하고 검증한다. (FR-159~FR-162)

T187 → T188 → T189 순으로 구현한다. 기존 파일 변경을 공유하므로 순차 실행한다. US61의 독립 기준은 조건 기본값과 라벨 정책, US62는 경로 점의 동일 변화량이다.

## 55단계: 조건 편집 차단과 정수 좌표

- [x] T190 [US63] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagram.kt·ModelDiagramCanvas.kt·ModelDesignEditorProvider.kt에 InitialState 조건 편집 차단을 적용한다. (FR-163)
- [x] T191 [US63] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramXmlText.kt와 ModelDesignEditorProvider.kt에 좌표 정수 저장과 편집 대상 정규화를 적용하고 weight를 보존한다. (FR-164, FR-165)
- [x] T192 [US63] src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 편집 정책과 좌표 정수화·비좌표 보존 회귀를 검증하고 specs/003-model-diagram-design/quickstart.md에 결과를 기록한다. (FR-163~FR-165)

T190 → T191 → T192 순으로 실행한다. 동일 Provider를 수정하므로 순차 진행한다. 독립 검증 기준은 InitialState 편집 불가와 모든 대상 좌표의 정수 쌍 저장이다.

## 56단계: 서비스 출력 순서 호환

- [x] T193 [US64] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceOutputOrder.kt에 transition 중복 병합·property 버킷 순서 함수를 구현한다. (FR-166~FR-168)
- [x] T194 [US64] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlExporter.kt에서 순서 함수를 사용하여 예제와 같은 순서로 출력한다. (FR-166~FR-169)
- [x] T195 [US64] src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt와 ModelServiceOutputOrderTest.kt에 예제 순서·중복·기본값 검증을 추가하고 specs/003-model-diagram-design/quickstart.md에 검증 결과를 기록한다. (FR-166~FR-169)

T193 → T194 → T195 순서로 진행한다. 독립 검증은 예제 5종 property 순서 및 Router transition 이름·목적지 순서 일치이다. 공유 변환기를 다루므로 순차 실행한다.

## 57단계: 서비스 직렬화 형식 고정

- [x] T196 [US65] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlWriter.kt에 명시적 속성 순서·공백 없는 빈 태그·JDOM 이스케이프를 구현한다. (FR-170~FR-172)
- [x] T197 [US65] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlExporter.kt의 최종 직렬화에 ModelServiceXmlWriter를 연결한다. (FR-170~FR-172)
- [x] T198 [US65] src/test/kotlin/com/look4counter/ModelServiceXmlWriterTest.kt에 문자열·속성 순서·특수문자·공백 보존 검증을 추가하고 specs/003-model-diagram-design/quickstart.md에 결과를 기록한다. (FR-170~FR-172)

T196 → T197 → T198 순서로 실행한다. 독립 검증은 예측한 서비스 문자열과의 일치 및 재파싱 후 값 보존이다. 순서 함수의 기존 회귀도 함께 실행한다.

## 58단계: 서비스 XML 마지막 개행 제거

- [x] T199 [US66] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlWriter.kt의 후행 LF를 제거하고 src/test/kotlin/com/look4counter/ModelServiceXmlWriterTest.kt의 기대 문자열을 갱신하여 검증한다. (FR-173)

단일 작업이며 기존 전체 문자열·값 보존 회귀를 실행한다.

## 59단계: 모달 Service Design 갱신

- [x] T200 [US67] src/test/kotlin/com/look4counter/ModelEditorRefreshTest.kt에 신규 모달 갱신과 기존 비모달 예약 중 Open Service 갱신·중복 예약 무효화 회귀를 추가한다. (FR-174~FR-176)
- [x] T201 [US67] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 갱신 예약에 안전한 모달 상태 캡처와 세대 기반 교체를 적용한다. (FR-174~FR-176)
- [x] T202 [US67] 관련 컴파일·회귀 및 specs/003-model-diagram-design/quickstart.md의 수동 Open Service 검증 절차·결과 기록과 최종 수렴 검토를 수행한다. (FR-174~FR-176)

T200 → T201 → T202 순서로 진행한다. 같은 모달 창이 유지되는 동안 노드 표시가 갱신되는지를 독립 기준으로 삼으며 기존 쓰기 불가·폐기 보호도 검증한다.

## 60단계: IDE 폴더 선택 창

- [x] T203 [US68] modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueModelEditorConfigurable.kt의 Service 폴더 선택에 IntelliJ 창 사용을 강제하고 기존 루트·상대 경로 보존을 검증한다. (FR-177)

T201 이후 T203을 반영하고 T202의 통합 검증에 포함한다.

## 61단계: 모델 파일 미사용 설정 항목

- [x] T204 [US69] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelEditorSettings.kt와 GlueModelEditorConfigurable.kt에 기본 해제인 No use .glue_uml_ad file 체크박스와 프로젝트별 저장을 연결한다. (FR-178~FR-180)
- [x] T205 [US69] src/test/kotlin/com/look4counter/ModelEditorSettingsTest.kt에서 기본값·적용·Reset·화면 재생성·기존 설정 호환을 검증하고 specs/003-model-diagram-design/quickstart.md에 결과를 기록한다. (FR-178~FR-180)

T204 → T205 순서로 진행한다. 독립 기준은 설정 UI와 영속 상태의 왕복이며 기존 모델 파일 동작은 변경하지 않는다.

## 62단계: Service 직접 Design 편집

- [x] T206 [US70] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramLayout.kt에 단계별 결정적 자동 배치와 순환·비연결 처리를 구현한다. (FR-183, FR-188)
- [x] T207 [US70] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt에 서비스 PSI의 메모리 다이어그램 투영과 오류 검증을 구현한다. (FR-182, FR-185)
- [x] T208 [US70] 같은 ServiceDiagramAdapter.kt에 Activity·transition·property·트랜잭션 부분 갱신 및 원본 메타데이터 보존을 구현한다. (FR-184, FR-186)
- [x] T209 [US70] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 문서 접근·쓰기 명령·갱신을 서비스 어댑터에 연결한다. (FR-181, FR-184, FR-185, FR-188)
- [x] T210 [US70] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelSourceEditorProvider.kt와 ServiceFiles.kt에 옵션별 수용·탐색·열기 분기를 적용한다. (FR-181)
- [x] T211 [US70] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceSaveListener.kt에서 직접 모드의 자동 내보내기를 중지한다. (FR-187)
- [x] T212 [US70] src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt와 ServiceDiagramLayoutTest.kt에 투영·부분 편집·배치·모드 분기·원본 보호 회귀를 구현한다. (FR-181~FR-188)
- [x] T213 [US70] 컴파일과 회귀를 실행하고 specs/003-model-diagram-design/quickstart.md에 모드 전환·재열기·자동 배치 검증을 기록한 뒤 수렴 점검한다. (FR-181~FR-188)

T206 → T207 → T208 → T209 → T210 → T211 → T212 → T213 순으로 진행한다. 기존 모델 모드 회귀도 확인한다. 독립 검증 기준은 서비스 파일 하나로 Design·Source의 의미가 일치하고 배치 파일이 생성되지 않는 것이다.

## 63단계: ELK Layered 자동 배치

- [x] T214 [US71] gradle/libs.versions.toml과 modeleditor/build.gradle.kts에 ELK Layered 0.12.0 실행 의존성을 고정한다. (FR-193)
- [x] T215 [US71] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramLayout.kt의 BFS를 실제 ELK 그래프·Layered 실행·노드/edge section 결과 변환으로 교체한다. (FR-189, FR-190, FR-192)
- [x] T216 [US71] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt에서 ELK 노드와 route를 메모리 좌표로 인코딩하고 수동 우회선을 제거한다. (FR-191, FR-193)
- [x] T217 [US71] src/test/kotlin/com/look4counter/ServiceDiagramLayoutTest.kt와 ModelServiceDirectEditorTest.kt에 ELK 배치·경로·재현성·원본 보존 회귀를 갱신한다. (FR-189~FR-193)
- [x] T218 [US71] 실제 ELK 의존성으로 컴파일·실행 검증하고 specs/003-model-diagram-design/quickstart.md에 결과를 기록한 뒤 최종 수렴 검토한다. (FR-189~FR-193)

T214 → T215 → T216 → T217 → T218 순서로 실행한다. 의존성 확보 전에는 실제 ELK 실행 통과를 주장하지 않는다. 기존 모델 모드와 서비스 편집 회귀도 확인한다.

## 64단계: ELK 런타임 패키징 보완

- [x] T219 [US71] build.gradle.kts의 모듈 합성 의존성을 implementation(project(...))에 연결하여 최종 runtimeClasspath에 ELK 전이 라이브러리를 포함한다. (FR-194, FR-195)
- [x] T220 [US71] 실제 Gradle compileKotlin·ELK 회귀·prepareSandbox·buildPlugin으로 컴파일과 패키징을 검증하고 specs/003-model-diagram-design/quickstart.md에 결과를 기록한다. (FR-193~FR-195)

T219 → T218의 컴파일/회귀 확인 → T220 순서로 진행한다. 프로젝트 합성 JAR뿐 아니라 ELK·전이 의존성 JAR의 lib 포함 여부를 독립 기준으로 삼는다.

## 65단계: wire 선분 겹침 완화

- [x] T221 [US72] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramLayout.kt에 연결별 포트·간격·실제 폭·원형 접점을 구현한다. (FR-196, FR-197)
- [x] T222 [US72] modeleditor의 ModelDiagram.kt·DiagramGeometry.kt·ServiceDiagramAdapter.kt·DiagramEdits.kt·ModelDesignEditorProvider.kt·ModelDiagramCanvas.kt에 메모리 접점 보존과 편집 전환을 연결한다. (FR-198~FR-200)
- [x] T223 [US72] src/test/kotlin/com/look4counter의 ServiceDiagramLayoutTest.kt·ModelServiceDirectEditorTest.kt·ModelDiagramTest.kt에 실제 겹침·접점·이동·기존 모델 회귀를 추가한다. (FR-196~FR-200)
- [x] T224 [US72] 실제 Gradle 컴파일·배치/편집 회귀·prepareSandbox_runIde·배포를 확인하고 specs/003-model-diagram-design/quickstart.md에 기록한 뒤 수렴을 점검한다. (FR-196~FR-200)

T221 → T222 → T223 → T224 순서로 실행한다. 선분의 양의 길이 공유와 한 점 교차를 구분하며 ELK뿐 아니라 표시 경로를 검사한다.

## 66단계: 회귀 검사 기대값 정합성

- [x] T225 [US72] ModelDiagramTest의 라벨 클릭 검사를 InitialState 이외의 원형 상태로 수행하고 ModelDiagramClipboardTest의 확대 붙여넣기 기대값에 정수 좌표 반올림 오차를 반영한 뒤 선택 회귀를 다시 실행한다. 기존 FR-160·FR-164와 FR-200을 함께 검증한다.

## 67단계: 서비스 직접 모드 구조 변경 차단

- [x] T226 [US73] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 serviceAdapter 기반 편집 정책을 정의하고 child/wire 수정·이동·resize·paste·delete 및 생성 콜백을 차단한다.
- [x] T227 [US73] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt와 ModelDiagramCanvas.kt에 생성 도구 및 구조 변경 입력 차단을 전달하고 선택·탐색은 보존한다.
- [x] T228 [US73] 실제 Gradle 컴파일과 수동 코드 경로 검토로 서비스 직접 모드 및 `.glue_uml_ad` 기존 모드 정책을 확인하고 quickstart.md에 결과를 기록한다.

T226 → T227 → T228 순서로 수행한다. 별도 UI 스위치 없이 ServiceDiagramAdapter 사용 여부를 정책의 기준으로 삼는다.

## 68단계: ELK 포트 순서 조정으로 교차 최소화

- [x] T229 [US74] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramLayout.kt의 고정 포트 좌표를 면 고정·위치 자유 포트로 변경하고 LayerSweep 교차 최소화 전략을 명시한다.
- [x] T230 [US74] 기존 결정적 transition 순서, 개별 접점 추출, 직각 경로 및 서비스 XML 무변경 요구와 새 배치 정책이 일치하는지 정적 검토하고 specs/003-model-diagram-design/quickstart.md에 제한을 기록한다.

T229 → T230 순서로 수행한다. 교차 제거를 보장하지 않고 ELK가 선택할 수 있는 접점 배치를 넓혀 가능한 범위에서 선 교차를 줄인다.

## 69단계: ELK 서비스 그래프 편집 활성화

- [x] T231 [US75] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 serviceAdapter를 차단하던 구조 편집 gate를 writable 문서 공통 조건으로 바꿔 생성·이동·Activity/Wire 수정·paste/delete를 활성화한다.
- [x] T232 [US75] ServiceDiagramAdapter의 기존 apply 경로 및 ModelDiagramCanvas의 2단계 Wire gesture를 대조해 변경 완료 뒤 XML 반영·ELK refresh, 미완성 초안 무저장을 확인하고 specs/003-model-diagram-design/quickstart.md에 기록한다.
- [x] T233 [US75] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 Activity/transition 구조 변경 후 service projection을 무효화해 ELK 재배치를 수행하고 위치 변경은 메모리에서 유지한다.

T231 → T233 → T232 순서로 진행한다. 자동 배치 좌표를 서비스 XML에 저장하지 않으며 읽기 전용 원본은 편집할 수 없다.

## 70단계: Service XML Commit 전 검증 및 일괄 반영

- [x] T234 [US76] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt에 pending 상태·원본 revision 검사·정합성 사전 검증을 추가하고 성공 시에만 원본을 적용하며 실패 시 pending 편집을 보존한다.
- [x] T235 [US76] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 서비스 writeModel을 메모리 staging 방식으로 전환하고 Commit callback에서 검증·단일 XML 적용·ELK 재배치를 수행한다.
- [x] T236 [US76] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt에 서비스 모드 한정 Commit 버튼과 pending 상태 활성화를 연결하고 quickstart.md에 검증을 기록한다.

T234 → T235 → T236 순서로 진행한다. Source 문서 무변경과 검증 실패 원본 보존을 확인하고 `.glue_uml_ad` 즉시 저장 동작을 대조한다.

## 71단계: Rollback·pending 변경 표시·닫기 확인

- [x] T237 [US77] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt 및 ModelDiagram.kt에 기준/대기 비교로 추가·삭제·수정 node/wire 변경 집합을 계산하고 Rollback으로 pending projection을 폐기한다.
- [x] T238 [US77] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 초록 추가·빨강 삭제 ghost·주황 수정 outline/path 표현을 적용한다.
- [x] T239 [US77] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt와 ModelDesignEditorProvider.kt에 Rollback 버튼과 재배치 흐름을 연결한다.
- [x] T240 [US77] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt 및 modeleditor/src/main/resources/META-INF/modeleditor.xml에 VirtualFilePreCloseCheck를 등록해 pending close 확인·취소를 제공하고 quickstart.md에 검증을 기록한다.

T237 → T238 → T239 → T240 순서로 수행한다. 기존 Source XML은 롤백으로 변경하지 않고, close 확인에서 계속 편집을 고르면 열린 탭과 pending 변경을 보존한다.

## 72단계: 서비스 직접 편집 Undo/Redo 분리

- [x] T241 [US78] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt에 projection snapshot undo/redo 스택을 구현하고 restore 시 service 원본 tag 연결을 재구성한다.
- [x] T242 [US78] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에서 서비스 staged write를 WriteCommandAction에서 분리하고 일반 `.glue_uml_ad` 명령·Commit 원본 XML 명령은 유지한다.
- [x] T243 [US78] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt 및 ModelDesignEditorProvider.kt에 전용 Undo/Redo 버튼과 서비스 Design의 Ctrl+Z/Ctrl+Shift+Z 동작을 연결한다.
- [x] T244 [US78] Undo/Redo 뒤 표시·Commit 가능 상태를 갱신하고 Commit/Rollback에서 history를 초기화하며 specs/003-model-diagram-design/quickstart.md에 수동 검증 결과를 기록한다.

T241 → T242 → T243 → T244 순서로 진행한다. 서비스 staged 명령은 원본 파일의 Undo stack과 분리하고 Commit은 XML에 하나의 일반 편집 명령으로 남긴다.
## 73단계: 대기 편집 Undo/Redo 비활성화와 툴바 라벨

- [x] T245 [US79] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt의 staged undo/redo history 및 restore 경로를 제거하고 service write는 XML 명령 기록과 분리된 projection write로 유지한다.
- [x] T246 [US79] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt와 ModelDesignEditorProvider.kt에서 서비스 Undo/Redo 버튼·단축키를 제거하고 Magnetic 및 Properties 텍스트 라벨을 표시한다.
- [x] T247 [US79] `.glue_uml_ad` 및 Commit 후 Source XML의 일반 Undo/Redo 경로를 보존하는지 코드 경로를 확인하고 quickstart.md, spec.md, plan.md를 현재 동작으로 맞춘 뒤 compileKotlin·buildPlugin·diff 검사를 수행한다.

T245 → T246 → T247 순으로 수행한다. 미커밋 projection은 Undo/Redo 기록에 들어가지 않고 Commit 또는 Rollback으로만 확정/폐기하며, Commit된 XML은 Source의 일반 Undo/Redo로 다룬다.
## 74단계: Magnetic·Properties 아이콘 제거

- [x] T248 [US80] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ChildCreationToolbar.kt에서 Magnetic 및 Properties 액션의 아이콘을 제거하고 텍스트 라벨만 표시한다.
- [x] T249 [US80] `:modeleditor:compileKotlin` 및 플러그인 빌드, `git diff --check`로 UI 액션 변경을 확인하고 quickstart.md에 결과를 기록한다.

T248 → T249 순으로 수행한다. 나머지 툴바 액션 아이콘은 그대로 둔다.