# 구현 계획: 확정 출력 컬럼 오류와 주석 색상

## 기술 맥락과 헌장

기존 Kotlin·SqlQueryParser·범위 해석기·EditorTextField·markupModel을 사용한다. 새 외부 의존성이나 DB 조회를 추가하지 않는다. 헌장은 미작성 템플릿으로 규범 검사를 생략하며 한글 문서와 speckit 순서를 따른다.

## 구현 단계

1. `sql/SqlQueryParser.kt`: 검증 전용 DML 포함 모드를 추가한다. 기본 자동완성·TRACE 파싱은 기존대로 유지하고 UPDATE·DELETE·MERGE·INSERT의 관계와 구역을 검사기에 제공한다.
2. `sql/SqlColumnValidator.kt`: 위치와 확정 여부를 보존한 출력 스키마를 계산한다. CTE·파생·출력 별칭·명시 목록·별표를 순환 방지 캐시로 분석한다. 전체 문장의 컬럼 참조를 수집하고 확정적으로 부재한 컬럼만 진단한다. 선언·함수·자료형·리터럴·파라미터·의사 컬럼과 불완전 구문을 보호한다.
3. `sql/SqlColumnErrorHighlighting.kt`, `QuerySqlEditor.kt`: 문서 리스너와 지연 요청·백그라운드 분석·세대/문서 시각 검사를 사용한다. ERROR 테마 속성의 정확한 범위 표시와 설명을 적용하고 수정·종료 시 기존 표시를 정리한다.
4. `sql/SqlSyntaxHighlighter.kt`: 별도 주석 텍스트 속성 키에 밝은/어두운 녹색 팔레트를 적용한다.
5. `src/test/kotlin/com/look4counter/SqlColumnValidatorTest.kt`와 `QuerySqlEditorTest.kt`: 확정성·다단계·별표·각 구역·DML·범위·별칭·자료형·미완성·실제 오류 갱신과 주석 색상을 검증한다. README·quickstart를 기록하고 전체 test·buildPlugin·verifyPluginStructure를 실행한다.

## 설계 제약

불명 스키마와 알려진 이름 목록을 구분한다. 물리 테이블은 항상 불명으로 다루며 파생의 명시 SELECT 목록은 입력 테이블과 별개로 출력 이름을 확정할 수 있다. 이름 없는 계산식·불명 별표·부분 명시 목록·순환은 닫힌 스키마로 간주하지 않는다. 비한정 이름은 보이는 관계 모두가 닫혀 있을 때만 부재를 판정한다. 오류 표시만 추가하며 문서나 편집 명령을 수정하지 않는다.

기존 IDE 소켓 제한으로 검색 옵션 재생성은 제외하고 결과에 기록한다. 실제 사용자 IDE 수동 확인과 자동 테스트를 구분한다.
