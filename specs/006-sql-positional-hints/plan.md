# 구현 계획: 위치 파라미터 색상과 인레이 힌트

## 기술 맥락

Kotlin/JVM, IntelliJ Platform 2025.3.6.1, 기존 EditorTextField 기반 SQL 편집기와 JUnit을 사용한다.
SQL은 별도 문서이므로 PSI 제공자 등록 대신 생성된 편집기의 InlayModel에 기본 파라미터 힌트를 추가한다.

## 헌장 확인

헌장은 미작성 템플릿이다. 한글 문서 및 전체 speckit 절차를 준수한다. 설계 전후 위반은 없다.

## 설계와 단계

1. queryeditor/sql/SqlLexer.kt에서 인용문·주석 외의 ?를 기존 PARAMETER 토큰으로 분류한다.
2. sql/SqlParameterHints.kt는 같은 lexer의 PARAMETER 중 길이 1인 ?만 순서대로 수집한다.
3. IntelliJ ParameterHintsPresentationManager로 물음표 앞에 0: 형식의 기본 인레이를 만든다.
4. QuerySqlEditor.createEditor에서 설치하고 DocumentListener로 SQL 변경 시 소유한 인레이만 갱신한다.
5. 리스너는 EditorUtil.disposeWithEditor에 연결한 Disposable에 등록하고 소유한 힌트도 해제한다.
6. QuerySqlEditorTest.kt에서 실제 토큰 색상, 힌트 위치와 순번, 제외 영역, 문서 편집·전환·저장·실행 취소를 검증한다.

XML 문서나 SQL 원문에는 힌트 텍스트를 쓰지 않는다. 기존 자동완성과 이름 파라미터 색상은 재사용한다.
추가 외부 의존성은 없다. 관련 설계는 research.md·data-model.md·contracts/editor.md·quickstart.md에 정의한다.


## 시작 순번 구현 계획

SqlParameterHints의 카운터를 0부터 후위 증가시키고 QuerySqlEditorTest의 순번 기대값을 0부터로 바꾼다.
밝은·어두운 테마 렌더링, 편집·실행 취소·쿼리 전환·저장 검증을 다시 수행한다.
