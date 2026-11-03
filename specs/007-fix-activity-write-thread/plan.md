# 구현 계획: Activity 편집 스레드 오류
**날짜**: 2026-10-26 | **명세**: [spec.md](spec.md)

## 개요
모델 편집 요청을 쓰기 의도 잠금과 쓰기 안전 문맥이 있는 UI 실행으로 연결한다. 모달 창 전체를 쓰기 액션으로 감싸지 않는다. 기존 확인·취소·충돌 검사와 쓰기 명령을 유지한다.

## 기술 배경
- 언어: Kotlin 2.3.20, Java 21 클래스 형식.
- 의존성: IntelliJ Platform 2025.3.6.1, Swing, PSI, TransactionGuard.
- 저장: 기존 XML 형식 유지.
- 검증: BasePlatformTestCase의 실제 캔버스 입력과 기존 속성 테스트.
- 대상: IntelliJ 데스크톱 플러그인 modeleditor.
- 제약: UI 스레드 여부만으로 쓰기 권한을 추정하지 않는다.
- 성능: 잠금이 있으면 즉시 실행하고 없으면 이벤트 큐에 한 번 예약한다.
- 범위: 세 종류의 속성 편집과 공통 화면 갱신 가드.

## 규약 확인
헌법은 미작성 템플릿이므로 구체 규칙 검사를 생략한다. AGENTS.md의 한글 문서 및 speckit 순서를 따른다. 설계 전후 위반 없음.

## 조사 및 설계 단계
1. 로컬 SDK의 commitDocument와 UI 콜백 호출 체인을 조사한다.
2. 잠금이 없으면 Application.invokeLater로 쓰기 안전 모달리티에 재예약한다.
3. Activity·Transition·Diagram 편집 메서드 첫 진입에 공통 가드를 적용한다.
4. refresh의 기존 안전 문맥 검사에 쓰기 의도 잠금 검사를 추가한다.
5. 잠금 없는 EDT에서 실제 요청을 검증하고 기존 속성 테스트를 실행한다.
6. 기존 Transition 검증은 빈 조건의 원문 저장과 화면 기본값을 구분한다. 기존 ModelDiagram 파서 및 003 계획의 빈 조건 success 표시 정책을 유지한다.

## 구조
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt: 진입 가드와 갱신 조건.
- src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt: 회귀 검증.
- specs/007-fix-activity-write-thread/: 명세·조사·설계·계약·검증·작업 목록.

## 복잡성 점검
새 의존성·서비스·저장 개체 없이 기존 이벤트 예약 방식을 재사용한다.
