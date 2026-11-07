# 빌드·설치 검증 안내

## 준비와 실행

JDK 21과 네트워크 또는 필요한 의존성 캐시가 필요하다. GRADLE_USER_HOME은 저장소 내부 경로로 지정한다. IDE ZIP 추출 위치의 jbr을 JAVA_HOME으로 지정해 빌드한다.

전체 test와 buildPlugin을 실행하고 XML 결과의 총 사례·실패·오류·건너뜀을 합산한다. verifyPluginStructure로 구조를 검사하고 buildSearchableOptions를 새로 실행해 격리 IDE 기동을 확인한다. 생성 ZIP의 필수 항목과 최신 클래스, SHA-256을 검사한다.

## 실제 사용자 IDE 확인

생성 ZIP을 사용자 IDE의 디스크 설치 기능으로 설치하고 업무 파일 복사본에서 Design/Source 전환·Commit·저장·닫기·재열기를 확인한다. 이번 자동 검증에서 수행한 범위와 수동으로 남은 범위를 결과에 명확히 구분한다.

## 검증 결과: 2026-11-07

### 환경과 복구

- 기존 IDE ZIP 캐시 누락을 정상 의존성 다운로드로 해소했다. 다운로드한 idea-2025.3.6.1-win.zip은 1,532,765,783바이트다. 임시 로컬 IDE 교체 스크립트는 archive/local 중복 등록으로 실패해 최종 빌드에서는 사용하지 않았다.
- 시스템 기본 JDK 25 대신 대상 IDE에 포함된 JBR 21.0.11을 JAVA_HOME으로 지정했다. Kotlin 컴파일 데몬 연결 실패는 실행 옵션으로 in-process 방식을 지정해 피했다. 저장소 빌드 설정이나 의존성 버전은 변경하지 않았다.

### 테스트와 설치 정보 보완

- 최초 정식 전체 실행: 400개 중 398개 통과, 2개 실패, 오류·건너뜀 0개. 실패 사례는 Wire 저장·Undo/Redo와 종료 상태 링 배경 검사였다.
- Wire 테스트는 편집기·스크롤·뷰포트를 실제 크기로 배치하고 양 끝점이 visibleRect에 포함됨을 먼저 확인하도록 수정했다. 미리보기·한 번 생성·양쪽 참조·Undo/Redo 검사는 보존했다.
- 종료 상태 테스트는 내부 원의 반투명 경계 픽셀 대신 빈 링 상하좌우의 배경을 확인하도록 보완했다. 검정 중심과 캔버스 크기 보존 검사는 유지했다. 제품 렌더링과 입력 코드는 변경하지 않았다.
- 수정한 두 사례의 집중 실행이 통과했고, 이후 전체 400개가 실패·오류·건너뜀 각각 0개로 통과했다. 설치 정보 보완 후에도 전체 실행 결과는 400개 통과다.
- 구조 검사에서 기본 소개·공급자·주소 및 이름의 IntelliJ 표기 오류를 발견했다. 표시 이름은 GlueFramework, 공급자는 실제 Git 원격 소유자 look4counter와 저장소 URL, 소개는 실제 기능 설명으로 바꿨다. 플러그인 ID와 버전은 유지했다. 첫 소개 문장은 구조 검사 규칙에 맞는 영문으로 보완했다.
- 마지막 전체 테스트 이후 변경은 소개 첫 문장뿐이며 제품 클래스는 변경하지 않았다. 해당 최종 설명을 포함해 ZIP 재생성과 verifyPluginStructure를 다시 실행했고, 설명 관련 경고도 사라졌다.

### SVG 조사

기존 캐시와 새 IDE의 jsvg 및 util-8 JAR SHA-256이 같았다. 정식 Gradle 테스트는 PathClassLoader를 사용하며 최종 테스트 로그에서 IllegalAccessError와 JSvgDocumentFactory 오류가 0건이다. 이전 보조 실행은 core classloader를 강제했다. 따라서 보조 실행의 클래스 로딩 차이를 원인 후보로 좁혔으며, 라이브러리 손상이나 제품 코드 결함으로 단정하지 않았다.

### 산출물과 구조 검사

- 설치 파일: build/distributions/intellij-glueframework-1.0.3.zip.
- SHA-256: ED538C7768FCA947777E23FDDCD6CA927FD1D11643A607BEFB4AD2EB6078647B.
- 해시 파일: build/distributions/intellij-glueframework-1.0.3.zip.sha256.
- 최종 buildPlugin과 verifyPluginStructure는 성공했으며 구조 검사 경고가 없다.
- ZIP의 메인 JAR 해시가 build/libs의 최신 빌드 JAR과 일치했다. 저장하지 않은 서비스 변경을 보호하는 코드가 실제 클래스에 포함됨을 확인했다. 해당 클래스의 바이트코드 버전은 Java 21인 65다.
- plugin.xml, 두 모듈 등록 XML, 모델 저장 리스너, 쿼리 편집기, JDBC 정리 클래스와 DBeaver 라이선스·출처 고지를 확인했다. ELK·EMF·Guava 등 런타임 JAR도 포함됐다. 플러그인 ID는 com.look4counter.intellij-glueframework, 버전은 1.0.3, 최소 빌드는 253이다.

### 기동 검증의 제한

buildSearchableOptions를 강제로 재실행해 격리 IDE 기동을 시도했다. 그러나 IDE 디렉터리 잠금용 소켓을 만드는 단계에서 Permission denied: bind가 발생해 플러그인 로딩 전에 종료 코드 3으로 실패했다. 이 결과는 IDE 기동 성공이나 사용자 IDE 설치 완료로 간주하지 않는다.

최종 ZIP 빌드는 buildSearchableOptions만 제외하고 기존 검색 옵션 산출물을 재사용했다. 설치 ZIP 생성·구조 검사·전체 테스트 성공과 검색 옵션 재생성 실패를 구분한다. 실제 사용자 IDE 설치, 업무 파일 복사본의 편집·저장·재열기와 IDE 재시작 후 확인은 수행하지 않았다. 권한 제한이 없는 사용자 환경에서 아래 기본 명령과 수동 확인 절차를 마무리해야 한다.

## 재현 명령

일반 개발 환경에서는 설치한 JDK 21을 JAVA_HOME으로 지정하고 실행한다.

```powershell
.\gradlew.bat test buildPlugin verifyPluginStructure
```

이번 검증 환경의 실제 실행 설정은 다음과 같다. 캐시의 해시 디렉터리는 환경마다 다를 수 있다.

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-validation'
$env:JAVA_HOME = Join-Path $PWD '.gradle-validation/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr'

# 두 실패 사례를 보완한 뒤 집중 검증
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test --tests 'com.look4counter.ModelDiagramPropertiesTest.testWireCreationPersistsBothReferencesAndSingleUndoRedo' --tests 'com.look4counter.ModelDiagramTest.testFinalStateRingKeepsBackgroundAndCanvasDoesNotShrink'

# IDE 기동 확인 시도: 이 환경에서는 소켓 권한 오류로 실패
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' buildSearchableOptions --rerun buildPlugin verifyPluginStructure

# 전체 테스트·ZIP·구조 검사: 검색 옵션 재생성만 제외
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' test buildPlugin verifyPluginStructure -x buildSearchableOptions

# 최종 소개 첫 문장 보완 후 ZIP·구조 재검증
.\gradlew.bat --offline --no-daemon '-Pkotlin.compiler.execution.strategy=in-process' buildPlugin verifyPluginStructure -x buildSearchableOptions
```

## 검증 근거

- build/reports/release-baseline-results와 release-baseline-summary.json: 최초 두 실패.
- build/reports/release-focused-results: 수정한 두 사례 통과.
- build/test-results/test와 build/reports/release-final-test-summary.json: 최종 전체 400개 통과.
- build/reports/release-artifact-summary.json: 설치 정보·필수 항목·런타임 JAR·해시 대조 결과.
- .gradle-validation/release-focused.log, release-verified.log, release-artifact.log: 성공한 집중·전체·최종 산출물 검증.
- .gradle-validation/release-startup.log: 소켓 권한으로 IDE 시작에 실패한 근거.
- .intellijPlatform/sandbox/intellij-glueframework/IU-2025.3.6.1/log-test/idea.log: 정식 테스트 IDE 로그.

명세 품질 점검은 6개 모두 통과했다. 최종 명세·계획·작업 분석은 요구사항 6개, 작업 9개로 대응률 100%이며 모호함·중복·중요 충돌이 없다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다.
