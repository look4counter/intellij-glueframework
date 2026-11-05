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


