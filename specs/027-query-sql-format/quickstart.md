# 검증 안내

JDK 21에서 SqlFormatterTest와 QuerySqlEditorTest를 먼저 실행한다. 사용자의 전체 예제를 정확히 비교하고 반복 적용·보존 토큰·CTE·CASE·함수·조건·오류 보호·선택 쿼리 Source 반영·단일 Undo/Redo·저장·읽기 전용을 확인한다.

전체 test와 buildPlugin·verifyPluginStructure를 실행한다. 기존 환경의 IDE 소켓 제한 때문에 검색 옵션 생성은 제외하고 기록한다. 실제 사용자 IDE에서 SQL 포맷 버튼과 코드 정리 단축키를 사용한 확인과 자동 검증을 구분한다.

## 검증 결과: 2026-11-07

- 사용자가 제공한 WITH·SELECT·CASE·조건·GROUP BY·ORDER BY 전체 예제를 정확한 기준 문자열로 검증했고 일치했다. 동일 결과에 다시 적용해도 바뀌지 않는다.
- 최종 포매터 단위 검증 17개와 QuerySqlEditor 통합 검증 25개가 모두 통과했다. 중첩 CTE·CASE·함수 인자·BETWEEN·JOIN·집합 연산·서브쿼리·여러 문장·주석·리터럴·인용 이름·파라미터·과도한 입력을 확인했다.
- 복합 연산자 `<=>`, `!~*`, `&&`가 잘못 분리되는 경계를 새 테스트로 재현하고, 인접 연산자 보존과 명명 파라미터·주석 시작 경계 분리를 보완했다. 숫자형 바인드·`@` 접두어·캐스트·연결 이름도 보존 검증에 포함했다.
- 버튼과 IDE 코드 정리 단축키 등록, 선택 SQL만 Source에 반영, 저장·단일 Undo/Redo, 반복 포맷 시 문서 stamp 유지가 통과했다. 읽기 전용 파일·문서, 잘못된 XML·혼합 본문·대상 없음·폐기, 잘못된 SQL 거부, 대기 편집과 새 Source 충돌, 지연 중 선택 변경 보호를 확인했다.
- 쓰기 잠금 없는 호출 검증에서 초기에 PSI 포인터를 일찍 읽는 오류를 발견했다. PSI 대상·쓰기 가능 여부 확인을 안전한 문맥으로 지연한 뒤 해당 두 테스트와 전체 검증이 통과했다.
- 문맥 자동완성을 포함한 최종 전체 검증은 451개 통과, 실패·오류·건너뜀 각각 0개다. buildPlugin과 verifyPluginStructure도 통과했다. git diff --check에서 공백 오류는 없다.
- 검색 옵션 생성은 이전부터 확인된 IDE 소켓 권한 제약 때문에 제외했고 기존 산출물을 재사용했다. 실제 사용자 IDE에서 키보드 입력과 업무 파일로 수동 조작한 것은 아니다. 단축키는 등록된 실제 액션과 안전하지 않은 쓰기 문맥에서의 호출로 자동 검증했다.

## 실행 명령

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'

# 순수·편집기 집중 검증
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test --tests 'com.look4counter.SqlFormatterTest' --tests 'com.look4counter.QuerySqlEditorTest'

# 전체 검증 및 설치 ZIP·구조 검증
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 설치 파일과 근거

설치 ZIP은 build/distributions/intellij-glueframework-1.0.3.zip이다. SHA-256은 10B4808D195B3A0E1F088CE9CCDADE3DEF49F35CD42D1473112F657A1F68F9AD이며 같은 경로의 .sha256 파일에도 기록했다. ZIP 내 메인 JAR이 최신 빌드 JAR과 일치하고 SqlFormatter·SqlFormattingTokens·SqlCompletionAnalyzer·SqlQueryParser·SqlQueryScope 클래스가 포함됐음을 확인했다.

- .gradle-validation/sql-format-red.log: 기존 포맷 기능 부재 재현.
- .gradle-validation/sql-format-unit.log: 순수 포매팅 검증.
- .gradle-validation/sql-format-operator-red.log: 복합 연산자 보존 실패 재현.
- .gradle-validation/sql-format-focused.log: 최종 42개 집중 검증.
- .gradle-validation/sql-format-final.log: 전체 테스트·ZIP·구조 검증 성공.
- build/reports/sql-format-verified-focused-results와 sql-format-final-results: 집중·전체 XML 결과.
- build/reports/sql-format-test-summary.json과 sql-format-artifact-summary.json: 테스트 집계와 ZIP 대조 근거.

명세 품질 6개가 모두 통과했고 명확화 질문은 0개였다. 분석 단계에서 요구사항 8개는 작업 9개에 모두 연결됐으며 중요한 모호함·중복·충돌은 없었다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했다.

## 후속 검증 결과: 버튼 제거·DML·주석·선택 영역

SQL 포맷 버튼을 제거하고 기존 IDE 단축키를 유지했다. UPDATE·INSERT는 E·T 열에 절을 맞추고 SET·컬럼·VALUES 목록을 앞쪽 콤마로 정렬한다. MERGE는 긴 UPDATE 절과 같은 열을 사용할 수 있도록 첫 MERGE 앞에 한 칸을 두고 그 E 열에 INTO·USING·ON·WHEN·THEN·UPDATE·INSERT를 정렬한다. 함수 인자는 인라인을 유지한다.

주석만 있는 줄은 원래 들여쓰기·탭·앞뒤 공백을 보존한다. 선택 영역이 있으면 그 텍스트만 포맷하고 선택 밖 문자열과 경계 줄바꿈을 유지한다. 선택이 주석·문자열·파라미터 중간에서 끊기면 거부하며, 지연 중 버퍼 변경이나 선택 쿼리 변경에도 적용하지 않는다. 선택이 없으면 전체 쿼리를 포맷한다.

UPDATE·INSERT VALUES·INSERT SELECT·MERGE의 정확 결과 및 반복 적용 동일성, 단독 주석 원형 보존, 선택 외부 불변·단일 Undo/Redo, 주석 선택 경계 보호를 검증했다. 최종 전체 테스트는 실패·오류·건너뜀 0개이며 buildPlugin·verifyPluginStructure도 통과했다. 로그는 .gradle-validation/sql-dml-focused.log·sql-dml-final.log·sql-selection-focused.log·sql-selection-final.log, 집계는 build/reports/sql-selection-test-summary.json에 있다. 설치 ZIP과 .sha256 파일을 최신 빌드로 갱신했다.

이전 버튼 관련 설명은 후속 FR-009로 대체되고 전체 쿼리 전용 설명은 FR-012로 대체된다. 선택 텍스트는 독립적으로 포맷하며 Source XML 전체를 포맷하지 않는다. 기존 IDE 소켓 권한 제약 때문에 검색 옵션 생성은 제외하고 기존 산출물을 재사용했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.

## 최신 예제 배치 검증

최신 사용자 세 예제를 정확 결과로 비교해 통과했다. SELECT 목록 서브쿼리는 여는 괄호 열+4에 SELECT를 배치하고 닫는 괄호를 여는 열로 복원한다. INSERT INTO는 한 줄이며 컬럼·값 목록은 6칸, 콤마는 4칸이다. MERGE INTO·USING·ON과 WHEN 분기, 내부 INSERT 및 VALUES의 줄바꿈도 최신 예제를 따른다. 이전 INSERT 및 MERGE 배치 규칙은 FR-014~015로 대체됐다.

정확 비교와 반복 적용 동일성, 함수 인라인·주석 보존·선택 영역 보호·Undo/Redo를 포함한 집중 검증 및 전체 테스트가 모두 통과했다. 최종 전체 결과는 build/reports/sql-layout-test-summary.json, 로그는 .gradle-validation/sql-layout-focused.log 및 sql-layout-final.log에 있다. buildPlugin과 verifyPluginStructure도 성공했고 ZIP 및 해시 파일을 갱신했다. 기존 IDE 소켓 제약 때문에 검색 옵션 생성은 제외했으며 실제 사용자 IDE 수동 확인은 수행하지 않았다.

## PIVOT·UNPIVOT 검증 결과

사용자 PIVOT 예제를 인접 서브쿼리와 별칭까지 문자열 전체로 비교해 일치했다. UNPIVOT INCLUDE/EXCLUDE NULLS, 소문자 키워드, 튜플과 IN 목록·함수 인라인·반복 적용 동일성도 통과했다. 빠진 구분자를 추가하지 않고 원문 토큰 순서를 유지한다.

최종 전체 테스트는 460개 통과, 실패·오류·건너뜀 각각 0개다. 기존 SELECT·CTE·DML·주석·선택 범위·Undo/Redo 회귀도 포함한다. buildPlugin·verifyPluginStructure가 성공했고 설치 ZIP 및 SHA-256 파일을 갱신했다. 근거는 .gradle-validation/sql-pivot-focused.log·sql-pivot-final.log와 build/reports/sql-pivot-test-summary.json이다.

후속 명세·명확화·계획·작업·분석·구현 후 수렴 점검에서 FR-016~017의 누락·충돌은 없다. 검색 옵션 재생성은 기존 IDE 소켓 권한 제약 때문에 제외했고 실제 사용자 IDE 수동 확인은 수행하지 않았다.
