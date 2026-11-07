# 구현 계획: SQL 문맥 자동완성

작성일: 2026-11-07
명세: [spec.md](spec.md)

## 요약

기존 SqlLexer와 테이블별 메타데이터를 사용해 입력 중 SQL을 허용하는 범위 분석기를 추가한다. SQL 문장·괄호·쿼리 블록과 CTE를 구문 구조로 구분하고 후보 생성과 IntelliJ 표시·삽입을 분리한다. 새 라이브러리나 DB 연결은 추가하지 않는다.

## 기술 맥락

Kotlin 2.3.20, IntelliJ IDEA 2025.3.6.1, JBR 21, 기존 TextCompletionProvider와 JUnit/BasePlatformTestCase를 사용한다. 기존 MetadataSnapshot의 tables와 각 columns를 읽기만 한다. 문서는 변경하지 않고 PSI 커밋 없이 편집 버퍼 전체와 커서 오프셋을 분석한다.

## 헌장 검사

헌장은 미작성 템플릿으로 규범 검사를 생략한다. 한글 문서와 speckit 단계 순서를 준수하며 설계 후에도 위반이 없다.

## 조사와 설계

[research.md](research.md), [data-model.md](data-model.md), [완성 계약](contracts/completion.md)을 따른다. 구문 오류를 엄격히 거부하는 외부 파서 대신 기존 어휘 분석기로 토큰·괄호·구역을 구분한다. 기술 선택은 기존 코드 내에서 해결됐고 추가 조사 에이전트가 필요한 미해결 기술 질문은 없다.

## 파일과 단계

1. src/test/kotlin/com/look4counter/SqlCompletionAnalyzerTest.kt에 구역·별칭·CTE·서브쿼리·경계 단위 검증을 추가한다.
2. queryeditor/src/main/kotlin/com/look4counter/queryeditor/sql/SqlQueryParser.kt에 토큰화, 제한, 괄호 연결, 문장·블록·CTE·관계 파싱을 둔다.
3. 같은 경로의 SqlQueryScope.kt에 범위별 이름 해석과 CTE·파생 출력 컬럼 추론을 둔다. 재귀는 방문 집합으로 끊고 명시적 출력 컬럼을 우선한다.
4. 같은 경로의 SqlCompletionAnalyzer.kt에서 커서 구역·접두·한정자를 판단하고 삽입 문자열·검색 이름·종류·출처 후보를 만든다. 알려진 컬럼 구역에는 테이블을 섞지 않으며 모르는 한정자에는 엉뚱한 컬럼을 제시하지 않는다.
5. SqlKeywordCompletionProvider.kt를 새 분석 결과에 연결하고 기존 키워드·보호 조건·접두 검색을 유지한다. 실제 후보 선택과 저장·Undo/Redo는 src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 검증한다.
6. README.md와 이 기능의 quickstart.md에 지원·제약을 기록한다. 전체 테스트, buildPlugin, verifyPluginStructure를 수행한다. 이 환경의 IDE 소켓 제한으로 검색 옵션 재생성은 이전 검증처럼 제외하고 명시한다.

## 실행 및 복잡성

깊이 48, 토큰 20,000개, SQL 262,144자 범위를 넘으면 일반 후보로 복구한다. 스캔 중 취소를 확인하며 취소 예외는 숨기지 않는다. 동일 요청 내 관계·출력 컬럼을 재사용하고 순환·과도한 재귀를 차단한다. 기존 여러 데이터 소스의 동일 스키마·테이블 정보가 합쳐지는 제약은 유지한다. SQL 전체 문법 검증이나 JDBC 실행으로 확장하지 않는다.

## 파생 테이블 경계 보완 설계

SqlQueryParser.parseSources에서 별칭 뒤 괄호의 첫 토큰이 SELECT/WITH이면 컬럼 목록이 아닌 다음 파생 쿼리로 처리한다. 직후 인접 파생 쿼리만 새 관계로 받아들이고 ON 등의 일반 표현식 괄호는 기존 경로로 처리한다. SqlCompletionAnalyzerTest.kt와 QuerySqlEditorTest.kt에 메타데이터 없는 A/B 분리·실제 삽입 검증을 추가한다.

## 두 번째 SELECT 항목 확인 계획

사용자가 제시한 전체 SQL을 분석기와 실제 편집기 테스트에 사용한다. 두 번째 항목 B의 점 바로 뒤 오프셋으로 후보 집합과 B.DEPT2 삽입을 검증한다. 실패할 경우 해당 계층을 수정하고, 통과하면 이전 수정이 이 경우도 지원함을 기록한다. 전체 회귀·설치 ZIP·구조 검사를 마친 뒤 명세와 구현을 대조한다.

## 종류·문맥 정렬 계획

SqlKeywordCompletionProvider.kt에서 후보 및 종류 병합이 끝난 뒤 CompletionSorter.emptySorter에 종류 우선순위와 이름의 두 LookupElementWeigher를 적용한다. 기본 정렬의 통계·접두어 가중치보다 요구한 종류·ABC순을 명시적으로 적용한다. 기존 접두 매칭과 후보 생성은 유지한다.

컬럼·출력 컬럼은 같은 그룹이다. TABLE 문맥에서는 CTE와 테이블을 같은 최상위 그룹으로 합친다. 일반 후보의 기본 순서는 컬럼·CTE·테이블·키워드이며 여러 종류가 병합된 이름은 가장 앞선 순위를 쓴다. 이름은 Locale.ROOT의 소문자로 비교한다. 새 저장 상태나 의존성은 없다.

src/test/kotlin/com/look4counter/QuerySqlEditorTest.kt에서 실제 Lookup.items 순서를 검증한다. 컬럼이 키워드보다 앞서는 경우, CTE·테이블의 혼합 ABC순, 문맥 미확정 종류별 순서, 한정자 뒤 ABC순 및 기존 삽입·Source·실행 취소 회귀를 포함한다. SqlCompletionAnalyzerTest.kt로 문맥별 기존 후보 집합을 재검증하고 설치 ZIP·구조 검사를 수행한다. 기존 헌장은 미작성 템플릿이며 확장 훅은 없다.
