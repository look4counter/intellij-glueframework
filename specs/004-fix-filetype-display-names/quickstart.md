# 검증 안내

1. 프로젝트의 JDK와 Gradle 환경에서 `./gradlew.bat test --tests com.look4counter.EditorFileTypeTest`를 실행한다.
2. 표시 이름 유일성, XML 언어와 PSI, 확장자 등록 및 편집·저장 테스트가 모두 통과해야 한다.
3. `git diff --check`가 통과해야 한다.
4. 필요하면 개발 IDE를 재시작하고 파일 형식 설정에 두 고유 표시 이름이 보이며 기존 중복 오류가 없는지 확인한다.

## 실행 결과 (2026-10-24)

- 수정 전 새 표시 이름 테스트는 예상대로 실패하여 기존 오류를 재현했다.
- 수정 후 `./gradlew.bat test --tests com.look4counter.EditorFileTypeTest`가 성공했다. 테스트 5개 통과, 실패 및 건너뜀 0개이다.
- `git diff --check`가 통과했다.
- 개발 IDE의 수동 재시작 검증은 수행하지 않았다.
