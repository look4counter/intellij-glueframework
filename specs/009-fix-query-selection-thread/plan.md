# 구현 계획: Query 목록 전환 스레드 오류

**날짜**: 2026-10-26 | **명세**: [spec.md](spec.md)

## 개요

QueryDesignEditor의 목록 선택 전환 전체를 안전한 UI 실행 문맥으로 연결한다. 선택 대상을 이벤트 시점에 캡처하고 대기 편집은 기존 대상에 반영한다.

## 기술 배경

- Kotlin 2.3.20, IntelliJ 2025.3.6.1, Swing JList, XML PSI, EditorTextField.
- 기존 QueryMapDocumentModel과 QueryEdit, SQL 버퍼를 재사용한다.
- BasePlatformTestCase의 실제 목록 및 문서 입력으로 검증한다.
- 잠금이 있으면 기존처럼 즉시 전환한다. 없으면 현재 쓰기 안전 모달리티 또는 비모달 상태로 한 번 예약한다.
- 요청 순번으로 대체된 전환을 버린다. 원문과 저장 형식은 유지한다.

## 규약 확인

헌법은 미작성 템플릿이므로 구체 규칙 검사를 생략한다. 한글 문서 및 speckit 절차를 따른다. 설계 전후 위반 없음.

## 조사와 설계

1. 목록 리스너에서 Query 대상 포인터와 선택 요청 순번을 확보한다.
2. 선택 처리 함수에서 잠금·쓰기 안전 문맥을 확인한다.
3. 필요한 경우 Application.invokeLater로 캡처한 요청을 예약한다.
4. 실행 시 폐기 여부와 최신 요청인지 확인한 뒤 flushPending과 refresh를 순서대로 수행한다.
5. 잠금 없는 전환·보류 편집·연속 선택·폐기·쓰기 안전하지 않은 문맥을 검증한다.
6. 기존 Design 및 SQL 테스트와 설치용 ZIP을 빌드한다.

## 구조

- queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt: 목록 전환의 실행 가드.
- src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt: 실제 목록 입력 회귀 검증.
- specs/009-fix-query-selection-thread/: 명세, 조사, 데이터 모델, UI 계약, 검증 안내, 작업 목록.

## 복잡성 점검

선택 순번과 작은 처리 함수를 추가한다. 새 서비스·의존성·저장 데이터는 없다. 외부 탐색 경로는 이번 목록 선택 오류 범위에 포함하지 않는다.
