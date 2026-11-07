# 검증 안내

Settings > GlueFramework > GlueQueryEditor에서 두 옵션의 기본 해제, 각각 선택과 Apply·Reset을 확인한다. 설정을 저장한 후 Query Design에서 Ctrl+Alt+L을 실행해 WHERE AB = AB와 AND HELO = HI의 등호가 같은 열에 맞는지 확인한다. SELECT A AS X와 LONG_NAME AS Y도 같은 방식으로 확인한다.

선택 범위·중첩 쿼리·주석·CAST와 반복 포매팅을 검증하고 Source 및 Undo/Redo를 확인한다. 기존 로컬 JBR·Gradle 캐시를 사용해 집중 검증과 전체 test·buildPlugin·verifyPluginStructure를 실행한다. IDE 소켓 제약 때문에 검색 옵션 생성은 제외하고 그 사실을 결과에 기록한다.

## 자동 검증 결과: 2026-11-07

- 전체 493개 테스트가 통과했다. 실패·오류·건너뜀은 각각 0개다. 기존 483개에 설정 3개·포매터 5개·편집기 2개 검증을 추가했다.
- 두 옵션의 개별·동시 적용과 기본 해제, Apply·Reset·초안 취소·화면 재생성·직렬화·방어적 복사를 확인했다. DataSource 검증이 실패하면 포매팅 초안도 저장하지 않는다. 스타일만 변경할 때 DataSource와 메타데이터 캐시를 유지한다.
- WHERE 단독 등호, SELECT 출력 AS, CAST·주석·문자열·다른 비교 연산자 보호, CTE·서브쿼리·문장별 격리, CASE, 괄호 안 조건, UPDATE SET 보존과 반복 포매팅을 확인했다. 등호 뒤 서브쿼리의 여는 괄호·본문·닫는 괄호가 함께 이동하는 것도 확인했다.
- 실제 편집기 통합 검증에서 저장값을 전체·선택 포매팅에 적용하고 선택 밖 텍스트를 유지했다. 설정 저장 자체는 SQL을 바꾸지 않으며 Source 동기화와 Undo/Redo가 통과했다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 기동 소켓 제약으로 검색 옵션 재생성만 제외했다. 실제 사용자 IDE 수동 조작과 실제 DB 접속은 수행하지 않았다.
- 근거: `.gradle-validation/sql-alignment-focused.log`, `sql-alignment-final.log`, `build/reports/sql-alignment-test-summary.json`. 설치 파일 `build/distributions/intellij-glueframework-1.0.3.zip`과 SHA-256 파일을 갱신했다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 수렴 검토

요구사항 7개·성공 기준 3개·사용자 이야기 2개와 구현 단계 5개를 저장 서비스·설정 화면·포매터·편집기·검증 결과에 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 새 수렴 작업은 필요하지 않다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했고 확장 훅은 없다.

## IntelliJ 설정 화면 외형 보완: 2026-11-07

- SQL 포매팅 스타일 영역의 사각 TitledBorder를 제거하고 IntelliJ TitledSeparator로 제목과 가로 구분선을 표시했다. 체크박스는 아래쪽의 들여쓴 패널에 배치하고 JBUI로 간격·배율을 적용했다.
- 기존 설정 회귀 13개가 통과했으며 실패·오류·건너뜀은 각각 0개다. 저장·Reset·직렬화·검증 실패·캐시 보존 동작을 유지한다. 외형 변경에 대해 구현을 복제하는 새 테스트는 추가하지 않았다.
- 자동 렌더링 이미지 `.gradle-validation/datasource-settings.png`를 확인해 사각 테두리가 없고 제목 구분선과 두 체크박스가 배치된 것을 확인했다. 실제 사용자 IDE의 테마별 수동 확인은 수행하지 않았다.
- `test --tests 'com.look4counter.DataSourceSettingsTest' buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. ZIP과 SHA-256을 갱신했다. 검색 옵션 생성은 기존 IDE 소켓 제약으로 제외했다.
- 근거: `.gradle-validation/format-settings-ui-final.log`, `build/reports/format-settings-ui-test-summary.json`.
- 수렴 검토에서 FR-008의 기본 컴포넌트·테두리 제거·테마/배율·기존 설정 보존을 코드·렌더링·회귀와 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 0개이며 새 수렴 작업은 없다.
