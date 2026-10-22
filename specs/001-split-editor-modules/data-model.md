# 구성 모델

| 구성 요소 | 식별자 | 담당 내용 |
| --- | --- | --- |
| 통합 플러그인 | com.look4counter.intellij-glueframework | 설치, 배포, 기존 도구 창, 두 모듈 조립 |
| 쿼리 편집기 모듈 | queryeditor | glue_sql 파일 형식 |
| 모델 편집기 모듈 | modeleditor | glue_uml_ad 파일 형식 |

각 파일 형식은 이름, 설명, 기본 확장자, 아이콘, 텍스트 여부를 제공한다. 두 모듈 사이의 의존성과 도메인 데이터 구조는 없다.

## 멀티뷰 상태

각 파일은 하나의 VirtualFile과 XML Document를 갖는다. 쿼리 Design은 해당 문서의 선택 query를 편집하고 모델 Design은 안내 패널이다. Source는 같은 원문을 사용하는 IDE TextEditor다. 선택한 편집기와 Source의 커서·스크롤 상태는 IDE가 저장한다. 형식 이름·확장자·플러그인 식별자는 그대로 유지한다.

## 쿼리 편집 모델

| 데이터 | 내용 |
| --- | --- |
| queryMap 스냅샷 | desc, 문서 순서의 query 목록, 오류 안내 |
| query 스냅샷 | 태그 스마트 참조, id, desc, resultType, isNamed 원문, 디코딩한 SQL, SQL 본문 편집 가능 여부 |
| 선택 | 스마트 참조를 우선하며 Source 재작성 시 이전 행 위치를 보조로 사용 |
| 대기 수정 | 선택한 태그 참조, SQL 또는 속성명, 기존 값, 새 값 |
| SQL 표시 문서 | 쿼리별 일반 텍스트 PSI에 연결된 다중 행 편집 문서와 키워드 완성 제공자, 실제 저장은 Source 원문 문서 |

id는 목록 표시 값이며 내부 고유 키로 사용하지 않는다. 따라서 중복·누락·수정 중인 id도 행을 구분할 수 있다. 쿼리 추가·삭제용 데이터는 이번에 제공하지 않는다.

## 루트 설명 수정

queryMap 스냅샷은 루트 스마트 참조를 추가한다. 루트 대상 QueryEdit는 desc만 수정할 수 있다. 쿼리가 없어도 유효하며 현재 값과 기존 값이 다르면 수정하지 않는다.

## ResultType 참조 문서

ResultType 전용 Java 참조 문서는 완성용 임시 문서다. 저장 데이터는 기존 query.resultType 문자열이며 새 저장 형식은 없다. 수정은 기존 선택 태그 참조와 원래 값을 사용하여 Source 문서에 반영한다. 프로그램 표시 갱신은 원문 수정으로 다시 전달하지 않는다.

## 파일 형식 아이콘 리소스

쿼리 파일 형식은 /icons/glueSql.svg, 모델 파일 형식은 /icons/glueUmlAd.svg를 참조한다. 저장 문서와 도메인 모델의 변경은 없다.

## 속성 표시 변경

저장 모델은 그대로다. id와 desc의 화면 이름만 ID와 Description으로 바뀌며 텍스트 없는 체크박스는 기존 isNamed 불리언 값을 편집한다. ResultType의 Java 참조 문서는 완성용으로 유지한다.

## ResultType 포커스 표시 상태

입력 내부 포커스 상태는 외부 테두리 표시만 제어하며 XML 데이터에는 저장하지 않는다. 클래스 참조 문서와 기존 query.resultType 저장 방식은 그대로다.

치수 조정은 표시 크기·위치만 바꾸며 ResultType의 저장 모델과 클래스 참조 문서는 유지한다.

## SQL 토큰과 완성 상태

SQL 표시 문서는 분리된 일반 텍스트 PSI와 연결하여 IntelliJ 완성 문맥을 제공한다. 완성 제공자는 문서의 PSI에만 연결되고 실제 .glue_sql Source는 XML 언어를 유지한다. 쿼리별 버퍼 수명과 원문 저장 방식은 그대로다.

키워드 목록은 고정된 DBeaver 출처에서 추출한 367개의 대문자 값이며 강조·완성에서 같은 집합을 사용한다. 렉서 토큰은 종류와 원문 범위를 갖는다. 색상과 완성 접두어는 표시 상태이며 XML에 저장하지 않는다. DB 연결·스키마 데이터·새 영속 모델은 추가하지 않는다.
## 프로젝트 DataSource 상태

`DataSourcesState.dataSources`는 순서 있는 `DataSourceDefinition` 목록이다. 각 항목은 안정적인 UUID `id`, `name`, `jdbcUrl`, `driverClass`, `username`, `passwordStored`를 가진다. 프로젝트 PersistentStateComponent가 `glue-query-editor.xml`에 저장하며 읽기·쓰기·로딩은 깊은 복사한다. 비밀번호 문자열은 직렬화하지 않는다.

UI 초안은 정의 복사본과 미적용 비밀번호·삭제 플래그를 보관한다. 보안 저장소 키는 프로젝트 locationHash와 항목 UUID로 구분한다. 비밀번호 변경 성공 후 메타데이터를 적용하며 실패 시 이전 자격 증명 복원을 시도한다. Reset과 UI 해제 시 초안을 비운다.

## DB 메타데이터 확장

DataSourceDefinition은 databaseKind와 driverJars 목록을 추가한다. 누락된 종류는 JDBC URL로 유추한다. DataSourcesState.selectedDataSourceId는 프로젝트의 선택 DB를 유지한다. 조회와 상태의 JAR 목록도 깊은 복사한다.

TableMetadata는 catalog/schema/name과 컬럼 목록을 가진다. MetadataSnapshot은 테이블 관계와 Distinct 테이블·컬럼 이름을 제공한다. DatabaseMetadataService는 DataSource ID/정의/세대와 스냅샷을 메모리에 저장한다. 설정 변경·삭제로 무효화하고 새로고침 실패·취소는 같은 정의의 기존 성공 스냅샷을 유지한다.

