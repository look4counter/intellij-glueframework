# 검증 절차

1. `./gradlew.bat test --tests com.look4counter.DatabaseMetadataTest --tests com.look4counter.QuerySqlEditorTest`를 실행한다.
2. 두 Datasource의 JDBC JAR과 접속 정보를 설정하고 프로젝트를 다시 연다.
3. 에디터 선택 없이 전체 DB 조회가 실행되는지 확인한다.
4. 에디터에 Datasource 도구 모음이 없고 중복 없는 전체 후보가 나타나는지 확인한다.
5. 한 DB 접속 실패 시 다른 결과 유지 및 안전한 실패 상태를 확인한다.
6. 설정 변경과 설정 화면의 전체 새로고침으로 결과가 갱신되는지 확인한다.
