# 빠른 검증

## 준비

대상 IDE 의존성을 받을 수 있는 네트워크와 Gradle 실행 JDK가 필요하다. 제한된 환경에서는 쓰기 가능한 GRADLE_USER_HOME을 사용한다.

## 자동 검증

```powershell
.\gradlew.bat :queryeditor:compileKotlin :modeleditor:compileKotlin test buildPlugin
```

두 모듈 컴파일과 파일 형식 통합 테스트가 통과해야 한다. build/distributions의 단일 ZIP에 두 형식 클래스와 META-INF 등록 조각이 들어 있어야 한다.

## IDE 검증

```powershell
.\gradlew.bat runIde
```

1. 빈 example.glue_sql과 example.glue_uml_ad를 생성하고 각 형식으로 인식되는지 확인한다.
2. 두 파일 모두 Design, Source 순서의 두 탭만 표시되는지 확인한다. 쿼리 Design에는 queryMap 편집 화면이, 모델 Design에는 안내가 표시되어야 한다.
3. Source에서 `<root><child/></root>`를 입력하고 XML 구문 강조를 확인한다. 태그 자동 완성과 서식 정리도 확인한다.
4. 잘못된 닫기 태그를 입력해 구문 오류가 표시되는지 확인하고 다시 수정한다.
5. 저장하지 않고 Design으로 전환한 뒤 Source로 돌아와 변경 내용이 유지되는지 확인한다. 실행 취소·다시 실행도 확인한다.
6. 저장한 뒤 파일을 닫고 다시 열어 내용과 확장자가 유지되는지 확인한다. Source의 커서 위치와 탭 선택도 복원되어야 한다.
7. 읽기 전용 파일의 Source가 수정 불가능한지 확인한다. 빈 파일과 잘못된 XML도 Source로 전환해 수정할 수 있어야 한다.
8. queryeditor/sample-query.glue_sql과 modeleditor/sample.glue_uml_ad를 열어 기존 원문이 재작성되지 않는지 확인한다.
9. 기존 MyToolWindow가 열리는지 확인한다.
10. 일반 sql, uml, txt, xml 파일에 전용 Design/Source 탭이 추가되지 않았는지 확인한다.

## 범위

쿼리 Design은 queryMap 편집 화면이고 모델 Design은 안내 화면이다. SQL 문법·실행과 그래픽 모델 편집은 후속 작업이다. Source의 XML 구문 분석은 IDE에 포함된 XML 지원을 사용하며 전용 스키마는 제공하지 않는다.

## 쿼리 Design 검증

1. queryeditor/sample-query.glue_sql을 열어 queryMap desc가 sample이고 오른쪽에 쿼리 id 7개가 표시되는지 확인한다.
2. 각 쿼리를 선택하며 SQL 본문과 id·desc·resultType·isNamed가 원문과 일치하는지 확인한다.
3. SQL과 네 속성을 수정하고 Source로 전환하여 선택한 query만 변경되었는지 확인한다. id 변경 후 목록의 선택도 유지되어야 한다.
4. SQL 수정 직후 다른 query를 선택하여 이전 쿼리에 변경이 남고 새 쿼리는 오염되지 않는지 확인한다.
5. Source에서 수정·실행 취소·다시 실행한 뒤 Design에서 같은 결과를 확인한다.
6. SQL에 `<`, `&`, `]]>`를 포함해 저장하고 재열어 내용이 일치하는지 확인한다. 다른 쿼리·주석·xmlns도 유지되어야 한다.
7. 오른쪽 구분선을 끌어 너비를 조절한다. 입력과 목록은 좁은 패널 안에서도 사용할 수 있어야 한다.
8. 빈 queryMap, 중복/누락 id, 잘못된 XML, 읽기 전용 파일을 확인한다. 중첩 본문은 Source 편집 안내가 나오고 덮어쓰지 않아야 한다.

## 설명 편집과 화면 정리 검증

1. 최상단 queryMap desc를 특수문자가 포함된 문자열로 수정·비우고 Source 반영·실행 취소·다시 실행·저장·재열기를 확인한다. 다른 쿼리와 선택은 유지되어야 한다.
2. 쿼리 없는 queryMap과 desc 없는 queryMap에서도 설명을 입력한다. 잘못된 XML과 읽기 전용에서는 수정이 제한되어야 한다.
3. isNamed가 resultType과 같은 행 오른쪽에 있으며 선택 쿼리만 수정하는지 확인한다.
4. 정상 문서 하단에 쿼리 개수나 빈 상태 영역이 없는지 확인한다. 오류·읽기 전용·빈 목록·중첩 SQL 안내는 유지되어야 한다.

## Java 클래스 자동완성 검증

1. Java 지원을 활성화한 IDE에서 Java 프로젝트와 의존 라이브러리를 준비하고 인덱싱 완료를 기다린다.
2. 속성 라벨이 Id, Desc, ResultType, IsNamed인지 확인하고 IsNamed가 ResultType 오른쪽인지 확인한다.
3. 서로 다른 패키지의 동명 클래스를 추가한다. ResultType에 클래스명을 입력하거나 Ctrl+Space로 후보를 호출하고 패키지를 구분해 선택한다. Source의 resultType에는 전체 이름이 들어가야 한다. 라이브러리 클래스도 확인한다.
4. 직접 입력·빈 값·빠른 쿼리 전환·Source 수정·실행 취소·다시 실행·저장 후 재열기를 확인한다.
5. 읽기 전용·잘못된 XML에서는 ResultType이 비활성화되고 인덱싱 중에는 IDE 기본 제한을 따르는지 확인한다.

## 파일 형식 아이콘 검증

1. 플러그인을 설치한 IDE에서 .glue_sql과 .glue_uml_ad 파일을 열고 프로젝트 트리와 편집 탭을 확인한다.
2. 쿼리 파일에는 하늘색 원과 흰색 Q, 모델 파일에는 연주황 원과 흰색 M이 표시되어야 한다.
3. 밝은 테마·어두운 테마와 IDE 배율 변경 후 원과 글자의 식별이 유지되는지 확인한다.
4. 두 파일이 기존 XML Source 및 Design 멀티뷰로 열리는지 확인한다.

## 속성 외형 통일 검증

1. 쿼리 Design에서 ID, Description, ResultType 라벨과 텍스트 없는 체크박스를 확인한다.
2. ResultType과 ID 입력의 테두리·배경·글꼴·높이를 비교한다. 밝은 테마와 어두운 테마, 비활성화 상태에서도 확인한다.
3. ResultType에서 Ctrl+Space로 클래스를 선택하고 전체 이름이 Source에 저장되는지 확인한다. 직접 입력·비우기·선택 전환도 확인한다.
4. 체크박스가 ResultType 오른쪽에 있고 마우스를 올리면 isNamed 도움말이 나타나며 기존 속성을 수정하는지 확인한다.

## ResultType 렌더링 확인

1. 실제 ResultType 입력에서 문자열이 세로 중앙에 있고 좌우 여백이 있는지 확인한다.
2. 다른 입력으로 포커스를 옮겼을 때 일반 테두리가 보이고, ResultType을 선택하면 더 굵은 포커스 테두리가 나타나는지 확인한다. 텍스트 위치가 움직이면 안 된다.
3. 밝은 테마·어두운 테마와 배율 변경 후 표시와 클래스 자동완성을 확인한다.
4. 자동 렌더링 결과는 .gradle-validation/result-type-normal.png와 result-type-focused.png에서 확인한다.

ResultType의 높이가 이전보다 2픽셀 작고 입력 위치가 오른쪽으로 2픽셀 이동했는지 확인한다. 좌우 내부 패딩은 각각 1픽셀이며 일반·포커스 상태에서도 글자와 테두리가 겹치지 않아야 한다. 기존 렌더링 이미지와 클래스 자동완성도 확인한다.

## 왼쪽 패딩 확인

기존 Gradle 및 IDE 테스트 환경에서 `.gradle-validation/gradle-9.7.1/bin/gradle.bat test --tests com.look4counter.QueryDesignEditorTest`를 실행한다. 왼쪽 전체 인셋은 배율 적용 5픽셀이고 오른쪽은 3픽셀이며 중앙 정렬·포커스 테두리·자동완성·편집 검증이 통과해야 한다.

## 세 입력 비교 캡처

쿼리 편집 테스트 실행 후 `.gradle-validation/input-comparison-normal.png`와 `input-comparison-focused.png`를 열어 ID·Description·ResultType의 왼쪽 경계·높이·텍스트 시작 여백을 비교한다. 캡처는 테스트 환경에서 실제 Swing 컴포넌트를 그린 이미지이며 실행 중인 IDE 창의 화면 캡처와 구별한다.

## 어두운 테마 여백 회귀 확인

전체 검증 명령은 `.gradle-validation/gradle-9.7.1/bin/gradle.bat test buildPlugin`이다. `.gradle-validation/input-comparison-dark-normal.png`, `input-comparison-dark-focused.png`, `input-comparison-light-normal.png`를 열어 세 입력의 텍스트 시작·높이·테두리를 비교한다. Darcula에서는 기준 입력의 텍스트 시작 좌표 9픽셀과 ResultType 좌표가 같아야 한다. 테마와 배율이 다른 경우 하드코딩한 값이 아니라 해당 UI가 계산한 실제 좌표를 비교한다.

## 쓰기 비안전 문맥 회귀 검증

`.gradle-validation/gradle-9.7.1/bin/gradle.bat test buildPlugin`을 실행한다. 쿼리 테스트의 생성·활성화 회귀 검증은 미커밋 Source를 쓰기 비안전 문맥에서 열고 콜백이 커밋하지 않는지 검사한다. 이벤트 처리 후 최신 값이 표시되어야 한다. 비활성화 시 대기 편집도 콜백 안에서는 적용되지 않고 안전한 예약 실행에서 반영되어야 한다. 초기화 예약 직후 폐기 시 문서 변경이나 오류가 없어야 한다.

## 저장 시 SQL 공백 정리 확인

1. Source에서 .glue_sql의 query 본문에 CDATA 바깥 들여쓰기 또는 CDATA 안의 앞뒤 줄바꿈을 넣고 저장한다.
2. 저장된 본문이 공백 없이 CDATA 안의 SQL로 시작·종료되는지 확인한다. SQL 내부의 줄바꿈과 들여쓰기는 유지되어야 한다.
3. Design SQL 입력에서 앞뒤 공백을 넣고 저장한 뒤 선택과 화면 값이 유지되면서 앞뒤 여백만 정리되는지 확인한다. 실행 취소하면 정리 전 본문이 복구되어야 한다.
4. 중복 id의 두 쿼리도 각각 정리되고 속성·XML 주석·네임스페이스가 보존되는지 확인한다. 복합 본문·잘못된 XML·읽기 전용 문서·일반 .xml 파일은 변경되지 않아야 한다.

자동 검증은 `.gradle-validation/gradle-9.7.1/bin/gradle.bat test buildPlugin`으로 실행한다. QuerySaveListenerTest의 실제 저장·디스크 비교와 QueryDesignEditorTest의 저장 동기화·실행 취소 회귀 검증을 포함한다.

## 파일 열기 정리 확인

앞뒤 여백이 있는 query 본문을 포함한 .glue_sql을 열고 Source와 Design에서 같은 정리 결과를 확인한다. 내부 줄바꿈은 유지하고 CDATA 안팎의 앞뒤 여백만 제거되어야 한다. 열기만으로 디스크 원본은 변경하지 않으며 문서는 미저장 상태가 된다. 실행 취소·다시 실행을 확인한 뒤 저장하고 다시 열면 추가 변경이 없어야 한다. 읽기 전용·잘못된 XML·복합 본문·일반 XML은 보존되어야 한다.

자동 검증 명령은 `.gradle-validation/gradle-9.7.1/bin/gradle.bat test buildPlugin`이며 QueryOpenListenerTest의 실제 FileEditorManager 열기와 이벤트 안전성 검증을 포함한다.

## SQL 강조와 완성 확인

1. Design SQL 입력에 다음 SQL을 입력한다.

```sql
select empno, 123, 3.14, 'hello SQL', "EMP_NAME"
from emp
where empno = 42 and ename = 'SCOTT';
-- SELECT 99 'comment'
/* UPDATE 5 */
```

2. 키워드는 주황색, 숫자는 파란색, 두 종류 따옴표 토큰은 녹색이며 주석 전체는 기본 주석 색인지 확인한다. 밝은 테마와 어두운 테마에서 비교한다.
3. SQL에서 se를 입력하고 Ctrl+Space를 누른다. SELECT 등의 접두어 후보가 표시되고 선택하면 현재 쿼리에만 반영되어야 한다. IDE 자동 완성이 켜져 있으면 입력 중에도 후보를 표시한다.
4. 문자열·주석·숫자 내부와 읽기 전용 입력에서는 키워드 후보가 없어야 한다. 쿼리를 전환하고 Source에서 실행 취소·다시 실행한 뒤에도 강조와 완성을 확인한다.
5. 앞뒤 여백이 있는 SQL의 파일 열기·저장 정리 이후에도 강조와 완성이 유지되어야 한다. ResultType Java 완성은 기존대로 동작해야 한다.

자동 검증은 `.gradle-validation/gradle-9.7.1/bin/gradle.bat test buildPlugin`으로 실행한다. `.gradle-validation/sql-syntax-light.png`와 `sql-syntax-dark.png`를 열어 실제 SQL 편집기 렌더링을 확인한다. 배포 JAR에는 sql/dbeaver-keywords.txt와 licenses/dbeaver-Apache-2.0.txt, dbeaver-NOTICE.txt가 포함되어야 한다.
## 파라미터·여백·DataSource 확인

1. Design SQL에 `select empno from emp where empno=:empno`를 입력하고 파라미터가 cyan인지 확인한다. 문자열·주석·`::type`은 해당 색에서 제외되어야 한다.
2. 우측 쿼리 ID의 좌우 여백과 줄 번호 오른쪽 빈 공간을 확인한다.
3. Settings > GlueFramework > GlueQueryEditor에서 DataSource 두 개를 등록한다. 이름·JDBC URL을 입력하고 Apply 후 다시 열어 복원되는지 확인한다.
4. 미적용 수정·삭제를 Reset/Cancel로 버리고, 중복 이름과 잘못된 JDBC URL이 거부되는지 확인한다.
5. 비밀번호를 비워 두면 유지되고 삭제 체크 또는 항목 삭제로 제거되는지 확인한다. 이 화면은 프로젝트별 등록 정보를 관리하며 DB 접속 테스트는 수행하지 않는다.

자동 검증 캡처는 `.gradle-validation/sql-syntax-light.png`, `sql-syntax-dark.png`, `query-layout-padding.png`, `datasource-settings.png`에 생성된다. 실제 Swing 컴포넌트의 오프스크린 캡처이며 실행 중 IDE 창의 OS 화면 캡처는 아니다.

## 선택 DB 자동완성 확인

1. Settings에서 DB 종류와 JDBC URL·계정을 지정하고 로컬 JAR들을 선택하거나 자동 다운로드로 받는다. Impala 다운로드는 직접 HTTPS JAR/ZIP 주소가 필요하다. Apply로 저장한다.
2. Design 상단에서 DataSource를 고르고 메타데이터 새로고침을 누른다. 완료 후 테이블·컬럼 수를 확인하고 SQL에서 접두어와 Ctrl+Space로 후보를 확인한다.
3. 다른 DB 선택 시 이전 DB 후보가 섞이지 않는지, 설정 변경 시 새로고침이 필요한지 확인한다. 실패/취소 시 이전 성공 후보와 키워드가 유지되어야 한다.
4. 가운데 구분선을 드래그할 수 없는지와 줄 번호 오른쪽 여백이 10픽셀 더 늘었는지 캡처로 확인한다.

