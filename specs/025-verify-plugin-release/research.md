# 조사 결과

- 결정: 기본 Gradle 의존성 다운로드를 사용한다. 근거: 누락된 1.5GB IDE ZIP을 확보해 정상 해석과 추출이 완료됐다. 임시 로컬 IDE 교체 스크립트는 archive와 local 의존성 중복을 일으켰으므로 사용하지 않는다.
- 결정: 다운로드한 IDE의 JBR 21을 JAVA_HOME으로 지정한다. 근거: 시스템 기본 JDK 25는 빌드의 Java 21 요구사항 자동 탐지에 포함되지 않았다.
- 결정: Wire 테스트에 실제 viewport 배치를 추가한다. 근거: 현재 selectWireCreationPoint는 visibleRect 밖 입력을 거부하며 해당 테스트의 편집기 화면 크기는 0이다. 다른 테스트에 이미 동일 배치 패턴이 있다. 제품 경계 검사를 완화하지 않는다.
- 결정: 종료 상태의 빈 링을 경계 밖에서 측정한다. 근거: 최근 도형은 지름 15인 내부 원을 2.5 오프셋으로 중앙 배치한다. 기존 x=102는 안티앨리어싱 경계이므로 배경과 다르다. 상하좌우의 빈 영역과 검정 중심을 검사한다.
- 결정: 정식 테스트에서 SVG 오류를 재검증한다. 근거: 기존·새 IDE의 jsvg 및 util-8 JAR 해시가 동일해 단순 파일 손상은 아니다. 보조 실행은 core classloader를 강제했지만 정식 Gradle은 PathClassLoader를 사용한다.
- 대안: 검사를 삭제·무시하거나 제품 렌더링·입력 동작을 되돌리는 방법은 현재 사용자 동작 검증에 어긋나므로 채택하지 않는다.
- 미해결 기술 질문과 새로운 의존성 없음.
- 추가 조사: verifyPluginStructure는 기본 description·vendor·vendor URL과 이름의 IntelliJ 표기를 거부했다. 실제 원격 저장소는 https://github.com/look4counter/intellij-glueframework.git이다. 공급자 look4counter와 저장소 URL, 이미 도구 창에 쓰는 GlueFramework 명칭 및 실제 기능 설명으로 보완한다. 검사 무시 옵션은 사용하지 않는다.
