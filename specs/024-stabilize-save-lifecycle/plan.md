# 구현 계획: 저장 생명주기 안정화

작성일: 2026-11-07
명세: [spec.md](spec.md)

## 요약

자동 생성 대상 문서의 미저장·읽기 전용 상태를 쓰기 직전에 검사한다. 직접 편집의 탭 전환·Commit·저장·재열기·충돌·Rollback은 통합 회귀 테스트로 검증한다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ IDEA 2025.3.6.1, JUnit와 BasePlatformTestCase를 사용한다. 저장소 기존 Gradle 설정을 유지한다. XML과 IDE Document가 저장 매체이며 UI를 막는 추가 입출력 없이 기존 쓰기 액션 안에서 상태를 검사한다.

## 헌장 검사

헌장은 미작성 템플릿이므로 규범 검사를 생략한다. AGENTS.md의 한글 문서와 speckit 전 단계를 준수한다. 설계 후 재검토 결과 위반 없음.

## 조사와 설계

[research.md](research.md), [data-model.md](data-model.md), [계약](contracts/save.md)을 따른다. 미저장 문서는 결과가 같아도 거부한다. 충돌의 자동 병합과 강제 덮어쓰기는 제공하지 않는다. 기존 IllegalArgumentException 알림 경로를 재사용한다.

## 코드 구조와 단계

1. src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt에 보호 회귀 테스트를 먼저 추가한다.
2. modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceSaveListener.kt의 cached document 변경 전에 보호 조건을 추가한다.
3. src/test/kotlin/com/look4counter/ModelServiceDirectEditorTest.kt에 저장 생명주기와 Source 충돌 회귀 검증을 추가한다. 기존 보호가 통과하면 제품 코드를 불필요하게 바꾸지 않는다.
4. 관련 쿼리·모델 회귀 검증과 buildPlugin을 시도하고 [quickstart.md](quickstart.md)에 결과와 한계를 기록한다.

## 복잡성 검토

새 의존성과 파일 형식 변경 없음. 실제 IDE 수동 조작은 자동 테스트와 구분한다.
