# 조사 결과

- 결정: 기존 Swing 기반 Design 에디터를 확장한다. 이유: IntelliJ 에디터 수명 및 Source 문서를 그대로 사용하고 GEF/SWT 의존성을 추가하지 않는다.
- 결정: XML 파싱, 순수 좌표 계산, Swing 렌더링을 분리한다. 이유: 사용자가 정의한 연결 규칙을 좌표 단위로 검증할 수 있다.
- 결정: IntelliJ의 XML PSI로 읽는다. 이유: Source의 공유 문서 및 불완전한 XML을 플랫폼에서 처리하며 외부 XML 파서를 도입하지 않는다.
- 결정: DocumentListener와 Design 선택 시 재조회로 미저장 문서를 반영한다. 저장 동작은 추가하지 않는다.
- 대안: GEF 라이브러리 이식은 기존 UI와 맞지 않고 현재 읽기 전용 표시 범위보다 크므로 사용하지 않는다.

## Child 관통 방지 병합 조사

기존 5px 판정은 DiagramEdits에 모여 있으며 Canvas는 Down 당시 wire로 매번 미리보기를 계산한다. Wire의 start·end만 대상으로 이동한 B의 양옆 선분 A–B와 B–C에 도형 내부 교차 검사를 적용한다. State의 정상 원 외곽 접속은 허용하고 내부 통과는 차단한다. 외부 라이브러리나 저장 형식 변경은 필요 없다.


연결 Child 검사 범위: 관통 검사 대상은 해당 Wire의 start·end만이며 다른 Child는 무시한다. 기존점·중점 모두 같은 규칙으로 동작한다.

## 작은 핸들 접점 이동 조사

직결 Activity 경로는 firstRelativePoint의 한 축으로 양쪽 접점을 결정한다. 중점 드래그에서 같은 유효 구간과 수직 우선순위를 적용하면 기존 기하·XML 저장·Undo 경로를 재사용할 수 있다. 유효 구간을 넘는 드래그는 마우스 위치의 bendpoint 삽입으로 처리한다. State 직선의 5px 분할 테스트는 별도 State 예제로 보존한다.

## 범위 밖 분할 검증

공통 유효 구간의 경계까지는 직결 이동이며 조금이라도 벗어나면 마우스 위치를 새 bendpoint로 저장한다. 원본 Wire로 매번 계산하여 밖→안→밖 전환 시 임시 점이 누적되지 않는다. Navigation 후에도 모델 좌표로 같은 판단을 적용한다.

## 직결 복원의 관통 검사 재사용

직결 이동의 조기 반환이 기존 canMergeAt을 건너뛰던 문제를 확인했다. 원본 Wire에 B를 삽입한 경로로 인접 선분을 검사하고, start·end 관통이 있으면 범위 안에서도 분할을 유지한다. 안전한 위치에서만 직결로 복원한다. 비연결 Child는 기존과 같이 무시한다.

## Esc 및 복사·붙여넣기 조사

- 로컬 IntelliJ 2025.3.6.1 SDK의 CopyProvider·PasteProvider·UiDataProvider·DataSink API를 확인했다. Canvas가 provider를 데이터 스냅샷으로 제공하면 기본 Copy/Paste 액션과 사용자 키맵을 재사용할 수 있다. Esc는 CommonShortcuts.ESCAPE를 등록한다.
- Source 문자열을 운영체제 클립보드에서 임의로 파싱하지 않고 CopyPasteManager의 JVM 로컬 DataFlavor로 선택 스냅샷을 전달한다. 같은 IDE 내 문서 간 사용을 지원한다.
- copy 시점에 선택 Child와 양 끝이 선택된 Wire의 XML을 보존한다. 붙여넣기에서만 ID·location·input/output 및 절대 접점 좌표를 바꾼다. bendpoint 상대 벡터는 양쪽 Child가 같은 양만큼 이동하므로 보존한다.
- 화면 위치는 모델 좌표에 origin+pan-visibleRect 원점을 더한 값이다. paste delta는 복사 오프셋-현재 오프셋+(15×횟수,15×횟수)다. 숫자 ID는 BigInteger 최댓값 다음부터 할당한다.
- Esc는 제스처 시작의 모델·선택·Navigation을 복원하고 모든 진행 필드를 비운다. Source refresh도 진행 필드를 비우므로 최신 문서를 오래된 dragBase로 되돌리지 않는다.

빈 문서·Navigation·음수 좌표의 조합에서는 첫 노드 추가 시 자동 원점 보정이 적용되면 안 된다. paste refresh에서 현재 원점을 고정하여 복사 화면 기준 위치를 지킨다. 스크롤 위치와 Navigation은 별도로 보정하고 그대로 유지한다.

## 선택 삭제 조사

DeleteProvider는 UiDataProvider의 DELETE_ELEMENT_PROVIDER로 제공하고 Delete 키는 Canvas 액션에 연결한다. 실제 SDK의 DeleteProvider 공개 인터페이스를 확인했다. 삭제 집합은 선택 Wire와 선택 Child의 input/output 선언 인덱스의 합집합이다. 남은 Wire를 기존 순서대로 순회해 기존→새 인덱스 매핑을 만든 뒤 남은 모든 Child 참조를 갱신해야 한다. 화면에 표시하지 않는 Child도 XML에 남으므로 참조 정리에서 제외하지 않는다.

삭제는 한 WriteCommandAction으로 수행하고 현재 파싱 모델과 Canvas를 비교하여 Source가 바뀐 선택을 보호한다. 삭제 후 선택을 비워 같은 인덱스로 당겨진 다른 Wire가 선택되지 않게 하고 원점·Navigation은 유지한다. 복사 스냅샷은 문서와 독립적이므로 삭제 후에도 붙여넣기가 가능하다.

## 속성 창·툴바 조사

DialogWrapper와 Swing 입력 패널을 사용한다. 모델 편집 창은 프로젝트 서비스로 분리하여 플랫폼 테스트에서 확인·취소·Source 동시 변경을 실제 XML 저장 경로와 함께 검증한다. 일반 property 행에는 원래 XML property 인덱스를 보관하여 key를 바꿔도 추가 속성을 보존한다. description은 테이블에서 제외하고 별도 textarea로 편집한다.

IntelliJ XML PSI의 getAttributeValue/setAttribute는 이 경로에서 XML 엔티티 원문을 취급하므로 사용자 텍스트에는 명시적인 복원·이스케이프가 필요했다. DiagramXmlText를 통해 name·guardcondition·property key/value를 읽고 쓰며 CR/LF/탭은 숫자 엔티티로 저장한다. ModelDiagramParser도 name과 guardcondition을 복원하여 화면에서 실제 문자열을 표시한다.

Child 추가는 프로젝트 PropertiesComponent의 마지막 종류와 하나의 토글을 사용한다. ghost는 모델 좌표에 현재 원점·Navigation 변환을 적용한 실제 도형 스타일이며 클릭 전에는 XML 요소가 아니다. 생성은 기존 숫자 최대 ID+1, namespace 및 Wire 앞 삽입, preserveOrigin refresh를 사용한다.

## 편집 창 개선과 property 행 클립보드 조사

Transition 창은 입력 칸을 45→22로 줄이고 별도 guardcondition 라벨을 제거한다. Activity의 5개 버튼은 GridLayout으로 균등 너비를 사용하며 Key/Value의 선호 너비는 160/480이다. Description JTextArea 자체에 8px border 여백을 적용한다.

property 행은 Canvas 복사와 다른 DataFlavor를 사용하고 key/value만 복제한다. quoted TSV도 제공하여 탭·줄바꿈·따옴표를 보존한다. 붙인 행은 sourceIndex=null이므로 기존 XML property를 덮어쓰지 않는다. 테이블 셀 편집 중에는 행 provider를 데이터 스냅샷에 제공하지 않아 입력 텍스트 액션을 방해하지 않는다.

Open Class는 편집 중 셀을 먼저 commit한 뒤 class 값을 읽고 JavaPsiFacade/allScope로 찾은 navigationElement를 연다. XML 확인 동작과 독립적이며 실패는 패널 안에서 안내한다. Java 플러그인은 기존 필수 의존성을 modeleditor 모듈에도 명시한다. Open Query/Open Service는 비활성이다.

ChildCreationButton 하나에 본체 토글·오른쪽 화살표·종류 메뉴·Alt+Down을 결합했다. 마지막 종류·Esc·연속 배치 상태는 유지한다. ghost는 실제 drawNode와 같은 불투명 렌더링을 사용하며 실제 폰트·색·stroke를 맞춘 픽셀 비교로 검증한다.

## 표준 생성 툴바와 접점 조사

SDK의 SplitButtonAction은 본체 액션과 화살표 메뉴, ToggleAction 눌림 상태를 기본 제공한다. ActionToolbar에 이를 한 개 배치하고 useDynamicSplitButton=false로 종류 메뉴가 고정 본체 토글을 대체하지 않게 한다. 상태는 templatePresentation을 직접 활성/선택 변경하지 않고 update(event)의 실제 Presentation으로 전달한다.

Activity의 기존 toward는 상대 x가 밖이면 y를 보지 않고 corner 보정으로 넘어갔다. y가 도형 높이 안에 있는 좌우 접속 분기를 추가하여 y를 8px 여백으로 제한한다.

State 접점은 반경 10의 외곽으로 통합한다. 원 안의 유효한 선호 점은 마우스 방향으로 투영하고, 중심/미지정/오래된 외부 좌표는 이웃 방향으로 투영한다. 가상 State는 드래그 쪽 선호를 비워 이전 접점의 영향을 제거한다. 실제 원 경계도 유효한 재연결 대상으로 허용한다. State 이동은 선호 점을 같은 delta로 옮겨 접점을 유지한다.

## 수직 접점 clamp와 미니맵 조사

Activity.toward의 위·아래 변 분기만 x를 그대로 반환해 8px 여백을 우회했다. inset=min(8,width/2)로 제한하여 State·가상 State·bendpoint의 양방향에 동일 적용한다. 너비 16px 미만은 중앙으로 축소한다.

미니맵은 전체 벡터 경계(Child·Wire·라벨)를 축소 투영하고 visibleRect와 origin+pan 차이로 현재 뷰포트를 계산한다. 모델과 관계없이 frame은 visibleRect 우측 상단에 고정한다. 캔버스 크기 비트맵을 생성하지 않으며 기존 drawNode/drawWire/drawLabel을 재사용한다. 스크롤 리스너는 repaint만 요청한다.

읽기 전용 미니맵의 왼쪽 클릭은 뒤의 선택·편집·생성에 전달하지 않는다. 오른쪽 Navigation과 진행 중인 Child 드래그는 유지한다. 정렬 보조선/스냅 가능성은 alignment-review.md에만 정리했으며 구현하지 않았다.

## 정렬 및 클래스 자동완성 조사

검토만 하던 보조선은 후속 사용자 승인으로 실시간 스냅으로 구현했다. DiagramAlignment가 순수 이동·너비 보정량과 표시선을 반환하고 기존 제스처 저장 경로를 사용한다. 5px 임계치·Alt 해제·최소 너비와 다중 선택을 함께 검증한다.

기존 QueryResultTypeEditor에서 사용 중인 JavaReferenceEditorUtil.createDocument와 CLASS_NAME_REFERENCE_FRAGMENT 방식을 확인했다. 같은 참조 문서를 class Value 편집 셀에 적용하면 IntelliJ Java 자동완성이 짧은 클래스명을 검색하고 전체 클래스명을 삽입한다. 실제 프로젝트 클래스를 추가한 자동완성 테스트로 후보 및 삽입을 확인했다. 일반 문자열 목록을 직접 만들어 인덱싱하거나 외부 검색을 하지 않는다.

미니맵은 종전 맞춤 배율에 0.7을 곱하고 제목만 제거한다. 현재 뷰포트도 동일 투영을 사용한다.

## 같은 간격과 메모리 Query ID 수집 결정

- 결정: 간격은 중심이 아닌 외곽 경계에서 측정한다. 축별 후보를 정렬해 인접한 두 기준 요소만 비교하여 사이 배치와 앞·뒤 반복을 제공한다. 기존 정렬과 가장 작은 보정량으로 경쟁하고 동률은 기존 정렬을 보존한다. 모든 쌍의 비교는 불필요한 먼 요소와 비용을 늘리므로 사용하지 않는다.
- 결정: Query ID는 사용자의 명시적 요청대로 프로젝트 서비스의 메모리에만 보관한다. FileBasedIndex나 PersistentStateComponent는 사용하지 않는다. 기존 IDE의 파일/PSI 캐시 사용과는 별개다.
- 근거: QueryMapDocumentModel은 파일별 XML과 미저장 Document를 이미 사용한다. Query ID 전용 디스크 인덱스가 없는 상태이므로 ProjectFileIndex의 콘텐츠 순회와 cached Document로 최초 수집하고 Document/VFS/루트 리스너로 갱신한다.
- 결정: 변경은 MergingUpdateQueue에서 150ms 단위로 병합하여 백그라운드 처리한다. 자동완성은 불변 메모리 스냅샷만 읽는다. 프로젝트 해제 시 서비스와 큐가 함께 폐기된다.
- 파싱: JDK namespace-aware SAX에서 외부 DTD/엔터티 접근을 끄고 직접 자식 query의 id와 desc만 수집한다. SQL 본문은 보관하지 않는다. 잘못된 XML은 빈 후보로 교체하고 유효한 수정에서 복구한다.
- 연결: modeleditor의 compileOnly 의존성으로 queryeditor 서비스의 공개 스냅샷을 읽는다. 두 모듈은 기존 루트 플러그인에 함께 포함된다.

## 현재 화면을 포함하는 미니맵 투영 보정

- 원인: 기존 배율은 Child·Wire·조건 라벨만으로 계산했고 viewport와 내부 영역의 교차 사각형을 그렸다. 내용이 작거나 Navigation이 멀어지면 크기·비율이 달라 보이거나 사각형 자체가 사라졌다.
- 결정: modelViewport는 visibleRect에서 origin+pan을 뺀 실제 현재 화면이다. modelBounds와의 합집합 overviewBounds를 0.7 여백 배율로 맞추고 중심 offset도 합집합을 기준으로 계산한다.
- 결과: 사각형 전체와 다이어그램이 같은 배율로 안에 들어간다. 멀리 이동하면 배율만 작아지고 미니맵 프레임은 고정된다. 전체 문서 preferredSize를 viewport 크기로 사용하거나 사각형만 별도 축소하지 않는다.
- 검증: 수정 전 4개 재현 테스트가 실패했고 수정 후 작은 내용/큰 화면, 화면 크기 변경, 상하좌우 큰 Navigation 및 음수 좌표를 확인한다. 실제 렌더링에서 파란 사각형의 네 변을 검사하고 model-minimap-navigation.png를 확인한다.

## 미니맵 기본 배율 복원

사용자 요청으로 30% 추가 축소를 취소했다. 배율은 min(0.5,내부 너비/합집합 너비,내부 높이/합집합 높이)이며 앞선 0.7 추가 여백 결정은 대체한다. 내용과 현재 화면의 합집합 및 Navigation 자동 맞춤은 유지한다. 기존 11개 미니맵 테스트를 통과했다. 내부 영역 경계에 정확히 맞추므로 포함 여부 테스트는 부동소수점 오차 0.000001px만 허용한다.

## 탐색·설정·Navigation 확장 조사

- Query 탐색은 NavigatableFileEditor의 canNavigateTo/navigateTo로 OpenFileDescriptor를 처리한다. 로컬 IntelliJ SDK의 FileEditorManagerImpl.openEditorImpl이 이 인터페이스를 검사하여 navigateAndSelectEditor를 호출함을 확인했다. Find in Files 전용 검색 코드를 복제하지 않는다.
- QueryNavigation.find/open과 QueryEditorNavigation.selectQuery/selectQueryAtOffset을 공개한다. ID 탐색은 메모리 인덱스 후보를 최신 PSI로 재확인하고 중복은 파일 chooser로 처리한다. 에디터 로딩이 지연되면 파일의 요청 ID를 Design.refresh에서 소비하여 선택을 보존한다. UI 탐색 API는 EDT에서 호출한다.
- 프리셋은 사용자 확인대로 Settings 저장·편집만 제공한다. 프로젝트 설정에는 디렉터리와 Name·속성·Description·#RRGGBB를 복제 저장한다. XmlSerializer 직렬화 왕복과 Apply/Reset을 검증한다. 기존 Activity 색상이나 생성 툴바의 종류를 변경하지 않는다.
- Service 후보는 일반 .xml 이름이고 Open Service 대상은 .glue_uml_ad임을 사용자가 재확인했다. 폴더 직접 자식만 사용하며 value에 디렉터리 구분자가 있으면 열지 않는다. 설정 변경과 VFS 변경은 다음 후보 조회에서 반영한다.
- 자석은 기존 직접 Activity 연결의 선호 좌표를 재사용한다. Activity↔State 또는 꺾인 Wire는 선호 좌표만 바꿔서는 실제 접점이 움직이지 않으므로 인접 bendpoint를 보정하거나 필요할 때 하나 추가한다. 기존 firstDimension/secondDimension 표현으로 저장하여 별도 XML 확장을 만들지 않는다. 중심±30n 격자와 8px 제한을 지킨다.
- navigationOnly Canvas는 Scrollable의 양 축 추적과 scrollbar NEVER로 뷰포트 크기를 사용한다. 휠은 30px Navigation이며 Ctrl은 가로다. 진행 중 드래그는 취소한 후 이동한다. 미니맵 클릭은 현재 투영으로 모델 점을 역변환해 화면 중심에 놓는다.
- 미니맵 모델 렌더링만 contentArea에 clip하고 파란 테두리는 원래 clip에서 그려 맞춤 경계의 아래/오른쪽 선도 보존한다. Range box는 alpha 하늘색 fill을 추가한다.

## 후속 편집과 서비스 입력 개선 조사

- 루트 description은 파싱 데이터에 보존하고 HUD만 필터한다. DiagramPropertiesDialog는 ActivityPropertiesPanel의 showName=false와 showOpenButtons=false를 재사용한다. 편집기는 sourceIndex로 기존 property 태그/사용자 속성을 보존하고 확인 후 modificationStamp를 재검사한다. 한 번의 WriteCommandAction으로 Undo한다.
- 끝점 외의 핸들은 기존 previewBendpoint에서 WireMagnet.dragPosition을 적용한다. Activity별 x 격자와 y 중심 후보는 해당 축의 8px 범위 안에서만 만들고 가까운 후보를 선택한다. raw 입력과 원래 Wire를 사용하며 기존 DiagramEdits 병합·관통 검사를 바꾸지 않는다.
- 기존 ServiceNameCellEditor는 PlainText EditorTextField에 provider만 설치했다. 표준 TextFieldWithCompletion으로 바꾸고 자동 팝업과 강제 자동 팝업을 켰다. 로컬 SDK에서 이 컴포넌트가 Editor의 ALWAYS_AUTO_POPUP을 설정하는 것을 확인했다. 후보는 basename 외에 .xml 이름도 lookup 문자열로 제공하여 기존 XML 값을 완성할 수 있다.
- 경로는 프로젝트 기준 VirtualFile에서 상대 경로로 탐색한다. 내부 absolute/URL은 호환하고 조상 검사로 외부 경로를 거부한다. 폴더 picker도 프로젝트 루트로 제한한다. Open Service는 .xml, -service 접미사를 차례로 제거한다.
- Settings는 QueryEditor와 같은 Configurable.NoScroll, JBUI 여백·FormBuilder·목록/상세 BorderLayout을 사용한다. 선호 크기는 680×440이고 compact 편집기로 Description과 최소 폭을 줄였다. 이 크기에서 빈번히 쓰는 소수 행의 내부 스크롤바도 없음을 검사했다.
- 미니맵 드래그 입력은 press 시 투영을 고정한다. 화면 투영은 기존 자동 맞춤을 유지하고 입력 좌표만 시작 contentArea에 제한한다. miniMapDrag를 제스처 상태로 관리하여 release·Esc·Source 교체에서 해제한다.
- 프리셋·Wire 생성 확장 검증: 관련 모델·Query 테스트 206개가 모두 통과했다. IntelliJ Settings 아이콘과 같은 일반 #6C707E·다크 #CED0D6 팔레트를 사용한다. Service 검색은 프로젝트 콘텐츠까지 확장하며 중복 파일은 경로로 선택한다. 프리셋 색상은 Activity·ghost·미니맵 공통 렌더링으로 확인했다. 실제 IDE 중복 파일 팝업 조작은 자동 검증 범위 밖이다.
## 생성 미리보기 원인과 수정 결정

빈 화면 포인터를 가상 InitialState로 취급하면서 자석의 attach가 실제 State 연결처럼 접점을 맞추기 위한 bendpoint를 생성했다. 빈 화면 단계에서는 attach를 건너뛰어 직선으로 표시하고 실제 Child 위에서는 기존 경로를 사용한다. Foreground는 기존 name 글자색 RGB(145,145,145)를 기본값으로 유지한다. 배경 저장 필드 color는 기존 설정 호환을 위해 이름을 바꾸지 않는다.

추가 렌더링 조사: 선택 핸들 렌더링 이후 안티앨리어싱 상태를 상속하여 ghost 선의 가장자리가 실제 Wire와 달랐다. Wire 미리보기에서 안티앨리어싱과 1.2px 굵기를 명시하고 반투명 합성을 제거했다. 실제 GUI 파일 열기 테스트는 SDK 비동기 대기 때문에 기록용 관리자와 실제 Design/Source 에디터로 선택 요청을 검증한다.

## 텍스트 툴바·배율·저장 조사

SDK SplitButton은 아이콘용 ActionButton을 사용하므로 텍스트 본체는 표준 텍스트 ActionToolbar와 메뉴 화살표를 조합한다. 메뉴 presentation의 KeepPopupOnPerform.Never로 선택 후 닫는다. 화면 배율은 저장 좌표와 분리한다. 서비스 변환은 좌표나 Wire ID가 아닌 input/output의 선언 인덱스로 연결한다. 출력 순서 차이는 의미에 영향을 주지 않으므로 원본 순서로 고정한다. 사용자 최종 답변에 따라 false transaction도 출력하며 description은 항상 제외한다.

## TransactionGuard 오류 원인과 결정

selectNotify → refresh → commitDocument의 동기 호출이 원인이다. UI 스레드와 NON_MODAL 상태만으로 쓰기가 허용되지는 않는다. 공개 TransactionGuard.isWritingAllowed로 현재 문맥을 확인하고 nonModal invokeLater에서 갱신한다. WriteAction을 추가하는 방식은 쓰기 안전 문맥 자체를 만들지 못하므로 사용하지 않는다. 테스트의 performActivity(false)는 같은 NON_MODAL 상태에서 쓰기 허용만 차단하므로 사용자 스택을 재현할 수 있다.

생성 클릭 누락 원인: createChild는 scheduled=true이면 즉시 반환하고 최신 PSI와 Canvas가 달라도 갱신만 하고 요청을 버렸다. 예약 여부는 문서 변경 여부를 의미하지 않으므로 최신 정상 XML을 기준으로 생성하는 것으로 대체한다. 생성 명령은 실제 마우스 클릭과 같은 쓰기 안전 문맥에서 즉시 처리하고 그렇지 않은 경우만 비모달 실행으로 넘긴다.

배율 UI는 사용자 후속 요청에 따라 IntelliJ ComboBox로 교체한다. 14개 고정 항목을 사용하고 사용자 정의 배율 입력은 제공하지 않는다. 선택 갱신과 Canvas 알림의 반복 호출은 동기화 플래그로 방지한다.

사용자의 ghost 표시와 일정 시간 후 정상 동작이라는 확인으로 clickCount 필터를 조사했으나 실제 입력 횟수는 확인되지 않았다. SDK AbstractPopup은 종료 시 blockNextEvents를 사용하지만 이것이 실제 보고 사례의 원인인지도 확인되지 않았다. 사용자의 큐 제안을 반영하여 생성 콜백에 도착한 요청의 FIFO 순서와 안전한 실행을 보장한다. 임의 클릭 횟수 예외는 구현하지 않는다.

진단 위치: 설치 SDK의 IdeEventQueue preprocessor는 실제 이벤트 전달보다 앞서 호출된다. 이 관찰 지점과 Canvas 마우스 리스너를 함께 기록하면 팝업/IDE 단계에서 소비되는 입력을 구분할 수 있다. INFO 로그는 사용자가 Debug 카테고리를 켜지 않고 바로 수집할 수 있다. 마우스 이동은 제외하고 생성 모드·캔버스 영역으로 원시 관찰을 제한한다.


## Wire 수정 조사

절대 기준점은 동반 이동하고 bendpoint는 노드 중심 상대 좌표이므로 유지한다. 기존 문서는 파싱 기본값만 적용하여 열기 시 변경을 방지한다.

## 정수 좌표 결정

사용자 확인에 따라 음수는 허용한다. XML 저장 경계에서 가장 가까운 정수로 반올림하고 기하 계산은 Double을 유지한다. weight는 좌표가 아니므로 변환에서 제외한다.

## 서비스 출력 순서 조사

제공 예제의 5종 property 순서를 모두 비교한 결과, 원본 속성 전체 개수 N에 대한 버킷 수 2N+1과 양수 Java 문자열 해시의 나머지 오름차순이 일치했다. 동일 버킷은 원본 순서를 유지한다. class·description은 개수 산정 후 출력에서 제외한다. transition은 기본 HashMap 순서와 마지막 조건 덮어쓰기로 예제 전체가 일치한다. 기존 생성기 구현을 확인한 사실은 아니며 이 예제로 확인한 호환 규칙이다.

## 서비스 속성 직렬화 결정

JDOM 기본 출력은 namespace 선언을 별도로 배치하므로 명시한 속성 순서를 보장하는 서비스 전용 직렬화를 사용한다. 값은 JDOM의 속성 이스케이프 함수로 처리한다. 문자열 전체 치환은 속성 내부 공백을 훼손할 수 있어 사용하지 않는다.

## 마지막 개행 제거 결정

문자열 전체의 공백을 자르지 않고 직렬화기가 덧붙인 마지막 LF 하나만 제거하여 기존 값과 내부 포맷을 보존한다.

## 모달 상태와 폴더 선택 창 조사

SDK의 TransactionGuard.isWriteSafeModality로 현재 예약 상태의 안전성을 확인할 수 있다. 비모달 예약만 사용하면 속성 창 종료까지 기다리므로 안전한 현재 모달 상태를 사용하고 상태 변경 시 예약 세대를 교체한다. TreeFileChooserFactory는 PsiFile 선택용이며 저장 폴더에는 기존 FileChooserDescriptor의 isForcedToUseIdeaFileChooser 설정을 사용한다.

## 모델 파일 미사용 체크박스 결정

사용자 명확화에 따라 UI와 프로젝트별 저장만 추가한다. 기존 ModelEditorState 영속화와 data class 비교를 재사용하고 기본값은 false로 둔다. 기존 생성자 위치 인자 호환을 위해 마지막 필드로 추가한다.

## Service XML 직접 편집 조사와 결정

사용자는 기존 glue_uml_ad용 편집기의 옵션 분기를 원하며 좌표·크기를 저장하지 않는다고 명확히 했다. 서비스 문서를 비물리적 GlueDiagram PSI로 변환하여 Canvas와 편집 도구를 재사용한다. 초기 연결을 기준으로 BFS 단계를 배정하고 XML 순서로 가로 배치한다. 순환은 이미 방문한 Activity를 다시 배정하지 않아 종료하며 비연결 구성요소도 별도로 배치한다. 역방향·긴 연결·동일 대상 다중 연결은 바깥쪽 개별 경로를 만든다.

전체 서비스 재직렬화 대신 원본 PSI 일부를 수정하여 기존 이름·namespace·주석·추가 속성과 타 namespace 요소를 보존한다. 실제 문서의 변경 번호로 투영을 무효화하고 편집 결과는 같은 쓰기 명령에 반영하여 Undo·Redo를 지원한다. 메모리 투영만 이동하면 원본 문서의 의미가 같으므로 수정하지 않는다. 기본 생성·복사 시 서비스 Activity 이름 충돌은 숫자 접미사로 해소한다.

## ELK Layered 전환 조사

ELK 공식 알고리즘 문서와 순수 Java 실행 지침을 확인했다. 0.12.0의 org.eclipse.elk.alg.layered를 고정하고 RecursiveGraphLayoutEngine의 org.eclipse.elk.layered 알고리즘을 호출한다. 메타데이터 검색에는 플러그인 ClassLoader를 사용하고 미등록인 경우 LayeredMetaDataProvider를 등록한다. DOWN·ORTHOGONAL·randomSeed=1, 중심 고정 포트를 사용한다. 도형 폭을 짝수로 맞춰 정수 반올림 후에도 중앙 포트의 직각 경로가 유지되도록 한다.

공식 참고: https://eclipse.dev/elk/reference/algorithms/org-eclipse-elk-layered.html 및 https://eclipse.dev/elk/documentation/tooldevelopers/usingplainjavalayout.html . Maven 접근은 현재 프록시 127.0.0.1 연결 실패로 차단되며 로컬 ELK 캐시도 없어 실제 실행 검증은 의존성 확보 후 수행해야 한다. 기존 BFS 결과나 모의 엔진으로 ELK 검증을 대체하지 않는다.

## 모듈 합성과 런타임 누락 원인

ELK JAR 다운로드 뒤에도 runIde 플러그인의 lib에는 프로젝트 합성 JAR만 있었다. pluginComposedModule(project(...))는 클래스 합성 대상으로만 연결되므로 루트 implementation에도 프로젝트 의존성을 연결해야 전이 라이브러리가 sandbox runtimeClasspath에 포함된다. 공식 예제인 pluginComposedModule(implementation(project(...)))로 수정했다. https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#multi-module-setup 참조.

## 포트 공유와 표시 경로 조사

이전 ELK 구현은 노드별 중앙 포트를 모든 연결이 공유하여 mergeEdges=false만으로 도형 근처의 겹침을 없앨 수 없었다. 연결별 고정 포트, Activity 포트 공간, edge/port 간격을 적용한다. 별도 접점을 원래 중심 기반 렌더링으로 다시 합치지 않도록 서비스 자동 wire만 메모리 정책으로 구분한다. 단일 점 교차와 양의 길이를 공유하는 선분은 별도로 검증한다.
