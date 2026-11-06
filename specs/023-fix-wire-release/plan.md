# 구현 계획: Wire 놓음 입력 보완

작성일: 2026-11-06. 명세: [spec.md](spec.md)

## 요약

ModelDiagramCanvas의 Wire 선택 처리를 공통 함수로 분리한다. Wire 모드에서 왼쪽 누름을 기록하고 놓음에서 선택한다. 기존 suppressEditClick을 사용해 후속 클릭을 제외하며 단독 클릭 경로는 유지한다. 모드 전환과 clearGesture에서 누름 상태를 해제해 오래된 입력을 차단한다.

## 기술 맥락

Kotlin, Swing MouseAdapter, IntelliJ 플랫폼 BasePlatformTestCase를 사용한다. 저장소나 외부 의존성 추가는 없다. 상태는 캔버스별 유효 누름 여부 하나이며 이벤트 처리는 상수 시간이다. Windows IntelliJ 플러그인이 대상이다.

## 헌장 점검

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. 한글 문서와 speckit 순서를 준수한다. 설계 후 재점검 결과 위반은 없다.

## 구조와 구현 단계

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramCanvas.kt: 누름 상태, 놓음 확정, 후속 클릭 제외, 공통 선택 함수와 상태 초기화.
- src/test/kotlin/com/look4counter/ModelDiagramWireCreationTest.kt: 클릭 없는 이동 입력, 후속 클릭 중복, 콜백 중 모델 갱신, 무효 위치, 권한, 취소, 모드 전환, 모델 교체 및 확대 좌표 회귀 검증.
- 기존 ModelEditorFeaturesTest와 미니맵·성능·확대 테스트: 기존 단독 클릭·스냅·패닝·렌더링 동작 확인.

순서: 회귀 검증 작성 → 누름·놓음 수명과 공통 선택 구현 → 관련 검증 → 수렴 점검. 단일 캔버스 경로이므로 순차 진행한다.

## 설계 산출물

research.md, data-model.md, contracts/wire-input.md, quickstart.md를 참조한다. 복잡성 예외는 없다.
