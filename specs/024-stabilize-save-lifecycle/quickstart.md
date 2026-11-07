# 검증 안내

## 자동 검증

Gradle test와 buildPlugin을 실행한다. 의존성 제약이 있으면 기존 .gradle-validation 인자를 기반으로 현재 소스와 관련 테스트를 별도 컴파일하고 JUnitCore를 실행한다. 구체 명령·결과는 아래에 기록한다.

미저장·동일 내용 미저장·읽기 전용 출력 보호와 정상 생성, 직접 편집 탭 재선택·Commit·저장·재열기, Source 충돌 거부·Rollback을 확인한다. 기존 쿼리 저장·탭 동기화와 모델 관련 회귀 검증을 포함한다.

## 실제 IDE 수동 확인

실제 업무 파일의 복사본으로 Design 수정→Source 전환→Design 복귀→Commit→저장→닫기·재열기를 수행한다. Source 수정 중 Commit 거부와 Rollback 복구를 확인한다. 모델 모드에서는 출력 서비스에 미저장 변경을 만들고 모델을 저장해 알림과 내용 보존을 확인한다. 이 절차는 자동 테스트만으로 수동 확인 완료라고 표시하지 않는다.

## 검증 결과: 2026-11-07

- 수정 전 실행에서 미저장 서비스 덮어쓰기와 읽기 전용 문서 변경 예외를 재현했다. 초기 보조 실행의 샘플 리소스 누락은 테스트 클래스패스에 src/test/resources를 넣어 해결했다.
- 현재 모델·쿼리 소스와 관련 테스트 직접 컴파일에 성공했다. JDBC 정리용 Java 클래스도 --release 21로 별도 컴파일했다.
- 저장 생명주기 집중 검증 43개가 모두 통과했다. 대상은 ModelEditorServiceExportTest, ModelServiceDirectEditorTest, QuerySaveListenerTest, QueryTabNavigationTest다. SVG 접근 오류와 TeamCity 실패 로그는 이 집중 실행에서 0건이다.
- 확대 검증은 서비스·클립보드·성능·미니맵·확대·배치·Wire·쿼리 Design을 포함해 JUnit 158개가 통과했다. 다만 이 보조 실행에서는 쿼리 외관 검사 중 IntelliJ SVG 렌더러의 IllegalAccessError와 TeamCity 오류 로그가 별도로 발생했다. 따라서 정식 IDE 테스트 환경의 완전한 무오류 실행으로 간주하지 않는다.
- Gradle의 --offline test buildPlugin은 idea-2025.3.6.1-win.zip의 캐시 누락으로 의존성 해결 단계에서 실패했다. 테스트 태스크 실행과 배포 ZIP 생성은 완료하지 못했다. 네트워크가 가능한 정식 개발 환경에서 다시 실행해야 한다.
- 실제 업무 파일을 사용한 IDE 수동 조작과 IDE 프로세스 재시작은 수행하지 않았다. 자동 재열기 검증은 Commit·저장 후 편집기를 폐기하고 새 편집기를 만드는 방식이다.
- git diff --check에서 공백 오류 없음.

### 실행 명령과 증거

작업 디렉터리는 저장소 루트다. .gradle-validation/stability-compile.args는 기존 wire-release-compile.args를 기반으로 현재 queryeditor 소스·추가 테스트를 포함하고 출력 디렉터리를 stability-classes로 분리했다. stability-resources에는 현재 모듈 등록 XML과 검증용 플러그인 서술자를 배치했다. 해당 디렉터리는 Git 제외 대상이다.

```powershell
javac --release 21 -d .gradle-validation/stability-classes queryeditor/src/main/java/com/look4counter/queryeditor/database/JdbcDriverCleanup.java
$compileArgs = Get-Content .gradle-validation/stability-compile.args
$compilerClasspath = $compileArgs[([Array]::IndexOf($compileArgs, '-classpath') + 1)].Trim('"')
java -cp $compilerClasspath org.jetbrains.kotlin.cli.jvm.K2JVMCompiler '@.gradle-validation/stability-compile.args'
java '@.gradle-validation/stability-lifecycle-test.args'
java '@.gradle-validation/stability-test.args'
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
.\gradlew.bat --offline test buildPlugin
```

결과 로그는 .gradle-validation의 stability-red-test.log, stability-compile.log, stability-lifecycle-test.log, stability-test.log, stability-gradle.log에 있다.

### 수렴 점검

FR-001~003은 쓰기 직전 미저장·문서 쓰기 가능 검사, 기존 오류 알림 경로와 3개 자동 생성 테스트 및 기존 정상 생성 테스트에 대응한다. FR-004~005는 새 직접 편집 회귀 테스트 2개와 기존 충돌 방지·Rollback 구현에 대응한다. 성공 기준 3개와 사용자 이야기 2개, 상태 전이·기존 알림 재사용·추가 의존성 없음의 설계 결정을 확인했다. 누락·부분 구현·명세 충돌·미요청 변경은 없으며 추가 구현 작업은 없다. 헌장은 미작성 템플릿이라 규범 검사를 생략했다. 정식 빌드와 실제 IDE 확인의 환경 제약은 위에 별도 기록했다.
