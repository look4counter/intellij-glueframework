# 검증 안내

1. JDK 21 환경에서 `./gradlew.bat test --tests com.look4counter.ModelEditorCreationTraceTest --tests com.look4counter.ModelDiagramPerformanceTest --tests com.look4counter.ModelEditorFeaturesTest`를 실행한다.
2. `./gradlew.bat buildPlugin`으로 배포 파일을 생성한다.
3. 문제 컴퓨터에 플러그인을 설치한 후 큰 다이어그램에서 빈 Activity를 선택해 움직이고 드래그해 놓는다.
4. Ghost의 잔상이 없어야 하며 놓은 위치에 하나만 추가되어야 한다. 확대·축소, 미니맵 위 놓기, 캔버스 외부 놓기, Esc 취소를 확인한다.
5. 자동 검증은 부분 갱신 범위와 화면 결과를 확인한다. 문제 컴퓨터의 체감 속도는 별도 현장 검증 대상이다.

## 실행 결과 — 2026-11-04

- 관련 테스트 181개 통과, 실패 0개. 대상은 ModelDiagram 전체 계열, ModelEditorCreationTraceTest, ModelEditorFeaturesTest, ChildCreationToolbarTest다.
- 2,000개 노드 및 50%·100%·200% 확대율에서 인접 미리보기 이동의 부분 갱신 결과가 전체 렌더링과 픽셀 단위로 일치했다.
- 드래그 추적, 놓기 위치 제출, 클릭 중복 방지, 캔버스 외부·미니맵·취소 보호를 확인했다.
- buildPlugin 성공. 산출물: build/distributions/intellij-glueframework-1.0.1.zip.
- 검증 환경: 기존 JDK 21.0.6 및 작업 폴더 내 Gradle 9.7.1. 네트워크 제약 때문에 기존 캐시의 ELK·EMF를 임시 로컬 저장소로 복사하고 검증용 초기화 스크립트에서 Guava 33.3.1-jre를 선택했다. 저장소의 의존성 설정은 변경하지 않았다.
- 문제 컴퓨터에서의 체감 성능은 직접 확인하지 못했으며 위 수동 검증 절차로 확인한다.
