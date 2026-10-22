# 구현 계획: 편집기 모듈 분리

**작성일**: 2026-10-22
**명세**: [spec.md](spec.md)

## 요약

루트는 단일 플러그인의 조립과 배포 및 기존 샘플 도구 창을 담당한다. queryeditor와 modeleditor는 독립적인 Gradle 하위 프로젝트로 구성한다. 각 모듈의 등록 조각을 루트 plugin.xml에서 XInclude로 포함하고 pluginComposedModule로 클래스와 리소스를 메인 JAR에 합친다.

## 기술 맥락

- Kotlin 2.3.20, Gradle Kotlin DSL, IntelliJ IDEA 2025.3.6.1 및 플랫폼 Gradle 플러그인 2.19.0 유지.
- 하위 프로젝트는 Kotlin JVM과 org.jetbrains.intellij.platform.module을 적용하고 루트 대상 IDE를 상속한다.
- 파일은 XMLLanguage를 사용하는 XmlLikeFileType으로 등록한다. 각 모듈에 Design과 Source FileEditorProvider를 둔다. 내용 변환과 별도 저장소는 없다.
- 검증: 루트 플랫폼 통합 테스트, 모듈별 컴파일, 배포 ZIP 내용 확인.
- 범위: 두 모듈, XML 파일 형식 두 개, Design/Source 멀티뷰. XML 분석은 IDE를 재사용하고 전용 그래픽 편집은 후속 작업이다.
- 성능: 추가 백그라운드 작업 없이 기존 텍스트 편집기를 사용한다.

## 원칙 확인

헌법은 미작성 템플릿이므로 별도 원칙 검사를 생략한다. AGENTS.md에 따라 한글 문서와 speckit 절차를 준수한다. 설계 전후 검사에서 단일 배포 및 모듈 독립성 위반은 없다. Git 메타데이터가 없어 브랜치를 생성하지 않는다.

## 프로젝트 구조

```text
build.gradle.kts
settings.gradle.kts
src/main/resources/META-INF/plugin.xml
src/test/kotlin/com/look4counter/EditorFileTypeTest.kt
queryeditor/build.gradle.kts
queryeditor/src/main/kotlin/com/look4counter/queryeditor/GlueSqlFileType.kt
queryeditor/src/main/resources/META-INF/queryeditor.xml
modeleditor/build.gradle.kts
modeleditor/src/main/kotlin/com/look4counter/modeleditor/GlueUmlAdFileType.kt
modeleditor/src/main/resources/META-INF/modeleditor.xml
```

## 구현 단계

1. 설정에 두 하위 프로젝트와 모듈 플러그인을 등록한다.
2. 루트 조립 의존성과 하위 모듈 빌드를 구성한다.
3. 모듈별 FileType과 XML 조각을 작성한다.
4. 루트 plugin.xml에 두 조각을 포함한다.
5. 통합 테스트와 README를 작성하고 컴파일·테스트·ZIP 구성을 검증한다.

## 복잡성 확인

추가 공통 모듈은 만들지 않는다. 환경 때문에 실행할 수 없는 검증은 성공으로 표시하지 않고 기록한다.

## 멀티뷰 확장 설계

### 편집기 구성

- 각 모듈에 독립적인 DesignEditorProvider와 SourceEditorProvider를 추가한다.
- Design은 PLACE_BEFORE_DEFAULT_EDITOR로 Source 앞에 배치한다. queryeditor는 queryMap 편집 패널을 제공하고 modeleditor는 기본 안내 패널을 유지한다.
- Source는 HIDE_DEFAULT_EDITOR 정책으로 중복 기본 탭을 숨긴다. TextEditorProvider가 만든 원래 TextEditor를 래핑해 이름만 Source로 표시한다.
- Source는 문서·편집 위치·선택·탐색·속성 변경을 원래 편집기에 위임하며 Disposer로 원래 편집기를 자식 등록하여 정확히 한 번 해제한다.
- Source 제공자의 readState/writeState도 TextEditorProvider에 위임해 커서·스크롤 복원을 유지한다.
- 각 제공자는 자신의 파일 형식만 수락하고 DumbAware를 구현한다. XML 내용이나 PSI를 검사해서 열기 여부를 결정하지 않는다.

### XML 지원

- 기존 이름과 확장자 및 싱글턴을 유지하면서 두 파일 형식을 XmlLikeFileType(XMLLanguage.INSTANCE)으로 변경한다.
- fileType 등록에 language="XML"을 명시한다. XML 기능은 플랫폼의 XML 모듈을 사용하고 루트 설명자에 com.intellij.modules.xml 의존성을 선언한다.
- 전용 XML 언어·파서·스키마·구문 강조기를 만들지 않는다. 구문 강조·구문 검사·완성·서식은 기존 XML 지원을 사용한다.
- 단순 Design 표시와 탭 전환은 원본을 변경하지 않는다. 명시적인 쿼리 Design 편집은 같은 VirtualFile의 XML Document에 반영한다.

### 변경 파일

기존 각 FileType 및 META-INF XML, 루트 plugin.xml, src/test/kotlin/com/look4counter/EditorFileTypeTest.kt를 수정한다. queryeditor에는 QueryDesignEditorProvider.kt와 QuerySourceEditorProvider.kt를, modeleditor에는 ModelDesignEditorProvider.kt와 ModelSourceEditorProvider.kt를 추가한다. 루트에는 src/test/kotlin/com/look4counter/EditorMultiViewTest.kt를 추가한다. README와 계약·빠른 검증 문서도 갱신한다.

### 검증 전략

기존 등록 테스트를 유지하고 XML PSI, 구문 강조기, 오류 탐지, 완성, 서식, 제공자 선택, 두 탭 이름·순서, 미저장 문서, 실행 취소·다시 실행, 저장 및 해제를 통합 테스트로 검증한다. 잘못된 XML·빈 파일·읽기 전용·일반 XML 및 상대 모듈 비수락도 포함한다. 기존 테스트 의존성 캐시 누락은 먼저 재확인하고 실패한 경우 실행 제한을 기록한다. 두 모듈 컴파일, 테스트 코드 컴파일, 단일 ZIP 내용 확인은 별도로 수행한다.

설계 후 원칙 검사: 모듈 간 의존성과 배포 단위 변경이 없고 모든 추가 문서는 한글이다.

### 로컬 테스트 실행 보완

외부 Platform 테스트 프레임워크의 캐시 누락이 확인되어, 대상 IDE에 포함된 Bundled 테스트 프레임워크로 검증할 수 있는지 확인한다. 지원되는 경우 build.gradle.kts의 테스트 프레임워크 선택을 Bundled로 전환한다. 대상 IDE에 맞는 테스트 API를 재사용하고 배포물의 런타임 의존성은 변경하지 않는다.

## 쿼리 Design 확장 설계

### 구성 요소

- QueryDesignEditorProvider.kt는 queryeditor의 새 QueryDesignEditor를 생성한다.
- QueryMapDocumentModel.kt는 Source의 Document를 XML PSI로 분석하고 queryMap의 직접 query를 순서대로 스냅샷으로 제공한다. root와 query의 네임스페이스가 일치해야 한다.
- QueryDesignEditor.kt는 왼쪽 SQL 편집기와 오른쪽 설명·속성·목록 및 문서 동기화를 담당한다.
- QuerySqlEditor.kt는 PlainTextFileType 기반의 다중 행 EditorTextField를 확장한다. 줄 번호와 스크롤을 제공하고 SQL 문법 의존성을 추가하지 않는다.
- 루트 src/test/kotlin/com/look4counter/QueryDesignEditorTest.kt에 실제 패널·문서 통합 테스트를 추가한다.

### 데이터와 수정

query의 참조는 SmartPsiElementPointer로 유지하여 id가 중복되거나 변경돼도 대상 행을 구분한다. SQL은 XmlText.value를 합쳐 CDATA·entity를 디코딩하고 앞뒤 공백을 보존한다.

속성 수정은 선택 태그의 setAttribute로 처리한다. SQL 수정은 value.textRange만 CDATA로 교체하고 `]]>`는 여러 CDATA 구간으로 분리한다. self-closing query는 해당 태그만 시작·종료 태그로 확장한다. 주석이나 하위 태그가 있는 본문은 SQL 쓰기를 제한한다. 루트와 다른 query는 재직렬화하지 않는다.

모든 원문 수정은 WriteCommandAction으로 처리한다. 문서 리스너 안에서 PSI 커밋이나 원문 재수정을 하지 않고 EDT 작업으로 모아서 실행한다. 대기 수정은 선택 당시의 스마트 참조·속성명·기존 값·새 값을 함께 보관한다. 선택 전환 전에 대기 수정을 반영하고, Source에서 같은 값이 먼저 바뀌었으면 이전 값을 덮어쓰지 않는다.

SQL 표시 문서는 쿼리별로 분리하여 선택을 바꾼 뒤 다른 쿼리에 이전 SQL 편집이 적용되지 않게 한다. 프로그램에 의한 표시 갱신은 재입력 방지 플래그와 투명한 실행 취소 작업을 사용한다. Source의 실행 취소·다시 실행과 외부 수정은 스냅샷을 다시 읽어 반영한다.

### 레이아웃과 수명

가로 OnePixelSplitter에 SQL과 사이드바를 배치한다. 오른쪽은 선호 너비 320, 최소 220픽셀을 사용하고 구분선을 이동할 수 있다. 첫 화면은 약 70:30 비율이며 분할 비율을 IDE 설정에 저장한다. 오른쪽에는 queryMap desc, id·desc·resultType 입력, isNamed 체크박스, 스크롤 가능한 query.id 목록을 배치한다.

빈 목록과 잘못된 XML은 안내하고 편집을 비활성화한다. 읽기 전용은 속성·SQL 수정을 막는다. 편집기 해제 시 EditorTextField와 splitter를 해제하고 문서·가상 파일 리스너 및 대기 작업도 취소한다.

### 검증

사용자 샘플의 모든 행 선택, 속성 및 SQL 쓰기, 선택 직후 전환, id 변경·중복, Source→Design 동기화, 실행 취소·다시 실행·저장, entity·CDATA 종료 문자열, 다른 쿼리·주석·네임스페이스 보존, self-closing 태그, 빈/잘못된 XML, 읽기 전용, 중첩 본문, 분할 너비 및 해제를 테스트한다. 기존 멀티뷰·XML 테스트와 함께 test buildPlugin을 실행한다.

설계 전후 검사: 기존 두 모듈의 독립성과 단일 플러그인 배포를 유지한다. 추가 공통 모듈·SQL 문법·실행 의존성은 없다.

## 설명 편집과 화면 정리 설계

QueryMapDocumentModel.kt 스냅샷에 루트 스마트 참조를 추가하고 QueryEdit에서 루트 desc만 추가 허용한다. query의 직접 자식·네임스페이스 검사와 기존 값 충돌·XML 이스케이프·실행 취소 처리는 유지한다.

QueryDesignEditor.kt 최상단을 접근성 이름이 있는 JBTextField로 바꾸고 루트 참조와 기존 설명으로 대기 수정한다. 쿼리 선택과 독립적으로 동작하며 빈 목록에서도 편집 가능하다. 잘못된 XML과 읽기 전용은 비활성화한다. GridBagLayout resultType 행 세 번째 열에 isNamed를 배치한다. 정상 상태 레이블을 숨기고 필요한 안내만 유지한다.

QueryDesignEditorTest.kt에서 설명 입력 타입을 갱신하고 특수문자·빈 값·누락 속성·빈 목록·선택 보존·Source 동기화·실행 취소·다시 실행·저장·쓰기 제한 및 배치를 검증한다. 전체 test buildPlugin을 실행한다. 한글 문서·모듈 독립성·단일 배포 유지에 위반이 없고 헌법은 미작성 템플릿이다.

## 라벨과 Java 클래스 완성 설계

- QueryDesignEditor.kt에서 표시 문자열만 첫 글자 대문자로 변환하고 체크박스는 IsNamed로 표시한다. 내부 이름과 XML 키는 유지한다.
- QueryResultTypeEditor.kt를 추가한다. JavaReferenceEditorUtil의 클래스 참조 Document와 JavaFileType의 한 줄 EditorTextField를 사용한다. PSI에 ClassNameReferenceEditor.CLASS_NAME_REFERENCE_FRAGMENT 표식을 적용하여 완성 선택이 전체 이름을 사용하도록 한다. IDE의 기본 Java 완성을 사용한다.
- 기존 Swing 입력은 id·desc에 유지하고 ResultType만 전용 필드로 분리한다. IDE DocumentListener를 통해 기존 QueryEdit 대기 수정에 연결한다. refresh는 재진입 방지 상태에서 입력과 쓰기 가능 여부를 동기화한다. setDisposedWith로 편집기 수명을 연결한다.
- build.gradle.kts와 queryeditor/build.gradle.kts에서 com.intellij.java 번들 의존성을 추가하고 src/main/resources/META-INF/plugin.xml에 필수 Java 플러그인을 선언한다.
- QueryDesignEditorTest.kt에서 ResultType 타입을 갱신한다. 라벨·동명 클래스 후보와 전체 이름 삽입·직접 입력·비우기·선택 전환·Source 동기화·실행 취소·다시 실행·쓰기 제한을 검증한다. 기존 저장 테스트도 전용 입력을 사용한다. test buildPlugin을 실행한다.

설계 전후 원칙 확인: 한글 문서·speckit 순서·모듈 독립성·단일 배포를 유지한다. 헌법은 미작성 템플릿이다. 기술 미확정 사항은 없다.

## 파일 형식 아이콘 설계

queryeditor/src/main/resources/icons/glueSql.svg와 modeleditor/src/main/resources/icons/glueUmlAd.svg를 추가한다. 각 SVG는 16×16 뷰포트, 중심 8·8 반지름 7의 원과 흰색 문자 도형을 사용한다. 쿼리 배경은 #87CEEB, 모델 배경은 #F6B26B다. 글꼴 의존성을 피하도록 Q는 타원 윤곽과 꼬리, M은 경로로 표현한다.

각 GlueSqlFileType.kt와 GlueUmlAdFileType.kt의 일반 텍스트 아이콘을 자기 모듈 클래스 기준 IconLoader.getIcon으로 교체하고 싱글턴 인스턴스에서 캐시한다. 추가 모듈·의존성과 파일 형식 등록 변경은 없다.

기존 EditorFileTypeTest를 실행하고 buildPlugin 배포 JAR에 두 SVG와 클래스가 포함되는지 검사한다. SVG XML 구조와 색·크기를 확인하고 가능한 로컬 렌더러로 외형을 확인한다. 기존 문서·소스 동작은 바꾸지 않는다. 한글 문서·모듈 독립성·단일 배포 원칙을 유지하며 헌법은 미작성 템플릿이다.

## 속성 외형 통일 설계

QueryDesignEditor.kt의 라벨을 id→ID, desc→Description, resultType→ResultType으로 명시하고 JBCheckBox의 텍스트를 제거한다. 접근성 이름과 툴팁은 isNamed로 유지한다. 오른쪽 배치와 XML 키는 바꾸지 않는다.

QueryResultTypeEditor.kt에 기준 JBTextField를 전달한다. 기준 입력의 글꼴·배경과 테두리 및 선호·최소 높이를 적용한다. 내부 편집기는 일반 텍스트 하이라이터와 입력용 색상을 사용하고 Java 검사 표시를 비활성화한다. Java 참조 문서·완성 표식·기존 DocumentListener와 수명 관리는 유지한다. 테마 갱신 시 기준 입력 스타일을 다시 반영한다.

QueryDesignEditorTest.kt의 기존 라벨 기대값을 갱신하고 기존 배치 검증에 높이·글꼴·배경·테두리 비교를 추가한다. 기존 Java 클래스 후보와 전체 이름 삽입·직접 입력·쓰기 제한 검증을 유지하여 test buildPlugin을 실행한다. 새로운 테스트 파일이나 의존성은 추가하지 않는다.

원칙 재검사: 한글 문서·speckit 순서·모듈 독립성·단일 배포 유지. 헌법은 미작성 템플릿이다.

## ResultType 실제 표시 보정 설계

QueryResultTypeEditor.kt에서 일반 JBTextField 테두리를 내부 스크롤 영역에 복사하는 코드를 제거한다. 외부 AbstractBorder는 테마의 TextField.borderColor·Component.focusColor를 사용하며 일반 1픽셀·포커스 2픽셀의 선과 배율 대응 모서리를 그린다. 내부 여백은 상하 3·좌우 6픽셀로 고정하여 포커스 전후 내용 위치가 바뀌지 않게 한다.

외부 배경은 테마 입력 배경으로 채우고 내부 편집기는 테두리를 비운다. doLayout에서 편집 컴포넌트를 줄 높이로 제한하여 남는 높이의 중앙에 배치한다. 실제 contentComponent 포커스 이벤트에 연결된 기존 EditorTextField focusGained/focusLost를 확장하여 외부 테두리를 갱신한다. 설정 제공자로 초기화 후 일반 텍스트 표시와 검사 비활성화를 적용한다.

QueryDesignEditorTest.kt의 기존 스타일 비교를 실제 컴포넌트 배치와 렌더링으로 보완한다. 일반·포커스 상태를 PNG로 기록하고 중앙 정렬·좌우 여백·상태별 테두리를 검증한다. 실제 이미지를 확인한 뒤 test buildPlugin으로 자동완성·편집 회귀와 배포를 확인한다. README와 검증 기록을 갱신한다.

원칙 검사: 한글 문서·speckit 순서·기존 모듈 독립성과 배포 유지. 헌법은 미작성 템플릿이고 기술 미확정 사항은 없다.

## ResultType 치수 조정

QueryResultTypeEditor.kt의 선호 높이는 기존 max(기준 입력 높이, 글꼴 높이+6픽셀)에서 2픽셀을 뺀다. 최소 높이도 기준 입력 최소 높이에서 2픽셀 줄인다. 상하 인셋은 2픽셀로 맞추고 좌우는 포커스 테두리 공간 2픽셀+내부 패딩 1픽셀로 총 3픽셀을 확보한다. 기존 줄 중앙 배치와 일반·포커스 테두리는 유지한다.

QueryDesignEditor.kt에서 ResultType 입력의 GridBagConstraints 좌측 인셋은 3→5, 우측 인셋은 3→1로 바꿔 총 가로 인셋과 너비를 유지하면서 입력만 오른쪽으로 2픽셀 옮긴다. 다른 컴포넌트에는 기본 인셋을 복원한다.

기존 QueryDesignEditorTest.kt의 높이·내부 영역·그리드 인셋 기대값을 갱신하고 렌더링 이미지와 test buildPlugin 결과를 확인한다. 새 테스트 파일·의존성·모델 변경은 없다. 원칙 검사는 기존 한글 문서·모듈 독립성·단일 배포를 유지하며 헌법은 미작성 템플릿이다.

## ResultType 왼쪽 패딩 설계

QueryResultTypeEditor.kt의 왼쪽 인셋을 테두리 공간 2픽셀과 내부 패딩 3픽셀의 합인 JBUI.scale(5)로 변경한다. 오른쪽 인셋·높이·그리드 위치는 유지한다. 기존 Kotlin·Swing 구조와 IDE 배율 처리를 재사용하며 데이터 모델·외부 인터페이스 변경은 없다. 헌법은 미작성 템플릿이며 확장 훅은 없다. 기술 미해결 사항은 없다.

기존 QueryDesignEditorTest.kt의 왼쪽 경계 기대값만 5픽셀로 갱신하고 쿼리 편집 테스트를 실행한다. FR-042~FR-043과 SC-017을 검증한다.

## 입력 외형 일치 설계

QueryResultTypeEditor.kt는 ID의 JBTextField에서 테두리·인셋·선호 높이·최소 높이를 그대로 가져온다. 별도 치수와 테두리 그리기를 제거한다. 내부 편집 줄은 중앙 배치하고 기본 텍스트 입력 여백에 맞춘다. QueryDesignEditor.kt의 ResultType 전용 그리드 오프셋을 제거한다. 기존 QueryDesignEditorTest.kt에서 실제 세 입력을 같은 부모에 배치해 경계와 높이를 비교하고 일반·포커스 비교 PNG를 저장한다. 기존 자동완성·편집 테스트를 실행한다. 데이터 모델·외부 계약 변경은 없다. 헌법은 미작성 템플릿이며 확장 훅은 없다.

## 어두운 테마 입력 렌더링 복구 설계

EditorTextField에 JBTextField 테두리 객체만 직접 할당한 구현을 대체한다. QueryResultTypeEditor.kt는 비어 있는 전용 JBTextField를 사용해 테마의 입력 배경과 테두리를 그린다. 텍스트 입력 UI의 modelToView2D로 실제 텍스트 시작 좌표를 측정하여 외부 인셋에 적용한다. 테두리 인셋에 포함되지 않는 UI 내부 여백도 확보한다. 전용 필드의 포커스 상태는 내부 편집기 포커스 이벤트와 연결한다. 테마 갱신 시 전용 필드와 내부 편집기의 배경·글꼴·텍스트 색상을 다시 적용한다. Java 참조 문서와 내부 편집기는 유지한다.

QueryDesignEditorTest.kt의 기존 비교 검증을 밝은 기본 테마와 Darcula 테마에서 실행한다. 테두리 픽셀과 텍스트 시작 여백을 실제 렌더링에서 비교하고 테마별 PNG를 저장하여 확인한다. 테스트 종료 시 원래 테마를 복원한다. 데이터 모델·외부 계약·의존성 변경은 없다. 헌법은 미작성 템플릿이며 확장 훅은 없다.

## 쓰기 안전 문맥 설계

QueryMapDocumentModel.snapshot()에서 commitDocument를 제거하여 조회만 수행하게 한다. QueryDesignEditor의 생성자·selectNotify·deselectNotify는 scheduleRefresh로 갱신·대기 편집 적용을 예약한다. invokeLater에 ModalityState.nonModal()을 명시하고 기존 중복 예약 방지 및 폐기 검사를 유지한다. refresh의 명시적 준비 단계에서 문서를 커밋한 뒤 읽기 전용 스냅샷을 만든다. 직접 refresh를 호출하는 나머지 경로는 사용자 목록 선택 이벤트와 안전하게 예약된 갱신뿐이다.

QueryDesignEditorTest.kt에서 TransactionGuardImpl.performActivity(false)로 비안전 문맥을 재현한다. 미커밋 Source로 생성·활성화할 때 즉시 커밋하지 않는지, 이벤트 처리 후 최신 값이 반영되는지, 예약 후 폐기 시 원본이 보존되는지 검증한다. 기존 테스트는 비동기 초기화·생명주기 갱신 후 이벤트 처리를 기다린다. 데이터 모델·외부 인터페이스·의존성 변경은 없다. 헌법은 미작성 템플릿이고 확장 훅은 없다.

## 저장 시 SQL 공백 정리 설계

queryeditor 모듈에 QuerySaveListener.kt를 추가하고 META-INF/queryeditor.xml의 applicationListeners로 FileDocumentManagerListener를 등록한다. beforeDocumentSaving에서 Glue SQL 파일과 쓰기 가능 여부를 확인하고 ProjectLocator로 소속 프로젝트를 찾는다. 현재 문서 텍스트의 분리된 XML PSI를 읽기 액션에서 분석하여 원본 PSI 강제 커밋 없이 유효한 queryMap의 직접 query 자식 중 순수 텍스트 본문만 처리한다.

SQL 텍스트를 trim하고 변경이 필요한 본문 범위를 수집한다. 쓰기 명령에서 원본 수정 시각이 같은지 확인한 뒤 뒤쪽 범위부터 CDATA로 치환한다. 내부 공백·태그 바깥 영역·속성은 유지하며 CDATA 종료 문자열은 기존 방식으로 분할한다. QueryMapDocumentModel.kt의 CDATA 직렬화를 내부 공용 함수로 분리하여 기존 Design 편집과 저장 정리가 재사용한다.

QuerySaveListenerTest.kt에서 Source 전용 실제 saveDocument·디스크 저장·두 사용자 예시·중복 id·내부 공백·이스케이프·반복 저장·복합 본문 및 대상 밖 보존을 검증한다. QueryDesignEditorTest.kt에 Design SQL 저장 후 화면 동기화·선택 유지·실행 취소 검증을 추가한다. 외부 계약은 저장 시 본문 정리 동작으로 확장하고 데이터 모델·의존성은 바꾸지 않는다. 헌법은 미작성 템플릿이며 확장 훅은 없다.

## 파일 열기 공백 정리 설계

QuerySaveListener.kt의 정리 로직을 QuerySqlNormalizer.kt의 내부 공용 객체로 분리한다. 저장 리스너는 기존 ProjectLocator 기반 호출을 유지하고 파일 열기 리스너도 같은 정리 함수를 호출한다.

QueryOpenListener.kt를 추가하고 META-INF/queryeditor.xml의 projectListeners에 FileEditorManagerListener로 등록한다. fileOpened에서 Glue SQL 파일만 대상으로 명시적 non-modal invokeLater를 예약한다. 실행 시 프로젝트·파일 유효성과 해당 파일이 열려 있는지 검사하고 문서를 가져와 공용 정리 함수를 호출한다. 자동 저장은 하지 않는다. 원본 PSI 강제 커밋 없이 기존 읽기 분석·쓰기 명령과 공통 CDATA 직렬화를 유지한다.

QueryOpenListenerTest.kt에서 실제 FileEditorManager 열기·Source/Design 표시·미저장 문서와 디스크 원본·실행 취소·반복 열기·비안전 이벤트·닫힌 파일 및 제외 대상 보존을 검증한다. 기존 저장 테스트와 전체 회귀 검증을 실행한다. 모델·외부 의존성 변경은 없다. 헌법은 미작성 템플릿이며 확장 훅은 없다.

## SQL 강조와 키워드 완성 설계

기존 Kotlin·IntelliJ Platform 구성과 EditorTextField를 유지하고 SQL 플러그인이나 DBeaver 런타임 의존성은 추가하지 않는다. DBeaver SQLConstants.java의 SQL2003_RESERVED_KEYWORDS·SQL_EX_KEYWORDS·QUERY_KEYWORDS·DDL_KEYWORDS를 고정 버전에서 추출한 리소스로 제공한다. 출처 커밋은 f2e25edd7d2eecd4d6541cdce70fdb25c7b07ea9이며 Apache-2.0 라이선스와 고지를 패키지에 포함한다. 하이라이팅과 완성은 하나의 목록을 공유한다.

queryeditor/.../sql/SqlKeywords.kt는 리소스를 읽고, SqlLexer.kt는 공백·키워드·식별자·숫자·따옴표·주석·기호를 구별한다. 작은따옴표·큰따옴표 반복 및 역슬래시 이스케이프, 미완성 토큰, 정수·소수·지수, 줄·블록 주석을 처리한다. SqlSyntaxHighlighter.kt는 IntelliJ 렉서 하이라이터와 텍스트 속성 키로 밝은/어두운 테마의 주황·파랑·녹색을 제공한다. 주석은 IDE 기본 주석 색을 따른다.

SqlKeywordCompletionProvider.kt는 IntelliJ TextCompletionProvider와 DumbAware를 사용한다. 접두어를 대소문자 무시로 필터링하고 LookupElement를 제공한다. 문자열·주석·숫자·viewer 상태는 실제 토큰 문맥에서 제외하며 이 SQL 버퍼에서는 다른 일반 단어 후보가 섞이지 않도록 결과 처리를 종료한다. 자동 팝업은 IDE의 기본 TextCompletionContributor를 재사용한다.

SqlDocuments.kt는 분리된 일반 텍스트 PSI 문서에 완성 제공자를 설치한다. QueryDesignEditor.createBuffer는 이 문서를 사용하며 기존 문서 리스너·수명 관리·XML 반영은 유지한다. QuerySqlEditor의 설정 제공자가 렉서 하이라이터를 적용하므로 선택에 따른 문서 교체·편집기 재생성에도 동일하게 동작한다. Source 파일 형식과 ResultType의 Java 문서는 변경하지 않는다. PSI 원본 강제 커밋 경로를 추가하지 않는다.

QuerySqlEditorTest.kt는 실제 Design SQL 편집기로 토큰·색상·후보 선택·XML 반영·실행 취소·쿼리 전환·주석/따옴표 제외를 검증한다. 밝은 기본 테마와 어두운 에디터 스킴에서 PNG를 생성하고 이미지 도구로 직접 확인한다. 기존 전체 회귀 테스트와 buildPlugin을 실행하여 리소스·라이선스 포함을 확인한다. 헌법은 미작성 템플릿이고 확장 훅은 없다.

## SQL 표시와 DataSource 설정 설계

기존 queryeditor 모듈과 IntelliJ Platform API를 재사용한다. SqlLexer.kt에 PARAMETER 토큰을 추가하고 :name을 별도 인식한다. ::type과 :=는 파라미터에서 제외하고 키워드 완성 제외 문맥에 파라미터를 추가한다. SqlSyntaxHighlighter.kt는 밝은 테마 #008B9A·어두운 테마 #56D4DD의 cyan을 제공한다.

QueryDesignEditor.kt의 쿼리 목록은 기본 셀 렌더러의 선택·포커스 테두리를 보존한 복합 테두리로 좌우 8픽셀을 추가한다. QuerySqlEditor.kt는 EditorGutterComponentEx의 오른쪽 free-painter 영역에 8픽셀을 예약하고 EditorUtil.disposeWithEditor에 연결한 Disposable로 해제한다. 필요하면 줄 마커 영역을 활성화하여 실제 여백이 표시되는지 경계와 캡처로 검증한다.

settings/DataSourceSettingsService.kt는 프로젝트 light service와 PersistentStateComponent를 사용한다. 별도 상태 bean과 DataSourceDefinition에 ID·이름·JDBC URL·드라이버 클래스·사용자명·비밀번호 저장 여부만 보관하고 glue-query-editor.xml에 저장한다. 조회와 로딩은 깊은 복사로 UI 초안과 저장 상태를 분리한다. 비밀번호 자체는 상태에 포함하지 않는다.

settings/DataSourcePasswordManager.kt는 프로젝트 식별자와 DataSource UUID로 CredentialAttributes를 만들고 PasswordSafe를 사용한다. 작업은 비 UI 스레드에서만 실행하며 여러 비밀번호 변경 전 값을 보관하고 실패 시 이전 값 복원을 시도한다. 메타데이터는 비밀번호 작업 성공 후에만 적용한다. 테스트는 주입한 메모리 CredentialStore로 실제 사용자 보안 저장소를 변경하지 않고 저장·삭제·프로젝트 구분·실패 복원을 검증한다.

settings/GlueFrameworkConfigurable.kt는 Settings 최상위 그룹을 제공한다. settings/GlueQueryEditorConfigurable.kt는 프로젝트 생성자와 SearchableConfigurable을 사용하고, DataSourceSettingsPanel.kt에서 목록·추가/삭제 버튼·이름/JDBC URL/드라이버/사용자/비밀번호/비밀번호 삭제 폼을 제공한다. 입력은 초안에만 반영하고 Apply에서 검증 후 저장한다. 비밀번호 변경이 있을 때에만 ProgressManager의 배경 작업으로 안전 저장소를 처리하고 실패를 ConfigurationException으로 전달한다. Reset/Dispose는 초안과 비밀번호 입력을 비운다. META-INF/queryeditor.xml에 상위 applicationConfigurable과 하위 projectConfigurable을 parentId로 연결한다.

QuerySqlEditorTest.kt와 QueryDesignEditorTest.kt에서 cyan·파라미터 완성 제외·목록 여백·실제 gutter 폭과 캡처를 검증한다. DataSourceSettingsTest.kt는 실제 설정 패널·EP 등록·직렬화·검증·초안 취소와 메모리 자격 증명 저장소를 검증한다. 전체 회귀 테스트와 buildPlugin 및 패키지 등록/리소스를 확인한다. 새 외부 의존성·DB 연결은 추가하지 않는다. 헌법은 미작성 템플릿이고 확장 훅은 없다.
## DB 메타데이터 자동완성 설계

기존 Kotlin·IntelliJ Platform·JDBC 표준 API를 사용하며 새 런타임 의존성은 추가하지 않는다. 헌법은 미작성 템플릿이고 확장 훅은 없다. 이번 설계가 이전 메타데이터 연결 제외와 8픽셀 예약 결정을 대체한다.

DatabaseKind enum은 다섯 DB 표시명·기본 클래스·기본 HTTPS 다운로드 주소를 정의한다. DataSourceDefinition에 databaseKind와 driverJars 목록을 추가하고 기존 상태는 URL로 유추한다. 목록/폼은 BorderLayout 고정 220픽셀 목록으로 바꾼다. JAR은 복수 파일 선택과 텍스트 목록을 지원한다. DriverDownloader는 HTTPS 전송의 시간·크기 한도, 취소, ZIP 내부 JAR 추출·검증을 수행하고 IDE 시스템 캐시 아래 새 디렉터리에 저장한다. 임시 파일/실패 결과는 지우고 성공 후에만 초안을 갱신한다. Impala는 공급자의 직접 HTTPS ZIP/JAR 주소를 입력받는다.

JdbcMetadataReader는 URLClassLoader와 Driver.connect를 직접 사용하여 전역 DriverManager 등록에 의존하지 않는다. 연결과 ResultSet·클래스로더를 use로 해제하고 TCCL을 복구한다. getTables와 테이블별 getColumns를 사용하여 catalog/schema/table 관계를 유지한다. getColumns의 패턴 와일드카드는 escape하고 결과의 실제 테이블 키를 재검사한다. 조회 취소를 행마다 확인하고 연결/소켓 timeout 기본값을 제공한다. 원본 SQL은 실행하지 않는다.

DatabaseMetadataService 프로젝트 서비스는 선택 ID·원본 DataSource 정의와 성공 스냅샷을 비교한다. 설정 변경은 캐시를 무효화하고 세대 번호를 증가시켜 진행 중인 오래된 조회를 폐기한다. 메타데이터는 메모리에만 유지하고 선택 ID만 프로젝트 상태에 저장한다. QueryDataSourceToolbar는 DB 선택과 새로고침·상태를 표시하고 Backgroundable Task에서 보안 저장소/조회 후 EDT를 갱신한다. SQL 완성은 프로젝트 서비스의 캐시만 읽어 키워드·테이블·컬럼을 정확한 이름 기준으로 합치고 종류를 표시한다. DB 미선택은 기존 키워드만 유지한다.

기존 설정 테스트를 확장하고 JDBC 프록시/가짜 Driver 및 로컬 JAR·ZIP을 사용한 다운로드 스트림 주입 테스트를 추가한다. 선택 전환·삭제·변경·취소·오류에서 캐시와 리소스를 확인하고 실제 완성 삽입을 테스트한다. 네트워크/사용자 DB 없이 기본 테스트를 수행하며 공개 다운로드 경로는 별도 smoke 확인한다. 전체 test buildPlugin과 화면 캡처 후 문서를 갱신한다.

