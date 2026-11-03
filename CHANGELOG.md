<!-- 변경 이력 작성 지침: https://keepachangelog.com -->

# Intellij-glueframework 변경 이력

## [Unreleased]

### 추가

- 자바 문자열 값이 쿼리 ID와 정확히 일치하면 Ctrl+클릭으로 해당 쿼리로 이동합니다. 같은 ID가 여러 곳에 있으면 이동할 대상을 선택할 수 있습니다.
- 자바 이스케이프 문자열과 텍스트 블록에서도 쿼리 ID 탐색을 지원합니다.

### 수정

- 찾기창에서 쿼리 파일을 열거나 검색 결과로 이동할 때 발생할 수 있는 `Access is allowed from write thread only` 오류를 수정했습니다.
- QueryEditor와 ModelEditor의 Design·Source 탭 생성 및 쿼리·모델 참조를 통한 파일 열기에서 스레드 잠금을 보장하도록 수정했습니다.
- 연속 검색 이동 시 마지막 요청을 유지하고, 지연 실행 전에 편집기가 닫히거나 파일이 삭제되면 해당 요청을 취소하도록 수정했습니다.
