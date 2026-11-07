# 검증 계약

전체 test는 기존 사례를 무시하지 않고 실행한다. Wire 사례는 화면에 배치한 편집기에서 실제 누름·놓음·클릭을 보내며 저장과 Undo/Redo를 확인한다. 종료 상태 사례는 빈 링의 상하좌우 배경과 중심 검정, 화면 크기를 확인한다.

ZIP은 단일 플러그인 디렉터리에 메인 JAR과 런타임 의존성을 포함한다. 메인 JAR에는 plugin.xml과 두 모듈 등록, 두 편집기·저장 보호·JDBC 클래스 및 라이선스가 존재해야 한다. 격리 IDE 기동은 실제 사용자 IDE 설치 완료라고 표시하지 않는다.

설치 표시 이름은 GlueFramework, 공급자는 원격 저장소 소유자 look4counter와 저장소 URL이다. 기본 템플릿 소개는 실제 기능 설명으로 바꾸고 com.look4counter.intellij-glueframework ID와 1.0.3 버전은 유지한다.
