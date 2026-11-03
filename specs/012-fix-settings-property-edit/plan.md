# 구현 계획: 설정 속성 편집 유지

**작성일**: 2026-10-27 | **명세**: [spec.md](spec.md)

## 요약

설정 변경 조회와 셀 입력 확정을 분리한다. 조회에는 편집기 값을 복사본에만 합성하고 Apply·프리셋 전환에서는 기존 확정 동작을 유지한다.

## 기술 맥락

Kotlin·Swing·IntelliJ Platform 2025.3.6.1 기반 데스크톱 플러그인이다. 영속화 형식은 변경하지 않는다. 기존 JUnit 플랫폼 테스트로 검증한다. 조회 비용은 현재 속성 목록 복사 수준이며 새 타이머나 비동기 작업은 없다.

## 헌법 및 제약 검사

헌법은 미작성 템플릿이므로 추가 원칙은 없다. 한글 문서와 speckit 단계 준수를 확인했다. 기존 사용자 변경 파일은 보존한다. 설계 후에도 위반 없음.

## 구조와 설계

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelDiagramDialogs.kt: ActivityPropertiesPanel에 편집을 종료하지 않는 스냅샷을 제공한다. 편집 행·열은 뷰 좌표에서 모델 좌표로 변환한다.
- modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueModelEditorConfigurable.kt: isModified는 조회용 value를 사용한다. 조회 시 프리셋 목록 복사본에 현재 편집을 합성한다. Apply와 전환의 확정 경로는 유지한다.
- src/test/kotlin/com/look4counter/ModelEditorSettingsTest.kt: 키·값의 미확정 변경, 반복 조회, 선택 영역 보존, 원래 값 복원, 적용·초기화·전환을 검사한다.

## 단계

1. 원인과 기존 확정 호출의 계약을 조사한다.
2. 순수 조회와 명시적 확정 인터페이스 및 데이터 모델을 설계한다.
3. 회귀 테스트를 먼저 추가하고 실패를 확인한 뒤 구현한다.
4. 설정 테스트와 관련 속성 테스트를 실행하고 명세와 코드의 차이를 점검한다.
