# 검증 안내

WITH t AS (SELECT id FROM physical) 이후 SELECT t.missing FROM t를 작성해 missing의 오류 표시를 확인한다. WHERE·ON·GROUP BY·HAVING·ORDER BY와 CTE를 참조하는 DML에서도 확인한다. t.id로 고치면 오류가 사라져야 한다. SELECT * FROM physical의 불명 출력과 물리 테이블 자체에는 없는 컬럼을 임의로 판정하지 않아야 한다.

CTE 명시 목록·다단계 별표·파생·중첩·상관·인용 이름과 함수·CAST·문자열·주석·파라미터를 검증한다. 문서를 바꾸거나 편집기를 종료하면 과거 표시가 남지 않아야 한다. 밝은/어두운 테마에서 한 줄·블록 주석의 녹색을 확인한다.

기존 로컬 JBR·Gradle 캐시로 집중 검증 후 전체 test·buildPlugin·verifyPluginStructure를 실행한다. IDE 소켓 제약으로 검색 옵션 생성을 제외하며 실제 사용자 IDE 수동 확인과 구분한다.

## 자동 검증 결과: 2026-11-07

- 최종 전체 517개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. 새 검사기 단위 검증 9개와 편집기 오류 수명 검증을 추가했다. 기존 밝은/어두운 토큰 색상 검증에 한 줄·블록 주석의 녹색 기대값을 포함했다.
- SELECT·WHERE·JOIN ON·GROUP BY·HAVING·ORDER BY와 UPDATE SET/WHERE·DELETE WHERE·MERGE ON/SET·INSERT 대상 컬럼 및 INSERT SELECT를 확인했다. CTE·다단계 별표·파생·명시 이름 변경·상관 범위·별칭 가림·문장 격리·인용 이름도 통과했다.
- 물리 테이블·불명 별표·부분 명시 목록·이름 없는 계산식·순환·닫히지 않은 구문·작성 중 JOIN과 CASE를 보수적으로 처리했다. PIVOT 등 출력 구조가 바뀌는 구문은 변환 전 스키마로 부재를 판정하지 않는다. 명시 SELECT 출력으로 다시 닫힌 이름은 외부에서 검증할 수 있다.
- 실제 편집기에서 오류 범위·설명과 ERROR 테마의 물결선 효과를 확인했다. 입력 수정 직후 기존 오류가 지워지고 쿼리 전환 후 과거 표시가 남지 않는다. SQL·Source 내용은 검사 때문에 바뀌지 않는다.
- 자동 렌더링 이미지 `.gradle-validation/sql-syntax-light.png`, `sql-syntax-dark.png`를 확인했다. 주석은 각각 진한/밝은 녹색이며 기존 키워드·숫자·문자열·파라미터 색상도 유지한다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. ZIP과 SHA-256을 갱신했다. 기존 IDE 기동 소켓 제약으로 검색 옵션 생성만 제외했고 사용자 IDE 수동 조작·실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/known-columns-focused.log`, `known-columns-ui-focused.log`, `known-columns-final.log`, `build/reports/known-columns-test-summary.json`. 최초 UI 컴파일의 색상 setter 호출을 플랫폼 API에 맞게 수정한 뒤 집중·전체 검증을 통과했다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 수렴 검토

요구사항 8개·성공 기준 3개·사용자 이야기 3개와 계획의 구현 단계 5개를 파서 검증 전용 모드·확정 스키마·오류 범위·표시 수명·색상·회귀 결과에 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 기존 자동완성·TRACE는 기본 파서 모드를 유지하고 전체 회귀를 통과했다. 새 수렴 작업은 필요하지 않다. 헌장은 미작성 템플릿으로 규범 검사를 생략했고 확장 훅은 없다.

## NULL 오탐 후속 검증 (2026-11-10)

수정 전 새 NULL 리터럴 회귀가 실패하여 SELECT 직접 출력의 NULL 오탐을 재현했다. 수정 후 검사기 11개와 실제 편집기 54개를 포함한 전체 SQL 회귀 195개가 실패·오류·건너뜀 없이 통과했다. 직접 NULL·대소문자·명시/암시 별칭·함수·CASE·IS NULL·UPDATE SET·INSERT VALUES·파생 출력과 실제 누락 컬럼의 유지 및 인용/한정 NULL 식별자 검증을 확인했다. 실제 편집기에서는 NULL과 missing이 공존하는 SQL의 missing에만 정확한 ERROR 범위가 생기고 SQL·Source가 변경되지 않았다.

로컬 Gradle 9.7.1, 프로젝트 .gradle-user-home 캐시, IDEA 2025.3.6.1의 JBR 21과 기존 오프라인 초기화 설정을 사용했다. 재실행 명령은 다음과 같다.

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-user-home"
$sqlJbr = "$PWD/.gradle-user-home/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr"
& .gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests 'com.look4counter.Sql*Test' --tests com.look4counter.QuerySqlEditorTest --offline --no-configuration-cache -I .gradle-validation/press-position.init.gradle "-Porg.gradle.java.installations.paths=$sqlJbr"
```

메인·테스트 컴파일과 diff 공백 검사도 통과했다. 사용자 IDE 수동 조작이나 배포 ZIP 재생성은 수행하지 않았다.
