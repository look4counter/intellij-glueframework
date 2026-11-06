# 검증 안내

기존 Gradle 환경에서 ModelDiagramPerformanceTest, ModelDiagramMiniMapTest, ModelDiagramZoomTest를 실행한다. IDE에서 우클릭으로 작은 이동과 화면보다 큰 이동을 수행하고 종료 후 글자 선명도와 미니맵 위치를 확인한다. 실제 체감 성능은 PC에서 비교해야 한다.

## 실행 결과

2026-11-06: 성능 12개, 미니맵 11개, 확대 4개로 총 27개 테스트가 통과했다. buildPlugin도 성공했다.

검증은 로컬 Gradle 9.7.1과 기존 오프라인 의존성 저장소, WebStorm의 JBR 21을 사용했다. 명령:

```powershell
.\.gradle-validation\gradle-9.7.1\bin\gradle.bat --offline --gradle-user-home .gradle-user-home --no-configuration-cache '-Porg.gradle.java.installations.paths=D:/Common/WebStorm-2024.3.2.1.win/jbr' -I .gradle-validation/ghost-offline.init.gradle test --tests '*ModelDiagramPerformanceTest' --tests '*ModelDiagramMiniMapTest' --tests '*ModelDiagramZoomTest' buildPlugin
```

연속 이동 중 노드 재렌더링 0회, 범위 밖 렌더링 보충, 종료 후 정상 화면과 픽셀 일치, 외형 변경 무효화 및 취소 복귀를 확인했다. 실제 IDE에서 체감 속도와 시작 시 지연은 별도 확인이 필요하다.

수렴 점검: 기능 요구사항 5개, 성공 기준 3개, 사용자 시나리오 3개 및 계획의 이미지 수명·메모리 제한·미니맵 재사용을 확인했다. 누락·부분 구현·충돌·미요청 변경은 없으며 추가 작업은 없다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다.

## 패닝 표시 경계와 종료 상태 보정 검증

2026-11-06: 미니맵 12개·성능 12개·확대 4개, 총 28개 테스트와 buildPlugin이 통과했다. 새 회귀 테스트는 수정 전 실패를 확인했고, 수정 후 네 방향의 큰 패닝에서 파란 표시가 콘텐츠 안에 유지되는 것을 확인했다. 일반 렌더링의 기존 사각형 위치와 종료 후 자동 맞춤도 유지된다.

FinalState는 바깥 원 중심이 (x+10, y+10)이며, 안쪽 원 중심도 (x+2.5+7.5, y+2.5+7.5)로 동일함을 검토했다. 공통 렌더러를 사용하므로 메인 화면·미니맵·패닝 이미지에 반영된다.

수렴 점검에서 FR-006~007, SC-004~005, 기존 이미지 재사용 요구사항과 관련 계획을 확인했다. 누락·충돌·추가 작업은 없으며 명세와 구현이 일치한다.

## 종료 상태 실제 픽셀 중심 검증 결과

2026-11-06: 이전 구현에서 내부 원 중심 약 29.995와 외부 윤곽 중심 약 30.513의 차이로 재현 테스트가 실패했다. 종료 상태에만 순수 윤곽 좌표를 적용한 뒤 기본·1.5·2배 배율의 두 원 중심 차이 0.2 장치 픽셀 이내 검증이 통과했다. 그래픽 복제본을 사용해 다른 도형의 렌더링 설정을 보존한다.

성능 13개·미니맵 12개·확대 4개로 총 29개 테스트와 buildPlugin이 통과했다. 수렴 점검에서 FR-008, SC-006, 렌더링 설정 복원 및 기존 요구사항과의 일관성을 확인했으며 추가 작업은 없다. 실제 스크린샷에서 내부 원은 상대적으로 왼쪽 위에 보이는 현상이며 이전 방향 설명을 정정했다.

## 주기적 상태 조회 검증 결과

2026-11-06: 추가한 회귀 테스트 두 개는 수정 전 각각 비교 결과 재사용 실패와 패닝 중 붙여넣기 가능 상태로 실패했다. 기존 29개는 통과했다. 수정 후 서비스·클립보드 31개가 모두 통과했다. 이어 성능·미니맵·확대·서비스 배치까지 합쳐 66개가 모두 통과했다.

현재 환경에는 이전 안내의 Gradle 경로와 오프라인 저장소가 없다. 작업 폴더의 Gradle 저장소에는 IntelliJ 배포 ZIP이 없어 Gradle test와 buildPlugin이 의존성 해석 단계에서 실패했다. 기존에 추출된 IntelliJ 2025.3.6.1의 JBR과 라이브러리, 저장된 컴파일·JUnit 인자를 재사용하여 모델 편집기 전체 소스 및 관련 테스트를 Kotlin 컴파일러로 직접 컴파일하고 JUnitCore로 실행했다. 컴파일은 성공했고 플러그인 ZIP은 새로 생성하지 못했다.

검증 재실행 명령은 아래와 같다. 인자 파일은 작업 폴더의 무시되는 검증 디렉터리에 있다.

```powershell
$runtime = 'C:/Users/tcpip147/.gradle/caches/9.7.1/transforms/41d2ee0b381ac1ce59d15aa3eb4932b1/transformed/idea-2025.3.6.1-win/jbr/bin/java.exe'
$compileLines = Get-Content .gradle-validation/panning-compile.args
& $runtime -cp $compileLines[5].Trim('"') org.jetbrains.kotlin.cli.jvm.K2JVMCompiler '@.gradle-validation/panning-compile.args'
& $runtime '@.gradle-validation/panning-regression.args'
```

같은 편집본 100회 조회의 비교 객체 재사용, XML 변경·원복 후 갱신, 롤백 후 보류 상태 해제, 패닝 중 붙여넣기 가능 여부 제한 및 종료 후 붙여넣기 복귀를 확인했다. 실제 IDE의 주기적인 끊김이 해소되는지는 설치 후 추가 관찰이 필요하다.

## 패닝 중 미니맵 위치 동기화 검증

2026-11-06: 추가 회귀 검증이 수정 전 실패했고 기존 66개는 통과했다. 수정 후 같은 직접 컴파일·JUnitCore 경로로 전체 67개가 통과했다. 작은 모델의 네 방향 연속 패닝에서 표시 픽셀 변화, 현재 화면과 모델 전체 포함, 역방향 복귀, 모델 객체 유지 및 배경 재렌더링 0회를 검증했다. 기존 종료 후 정상 렌더링의 픽셀 일치도 통과했다.

재실행은 위의 panning-compile.args와 panning-regression.args를 사용한다. JUnit 실행 시 IntelliJ 배포 경로의 lib/jna/amd64를 jna.boot.library.path 시스템 속성으로 지정했다. 상세 결과는 .gradle-validation/minimap-compile.log와 minimap-test.log에 있다.

Gradle은 사용자 홈 잠금 파일 접근 제한으로 실행하지 못했다. 설치된 Gradle을 직접 실행하고 -g .gradle-validation buildPlugin --offline을 사용했으나 IntelliJ 배포 ZIP이 오프라인 저장소에 없어 의존성 해석이 실패했다. 플러그인 ZIP은 새로 생성하지 못했다.
수렴 점검: 요구사항 11개, 성공 기준 9개, 사용자 이야기 1 및 현재 투영·배경 재사용·여백·수명 관리 설계를 확인했다. 누락·부분 구현·충돌·미요청 변경이 없으며 추가 작업은 없다. 헌장은 미작성 템플릿으로 규범 검사를 생략했다. 빌드 환경 제약은 위에 기록했다.

## 배율 유지 검증 절차

기존 직접 컴파일·JUnitCore 명령으로 회귀 테스트를 실행한다. ModelDiagramMiniMapTest의 시작 검증은 이동량 0인 패닝 전후 전체 투영의 동일성을 확인한다. 연속 이동 검증은 네 방향에서 배율·오프셋·모델 표시 위치와 이미지 재사용을 확인하며 파란 표시 픽셀은 이동해야 한다. 큰 이동은 표시가 콘텐츠 안에 남는지 확인한다. 종료 후에는 일반 자동 맞춤과 정상 렌더링을 검증한다. 실제 IDE에서는 우클릭 시작 전후 도형 크기 유지와 파란 표시 이동을 확인한다.
배율 유지 검증 결과: 모델 편집기 및 관련 테스트 직접 컴파일에 성공했고 서비스·클립보드·성능·미니맵·확대·배치 회귀 테스트 68개가 모두 통과했다. 시작 전후 동일 투영, 네 방향 연속·큰 이동의 배율 및 오프셋 유지, 파란 표시 이동·가시성, 이미지 재사용과 종료 픽셀 일치를 확인했다. 로그는 .gradle-validation/minimap-scale-compile.log와 minimap-scale-test.log이다. 플러그인 ZIP은 앞서 기록한 IntelliJ 배포 ZIP 의존성 누락으로 재생성하지 못했다.
균등 여백·요소 이동 검증 절차: 일반·스크롤 화면의 네 방향 콘텐츠 여백을 비교한다. 네 방향 연속 패닝에서 배율 유지, 요소 표시 픽셀 이동, 파란 표시 이동, 상대 거리, 역방향 복귀와 재렌더링 0회를 확인한다. 기존 직접 컴파일·JUnitCore 회귀 명령을 사용한다.
균등 여백·요소 이동 검증 결과: 직접 컴파일에 성공했고 관련 회귀 테스트 70개가 모두 통과했다. 일반·스크롤 화면의 8픽셀 균등 여백, 네 방향 요소 픽셀 이동, 파란 표시와의 상대 이동, 고정 배율·시작 무변화·복귀·경계 표시·이미지 재사용·종료 정상 렌더링을 확인했다. 로그는 .gradle-validation/minimap-motion-compile.log와 minimap-motion-test.log이다. 기존 IntelliJ 배포 ZIP 의존성 누락으로 플러그인 ZIP은 재생성하지 못했다.
자동 배율 검증 절차: 큰 패닝으로 모델·현재 화면 전체 포함과 요소 축소를 확인하고, 원점으로 되돌리며 재확대와 시작 투영 복귀를 확인한다. 기존 직접 컴파일·JUnitCore 명령을 사용한다.
자동 배율 검증 결과: 직접 컴파일에 성공했고 회귀 테스트 71개가 모두 통과했다. 시작 순간 투영 불변, 네 방향 전체 모델·현재 화면 포함, 요소의 실제 픽셀 축소·재확대·원점 복귀, 메인 줌과 모델 유지, 배경 이미지 재사용 및 종료 정상 픽셀을 확인했다. 로그는 .gradle-validation/minimap-autoscale-compile.log와 minimap-autoscale-test.log이다. 플러그인 ZIP은 기존 IntelliJ 배포 ZIP 의존성 누락으로 재생성하지 못했다.
