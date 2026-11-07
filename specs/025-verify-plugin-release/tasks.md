# 실행 작업: 정식 빌드와 설치 산출물 검증

## 1단계: 준비

- [x] T001 specs/025-verify-plugin-release/spec.md·설계·품질 점검을 검토하고 원본 실패 결과를 build/reports에 보존한다.

## 2단계: 기반

- [x] T002 specs/025-verify-plugin-release/plan.md와 tasks.md를 speckit-analyze로 검토해 모든 요구사항의 대응을 확인한다.

## 3단계: 사용자 이야기 1

독립 검증: 두 수정 사례와 전체 검증이 실패·오류·건너뜀 없이 통과한다.

- [x] T003 [US1] src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt의 Wire 테스트에 실제 편집기 배치와 화면 영역 사전 검사를 추가하고 기존 저장·Undo/Redo 검사를 보존한다. FR-002.
- [x] T004 [US1] src/test/kotlin/com/look4counter/ModelDiagramTest.kt의 종료 상태 테스트에서 빈 링 상하좌우 배경을 검사하고 검정 중심·캔버스 크기 검사를 보존한다. FR-003.
- [x] T005 [US1] build.gradle.kts의 기존 test를 두 사례와 전체 범위로 실행해 build/test-results/test 결과 및 SVG 오류 재현 여부를 확인한다. FR-001~003, SC-001.

## 4단계: 사용자 이야기 2

독립 검증: 최신 ZIP 생성·구조 검사·격리 IDE 기동 결과를 각각 확인한다.

- [x] T009 [US2] src/main/resources/META-INF/plugin.xml의 표시 이름·소개·공급자·주소를 실제 정보로 갱신하고 ID·버전은 보존한다. 변경 후 전체 테스트와 ZIP·구조 검증을 다시 실행한다. FR-006, FR-004.
- [x] T006 [US2] build/distributions/intellij-glueframework-1.0.3.zip을 buildPlugin으로 생성하고 verifyPluginStructure·검색 옵션 생성의 격리 IDE 기동·필수 항목·최신 클래스·해시를 확인한다. FR-004, SC-002~003.

## 5단계: 마무리

- [x] T007 README.md와 specs/025-verify-plugin-release/quickstart.md에 JDK 21·재현 명령·검증 결과·수동 확인 한계를 기록한다. FR-005, SC-003.
- [x] T008 specs/025-verify-plugin-release/spec.md·plan.md·tasks.md와 최종 상태를 speckit-converge로 대조하고 남은 작업을 완료한다.

## 의존성과 실행 전략

T001→T002→T003→T004→T009→T005→T006→T007→T008 순서로 진행한다. T009는 최초 구조 검사에서 확인된 오류를 명세·계획에 반영한 보완 작업이다. US1을 먼저 검증하고 US2를 확인한다. 서로 다른 두 테스트 수정은 독립적이지만 순차 실행한다. 검증을 삭제하거나 무시하지 않는다.
