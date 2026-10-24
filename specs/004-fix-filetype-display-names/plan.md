# 구현 계획: 파일 형식 표시 이름 충돌 수정

**날짜**: 2026-10-24
**명세**: [기능 명세](spec.md)

## 요약

두 파일 형식에서 getDisplayName()을 재정의하여 기존 고유 이름을 반환한다. XML 언어 및 파일 연결은 유지한다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ IDEA 2025.3.6.1, Gradle 기반 데스크톱 플러그인이다. 기존 BasePlatformTestCase 파일 형식 테스트를 사용한다. 저장소나 외부 서비스 변경은 없다. 상수 반환이므로 추가 성능 비용은 없다.

## 헌장 점검

헌장은 미작성 템플릿이므로 원칙 검증을 생략한다. 저장소 지침에 따라 한글 문서를 작성하고 speckit의 모든 단계를 순서대로 수행한다. 설계 후에도 위반 사항은 없다.

## 0단계: 조사

[조사 결과](research.md)에 상속된 표시 이름과 해결 방법을 기록한다. 새로운 기술 선택과 미해결 사항은 없다.

## 1단계: 설계

[데이터 모델](data-model.md), [표시 계약](contracts/display-names.md), [검증 안내](quickstart.md)를 따른다.

## 프로젝트 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/GlueSqlFileType.kt: 표시 이름 재정의
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueUmlAdFileType.kt: 표시 이름 재정의
- src/test/kotlin/com/look4counter/EditorFileTypeTest.kt: 기본 XML을 포함한 표시 이름 유일성 검증
- specs/004-fix-filetype-display-names/: 명세, 계획, 작업 및 검증 문서

## 검증

회귀 테스트를 추가하고 수정 전 실패를 확인한 후 수정한다. EditorFileTypeTest 전체를 실행하여 이름과 기존 동작을 확인한다. git diff --check로 공백 오류를 확인한다.
