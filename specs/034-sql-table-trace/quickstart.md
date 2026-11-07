# 검증 안내

기존 로컬 Gradle·JBR 캐시를 사용한다. 테이블 분석·컬럼 분석·QuerySqlEditor·SQL 완성·컬럼 오류 관련 검증 후 전체 테스트와 설치 ZIP·플러그인 구조 검사를 실행한다. 기존 IDE 소켓 제약으로 검색 옵션 재생성은 제외한다.

## 확인 예제

```sql
WITH a AS (
 SELECT * FROM physical
), b AS (
 SELECT * FROM a
)
SELECT b.id
FROM b
```

physical·a·b 및 b.id의 b를 선택하면 2행의 원본, 1행의 a 선언, 4행의 참조, 3행의 b 선언, 7행의 최종 참조가 나타난다. id를 선택하면 기존 컬럼 경로를 표시한다. 항목 클릭과 Enter로 토큰 이동을 확인하고 원본 SQL·Source가 바뀌지 않는지 확인한다.

분기·JOIN 격리·인용 이름·중첩 WITH·파생 테이블·재귀·UNION·다중 문장·주석/문자열·분석 한도도 자동 검증한다. 실제 IDE 수동 확인과 자동 검증을 구분해 결과를 기록한다.

## 자동 검증 결과: 2026-11-08

- 테이블 분석·기존 컬럼 분석·실제 QuerySqlEditor 집중 검증 69개가 통과했다. CTE 연쇄의 어느 지점에서 선택해도 전체 경로와 분기가 표시되고, 이후 JOIN의 다른 입력과 독립적인 같은 이름의 FROM 인스턴스가 섞이지 않는 것을 확인했다.
- 실제 편집기 테스트에서 한정자의 테이블 선택과 컬럼 선택 전환, 클릭·Enter의 정확한 위치 이동, SQL·Source 비변경, 편집 및 쿼리 전환 후 경로 갱신을 확인했다. 기존 가운데 안내·패딩·제목 및 하단 문구 없는 구성도 통과했다.
- 첫 전체 회귀 525개 통과 후 수렴 검토에서 구문 키워드의 별칭 오인 가능성을 발견했다. PIVOT·UNPIVOT·MODEL·MATCH_RECOGNIZE 선택 제외 및 인용 별칭 허용 검증을 추가했고 최종 전체 회귀 526개가 통과했다. 실패·오류·건너뜀은 각각 0개다.
- `test buildPlugin verifyPluginStructure -x buildSearchableOptions`가 성공했다. 기존 IDE 소켓 제약으로 검색 옵션 재생성만 제외했고 설치 ZIP 및 SHA-256을 갱신했다. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 근거: `.gradle-validation/table-trace-focused.log`, `.gradle-validation/table-trace-final.log`, `build/reports/table-trace-focused-test-summary.json`, `build/reports/table-trace-test-summary.json`.

## 구현 전 분석과 수렴

구현 전 요구사항 6개를 T003~T008에 대응시켰고 성공 기준 3개를 T004·T008·T009로 검증하도록 했다. 대응률 100%이며 중복·모호함·충돌·미대응 작업은 없었다. 품질 점검은 16/16 통과했다. 헌장은 미작성 템플릿이므로 규범 검사를 생략했고 확장 훅은 없다.

첫 수렴에서 FR-005의 구문 키워드 선택 제외가 부분 구현인 중간 심각도 항목 1개를 발견해 T011을 추가했다. 해당 작업을 구현하고 전체 회귀를 완료했다.

최종 수렴에서 요구사항 6개·성공 기준 3개·인수 시나리오 7개와 위치 보존·객체 식별·방향 그래프·기존 목록 및 컬럼 분석 재사용·분석 한도 결정을 대조했다. 누락·부분 구현·충돌·범위 밖 추가 항목은 각각 0개다. 수렴 단계에서 새 작업을 추가하지 않았고 이후 구현 완료 기록만 갱신했다. 실제 프로젝트 버전 1.0.4의 `build/distributions/intellij-glueframework-1.0.4.zip`과 같은 경로의 SHA-256 파일을 최종 산출물로 확인했다.
