# 구현 계획: QueryEditor SQL 포매팅

작성일: 2026-11-07
명세: [spec.md](spec.md)

## 요약

기존 SQL 어휘 분석기를 사용하되 문자열·주석을 원형대로 보존하는 포매팅 토큰과 괄호 연결을 만든다. SQL 구역·목록·CASE를 구분하는 작성기로 사용자 기준 문자열을 정확히 생성한다. 선택 쿼리는 기존 QueryMapDocumentModel.apply를 통해 Source에 한 번의 편집 명령으로 반영한다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ IDEA 2025.3.6.1, JBR 21, 기존 JUnit/BasePlatformTestCase를 사용한다. 새 라이브러리·DB 호출·파일 형식 변경은 없다. 최대 262,144자·20,000토큰·괄호 깊이 48을 사용하고 중간 취소를 확인한다.

## 헌장 검사

헌장은 미작성 템플릿이라 규범 검사를 생략한다. 한글 문서·speckit 단계를 준수한다. 설계 후 재검토에서도 위반은 없다.

## 조사와 설계

[research.md](research.md), [data-model.md](data-model.md), [포맷 계약](contracts/format.md)을 따른다. 기존 범위 분석기는 주석을 버리므로 포매터는 별도 보존 토큰을 사용한다. 외부 라이브러리와 미해결 기술 질문은 없다.

## 구조와 단계

1. src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에 포맷 버튼·선택 쿼리 반영·Undo/Redo·읽기 전용 검증을 먼저 추가하고 기존 구현의 실패를 확인한다.
2. src/test/kotlin/com/look4counter/SqlFormatterTest.kt에 사용자 예제의 정확 결과·멱등성·CASE·CTE·주석·문자열·함수·조건·오류 보호 검증을 추가한다.
3. queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlFormattingTokens.kt에 보존 토큰과 잘못된 인용·주석·괄호·길이·깊이 검사, SqlFormatter.kt에 정렬 작성기와 구역·CASE·CTE 포매팅을 구현한다.
4. queryeditor/src/main/kotlin/com/look4counter/queryeditor/QueryDesignEditor.kt의 SQL 상단에 SQL 포맷 버튼을 추가하고 기존 코드 정리 단축키를 SQL 컴포넌트에만 등록한다. 지연 중 대상 변경·쓰기 문맥·최신 XML을 검사하고 Source에 직접 모델 명령으로 반영한 뒤 버퍼를 갱신한다.
5. README.md와 이 기능 quickstart.md에 규칙·사용법·제한을 기록하고 문맥 자동완성 포함 전체 테스트·ZIP·구조 검증을 수행한다.

## 편집 안전성과 복잡성

열기·저장 시 자동 포맷하지 않는다. 불완전·과도한 입력은 원문을 보존하고 안내한다. 단축키는 IDE의 현재 키맵을 사용하며 Source XML 편집기에는 영향을 주지 않는다. 포맷 결과가 같으면 쓰기 명령을 만들지 않는다. 일반 함수 괄호는 인라인, SELECT/WITH 서브쿼리는 4칸 본문 들여쓰기를 사용한다. DB 전용 전체 문법의 정확성이나 자동 교정은 보장하지 않는다.

## 후속 구현 계획

SQL 상단 버튼과 래퍼를 제거하고 단축키 활성 상태를 별도 불리언으로 관리한다. 포매터 범위별 기준 폭을 SELECT·UPDATE·INSERT는 6, MERGE는 5로 정하고 문장 경계에서 초기화한다. INSERT의 컬럼·VALUES 괄호 목록만 별도 세로 목록으로 처리하며 함수 인자는 유지한다. MERGE WHEN 분기는 CASE와 구분하고 내부 UPDATE·INSERT는 바깥 기준 폭을 사용한다. 기존 버튼 통합 검증을 등록된 단축키 액션 검증으로 갱신한다.

## 주석·선택 영역 구현 보완

보존 토큰에 단독 주석 줄의 원래 공백을 포함한다. 작성기는 해당 줄을 원문으로 출력한다. 포맷 액션은 선택 범위와 버퍼 stamp를 캡처하고 안전한 실행 시점에서 대상이 그대로인지 확인한다. 모델 SQL 중 선택 부분만 교체한다. 원문 보존·선택 밖 불변·단일 Undo/Redo를 검증한다.

## 최신 배치 설계

기존 포매터의 괄호 처리에서 서브쿼리의 여는 열을 저장해 본문 열+4·닫는 열을 복원한다. INSERT와 MERGE 헤더를 복합 heading으로 처리하며 MERGE ON·분기와 INSERT VALUES 줄바꿈을 구분한다. INSERT·MERGE ON 목록은 고정 6칸 표현식/4칸 콤마를 사용하고 함수 목록은 유지한다. 세 예제의 정확 결과와 멱등성, 기존 보호·전체 회귀를 검증한다.

## PIVOT·UNPIVOT 설계

SqlFormatter.kt의 구역에 PIVOT·UNPIVOT을 추가하고 범위에 피벗 본문 모드를 전달한다. 바깥 괄호는 키워드 시작 열을, 내부 IN 괄호는 본문 시작 열을 기준으로 배치한다. 함수 괄호에는 피벗 모드를 전달하지 않는다. FROM의 인접 서브쿼리는 첫 표현식 열에서 새 줄을 시작한다. SqlFormatterTest.kt에서 정확 예제와 UNPIVOT·멱등성을 검증하고 전체 검증한다.
