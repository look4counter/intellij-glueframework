# 구현 계획: 정식 빌드와 설치 산출물 검증

작성일: 2026-11-07
명세: [spec.md](spec.md)

## 요약

기존 Gradle 빌드와 IDE 포함 JBR 21을 사용한다. 전체 400개 테스트에서 확인된 두 검증 오류의 준비 조건·픽셀 위치만 수정한다. 제품 소스와 의존성 버전은 유지한다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ IDEA 2025.3.6.1, Gradle 9.7.1, JBR 21.0.11과 기존 JUnit 기반 플랫폼 테스트를 사용한다. 저장소 안 GRADLE_USER_HOME으로 의존성 및 검증 파일을 관리한다. 별도 성능 목표는 추가하지 않으며 원래 400개 검증의 의미를 유지한다.

## 헌장 검사

헌장은 미작성 템플릿이므로 규범 검사는 생략한다. 모든 문서는 한글로 작성하고 speckit의 각 단계를 수행한다. 설계 후 재검토에서도 위반이 없다.

## 조사와 설계

[research.md](research.md), [data-model.md](data-model.md), [계약](contracts/verification.md)을 따른다. 새 기술 선택이나 미해결 기술 질문은 없으며 로컬 실패 로그와 기존 구현을 조사했다.

## 구조와 단계

1. src/test/kotlin/com/look4counter/ModelDiagramPropertiesTest.kt의 Wire 저장·Undo/Redo 테스트에서 편집기·스크롤·뷰포트를 배치하고 양 끝점이 화면 안에 있음을 확인한다. 기존 미리보기·생성 횟수·참조·Undo/Redo 검사는 보존한다.
2. src/test/kotlin/com/look4counter/ModelDiagramTest.kt에서 종료 상태의 내부 원 안티앨리어싱 경계 대신 빈 링의 상하좌우를 검사하고 검정 중심과 캔버스 크기 검사를 보존한다.
3. 수정한 두 사례를 먼저 실행하고 전체 test와 buildPlugin을 실행한다. 실패가 남으면 원인을 규명해 수정한다.
4. verifyPluginStructure와 검색 옵션 생성의 격리 IDE 실행을 확인한다. ZIP에 최신 클래스와 모듈 등록·라이선스·의존성이 포함됐는지 검사하고 해시를 기록한다.
5. README.md 개발 안내에 JDK 21과 재현 명령을 추가하고 specs/025-verify-plugin-release/quickstart.md에 실행 결과·제약을 기록한다.
6. src/main/resources/META-INF/plugin.xml의 템플릿 소개·공급자와 허용되지 않는 표시 이름을 실제 저장소 소유자·기능 설명·GlueFramework로 변경한다. ID와 버전은 보존한다. verifyPluginStructure로 검증하고 변경 후 전체 테스트와 ZIP 생성을 다시 수행한다.

## 복잡성 검토

편집 동작·설정·파일 형식은 유지하며 플러그인 표시 정보만 보완한다. 원본 실패는 build/reports 및 .gradle-validation 로그에 보존하고 수정한 검사와 전체 실행을 구분한다.
