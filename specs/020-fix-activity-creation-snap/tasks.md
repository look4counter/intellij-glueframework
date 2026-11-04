# 작업 목록: Activity 추가 스레드 및 Snap

## 1단계: 준비

- [x] T001 specs/020-fix-activity-creation-snap/spec.md 및 설계를 검토해 요구사항과 헌장 준수를 확인한다.

## 2단계: 사용자 이야기 1 — 안전한 추가

독립 검증: 쓰기 의도 잠금 없는 클릭·놓기에서 한 번만 추가하고 취소·폐기는 반영하지 않는다.

- [x] T002 [US1] src/test/kotlin/com/look4counter/ModelActivityCreationTest.kt에 쓰기 의도 잠금 없는 추가·취소·폐기·실행 취소 검증을 작성한다.
- [x] T003 [US1] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt에 추가 요청의 안전한 위임 및 모드 세대 재검증을 구현한다. FR-001, FR-002, FR-005.

## 3단계: 사용자 이야기 2 — 추가 Snap

독립 검증: Ghost와 제출 좌표가 정렬·간격 보정 결과와 같고 비활성화·Alt는 원래 좌표를 사용한다.

- [x] T004 [US2] src/test/kotlin/com/look4counter/ModelActivityCreationTest.kt에 정렬·동일 간격·확대·축소·Alt 및 Ghost와 확정 좌표 검증을 작성한다.
- [x] T005 [US2] modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt에 공통 미리보기 Snap, 설정 및 Alt 우회와 안내선 부분 갱신·정리를 구현한다. FR-003, FR-004, FR-005.

## 4단계: 통합 검증

- [x] T006 specs/020-fix-activity-creation-snap/quickstart.md의 모델 편집 회귀 검증과 build/distributions/ 설치 패키지 생성을 수행하고 결과를 기록한다.

## 의존성과 구현 전략

T001 → T002 → T003 → T004 → T005 → T006 순서로 진행한다. 이야기 1을 오류 복구 최소 단위로 먼저 검증하고 Snap을 통합한다. 두 이야기의 구현 파일은 다르지만 회귀 테스트 파일과 추가 경로를 공유하므로 순차 진행한다.
