# 검증 안내

한 줄 SQL과 사용자가 여러 줄로 나눈 같은 SQL에 각각 포매팅을 실행한다. 한 줄 SQL은 한 줄이며 다중 행 SQL의 각 줄에는 같은 토큰이 남아야 한다. 빈 줄·LF/CRLF·GROUP과 BY 사이 줄바꿈·주석·리터럴을 확인한다. 옵션을 켠 WHERE·SELECT는 기존 행들 사이에서 정렬되고 선택 밖은 변경하지 않아야 한다.

기존 로컬 JBR·Gradle 캐시를 사용해 SqlFormatterTest·QuerySqlEditorTest 집중 검증 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. IDE 소켓 제약으로 검색 옵션 생성은 제외한다. 자동 검증과 실제 사용자 IDE 조작 결과는 구분한다.

## 자동 검증 결과: 2026-11-07

- 최종 전체 498개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. 새 경계 검증 5개를 추가하고 기존 자동 줄바꿈 기대값을 새 사용자 행 구성 계약에 맞게 갱신했다.
- 단일 행의 SELECT·CTE·UPDATE·INSERT·MERGE·CASE·서브쿼리·PIVOT·UNPIVOT가 한 줄로 유지된다. 기존 다중 행 예제는 같은 형태를 유지하며 포매터 테스트의 공통 검증에서 각 비공백 토큰의 원래 행 번호가 변하지 않는지 확인했다.
- 빈 줄·시작과 끝의 줄바꿈·LF/CRLF·주석 줄의 공백과 CRLF·함수 인자·콤마 뒤 줄바꿈·GROUP/BY와 ORDER/BY 사이 줄바꿈·선택 경계의 빈 줄 보존을 확인했다. 선택 경계 줄바꿈이 중복되지 않는다.
- 등호·AS 정렬 입력은 사용자가 미리 나눈 행으로 변경해 개별·동시 정렬과 반복 실행을 확인했다. 실제 편집기의 전체·선택 포매팅, Source 동기화·Undo/Redo·읽기 전용·충돌 보호도 통과했다.
- 복합 키워드는 원본 경계를 개별 출력하면서 대문자 처리한다. 내부 렉서의 토큰 위치 검증은 테스트에서만 사용하며 운영 코드의 공개 API를 늘리지 않았다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 기동 소켓 제약으로 검색 옵션 재생성만 제외했다. 실제 사용자 IDE 수동 조작과 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/sql-linebreaks-focused.log`, `sql-linebreaks-final.log`, `build/reports/sql-linebreaks-test-summary.json`. 설치 ZIP과 SHA-256 파일을 `build/distributions`에 갱신했다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 수렴 검토

요구사항 6개·성공 기준 3개·사용자 이야기 2개와 계획의 구현 단계 5개를 Writer·토큰 경계·선택 처리·회귀 검증·문서에 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. README의 과거 자동 줄바꿈 설명을 현재 계약으로 갱신했다. 새 수렴 작업은 필요하지 않다. 헌장은 미작성 템플릿으로 규범 검사를 생략했고 확장 훅은 없다.

## 줄바꿈 대체 공백 취소

사용자가 공백 대체 요청을 취소해 같은 줄의 자동 줄바꿈 요청은 무시하도록 변경했다. 일반 토큰 구분 공백은 별도로 유지한다. `select A,B from EMP`는 `SELECT A, B FROM EMP`, 서브쿼리는 `(SELECT ...)`로 정리한다. 콤마 앞이나 괄호 안쪽에 대체 공백을 추가하지 않는다. 기존 사용자 줄바꿈·정렬 옵션·선택·주석 보호는 유지한다.

- 최종 전체 499개 테스트가 통과했으며 실패·오류·건너뜀은 각각 0개다. 단일 행의 콤마·서브쿼리·INSERT 목록·CASE 키워드 구분 회귀를 추가했다. 기존 다중 행·토큰 행 위치·LF/CRLF·정렬·편집기 Undo/Redo도 통과했다.
- 동일한 명령으로 ZIP 생성과 플러그인 구조 검사를 통과했다. 설치 ZIP과 SHA-256을 갱신했다. 기존 IDE 소켓 제약으로 검색 옵션 생성만 제외했고 사용자 IDE 수동 조작·실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/sql-no-break-padding-focused.log`, `sql-no-break-padding-final.log`, `build/reports/sql-no-break-padding-test-summary.json`.
- 후속 수렴 검토에서 수정된 FR-002를 포함한 요구사항 6개·성공 기준 3개와 보완 계획을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다.

## 인라인 서브쿼리 정렬 보완

`(SELECT *`처럼 SELECT가 여는 괄호와 같은 줄에 있는데도 괄호 다음 줄의 기본 들여쓰기인 여는 열 + 4칸을 후속 절 기준으로 사용하던 문제를 수정했다. SELECT 출력 직전 실제 열을 저장하고 FROM·WHERE·AND·GROUP BY·ORDER BY의 기준으로 사용한다. 사용자 예제에서 FROM 앞 공백은 13칸에서 10칸으로 줄어 FROM의 M이 SELECT의 T와 일치한다.

새 포매터·편집기 검증 3개는 수정 전 모두 실패했고 수정 후 집중 검증을 통과했다. 사용자 줄바꿈 보존, 정렬 옵션 동시 적용, 중첩 쿼리별 독립 기준, 기존 다음 줄 SELECT 들여쓰기와 반복 실행을 함께 확인했다.

- 최종 전체 502개 테스트가 통과했고 실패·오류·건너뜀은 각각 0개다. 동일 명령으로 설치 ZIP 생성과 플러그인 구조 검사를 통과했다. ZIP과 SHA-256 파일을 갱신했다.
- 근거: `.gradle-validation/sql-inline-anchor-red.log`, `sql-inline-anchor-focused.log`, `sql-inline-anchor-final.log`, `build/reports/sql-inline-anchor-test-summary.json`.
- 기존 IDE 소켓 제한으로 검색 옵션 생성만 제외했으며 사용자 IDE 수동 조작과 실제 DB 접속은 수행하지 않았다.
- 후속 수렴 검토에서 FR-007을 포함한 요구사항 7개·성공 기준 3개와 실제 열 기준 설계를 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다.
