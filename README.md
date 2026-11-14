# IntelliJ GlueFramework

쿼리 편집기와 모델 편집기를 별도 Gradle 모듈로 관리하고 하나의 IntelliJ 플러그인으로 배포한다.

## 모듈 구조

| 위치 | 역할 | 담당 확장자 |
| --- | --- | --- |
| 루트 프로젝트 | 플러그인 조립·배포, 공통 설정, 기존 샘플 도구 창 | 통합 등록 |
| queryeditor | queryMap의 목록·속성·SQL 편집 | .glue_sql |
| modeleditor | 모델 파일 형식과 향후 모델 편집 기능 | .glue_uml_ad |

```text
src/main/                    # 통합 플러그인과 샘플 도구 창
src/test/                    # 플랫폼 통합 테스트
queryeditor/
  build.gradle.kts
  src/main/kotlin/           # 쿼리 편집기 구현
  src/main/resources/        # queryeditor.xml 등록 조각
modeleditor/
  build.gradle.kts
  src/main/kotlin/           # 모델 편집기 구현
  src/main/resources/        # modeleditor.xml 등록 조각
specs/001-split-editor-modules/ # 명세·설계·작업·검증 기록
```

각 모듈은 자신의 구현과 확장자 등록을 소유하며 서로 의존하지 않는다. 루트 plugin.xml이 두 등록 조각을 포함하고 pluginComposedModule이 두 모듈을 메인 JAR에 합친다. 플러그인 ID는 com.look4counter.intellij-glueframework로 유지한다.

두 파일 형식 모두 IDE 하단의 Design / Source 탭으로 열린다. 최초에는 Design을 표시하고 이후 탭 선택은 IDE가 기억한다.

- 쿼리 Design: 왼쪽 SQL 편집기와 오른쪽 queryMap 설명·쿼리 속성·query.id 목록을 제공한다.
- 모델 Design: 기본 안내 화면이다. UML 그래픽 조작은 후속 구현 대상이다.
- Source: IntelliJ XML 편집기를 사용한다. XML 구문 강조·구문 검사·태그 완성·서식 정리와 실행 취소·다시 실행·저장을 지원한다.

두 형식의 확장자는 그대로 유지한다. XML 스키마가 연결되어 있으면 해당 스키마의 검증과 추천을 사용하며, 플러그인 자체 전용 스키마나 SQL 실행 기능은 아직 제공하지 않는다. 일반 XML 파일에는 전용 탭을 추가하지 않는다.

## 모델 Design 좌표 표시

ModelEditor의 화면 이동 x·y 좌표는 미니맵 아래에 표시한다. 좌측 상단에는 다이어그램 속성을 표시하며 좌표는 표시하지 않는다. 화면 이동·미니맵 탐색 중에도 현재 값을 갱신하고, 미니맵이 표시되지 않는 경우 좌표도 숨긴다.

GlueModelEditor 설정의 `No use .glue_uml_ad file` 옵션은 제거했다. 이전 저장값이 true여도 false로 처리하며 `.glue_uml_ad` 기반 편집과 일반 Service 내보내기를 사용한다. Service 저장 경로와 Activity preset 설정은 유지한다.

## 쿼리 Design 사용

queryeditor/sample-query.glue_sql을 열면 queryMap의 desc와 문서 순서의 query.id 목록이 오른쪽에 표시된다. 목록에서 쿼리를 선택하면 왼쪽에 CDATA를 제외한 SQL 본문이 표시되고 오른쪽 ID·Description·ResultType 입력과 체크박스도 함께 바뀐다.

최상단 queryMap desc도 직접 수정할 수 있다. isNamed는 resultType 입력 오른쪽에 있다. 하단 쿼리 개수는 표시하지 않으며 필요한 상태 안내만 나타난다.

SQL이나 속성을 수정하면 Source XML에도 반영된다. id 변경 후에도 선택한 쿼리를 유지하며 저장은 IDE의 일반 저장 기능을 사용한다. 오른쪽 영역은 약 320픽셀을 기본 기준으로 하고 구분선을 끌어 너비를 조절할 수 있다.

SQL 편집기는 키워드·숫자·따옴표 토큰을 색으로 표시하고 SQL 키워드 자동완성을 제공하는 다중 행 편집기다. SQL 오류 검사·실행은 제공하지 않는다. XML이 잘못되었으면 Source에서 먼저 수정해야 한다. query 본문에 하위 태그나 주석이 있으면 SQL 덮어쓰기를 제한하고 Source 편집을 안내한다. query 추가·삭제는 아직 제공하지 않는다.

## 개발 및 검증

기존 대상 IDE는 IntelliJ IDEA 2025.3.6.1이고 Kotlin은 2.3.20이다. 빌드에는 JDK 21과 최초 의존성 다운로드를 위한 네트워크가 필요하다. `JAVA_HOME`을 JDK 21 설치 경로로 지정한다. 대상 IDE에 포함된 `jbr`도 사용할 수 있다.

통합 테스트는 대상 IDE에 포함된 Bundled 테스트 프레임워크를 사용한다. XML 관련 API는 IDE의 XML 모듈에 의존한다.

```powershell
# 각 모듈 컴파일
.\gradlew.bat :queryeditor:compileKotlin :modeleditor:compileKotlin

# 확장자 등록, Design/Source 멀티뷰와 XML 편집 통합 테스트
.\gradlew.bat test

# 두 모듈이 포함된 IDE 실행
.\gradlew.bat runIde

# 하나의 설치 ZIP 생성
.\gradlew.bat buildPlugin

# 설치 ZIP의 플러그인 구조 검증
.\gradlew.bat verifyPluginStructure
```

배포물은 build/distributions에 생성된다. 하위 모듈을 따로 설치하거나 배포하지 않는다. 기존 MyToolWindow 샘플은 루트에 유지한다.

GRADLE_USER_HOME에 쓰기 권한이 없다면 쓰기 가능한 경로로 설정하고 실행한다.

```powershell
$env:GRADLE_USER_HOME = "$PWD/.gradle-user-home"
.\gradlew.bat test buildPlugin
```

상세 수동 확인 절차는 [빠른 검증 안내](specs/001-split-editor-modules/quickstart.md)를 참고한다. 플랫폼 설정의 근거는 [공식 다중 모듈 문서](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html#multi-module-project-structure)에 있다.

## ResultType 클래스 자동완성

쿼리 속성 라벨은 ID, Description, ResultType으로 표시한다. ResultType 오른쪽의 체크박스에는 텍스트를 표시하지 않으며 isNamed 툴팁을 제공한다. ResultType에 클래스명을 입력하거나 Ctrl+Space를 누르면 IDE의 Java 클래스 후보를 사용할 수 있다. 후보를 선택하면 패키지를 포함한 전체 클래스명이 선택 쿼리의 resultType에 저장된다. 직접 입력과 빈 값도 허용한다.

프로젝트와 의존 라이브러리의 클래스 인덱스를 사용하므로 Java 플러그인이 활성화되어 있어야 한다. 자동 팝업은 IDE 완성 설정을 따르며 인덱싱 중에는 IDE 기본 제한이 적용된다. XML 속성명은 기존 id, desc, resultType, isNamed를 유지한다.

## 파일 아이콘

.glue_sql은 하늘색 원 안의 흰색 Q, .glue_uml_ad는 연주황 원 안의 흰색 M 아이콘을 사용한다. SVG 리소스로 제공하여 IDE 배율에 맞춰 표시한다.

ResultType은 일반 입력 배경·글꼴을 사용하며 텍스트를 세로 중앙에 배치한다. 좌우 내부 패딩은 각각 1픽셀(테두리 공간 제외)이고 일반 테두리는 1픽셀, 포커스 테두리는 2픽셀로 IDE 배율을 반영한다. Java 클래스 후보 완성은 유지하며 입력란 자체에는 Java 코드의 구문 색상과 검사 표시를 적용하지 않는다.

ResultType 입력 높이는 이전보다 기본 배율 2픽셀 줄였으며 입력 위치는 오른쪽으로 2픽셀 이동했다. 너비와 중앙 정렬 및 클래스 자동완성은 유지한다.

## 열기 및 저장 시 SQL 공백 정리

.glue_sql 파일을 열거나 저장할 때 query SQL 본문 앞뒤의 공백·탭·줄바꿈을 제거한다. CDATA 바깥 들여쓰기와 안쪽의 앞뒤 여백이 모두 대상이며 SQL 내부 줄바꿈과 들여쓰기는 유지한다. Source와 Design 편집 모두에 적용하고 정리된 본문은 CDATA로 저장한다. 열 때의 정리는 편집 중인 문서에 적용되어 미저장 상태가 되며, 디스크에는 저장할 때 반영한다.

예를 들어 앞뒤 줄바꿈이 있는 SQL도 다음처럼 저장된다.

```xml
<query id="sample.emp.delete" desc="EMP 삭제" resultType="" isNamed="false"><![CDATA[delete from emp
where empno=?]]></query>
```

속성·다른 XML 영역은 보존하며, 하위 태그나 XML 주석이 섞인 본문·잘못된 XML·읽기 전용 문서는 정리하지 않는다. 저장으로 제거된 SQL 여백은 실행 취소할 수 있다.

## SQL 색상과 키워드 완성

Design SQL 입력은 DBeaver의 공통 SQL 키워드 367개를 주황색, 숫자를 파란색, 작은따옴표·큰따옴표 토큰을 녹색으로 표시한다. 한 줄·블록 주석은 녹색 계열이며 밝은 테마에서는 진한 녹색, 어두운 테마에서는 밝은 녹색을 사용한다.

자동완성은 대소문자를 구분하지 않는 접두어 검색을 사용한다. 입력 중 자동 팝업은 IDE 설정을 따르고 Ctrl+Space로 직접 호출할 수 있다. 문자열·주석·숫자·파라미터 내부 및 읽기 전용 SQL 입력에서는 후보를 제시하지 않는다. 공통 SQL 키워드와 선택 DB에서 조회한 테이블·컬럼 이름을 제공한다. DB 전용 키워드·함수 후보는 추가하지 않는다. Source XML 표시와 ResultType의 Java 클래스 완성은 유지한다.

키워드 출처는 [DBeaver SQLConstants.java](https://github.com/dbeaver/dbeaver/blob/f2e25edd7d2eecd4d6541cdce70fdb25c7b07ea9/plugins/org.jkiss.dbeaver.model/src/org/jkiss/dbeaver/model/sql/SQLConstants.java)이며 Apache-2.0 라이선스와 출처 고지를 배포물에 포함한다. 실행 시 DBeaver 설치나 인터넷 연결은 필요하지 않다.
## SQL 표시 여백과 프로젝트 DataSource

`:parameter`는 cyan으로 표시한다. 문자열·주석 안에서는 기존 색을 유지하며 `::type` 형 변환과 `:=` 대입은 제외한다. 파라미터 내부에서는 키워드 완성을 표시하지 않는다. 우측 쿼리 ID 목록에 좌우 각각 8픽셀 여백을 추가하고 줄 번호 오른쪽에는 18픽셀 공간을 예약한다. 모두 IDE 배율을 적용한다.

Settings > GlueFramework > GlueQueryEditor에서 프로젝트별로 여러 DataSource를 추가·수정·삭제할 수 있다. 이름과 JDBC URL은 필수이며 드라이버 클래스·사용자명·비밀번호는 선택 사항이다. 이름 중복은 허용하지 않는다. Apply/OK로 저장하고 Cancel/Reset으로 미적용 변경을 버린다.

접속 메타데이터는 프로젝트의 `.idea/glue-query-editor.xml`에 저장한다. 비밀번호는 IDE 보안 저장소에 분리하며 입력란을 비워 두면 기존 값을 유지한다. 삭제 체크박스 또는 DataSource 삭제로 저장된 비밀번호를 제거한다. DB 연결은 테이블·컬럼 메타데이터를 조회할 때 사용하며 SQL 실행은 제공하지 않는다.

설정 화면은 가운데 크기 조절 구분선 없이 고정된 목록과 폼을 사용한다. DB 종류는 Oracle, MySQL, MariaDB, PostgreSQL, Impala 중에서 선택한다. 기본 드라이버 클래스가 채워지며 수정할 수 있다. JDBC JAR은 여러 로컬 파일을 고르거나 세미콜론으로 구분하여 지정한다. 자동 다운로드 버튼은 HTTPS JAR/ZIP을 받아 경로를 초안에 반영한다. 네 DB는 기본 주소를 제공하며 Impala는 공급자의 직접 배포 주소를 입력해야 한다. 다운로드 파일은 IDE 시스템 캐시에 보관한다.

Design 상단에서 DataSource를 선택하고 **메타데이터 새로고침**을 누르면 접근 가능한 테이블과 각 테이블의 컬럼 정보를 읽는다. 테이블명·컬럼명의 중복 없는 목록이 키워드와 함께 자동완성 후보로 표시된다. 원본 이름이 정확히 같으면 중복을 제거하며 접두어 검색은 대소문자를 구별하지 않는다. 프로젝트의 선택 DB는 유지하고 메타데이터 캐시는 메모리에만 보관하므로 IDE 재시작 후 다시 조회해야 한다. 실패·취소는 직전 성공 결과를 유지하고 설정 변경·삭제는 캐시를 무효화한다. 자동완성 후보를 조회할 때는 DB에 접속하지 않는다.


## SQL 문맥 자동완성

파생 테이블의 별칭 뒤에서는 해당 SELECT가 출력하는 컬럼만 제시한다. `SELECT A. FROM (SELECT DEPT, JOB, SALARY FROM employee) A`의 후보는 DB 메타데이터 없이도 `DEPT`, `JOB`, `SALARY`다. 작성 중 두 파생 테이블 사이의 쉼표가 빠져 있어도 A와 B의 출력 컬럼을 분리하며 SQL 원문은 변경하지 않는다.

SELECT·WHERE·ON·GROUP BY·HAVING·ORDER BY에서는 현재 쿼리의 컬럼을, FROM·JOIN에서는 테이블과 유효한 CTE를 추천한다. SELECT 뒤에 있는 FROM도 분석하며 `emp e`와 `emp AS e`의 `e.` 뒤에서는 해당 테이블 컬럼만 제공한다. 같은 컬럼명이 여러 관계에 있으면 `e.id`, `d.id`로 구분해 삽입한다. 키워드와 접두 대소문자 무시 검색은 유지한다.

WITH의 명시적 컬럼 목록·SELECT 출력 별칭·확인 가능한 별표와 앞선 CTE 참조, 파생 테이블 출력 컬럼을 인식한다. 중첩 쿼리의 같은 별칭은 안쪽을 우선하고 상관 서브쿼리는 가려지지 않은 바깥 별칭을 사용할 수 있다. 서로 다른 SQL 문장의 이름은 공유하지 않는다. 이미 작성한 인용 테이블·별칭도 해석하지만 따옴표 안을 입력하는 동안은 기존처럼 완성을 표시하지 않는다.

메타데이터는 기존 캐시만 사용하며 자동완성 중 DB에 접속하지 않는다. 스키마 없는 같은 테이블명이 여러 데이터 소스에 있으면 관련 컬럼을 합치고 스키마를 명시하면 좁힌다. 관계를 알 수 없는 컬럼 구역에서는 캐시의 컬럼을 사용한다. 미지원 SQL은 일반 후보로 복구하며 DB별 전체 문법이나 임의 계산식의 출력 이름을 보장하지 않는다. 분석 제한은 262,144자·20,000토큰·괄호 깊이 48이며 초과하면 일반 후보를 사용한다.

## SQL 컬럼 오류 표시

CTE·서브쿼리의 출력 컬럼을 SQL 자체로 확정할 수 있으면 존재하지 않는 컬럼을 빨간 오류 물결선과 설명으로 표시한다. SELECT뿐 아니라 WHERE·JOIN ON/USING·GROUP BY·HAVING·ORDER BY와 해석 가능한 UPDATE·DELETE·MERGE·INSERT 구역도 검사한다. 입력을 고치거나 쿼리를 전환하면 갱신되며 SQL·Source·Undo 기록을 바꾸거나 저장·포매팅을 차단하지 않는다.

물리 테이블 자체의 컬럼이나 물리 테이블에서 온 `SELECT *`, 출력 이름·개수가 불명인 결과에는 부재 오류를 단정하지 않는다. 한정자 없는 컬럼은 접근 가능한 모든 관계가 확정된 경우에만 판정한다. 전체 DB 문법 검증이나 실행 전 서버 검사는 아니며, 복잡한 미지원 구문은 보수적으로 검사하지 않는다.

## SQL 컬럼 및 테이블 TRACE

Query Design에서 컬럼에 커서를 놓으면 우측 하단 목록에 원본부터 최종 사용처까지의 전체 경로가 표시된다. 중간 CTE의 컬럼을 선택해도 이후 전달 단계와 사용처 분기를 함께 보여준다. 예를 들어 CTE의 `SELECT SS`가 다음 CTE의 `SELECT *`를 거쳐 마지막 `SELECT SS`로 전달되면 어느 단계에서 선택하더라도 각 단계의 실제 SQL 줄 번호와 내용이 나온다. 별도 TRACE 제목 라벨은 표시하지 않는다. 항목을 한 번 클릭하거나 선택 후 Enter를 누르면 해당 컬럼으로 이동한다. 목록을 통한 이동은 경로를 유지하고 SQL이나 Source를 변경하지 않는다.

일반 커서 이동·SQL 수정·쿼리 전환 시 경로를 갱신한다. CTE·파생 테이블·출력 별칭·명시 컬럼 목록·별표 전달과 표현식의 참조 컬럼을 추적하며 DB에 접속하지 않는다. 출처가 모호하거나 순환 참조가 있으면 확인 가능한 부분만 표시한다. 물리 테이블 정의는 현재 SQL 밖이므로 표시하지 않으며 스칼라 서브쿼리 표현식과 DB 전용 전체 문법은 지원 범위에 포함하지 않는다.

TRACE 항목에는 쿼리 목록과 동일하게 좌우 각각 8 논리 픽셀의 패딩을 적용하며 IDE 배율을 따른다. 목록 아래의 별도 상태 문구는 표시하지 않는다. 빈 목록에는 초기·분석 중·추적 불가 상태 모두 `추적할 컬럼이나 테이블에 커서를 놓으세요.`만 목록 영역의 세로 가운데에 표시하며, 경로가 있으면 안내 문구는 숨긴다.

컬럼 추적 목록에는 SELECT 출력과 CTE·파생 테이블의 전달 단계만 표시한다. WHERE·ON·HAVING·GROUP BY·ORDER BY의 사용 위치와 조건절 안의 서브쿼리는 제외한다. 조건절에서 컬럼을 선택해도 그 컬럼의 SELECT 전달 경로만 보여주며, 출력 경로가 없으면 목록을 비운다. SELECT 안의 CASE·계산식에 쓰인 컬럼은 결과값의 출처이므로 추적을 유지한다.

테이블명·CTE 선언 및 참조 이름·파생 테이블 별칭에 커서를 놓으면 같은 목록에서 테이블 경로를 표시한다. `A.COL`의 A는 테이블, COL은 컬럼을 추적한다. 스키마 경로와 인용 이름을 인식하고 DB 연결 없이 원본 FROM·CTE 선언·이후 참조 분기까지 표시한다. 예를 들어 `physical → a → b`의 CTE 연쇄에서는 어느 단계에서 선택해도 원본부터 최종 FROM까지 확인할 수 있다. 테이블 추적은 같은 SQL 줄을 한 번만 표시하고 클릭·Enter로 그 줄의 테이블 관련 토큰으로 이동한다.

선택한 관계의 원본에서 이어지는 사용처를 모두 표시하며, 이후 JOIN에 함께 등장한 다른 입력을 새 원본으로 추가하지 않는다. 복수 입력을 가진 CTE 자체를 선택하면 그 원본은 모두 포함한다. 이름이 같은 독립적인 FROM 인스턴스·중첩 CTE·다른 SQL 문장은 합치지 않는다. SQL 밖 뷰 정의나 DML 쓰기 데이터 이동은 추정하지 않는다.

## SQL 포맷

Settings > GlueFramework > GlueQueryEditor의 **SQL 포매팅 스타일**에서 `조건절·SET절 등호에 맞춰 정렬`과 `SELECT절 AS에 맞춰 정렬`을 각각 선택할 수 있다. 기본값은 모두 해제이며 프로젝트별로 저장한다. Apply/OK로 저장한 뒤 SQL 포매팅을 실행하면 같은 WHERE·ON·HAVING 조건절 또는 SET 대입문의 각 줄 첫 단독 `=` 또는 같은 SELECT 출력 별칭의 `AS`를 가장 오른쪽 위치에 맞춘다. 절·CTE·서브쿼리·문장은 각각 독립적으로 정렬하며 CAST의 AS와 다른 비교 연산자는 제외한다. 설정 저장만으로 SQL이 바뀌지는 않는다.

사용자가 이미 여러 줄로 작성한 SQL에서 두 옵션을 켠 예시는 다음과 같다.

```sql
SELECT AB        AS A
     , LONG_NAME AS B
  FROM T
 WHERE AB   = AB
   AND HELO = HI
```

Query Design의 SQL 영역에서 IDE 코드 정리 단축키(Windows 기본 Ctrl+Alt+L)를 사용하면 선택한 쿼리 전체를 정리한다. 사용자 키맵을 따르며 Source XML 편집기의 일반 정리 동작에는 영향을 주지 않는다. 열기·저장 시에는 자동 포맷하지 않는다.

사용자가 입력한 줄바꿈·빈 줄·LF/CRLF를 보존하고 새 줄을 만들거나 기존 줄을 합치지 않는다. 기존 자동 줄바꿈 요청은 같은 줄에서 무시하며 대체 공백을 추가하지 않는다. 일반 구문 공백은 정리하므로 `select A,B from EMP`는 `SELECT A, B FROM EMP`, 서브쿼리는 `(SELECT ...)`로 정리한다. 콤마를 다른 줄로 옮기지 않으며 GROUP과 BY 사이에 사용자가 넣은 줄바꿈도 유지한다.

이미 행 시작에 있는 FROM·WHERE·AND 등 키워드와 콤마는 기존 SELECT T 열 기준의 들여쓰기를 적용한다. CTE·서브쿼리 본문과 CASE도 사용자가 줄을 나눈 곳에서만 들여쓰기를 정리한다. 다음 예시는 이 행 구성을 사용자가 입력한 경우다.

```sql
SELECT COL1
     , COL2
  FROM TABLE
 WHERE COL1 = COL
   AND COL2 = COL
 GROUP BY COL1
 ORDER BY COL2
```

함수 인자와 BETWEEN을 포함해 원래 토큰 사이의 줄바꿈을 유지한다. 문자열·인용 식별자·주석·파라미터 내용은 보존한다. 선택 SQL만 Source에 반영되며 한 번의 Undo/Redo로 복원한다. 같은 결과로 반복 포맷하면 새 편집 명령을 만들지 않는다.

읽기 전용·오류 XML·XML 하위 태그나 주석이 섞인 SQL 본문은 포맷할 수 없다. 닫히지 않은 인용·블록 주석·괄호·CASE나 과도한 입력은 원문을 유지하고 안내한다. 제한은 262,144자·20,000토큰·괄호와 CASE 깊이 48이다. SQL 오류 자동 교정이나 DB별 모든 문법 처리를 보장하지 않는다.

UPDATE·INSERT·MERGE의 절·목록·분기도 원래 행 구성을 유지하며 이미 나눈 줄의 들여쓰기만 정리한다. 한 줄 입력은 한 줄로 남는다. 포맷 버튼은 제공하지 않는다.

선택 영역이 있으면 선택한 텍스트만 포맷하며, 없으면 현재 쿼리 전체에 적용한다. 선택 밖 SQL과 선택 경계 줄바꿈은 유지한다. 주석만 있는 줄은 들여쓰기와 앞뒤 공백까지 보존한다. 주석·문자열·파라미터 중간에서 선택이 끊기면 변경하지 않고 안내한다.

서브쿼리 본문이 이미 다음 줄에 있으면 여는 괄호 열보다 4칸 안쪽으로 SELECT를 들여쓰고, 별도 줄의 닫는 괄호는 여는 열에 맞춘다. SELECT가 여는 괄호와 같은 줄에 있으면 실제 SELECT 시작 위치를 기준으로 후속 FROM·WHERE 등의 마지막 글자를 SELECT의 T 열에 맞춘다. 중첩 쿼리마다 기준을 독립적으로 계산한다. INSERT INTO·VALUES와 목록의 여는 괄호·항목·닫는 괄호도 기존 줄바꿈을 유지한다.

PIVOT·UNPIVOT도 원래 줄바꿈을 유지한다. 이미 별도 줄로 나눈 본문·목록·닫는 괄호에 기존 들여쓰기 규칙을 적용하며 INCLUDE/EXCLUDE NULLS와 튜플·인용 값·주석을 보존한다. 포매팅은 빠진 구분자를 임의로 추가하는 구문 교정을 수행하지 않는다.

## MERGE 정렬과 변경문 컬럼 추적

SQL 포맷은 키워드·식별자·함수·별칭의 대소문자와 기존 줄바꿈을 그대로 유지하고 공백·들여쓰기만 정리한다. MERGE의 USING 서브쿼리는 SELECT 4칸·FROM 6칸, 괄호 ON의 단일 조건은 4칸, 복수 조건의 첫 항목은 8칸·AND는 4칸으로 정렬한다. SET은 3칸이며 MERGE 내부 UPDATE도 WHERE·AND·OR의 끝을 UPDATE 끝에 맞춘다. INSERT·VALUES 목록은 첫 항목 6칸·콤마 4칸으로 정렬한다.

TRACE는 SELECT·CTE·파생 쿼리 사이의 컬럼 전달과 같은 소스를 사용하는 변경문 참조 전체를 표시한다. 출처·중간·사용처 어디를 선택해도 전체 경로를 보여준다. SELECT의 `'B' AS TEST`에서 `'B'` 같은 값 토큰을 선택해도 TEST 출력과 이후 참조를 추적한다. UPDATE·INSERT·MERGE·DELETE의 컬럼 참조는 자기 테이블 선언 또는 SELECT 출처와 동일 소스의 모든 참조를 포함한다. SET·INSERT 대상도 같은 규칙을 사용하며 대입 반대편 컬럼이나 단순 VALUES 상수와는 연결하지 않는다. 테이블명·한정자를 선택하면 기존 테이블 추적을 사용한다.

모델 캔버스에서 a는 Activity 추가, w는 Wire 추가, s는 Snap, o는 Orthogonal, p는 파일 속성 편집 단축키다. 버튼 툴팁에도 각 키를 표시한다. QueryEditor 우측 쿼리 ID 목록에서는 포커스를 둔 채 글자를 입력하면 IntelliJ 기본 Speed Search로 항목을 찾는다.

## 추적 출처로 이동과 탐색 기록

SQL에서 컬럼·테이블을 **Ctrl+왼쪽 클릭**하면 기존 추적 경로의 한 단계 이전 출처로 이동한다. 테이블 한정자는 소스 선언으로, CTE 참조는 정의로 이동한다. 출처가 여러 개면 이름·행·SQL 문맥이 있는 목록에서 선택하고, 출처가 없으면 이동하지 않는다.

SQL 추적은 IDE의 뒤로·앞으로 단축키를 점유하지 않는다. Ctrl+클릭과 트리 클릭·Enter로 출처에 이동할 수 있으며, 별도 추적 이력·탐색 버튼은 제공하지 않는다. 이동 자체는 SQL과 원본 XML을 변경하지 않는다.

## Ctrl 안내와 동명 컬럼 추적

Ctrl을 누른 채 이동 가능한 컬럼·테이블 위에 마우스를 올리면 밑줄과 손 모양 커서로 안내한다. Ctrl+클릭은 SQL 출처 이동 전용 액션으로 처리한다.

컬럼 추적은 **이름이 같은 구간만** 연결한다. `id AS employee_id`처럼 컬럼명이 바뀌는 지점에서는 추적과 Ctrl 이동을 멈춘다. 비인용 이름은 대소문자를 구분하지 않고 인용 이름은 기존 SQL 식별자 비교 규칙을 따른다. 테이블도 이름이 바뀌는 경계에서 멈추며, 테이블 별칭은 원래 테이블의 참조로 취급한다.

컬럼에 커서를 놓으면 같은 출처·이름으로 연결된 출력과 참조를 회색 배경으로 표시한다. 조건절의 같은 컬럼도 표시하지만 이름만 같은 다른 테이블의 컬럼은 섞지 않는다. 커서·쿼리를 바꾸거나 SQL을 수정하면 강조를 갱신한다. 밑줄과 배경은 SQL 텍스트를 변경하지 않는다.

테이블을 선택하면 같은 테이블의 이름·별칭 선언·한정자 참조도 회색으로 표시한다. `FROM employee e`의 `employee`와 `e.id`의 `e`는 함께 표시하지만, `WITH t AS (SELECT ... FROM employee)`의 `t`에서는 다른 이름의 `employee`로 추적하지 않는다. 같은 철자의 다른 FROM 바인딩·문장·중첩 CTE는 임의로 합치지 않는다.
