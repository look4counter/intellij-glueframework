# 구현 계획: Wire 추가 쓰기 스레드 보호

작성일: 2026-11-18 | 명세: [spec.md](spec.md)

## 요약

기존 deferModelEdit를 createWire 진입에 적용한다. expectedStamp 기본값으로 moveStamp를 최초 호출에서 고정하고 지연 람다에 전달한다. 실행 전 폐기·유효성 및 기존 편집 가능·모드·문서 일치 검사를 유지한다.

## 기술 문맥

Kotlin, JDK 21, IntelliJ IDEA 2025.3.6.1, Gradle 9.7.1 기반 데스크톱 플러그인이다. 저장은 기존 XML 문서이며 새 의존성이나 형식 변경은 없다. BasePlatformTestCase 실제 이벤트로 검증한다. 대기 시간을 추가하지 않고 안전한 UI 큐를 재사용한다. 범위는 Wire 추가 진입과 회귀 테스트다.

## 헌장 점검

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. 설계 전·후 모두 저장소 한글 문서 및 speckit 절차를 준수한다.

## 조사 및 설계

[research.md](research.md), [data-model.md](data-model.md), [입력 계약](contracts/wire-create.md)을 따른다. 알려지지 않은 기술 선택은 없다.

## 파일 및 실행 순서

1. src/test/kotlin/com/look4counter/ModelActivityCreationTest.kt에 잠금 없는 클릭·놓기, 취소 조건, 실행 취소 회귀를 추가하고 수정 전 실패를 확인한다.
2. modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt의 createWire에 생명주기 가드·안전 문맥 예약·고정 stamp를 적용한다.
3. 생성·연결·Service 회귀를 실행하고 quickstart.md에 결과를 기록한다.

## 후속 설계: 선택 편집 보호

ModelDesignEditorProvider.kt의 deleteSelection·copySelection·pasteSelection도 deferModelEdit를 문서 커밋 전에 호출한다. 기본 매개변수로 최초 문서 stamp 및 삭제·복사 선택 노드 집합·선택 Wire를 캡처하고 예약 람다에 전달한다. 폐기·유효성 검사 후 안전 문맥에서 provider의 사용 가능 상태와 원래 stamp 및 선택을 재검증한다. 붙여넣기는 전달된 DiagramClipboardData와 현재 클립보드의 동일성을 검사한다. 선택 변경은 삭제·복사만 취소한다. 기존 XML 생성과 오프셋 계산·pasteCount 갱신은 보존한다.

테스트 파일은 src/test/kotlin/com/look4counter/ModelDiagramClipboardTest.kt다. 잠금을 명시적으로 해제한 provider 호출로 세 경로의 수정 전 오류를 재현하고 지연 취소·실행 취소를 검증한다. 기존 클립보드와 Wire 생성 및 열기 회귀를 실행한다. 헌장과 저장소 규칙 재점검 결과 충돌이 없다.
