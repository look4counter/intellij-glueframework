# 실행 작업: 화면 이동 이미지 재사용

## 1단계: 준비

- [x] T001 명세와 설계를 검토하고 FR-001~005를 계획에 대응한다.

## 2단계: 기반 검증

- [x] T002 [US1] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 연속 이동 이미지 재사용·종료·바깥 영역 회귀 검증을 추가한다.

## 3단계: 사용자 이야기 1

- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 모델 렌더링을 분리하고 패닝 이미지 생성·재사용·해제·무효화와 영역 밖 보충을 구현한다. FR-001~005.

## 4단계: 검증 및 마무리

- [x] T004 관련 성능·미니맵·확대 테스트를 실행하고 specs/022-cache-navigation-image/quickstart.md에 결과를 기록한다.
- [x] T005 명세·계획·코드의 수렴 여부를 점검한다.

## 의존성과 전략

T001 → T002 → T003 → T004 → T005 순으로 진행한다. 같은 캔버스와 검증 흐름을 다루므로 병렬 작업은 적용하지 않는다. 사용자 이야기 1을 완성한 뒤 관련 회귀 검증으로 마무리한다.



## 5단계: 패닝 표시 경계 수정

- [x] T006 [US1] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에서 네 방향 패닝 중 표시의 경계 및 가시성을 검증한다. FR-006, SC-004.
- [x] T007 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 미니맵 표시 사각형을 콘텐츠 안쪽으로 제한한다. FR-006.
- [x] T008 관련 테스트와 플러그인 빌드를 실행하고 수렴 점검 결과를 quickstart.md에 기록한다.

의존성: T006 → T007 → T008. 기존 이미지 재사용 검증도 함께 수행한다.

## 6단계: 종료 상태 중심 보정

- [x] T009 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt의 FinalState 안쪽 원 오프셋을 2.5로 수정하고 중심 좌표를 검토한다. FR-007, SC-005.
- [x] T010 관련 캔버스 테스트 및 빌드 후 FR-006~007과 계획의 수렴 여부를 확인하고 quickstart.md에 기록한다.

의존성: 명세·명확화·계획·일관성 검토 → T009 → T010. T008의 최종 검증에 함께 포함한다.



## 7단계: 종료 상태 실제 렌더링 중심

- [x] T011 [US1] src/test/kotlin/com/look4counter/ModelDiagramPerformanceTest.kt에 내부·외부 원 픽셀 중심 회귀 검증을 추가하고 수정 전 실패를 확인한다. FR-008, SC-006.
- [x] T012 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 종료 상태 윤곽의 픽셀 정규화를 제거하고 그래픽 상태를 복원한다. FR-008.
- [x] T013 관련 테스트·빌드와 수렴 점검을 완료하고 quickstart.md에 결과를 기록한다.

의존성: T011 → T012 → T013. 명세의 모호성은 없고 요구사항과 계획·작업의 대응을 확인했다.



## 8단계: 패닝 중 상태 조회 비용 개선

- [x] T014 [US1] src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt와 src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt에 반복 조회 재사용·수정 무효화·패닝 중 붙여넣기 제한 회귀 검증을 추가한다. FR-009~010, SC-007~008.
- [x] T015 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ServiceDiagramAdapter.kt에 변경 비교 캐시와 수명 관리를 구현하고 ModelDesignEditorProvider.kt의 붙여넣기 가능 여부에 드래그 검사를 추가한다. FR-009~010.
- [x] T016 관련 서비스·클립보드·캔버스 테스트와 buildPlugin을 실행하고 specs/022-cache-navigation-image/quickstart.md에 검증 및 수렴 결과를 기록한다.

의존성: T014 → T015 → T016. 새 작업 3개는 사용자 이야기 1에 속한다. 두 테스트 파일의 작성은 병렬 가능하지만 동일한 검증 흐름으로 순차 진행한다. 독립 검증은 동일 편집본 100회 조회, 수정 및 원복, 패닝 시작·종료의 붙여넣기 상태이다.

## 9단계: 미니맵 위치 동기화

- [x] T017 [US1] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에 네 방향 연속 패닝 중 표시 픽셀 변화, 전체 범위, 복귀와 종료 회귀 검증을 추가하고 수정 전 실패를 확인한다. FR-011, SC-009.
- [x] T018 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 현재 투영을 갱신하고 시작 투영의 배경 이미지를 변환하여 재사용한다. FR-006, FR-011.
- [x] T019 관련 미니맵·성능 테스트와 buildPlugin을 실행하고 specs/022-cache-navigation-image/quickstart.md에 검증 및 수렴 결과를 기록한다.

의존성: T017 → T018 → T019. 독립 검증은 작은 모델에서 네 방향 중간 프레임을 비교한다. 같은 렌더링 경로이므로 순차 진행하며 병렬 실행 대상은 없다. 사용자 이야기 1의 동기화 수정을 최소 범위로 완료한다.

## 10단계: 패닝 미니맵 배율 유지

- [x] T020 [US1] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에 시작 전후 동일 투영 검증을 추가하고 연속·큰 이동 검증을 고정 배율·오프셋 기준으로 갱신한다. FR-012, SC-010.
- [x] T021 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 두 배 여백과 배경 변환을 제거하고 시작 투영 유지 및 화면 표시의 개별 경계 제한을 구현한다. FR-006, FR-011~012.
- [x] T022 회귀 테스트와 수렴 점검을 수행하고 specs/022-cache-navigation-image/quickstart.md에 검증 결과 및 빌드 환경 제약을 기록한다.

순서: T020 → T021 → T022. 단일 사용자 이야기의 렌더링 변경으로 순차 실행한다. 독립 검증 기준은 시작과 이동 중 배율·오프셋 유지, 표시 이동과 경계 가시성이다. 최소 범위는 사용자 이야기 1이며 병렬 실행 대상은 없다.

## 11단계: 균등 여백과 패닝 요소 이동

- [x] T023 [US1] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt에 일반·스크롤 화면의 균등 여백과 네 방향 요소 픽셀 이동 검증을 추가하고 연속·큰 이동 검증을 새 투영 정책으로 갱신한다. FR-013~014, SC-011~012.
- [x] T024 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/DiagramMiniMap.kt의 여백을 통일하고 ModelDiagramCanvas.kt의 표시 중심 갱신과 캐시 이미지 평행 이동을 구현한다. FR-012~014.
- [x] T025 관련 회귀 검증과 수렴 점검 후 specs/022-cache-navigation-image/quickstart.md에 검증 결과 및 환경 제약을 기록한다.

의존성: T023 → T024 → T025. 단일 사용자 이야기와 공통 렌더링 검증이므로 순차 실행한다. 독립 검증은 균등 여백, 고정 배율에서 요소·화면 표시 이동 및 캐시 재사용이다. 최소 범위는 사용자 이야기 1이며 병렬 작업은 없다.

## 12단계: 패닝 중 자동 배율

- [x] T026 [US1] src/test/kotlin/com/look4counter/ModelDiagramMiniMapTest.kt의 이동 검증을 현재 투영 기준으로 갱신하고 전체 범위·자동 배율·요소 픽셀 크기·원점 복귀를 검증한다. FR-015, SC-013.
- [x] T027 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에서 현재 범위에 맞춘 배율을 계산하고 캐시 배경의 배율·오프셋 변환을 구현한다. FR-015.
- [x] T028 관련 회귀 검증과 수렴 점검 후 specs/022-cache-navigation-image/quickstart.md에 결과와 빌드 제약을 기록한다.

의존성: T026 → T027 → T028. 사용자 이야기 1의 공통 렌더링이므로 순차 진행한다. 독립 검증은 시작 무변화, 큰 이동의 자동 축소, 복귀 시 확대, 전체 범위와 이미지 재사용이다. 최소 범위는 사용자 이야기 1이며 병렬 대상은 없다.
