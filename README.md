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

## 쿼리 Design 사용

queryeditor/sample-query.glue_sql을 열면 queryMap의 desc와 문서 순서의 query.id 목록이 오른쪽에 표시된다. 목록에서 쿼리를 선택하면 왼쪽에 CDATA를 제외한 SQL 본문이 표시되고 오른쪽 ID·Description·ResultType 입력과 체크박스도 함께 바뀐다.

최상단 queryMap desc도 직접 수정할 수 있다. isNamed는 resultType 입력 오른쪽에 있다. 하단 쿼리 개수는 표시하지 않으며 필요한 상태 안내만 나타난다.

SQL이나 속성을 수정하면 Source XML에도 반영된다. id 변경 후에도 선택한 쿼리를 유지하며 저장은 IDE의 일반 저장 기능을 사용한다. 오른쪽 영역은 약 320픽셀을 기본 기준으로 하고 구분선을 끌어 너비를 조절할 수 있다.

SQL 편집기는 키워드·숫자·따옴표 토큰을 색으로 표시하고 SQL 키워드 자동완성을 제공하는 다중 행 편집기다. SQL 오류 검사·실행은 제공하지 않는다. XML이 잘못되었으면 Source에서 먼저 수정해야 한다. query 본문에 하위 태그나 주석이 있으면 SQL 덮어쓰기를 제한하고 Source 편집을 안내한다. query 추가·삭제는 아직 제공하지 않는다.

## 개발 및 검증

기존 대상 IDE는 IntelliJ IDEA 2025.3.6.1이고 Kotlin은 2.3.20이다. Gradle을 실행할 JDK와 최초 의존성 다운로드를 위한 네트워크가 필요하다.

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

Design SQL 입력은 DBeaver의 공통 SQL 키워드 367개를 주황색, 숫자를 파란색, 작은따옴표·큰따옴표 토큰을 녹색으로 표시한다. 주석은 IDE 기본 주석 색을 따른다. 밝은 테마와 어두운 테마에서 읽을 수 있도록 색상을 조정한다.

자동완성은 대소문자를 구분하지 않는 접두어 검색을 사용한다. 입력 중 자동 팝업은 IDE 설정을 따르고 Ctrl+Space로 직접 호출할 수 있다. 문자열·주석·숫자·파라미터 내부 및 읽기 전용 SQL 입력에서는 후보를 제시하지 않는다. 공통 SQL 키워드와 선택 DB에서 조회한 테이블·컬럼 이름을 제공한다. DB 전용 키워드·함수 후보는 추가하지 않는다. Source XML 표시와 ResultType의 Java 클래스 완성은 유지한다.

키워드 출처는 [DBeaver SQLConstants.java](https://github.com/dbeaver/dbeaver/blob/f2e25edd7d2eecd4d6541cdce70fdb25c7b07ea9/plugins/org.jkiss.dbeaver.model/src/org/jkiss/dbeaver/model/sql/SQLConstants.java)이며 Apache-2.0 라이선스와 출처 고지를 배포물에 포함한다. 실행 시 DBeaver 설치나 인터넷 연결은 필요하지 않다.
## SQL 표시 여백과 프로젝트 DataSource

`:parameter`는 cyan으로 표시한다. 문자열·주석 안에서는 기존 색을 유지하며 `::type` 형 변환과 `:=` 대입은 제외한다. 파라미터 내부에서는 키워드 완성을 표시하지 않는다. 우측 쿼리 ID 목록에 좌우 각각 8픽셀 여백을 추가하고 줄 번호 오른쪽에는 18픽셀 공간을 예약한다. 모두 IDE 배율을 적용한다.

Settings > GlueFramework > GlueQueryEditor에서 프로젝트별로 여러 DataSource를 추가·수정·삭제할 수 있다. 이름과 JDBC URL은 필수이며 드라이버 클래스·사용자명·비밀번호는 선택 사항이다. 이름 중복은 허용하지 않는다. Apply/OK로 저장하고 Cancel/Reset으로 미적용 변경을 버린다.

접속 메타데이터는 프로젝트의 `.idea/glue-query-editor.xml`에 저장한다. 비밀번호는 IDE 보안 저장소에 분리하며 입력란을 비워 두면 기존 값을 유지한다. 삭제 체크박스 또는 DataSource 삭제로 저장된 비밀번호를 제거한다. DB 연결은 테이블·컬럼 메타데이터를 조회할 때 사용하며 SQL 실행은 제공하지 않는다.

설정 화면은 가운데 크기 조절 구분선 없이 고정된 목록과 폼을 사용한다. DB 종류는 Oracle, MySQL, MariaDB, PostgreSQL, Impala 중에서 선택한다. 기본 드라이버 클래스가 채워지며 수정할 수 있다. JDBC JAR은 여러 로컬 파일을 고르거나 세미콜론으로 구분하여 지정한다. 자동 다운로드 버튼은 HTTPS JAR/ZIP을 받아 경로를 초안에 반영한다. 네 DB는 기본 주소를 제공하며 Impala는 공급자의 직접 배포 주소를 입력해야 한다. 다운로드 파일은 IDE 시스템 캐시에 보관한다.

Design 상단에서 DataSource를 선택하고 **메타데이터 새로고침**을 누르면 접근 가능한 테이블과 각 테이블의 컬럼 정보를 읽는다. 테이블명·컬럼명의 중복 없는 목록이 키워드와 함께 자동완성 후보로 표시된다. 원본 이름이 정확히 같으면 중복을 제거하며 접두어 검색은 대소문자를 구별하지 않는다. 프로젝트의 선택 DB는 유지하고 메타데이터 캐시는 메모리에만 보관하므로 IDE 재시작 후 다시 조회해야 한다. 실패·취소는 직전 성공 결과를 유지하고 설정 변경·삭제는 캐시를 무효화한다. 자동완성 후보를 조회할 때는 DB에 접속하지 않는다.

