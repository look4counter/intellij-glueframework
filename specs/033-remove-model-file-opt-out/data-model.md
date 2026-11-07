# 데이터 모델

- 호환 입력: noUseGlueUmlAdFile=true인 이전 XML도 역직렬화할 수 있다.
- 유효 상태: loadState에서 복사한 값의 noUseGlueUmlAdFile은 false로 고정한다.
- 반환·재저장: 방어적 복사로 false를 반환하며 다른 저장 경로·preset·색상은 보존한다.
