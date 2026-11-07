# 검증 안내

## 자동 검증

### 기본 템플릿과 변수 설명 변경

최신 요청에 따라 기본 예시의 첫 내용 행을 QueryId 표기로 바꾸고 하단에 쿼리 ID·설명·현재 날짜와 시간의 의미를 표시했다. FR-007·FR-011 및 T015의 일관성 분석을 완료했으며 추가 명확화 질문은 없다. 사용자 예시 전체와 기본 문자열을 정확히 대조했고 변수별 설명과 실제 지원하는 QUERY_DESCRIPTION 표기를 확인했다. 차이 검사도 통과했다. 단순 문자열 변경으로 새 테스트와 빌드는 수행하지 않았다. 수렴 검토에서 기본값·안내 이외의 저장 및 치환 동작은 유지되며 남은 작업은 없음을 확인했다.

로컬 JBR 및 Gradle 캐시로 QueryCommentTemplateTest, DataSourceSettingsTest, QueryDesignEditorTest를 실행한다. 관련 편집기 회귀와 queryeditor 컴파일을 확인한다.

## 사용자 확인

1. Settings > GlueQueryEditor > Tool에서 예시 템플릿을 수정하고 적용한다. 초기화와 화면 재생성도 확인한다.
2. 쿼리 ID와 설명, SQL을 수정한 직후 아래로 오른쪽의 주석 추가를 누른다.
3. SQL 맨 앞에 최신 ID·설명·현재 날짜가 표시되고 Source에도 같은 값이 있는지 확인한다.
4. 실행 취소와 다시 실행, 반복 클릭, 빈 템플릿, 읽기 전용 및 중첩 태그 쿼리를 확인한다.

## 구현 전 일관성 분석

명세·계획·작업 목록을 읽기 전용으로 대조했다. 요구사항 7개 모두 T002~T007에 대응하고 성공 기준 3개는 T005·T007·T008로 검증한다. 대응률은 100%이며 중복·모호함·충돌·미대응 작업은 없다. 품질 점검 8/8 통과. 헌장은 미작성 템플릿이므로 규범 검사는 생략했고 확장 훅은 없다.

## 자동 검증 결과: 2026-11-08

- QueryCommentTemplateTest 2개, DataSourceSettingsTest 14개, QueryDesignEditorTest 36개로 총 52개가 통과했다. 실패와 오류는 0개다.
- 예시의 두 변수 표기, 날짜·시간, 알 수 없는 변수와 잘못된 날짜 형식 보존, 치환 값 재해석 방지 및 기존 SQL 보존을 확인했다.
- 설정 변경 감지·초기화·적용·화면 재생성·직렬화 및 빈 템플릿 저장을 확인했다.
- 아래로 바로 오른쪽의 버튼 배치, 최신 ID·설명·SQL 반영, CDATA 종료 문자열과 특수문자 보존, Source 동기화, 실행 취소·다시 실행을 확인했다.
- SQL 편집 불가·빈 쿼리 목록·읽기 전용 및 지연 실행 중 선택 변경 보호를 확인했다. 기존 편집기 및 데이터소스 설정 테스트도 함께 통과했다.
- 근거: .gradle-validation/query-comment-focused.log 및 build/test-results/test의 해당 클래스 XML 결과. 실제 사용자 IDE의 수동 조작은 수행하지 않았다.

## 패키지와 최종 수렴

설치 ZIP 빌드와 플러그인 구조 검사가 성공했다. 기존 환경의 검색 옵션 재생성 제약에 맞춰 buildSearchableOptions만 제외했다. 산출물은 build/distributions/intellij-glueframework-1.0.4.zip이며 SHA-256 파일도 갱신했다. 패키지 검증 로그는 .gradle-validation/query-comment-package.log이다.

speckit-converge에서 요구사항 7개·성공 기준 3개·수용 시나리오 5개와 프로젝트 저장, 설정 수명주기, 단일 패스 치환, 기존 QueryEdit 재사용 및 지연 선택 보호의 설계 결정 5개를 대조했다. 누락·부분 구현·충돌·범위 밖 추가는 모두 0개다. 헌장 규범은 미작성 템플릿으로 생략했다. 구현은 명세·계획·작업에 수렴했으며 수렴 단계에서 tasks.md는 변경하지 않았다. 추가 구현 작업은 없다.

## 설정 하위 항목 분리 검증

- 구현 전 명세·명확화·계획·작업 목록을 갱신하고 일관성 분석을 수행했다. FR-008·FR-009는 T010~T012에 대응하며 미해결 질문·누락·충돌은 없다. 기존 명세 품질 점검은 모두 통과한다.
- DataSourceSettingsTest 14개가 통과했다. 실제 Settings 트리의 Tool·Database, 페이지별 구성과 독립 적용, 포매팅·주석 영속 저장 및 데이터소스·비밀번호·접속 검증을 확인했다.
- QuerySqlEditorTest의 실제 로컬 JDBC 드라이버를 통한 메타데이터 새로고침 검증 1개도 통과했다. 메타데이터 도구는 Database에 존재하며 편집기에는 추가되지 않는다.
- buildPlugin과 verifyPluginStructure가 성공했다. 기존 환경 제약에 따라 buildSearchableOptions만 제외했다. 설치 ZIP과 SHA-256을 갱신했다.
- 근거: .gradle-validation/query-settings-split.log 및 .gradle-validation/query-settings-split-package.log. 실제 사용자 IDE에서의 수동 조작은 수행하지 않았다.

## 기본 상위 설정 화면 검증

- 명세·명확화·계획·작업 목록을 갱신하고 구현 전 일관성을 분석했다. FR-010은 T013·T014에 대응하며 미해결 질문·누락·충돌은 없다.
- 로컬 SDK의 PlatformExtensions.xml과 ConfigurableGroupEP·ConfigurableEditor를 확인해 IntelliJ 기본 설정 그룹을 사용했다. 설명과 직계 하위 설정 링크는 플랫폼이 생성한다.
- GlueFramework를 groupConfigurable로 등록하고 두 편집기를 groupId로 연결했다. 하위 설정 ID와 Tool·Database 계층 및 저장 서비스는 유지했다.
- buildPlugin 및 verifyPluginStructure가 성공했다. 기존 환경 제약에 따라 buildSearchableOptions만 제외했다.
- DataSourceSettingsTest 14개가 모두 통과했다. 실제 플랫폼 그룹의 한국어 설명, 기본 화면 위임, 두 편집기와 Tool·Database 계층 및 기존 저장·연결·비밀번호 회귀를 검증했다.
- 설치 ZIP과 SHA-256을 갱신했다. 근거: .gradle-validation/settings-native-group.log 및 build/test-results/test/TEST-com.look4counter.DataSourceSettingsTest.xml. 실제 사용자 IDE에서의 수동 클릭은 수행하지 않았다.

## 쿼리 그룹 링크 및 ResultType 검증

- 요청 세 가지를 명세·명확화·계획·작업 목록에 반영하고 구현 전 일관성을 분석했다. FR-007·FR-011~FR-013은 T016~T019에 대응하며 미해결 질문·누락·충돌은 없다.
- GlueQueryEditor는 GlueFramework 아래의 플랫폼 기본 그룹으로 등록했다. Tool·Database의 기존 페이지 ID 및 저장 서비스는 유지하며 상위 화면의 설명과 링크는 플랫폼이 생성한다.
- 기본 예시와 하단 설명은 달러 표기로 통일했다. 기존 저장 템플릿 및 샤프 문법의 치환 호환성은 유지한다.
- QUERY_RESULT_TYPE은 최신 쿼리 snapshot에서 가져오며 빈 값은 빈 문자열로 치환한다. 단일 패스 치환과 기존 Source 반영·실행 취소 경로를 사용한다.
- DataSourceSettingsTest 14개, QueryCommentTemplateTest 3개, QueryDesignEditorTest 36개로 총 53개가 통과했다. 실패·오류는 0개다. 두 기본 그룹과 하위 계층, 달러 표기 기본 예시, ResultType 빈값·단일 패스·최신 편집 및 Source 동기화를 확인했다.
- buildPlugin과 verifyPluginStructure도 통과했다. 기존 환경 제약에 따라 buildSearchableOptions만 제외했다. 근거: .gradle-validation/query-group-result-type.log 및 클래스별 XML 결과. 실제 사용자 IDE 수동 조작은 수행하지 않았다.
- 최종 수렴에서 요구사항 13개와 해당 계획·작업을 대조했으며 누락·부분 구현·충돌은 0개다. 추가 구현 작업은 없다.
