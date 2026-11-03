# 조사 결과
## 결정과 근거
쓰기 의도 잠금이 없는 UI 콜백은 Application.invokeLater로 재예약한다. 쓰기 안전하지 않은 모달리티는 기존 refresh처럼 비모달 상태로 예약한다.

로컬 SDK 바이트코드에서 commitDocument → TransactionGuardImpl.assertWriteActionAllowed → assertWriteIntentLockAcquired → ThreadingAssertions의 보고된 오류 문구를 확인했다. Activity 더블클릭은 commitDocument를 직접 실행하며 잠금을 보장하지 않는다. Diagram·Transition에도 동일 경로가 있다.

[JetBrains 변경 안내](https://platform.jetbrains.com/t/changes-in-threading-model-in-the-intellij-platform-2026-1/3800)는 사용자 입력 핸들러의 쓰기 의도 잠금 제거를 설명한다. 문제 PC의 IDE 버전은 아직 없어 환경 차이는 가설이다.

speckit-plan의 연구 에이전트가 로컬 SDK 호출 체인을 읽기 전용으로 별도 확인했다.

## 대안
- 창 전체를 WriteAction으로 감싸기: 모달 대기 중 강한 쓰기 잠금 유지 위험으로 제외.
- SwingUtilities.invokeLater만 사용: 필요한 잠금을 보장하지 않아 제외.
- JavaReferenceEditorUtil 수정: class 셀 편집 시 호출되므로 창 진입 오류 수정에서 제외.

## 기존 검증의 기대값 보정

전체 속성 검증에서 기존 빈 조건 테스트 1개가 실패했다. 변경 전 HEAD의 ModelDiagram.kt도 빈 조건을 success로 읽으며, 003 계획의 조건 기본값 정책도 같다. 따라서 운영 파서 동작은 유지하고 테스트가 원문의 빈 guardcondition 저장과 화면의 success 표시를 각각 검증하도록 보정한다.
