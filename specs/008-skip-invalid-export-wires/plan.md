# 구현 계획: 비정상 Wire 제외 저장

**날짜**: 2026-10-26 | **명세**: [spec.md](spec.md)

## 개요

ModelServiceXmlExporter에서 유효한 Wire 집합을 먼저 만들고 시작점과 전이 생성에 재사용한다. 잘못된 연결에 대한 전체 예외를 제거하고 연결 외 검증은 유지한다.

## 기술 배경

- Kotlin 2.3.20, IntelliJ 2025.3.6.1, XML PSI, JDOM.
- 기존 glue_uml_ad 문서는 읽기만 하며 Service XML 저장 경로와 출력 순서를 유지한다.
- BasePlatformTestCase의 ModelEditorServiceExportTest로 변환 및 자동 저장을 검증한다.
- Wire 선언 인덱스를 유지한다. 잘못된 참조 토큰은 파서가 이미 무시한다.
- 동일 Child의 중복 참조도 횟수에 포함하여 해당 Wire를 제외한다.
- 유효 시작 연결이 없거나 여러 개면 initial을 빈 문자열로 출력한다.
- 정상 모델의 출력 순서와 속성 집계 방식은 유지한다.

## 규약 확인

헌법은 미작성 템플릿이므로 구체 규칙 검사를 생략한다. 한글 문서 및 speckit 단계 순서를 따른다. 설계 전후 위반 없음.

## 조사 및 설계

연구 에이전트가 읽기 전용으로 저장 경로·Writer의 필수 initial 속성·기존 검증을 확인했다.

1. Wire의 시작·도착 참조 횟수를 수집한다.
2. 양 끝이 각각 한 번 참조되고 Service 표현이 가능한 방향인 Wire만 유지한다.
3. 시작점은 유지한 Wire 중 InitialState 출발 연결이 유일할 때만 지정한다.
4. Activity 전이는 기존 outputs 순서에서 유효 Wire만 선택해 만든다.
5. 연결 외 오류는 그대로 예외를 발생시켜 기존 출력 보호를 유지한다.

## 구조

- modeleditor/src/main/kotlin/com/look4counter/modeleditor/ModelServiceXmlExporter.kt: Wire 선별과 안전한 전이 생성.
- src/test/kotlin/com/look4counter/ModelEditorServiceExportTest.kt: 비정상 유형·자동 저장·출력 보호 검증.
- specs/008-skip-invalid-export-wires/: 명세, 조사, 데이터 모델, 계약, 검증 안내, 작업 목록.

## 복잡성 점검

새 의존성과 저장 형식 변경 없이 변환 내부에서만 Wire를 걸러낸다. 직접 Service 편집 모드에는 적용하지 않는다.
