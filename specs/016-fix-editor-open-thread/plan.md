# 구현 계획: 편집기 열기 스레드 오류

작성일: 2026-11-03 | 명세: [명세](spec.md)

## 요약

쿼리 편집기의 외부 탐색 진입점에서 쓰기 의도 잠금과 트랜잭션 안전성을 확인하고 필요한 경우 애플리케이션 큐로 이동을 예약한다. 모델 편집기의 기존 지연 갱신은 실제 잠금 없는 생성과 서비스 모드 테스트로 검증하고 결함이 발견되면 수정한다.

## 기술 문맥

Kotlin/JVM, IntelliJ Platform 2025.3.6.1, Swing, Gradle, JUnit의 BasePlatformTestCase를 사용한다. 저장소의 두 편집기 모듈과 루트 통합 테스트가 범위다. 데이터 저장 형식과 외부 인터페이스는 유지한다. 이벤트 스레드에서 동기 대기하거나 읽기 잠금에서 쓰기 잠금을 승격하지 않는다. 열기·탐색으로 불필요한 쓰기 액션을 추가하지 않는다.

## 헌장 점검

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. AGENTS.md의 한국어 문서와 전체 speckit 단계 준수, 사용자 기존 변경 보존을 적용한다. 설계 전후 위반 없음.

## 구조와 변경 지점

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt: ID·오프셋 이동과 초기 갱신의 안전성, 연속 요청·폐기 처리.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryResultTypeEditor.kt: 생성 시 Java 참조 문서 초기화 조사.
- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryNavigation.kt: 외부 이동과 검색 진입점 조사.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDesignEditorProvider.kt: 생성·선택 통지·갱신의 잠금 검증.
- src/test/kotlin/com/look4counter/EditorOpenThreadTest.kt: 두 모듈의 잠금 없는 열기, 연속 이동, 폐기와 원문 보존 회귀 검증.

## 단계

1. 조사 결과와 호출 계약을 문서화하고 재현 테스트를 작성한다.
2. 테스트를 실패시켜 원인을 확인한 후 필요한 경로만 수정한다.
3. 두 모듈의 관련 테스트와 컴파일을 실행하고 수렴 점검한다.

## 복잡성

신규 공용 서비스나 모듈은 추가하지 않는다. 편집기 내부의 요청 번호로 마지막 탐색을 보존한다.

## 재현에 따른 설계 보완

실제 잠금 해제 테스트에서 두 Design 제공자의 문서 획득이 읽기 접근 오류를 발생시켰다. 제공자의 생성 범위에 WriteIntentReadAction을 적용한다. 이는 강한 쓰기 액션이 아니며 초기 UI 생성과 PSI 접근의 플랫폼 계약을 보장한다. 같은 파일 열기에서 생성되는 QuerySourceEditorProvider.kt와 ModelSourceEditorProvider.kt도 동일하게 검증한다. QueryNavigation.kt의 두 open 진입점은 검색 및 파일 열기 전에 안전한 문맥으로 예약하여 모델 대화상자의 쿼리 탐색까지 보호한다.

## 파일 열기 호환성 보완 계획

FR-006을 위해 QueryDesignEditorProvider.kt, QuerySourceEditorProvider.kt, ModelDesignEditorProvider.kt, ModelSourceEditorProvider.kt의 생성 래퍼를 ApplicationManager.getApplication().runWriteIntentReadAction으로 교체한다. 기존 SDK에서 이 호출은 원래 래퍼의 위임 대상이므로 동기 반환·예외 전달·쓰기 의도 잠금 계약을 유지한다. 읽기 액션 또는 강한 쓰기 액션으로 대체하지 않는다. EditorOpenThreadTest와 EditorMultiViewTest를 실행하고 컴파일된 제공자의 바이트코드에서 변경된 정적 오버로드 참조가 없는지 확인한다. buildPlugin으로 설치 가능한 결과물을 만든다. 헌장 및 설계 후 점검에서 추가 위반은 없다.
이 보완 계획은 앞 절의 WriteIntentReadAction 정적 래퍼 선택을 대체하며 잠금 범위와 생성 시점은 그대로 유지한다.
