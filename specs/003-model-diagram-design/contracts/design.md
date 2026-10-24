# Design 화면 계약

- glue_uml_ad의 Design / Source 순서와 공유 문서를 유지한다.
- 노드 및 연결선 좌표는 XML과 사용자 연결 규칙으로 결정한다. Wire 번호는 XML 선언 순서다.
- 꺾임점 없는 Activity끼리는 좌우 8px 제외 교집합이 있으면 수직, 없고 위아래 8px 제외 교집합이 있으면 수평 연결한다. 해당 교집합에 좌표를 제한하고 가까운 변을 연결한다. 모두 없으면 가까운 둥근 모서리를 대각선으로 연결한다.
- Activity 직결은 시작·끝 어느 핸들을 잡아도 현재 마우스 좌표로 공통 x/y를 정한다. 공통 기준을 firstRelativePoint에 저장하며 end 드래그는 lastRelativePoint도 갱신해 Source 재파싱 후에도 유지한다.
- Activity는 흰색, 높이 30, 반경 8이다. State는 20×20이며 FinalState 외곽은 채우지 않고 내부 원은 (3,3)에 지름 15로 표시한다.
- 배경은 IDE 패널 배경이다. Wire와 글자는 RGB 145다. 조건 라벨은 지정 선분 중점에 배경을 두며 하단 1px 여백과 위로 2px 조정한 글자를 사용한다.
- 마지막 선택 요소와 핸들은 가장 앞에 표시한다. Child 핸들은 6/4px이며 마지막 대상 외에는 검정 테두리를 표시한다.
- Child 클릭은 선택이며 이미 선택된 Child의 클릭은 다중 선택을 유지한다. Shift는 추가 선택이다. 좌측 빈 공간 드래그는 완전 포함 범위 선택이다.
- 어디에서든 우측 드래그는 Navigation이다. 현재 x,y는 보이는 화면 좌측 상단에 고정한다. Child 편집으로 원점·Navigation·스크롤 위치가 바뀌지 않는다.
- 이동은 선택 Child 전체에 적용한다. 좌우 너비 조절은 선택 Activity 전체에 공통 변화량을 적용하고 반대쪽 변·최소 너비 16·XML size 높이를 유지한다.
- 선택 Activity 좌우 핸들은 resize 커서, Wire의 모든 핸들은 move 커서다. Wire 핸들 Hit는 모두 6×6이고 선분 Hit는 4px이다.
- Wire 경로점은 6/4px, 중점은 4/2px로 표시한다. 꺾임점 드래그는 이동이며 중점 드래그는 삽입이다. 다른 꺾임점 위치와 기존 weight를 유지한다.
- 작은 중점 핸들은 원래 직선에서 5px 초과일 때만 분할하며 5px 이내로 돌아오면 원래 선분으로 병합한다. 안쪽 release는 XML과 Undo 기록을 변경하지 않으며 바깥 release만 삽입을 저장한다.
- 기존 bendpoint 핸들도 제거 후보의 이웃점 직선에서 5px 이내이면 병합하고 초과하면 이동 상태로 복원한다. 안쪽 release는 해당 점만 삭제하고 나머지 점 위치·weight·속성 및 Undo/Redo를 유지한다.
- Wire 끝점은 빈 공간에서 마우스 중심의 가상 State로 기존 연결 보정을 적용하고, 유효한 Child 위에서는 해당 Child로 미리보기한다. 꺾임점은 원래 상대 벡터와 변경된 중심으로 first+(second-first)×(i+1)/(n+1)을 계산하여 비례 이동한다. 가상 State 도형은 표시하지 않는다. 빈 공간 또는 반대쪽과 같은 Child에 release하면 원복하고 유효 대상에 release하면 input/output의 해당 인덱스 참조를 갱신한다.
- 편집 release마다 한 번의 Undo 명령으로 XML을 갱신하며 저장·Source·Redo를 지원한다. 읽기 전용 또는 Source가 바뀐 드래그는 편집하지 않는다.
- 잘못된 XML은 안내하고 Source를 계속 사용할 수 있다. 관련 없는 속성·참조와 XML Wire 순서를 유지한다.

## 드래그 취소·클립보드 계약

- Esc는 현재 드래그를 시작 상태로 복원하고 release를 무시한다. 이동·너비·Wire 경로/끝점·Navigation·범위 선택에 적용한다. 새 XML 명령이나 Undo 기록을 만들지 않는다.
- Canvas는 UiDataProvider로 CopyProvider·PasteProvider를 제공한다. IDE 기본 Copy/Paste 메뉴와 현재 키맵을 사용하며 기본 단축키는 Ctrl+C/Ctrl+V다.
- 복사는 선택 Child와 양 끝 Child가 모두 선택된 Wire를 선언 순서대로 스냅샷으로 보관한다. Wire 단독 선택은 복사하지 않는다. 외부 input/output은 복제하지 않는다.
- 붙여넣기는 복사 화면 오프셋과 현재 origin·Navigation·스크롤 차이를 보정하고 성공 횟수×15px씩 이동한다. 기존 화면 위치는 유지한다. ID는 대상 숫자 최대치+1부터 Child·Wire 순서로 부여하고 Wire 참조는 선언 인덱스로 재매핑한다.
- 로컬 클립보드는 같은 IDE의 문서 간 사용을 지원한다. 읽기 전용·잘못된 XML·호환되지 않는 클립보드·진행 중인 드래그에서는 붙여넣기를 실행하지 않는다. 복사는 읽기 전용에서도 허용한다.
- 붙여넣기 한 번은 한 XML 명령이며 Undo/Redo·Source·저장을 지원한다. 붙여넣은 Child를 선택하며 원본과 비연결 요소는 수정하지 않는다.

## 선택 삭제 계약

- Delete 키와 IDE Delete 액션은 선택 Child 전체 또는 선택 Wire를 삭제한다. 삭제 Child에 연결된 Wire도 함께 삭제하며 상대편 Child에서는 해당 참조만 제거한다.
- Wire 참조는 ID가 아닌 선언 인덱스다. 삭제한 Wire 참조를 제거하고 남은 모든 Wire 참조를 새 선언 인덱스로 재매핑한다. 미지원 Child도 포함하며 남은 ID·순서·속성·경로는 보존한다.
- 삭제와 참조 변경은 한 번의 Undo/Redo로 처리한다. 삭제 후 선택을 비우고 Navigation·스크롤·원점·클립보드는 유지한다.
- 선택 없음·읽기 전용·드래그 중·Source 갱신 대기·잘못된 XML에서는 삭제하지 않는다. 삭제 직전 실제 Source와 표시 모델의 일치 여부도 확인한다.

## 속성 창과 생성 툴바 계약

- Wire 선/조건 라벨의 더블클릭은 guardcondition 창을 열고 Activity 더블클릭은 name·일반 property 테이블·description textarea를 연다. 확인 때만 단일 명령으로 변경하며 취소·무변경·Source stamp 변경·읽기 전용은 문서를 바꾸지 않는다.
- property key/value는 실제 사용자 텍스트로 보여주고 XML 엔티티를 변환한다. 테이블 행은 기존 태그 인덱스를 보관하여 기타 속성을 유지한다. description은 전용 입력이며 빈 값은 제거한다.
- 상단의 통합 토글 드롭다운은 프로젝트의 마지막 종류를 사용한다. 생성 모드에서 ghost 중심은 마우스이며 클릭당 최대 ID+1의 Child를 만든다. Activity는 100×30·이름 Activity이고 State는 20×20이다. 연결 참조는 추가하지 않는다.
- 모드는 연속 배치를 지원하며 Esc·토글 해제로 종료한다. Navigation·스크롤·원점 및 namespace를 보정하고 기존 Wire 순서/참조를 유지한다. 생성은 클릭당 한 Undo/Redo·Source·저장과 연동한다.

## 편집 창 배치·행 클립보드·클래스 탐색 계약

- Transition 편집 창은 기존 약 절반 너비이며 조건 라벨을 제거한다. Activity는 대문자 라벨과 8px textarea 여백, 5개 균등 버튼, Key/Value 약 1:3 너비를 사용한다.
- property 행 Copy/Paste는 새 sourceIndex=null 행을 삽입하며 값과 TSV 줄바꿈을 보존한다. XML 변경은 Activity 확인 시에만 수행한다. 셀 편집 중에는 행 provider를 제공하지 않는다.
- Open Class는 현재 class 값으로 실제 클래스 에디터를 연다. 실패는 인라인 안내이며 XML을 저장하지 않는다. Open Query/Open Service는 비활성이다.
- Child 종류 선택과 추가 모드는 통합 토글 드롭다운 버튼으로 제공한다. ghost는 실제 drawNode와 같은 불투명 스타일이며 기존 생성 좌표·취소·Undo를 유지한다.

## 표준 생성 액션과 접점 계약

- 생성 툴바는 ActionToolbar·SplitButtonAction·ToggleAction을 사용하며 커스텀 화살표/마우스 영역 판정을 사용하지 않는다. 본체 토글과 종류 선택을 분리하고 읽기 전용·Esc·마지막 종류·ghost를 유지한다.
- Activity 좌우 접점은 상대 점 y를 상하 8px 여백 안에서 반영한다. 수직 우선과 범위 밖 대각선 보정은 유지한다.
- 모든 State의 접점은 반경 10 원 외곽이다. 원 안/경계의 마우스를 투영하고 선호가 없거나 중심이면 이웃 방향을 사용한다. State 이동·Source·저장·Undo/Redo에서도 선호 접점을 보존한다.

## 수직 접점과 미니맵 계약

- Activity 위/아래 접점의 x는 좌우 8px 안쪽으로 제한한다. 너비가 16px 미만이면 중앙으로 축소한다. State·가상 State·bendpoint 및 양 방향에 적용한다.
- 미니맵은 현재 visibleRect 우측 상단에 반투명 벡터 개요와 뷰포트를 표시한다. 최대 200×140이며 빈/잘못된 문서와 작은 화면에서는 숨긴다. XML·선택·Undo를 변경하지 않는다.
- 왼쪽 클릭은 미니맵 안에서 소비하고 오른쪽 Navigation 및 이미 진행 중인 제스처는 유지한다. Source·편집 미리보기·Undo/Redo는 현재 모델을 통해 즉시 반영한다.
- 정렬 보조선과 release 스냅은 검토 전용이며 이번 코드에는 구현하지 않는다.

## 정렬·미니맵·클래스 자동완성 계약

- Child 이동·Activity 너비 조절의 실시간 스냅 임계치는 5px이며 Alt로 우회한다. 선택 집합의 간격과 반대편 변·최소 너비를 보존하고 release에서 기존 단일 XML 명령으로 저장한다.
- 보조선은 임시 표시이며 Source·Undo 데이터에 추가하지 않는다. 미니맵 프레임에는 그리지 않는다.
- 미니맵 제목을 제거하고 기본 맞춤 배율을 모든 모델 도형과 뷰포트에 공통 적용한다. 후속 요청에 따라 0.7배 추가 축소는 취소한다.
- ActivityPropertiesDialog는 프로젝트를 패널에 전달한다. class Value만 Java 참조 셀 편집기를 쓰며 후보 선택은 전체 클래스명을 테이블 모델에 전달한다. 확인/취소와 Open Class는 기존 경로를 유지한다.

## 균등 간격과 sql-key 계약

- Child 이동 중 동일 줄/열의 인접 두 Child를 기준으로 사이 또는 앞·뒤에 같은 외곽 간격을 맞춘다. 5px 임계치와 Alt 우회, 단일 Undo를 사용하며 간격마다 치수선·끝 표시·거리 값을 표시한다.
- 일반 정렬과 경쟁하면 보정량이 작을수록 우선하고 동률은 일반 정렬을 우선한다. 가이드는 Source와 미니맵에 넣지 않는다.
- QueryIdIndexService는 프로젝트 최초 실행 또는 최초 사용 시 메모리 목록을 백그라운드로 구성한다. 범위는 프로젝트 콘텐츠의 .glue_sql이며 queryMap의 같은 namespace인 직접 자식 query만 포함한다.
- 미저장 변경은 150ms 병합 큐에서 해당 파일만 갱신하고 폴더/루트 변화는 콘텐츠 목록을 다시 확인한다. 파일을 삭제·제외하거나 XML이 잘못되면 후보를 제거한다.
- sql-key Value는 메모리 Query ID를 자동완성하며 ID만 삽입한다. 후보의 설명·파일은 구분용이다. 인덱스용 파일이나 영속 설정은 추가하지 않는다. 초기 수집 중에도 직접 입력할 수 있다.

## 미니맵 현재 화면 전체 표시 계약

- 현재 화면은 캔버스 visibleRect의 크기와 원점·Navigation 역변환을 사용한다. 스크롤바를 포함한 에디터 전체 크기나 문서 preferredSize를 사용하지 않는다.
- overviewBounds는 내용과 현재 화면의 합집합이다. 도형과 viewport에 동일한 기본 맞춤 배율·offset을 적용하고 전체가 내부 영역에 포함되도록 한다. 추가 0.7 여백 배율은 적용하지 않는다.
- Navigation·스크롤·리사이즈 시 합집합을 다시 계산한다. 프레임은 우측 상단에서 크기를 유지하며 파란 사각형은 실제 화면 비율 그대로 표시한다. 교차 영역으로 사각형을 재구성하지 않는다.
- 읽기 전용·클릭 보호·XML 무변경은 유지한다.

## 공통 탐색과 설정·화면 조작 계약

- QueryNavigation.find(project,id)는 QueryLocation 목록을 반환한다. open(project,id,file?)은 단일 파일 또는 중복 파일 chooser를 열고 Design에서 ID를 선택한다. open(project,location)도 외부 호출에 제공한다. UI 탐색은 EDT에서 호출한다.
- QueryEditorNavigation.selectQuery/selectQueryAtOffset과 NavigatableFileEditor가 같은 선택 기능을 사용한다. OpenFileDescriptor가 Query 범위 안이면 Find in Files의 탐색도 처리하고 Query 밖이면 false를 반환하여 Source 탐색을 유지한다. 로딩 지연 시 요청 ID를 보존한다.
- Activity Open Query와 Open Service는 현재 셀 편집을 완료한 sql-key/service-name을 사용한다. 탐색은 확인 전 Activity XML을 변경하지 않는다. 없거나 준비 중인 Query는 인라인으로 안내한다.
- Service 자동완성은 프로젝트 내부 설정 폴더의 .xml 직접 자식 이름이며 확장자를 뺀다. 표준 자동 팝업과 Ctrl+Space를 사용하고 기존 .xml 값도 완성할 수 있다. Open Service는 입력 끝의 .xml과 -service를 제거하고 .glue_uml_ad를 붙인다. 디렉터리 구분자가 포함된 value를 허용하지 않는다.
- GlueModelEditor 설정은 프로젝트 경로와 프리셋을 Apply/OK에서 저장한다. 프리셋은 Name·Properties·Description·색상 피커와 Add/Delete를 제공하며 Open 버튼은 없다. 후속 FR-132~FR-133에 따라 생성 메뉴와 기존 도형의 class 색상에도 적용한다.
- 모델 캔버스는 scrollbar NEVER와 뷰포트 크기 추적으로 스크롤바를 만들지 않는다. 휠은 30px 세로, Ctrl+휠은 가로 Navigation이다. Range box는 alpha 하늘색이고 미니맵 click은 클릭 모델 점을 화면 중심으로 옮긴다.
- 루트 property HUD는 좌표 아래 고정 표시한다. Wire 자석은 기본 off이고 on에서 중심±30n 및 가운데 접점을 기존 제한 안에서 사용한다. Activity↔State/꺾인 연결은 인접 bendpoint로 실제 접점을 고정하며 기존 XML 표현으로 저장한다.

## 루트 편집과 연속 조작 계약

- 툴바 Diagram 속성은 Properties와 Description만 편집한다. 확인 전 문서를 변경하지 않고 Source가 바뀌거나 읽기 전용이면 저장하지 않는다. Child/Wire와 알 수 없는 속성은 보존한다.
- 자석 on이면 모든 Wire 핸들에 Activity x 격자/y 중심을 범위 안에서만 적용한다. State와 범위 밖의 자유 이동, 기존 병합 보호는 유지한다.
- 미니맵 드래그는 시작 투영을 기준으로 연속 이동하고 release 최종 위치를 반영한다. Esc는 시작 Navigation을 복원하고 XML은 변경하지 않는다.
- Service 저장 폴더는 프로젝트 내부 상대 경로다. .xml 후보는 basename으로 완성하고 Open Service에서 .xml/-service 접미사를 제거한다.
- Settings 외곽에는 스크롤을 만들지 않는다. 많은 프리셋·표·Description을 위한 내부 세로 스크롤은 유지한다.
- 후속 생성 계약: 프리셋 메뉴 선택 후 본체 토글로 연속 생성한다. 기존 Activity·ghost·미니맵에 같은 class 색상을 적용한다. Child와 Wire 생성은 상호 배타적이며 Wire는 시작 Child와 다른 끝 Child를 클릭하여 생성한다. Esc·Source 변경은 미완성 초안을 취소한다. Open Service는 프로젝트 콘텐츠에서 찾고 중복이면 전체 경로 선택창을 제공한다.
후속 FR-137~FR-140이 이전 생성 계약을 대체한다. 종류·프리셋 메뉴를 고르면 즉시 생성 모드를 켠다. Open Service는 Design을 명시적으로 선택한다. Background와 Foreground를 저장하며 Foreground는 name 글자색만 바꾼다. 빈 화면 Wire 미리보기는 자석용 bendpoint를 만들지 않고 실제 Wire 스타일로 표시한다.

텍스트 툴바 계약: 빈 Activity·InitialState·FinalState·프리셋 이름을 선택 표시하며 메뉴 선택 후 닫고 활성화한다. Wire는 텍스트로 표시한다. 배율 묶음은 [-][현재%][+]이고 가운데가 100%로 복원한다. 저장 계약: glue_uml_ad 저장 완료 시 프로젝트 내부 설정 폴더의 이름-service.xml을 갱신한다. 정상 그래프만 출력하며 false transaction과 한글/특수문자를 보존하고 description은 제외한다. 잘못된 입력은 기존 출력 유지와 오류 알림으로 처리한다.

Design 선택 알림은 동기 PSI 변경을 수행하지 않고 안전한 비모달 UI 작업으로 갱신을 예약한다. 생성자와 기타 refresh 호출도 쓰기 불가 문맥이면 같은 경로로 위임한다. 예약 중복과 폐기된 에디터를 보호한다.

예약 갱신 중에도 Child 생성 클릭을 처리한다. 생성 기준은 최신 정상 문서이며 Canvas 스냅샷과 다르다는 이유로 취소하지 않는다. 쓰기 불가 생성은 안전한 시점에 모드·프리셋을 다시 검사한다. 더블클릭·드래그·미니맵의 기존 입력 규칙을 유지한다.

FR-152에 따라 확대 버튼 묶음은 20~150%를 10% 간격으로 제공하는 단일 Select로 대체한다. 기본값과 복원 항목은 100%이며 모델 좌표는 변경하지 않는다.

Child 생성 콜백은 요청 스냅샷을 FIFO 큐에 넣고 비모달 UI 작업에서 순서대로 처리한다. 모드 세대가 달라지거나 에디터가 폐기되면 대기 요청을 취소한다. 기존 더블클릭 정책과 입력 전달은 유지한다.

진단 로그 접두사는 [ModelChildCreation]이다. RAW·CANVAS·IGNORE·QUEUE_ACCEPT·QUEUE_CANCEL·CREATE_BEGIN·CREATE_DONE 등의 단계와 식별자를 제공한다. INFO 기본 기록이며 이벤트를 소비하거나 기존 생성 정책을 바꾸지 않는다. 원시 관찰자는 에디터 폐기와 함께 제거된다.


## Wire 표시와 이동 계약

InitialState 조건은 그리기·이미지·범위·라벨 클릭에서 제외한다. 양 끝 동일 이동은 미리보기와 저장에 같은 기준점 변환을 사용하고 같은 쓰기 명령으로 Undo한다.

## 조건 편집 및 좌표 저장 계약

InitialState 출발 Wire는 더블클릭 대화상자를 열지 않는다. Design 좌표 쓰기는 정수로 반올림하며 음수와 0을 허용한다. 수정 대상의 기존 좌표도 정규화하고 weight와 기타 속성은 보존한다.

## 서비스 출력 순서 계약

Router transition은 success·find·custom·insert·delete·update2이며 success는 SeachEmp-SQL2로 연결된다. property는 FR-168의 버킷 순서를 사용한다. 모든 transition은 property보다 먼저 출력하며 Activity는 Child 순서를 유지한다.

## 서비스 XML 문자열 계약

service는 name·initial·xmlns, transaction-manager는 id·commit, activity는 name·class, transition·property는 name·value 순서이다. 빈 태그는 닫기 구문 앞 공백이 없으며 네 칸 들여쓰기·LF·UTF-8 선언을 사용한다. 값 내부 공백과 특수문자는 재파싱 후 보존한다.

## 서비스 출력 종료 규칙

FR-173에 따라 마지막 닫기 구문 직후 출력이 끝난다. 내부 LF 개행은 유지하고 파일 끝의 LF는 출력하지 않는다.

## 모달 열기와 저장 폴더 선택 계약

쓰기 안전한 Activity 속성 모달 창이 열린 상태에서도 Open Service의 Design 갱신을 실행한다. 쓰기 불가 문맥 보호와 폐기 보호는 유지한다. Settings의 Service 폴더 선택은 IDE 자체 창을 사용하며 프로젝트 루트·상대 경로 정책을 유지한다.

## 모델 파일 미사용 설정 UI 계약

GlueModelEditor 상단에 No use .glue_uml_ad file을 표시한다. 기본 해제이며 Apply로 저장하고 Reset으로 저장된 상태를 복원한다. 이번 단계에서는 파일 기능을 변경하지 않는다.

## Service 직접 Design 편집 계약

No use .glue_uml_ad file 활성 상태에서는 -service.xml에 기존 Design·Source 탭을 제공하고 Open Service는 해당 XML을 선택한다. 기본 Glue Service namespace를 확인하고 잘못된 참조·중복 이름·잘못된 XML은 원본을 유지한 채 Source 안내를 표시한다. 해제 상태에서는 기존 모델 탐색·변환을 유지한다.

도형 배치는 XML을 읽을 때 자동 생성하며 파일·설정에 기록하지 않는다. Design 편집은 실제 service의 대응 요소를 갱신하고 위치·크기·경로만 바꾸면 원본은 수정되지 않는다. 주석·추가 속성·외부 namespace 요소는 보존한다. 직접 모드에서는 .glue_uml_ad 저장에 따른 자동 서비스 덮어쓰기를 중지한다. 기존 파일이 이미 열려 있는 상태에서 옵션을 바꾼 경우 새 Provider 구성을 적용하려면 파일을 다시 연다.

## ELK Layered 배치 계약

FR-189에 따라 서비스 직접 모드의 BFS·수동 lane 규칙은 실제 ELK Layered로 대체한다. 방향은 DOWN이고 직각 경로 및 고정 seed를 사용한다. 시작·종료는 별도 처음·마지막 계층 제약을 사용한다. ELK route의 시작·끝은 firstRelativePoint·lastRelativePoint, 굽힘점은 각 연결 노드 중심의 상대 bendpoint로 인코딩한다. .glue_uml_ad 파일의 파싱·편집·저장 경로는 유지한다.

## ELK 배포 런타임 계약

최종 플러그인 lib에는 ELK layered·core·graph·common과 필수 전이 의존성이 포함되어야 한다. 모듈 컴파일용 의존성과 별도로 루트 runtimeClasspath 연결을 검증한다. 초기화에 실패한 이전 IDE 프로세스는 수정된 플러그인으로 다시 실행한다.

## 자동 wire 선분 분리 계약

각 transition의 포트가 분리되고 ELK route와 실제 표시 경로 모두에서 선분 공유를 검사한다. 대표 그래프의 같은 직선 구간 겹침을 제거하되 한 점 교차는 별도로 허용한다. 원형 State 포트는 원 테두리에 맞추며 정책·배치 값은 실제 서비스 XML에 저장하지 않는다.

## 서비스 편집 Commit 계약

서비스 직접 편집 모드의 편집 액션은 우선 메모리 projection만 변경한다. 변경 대기 상태일 때 툴바 Commit을 활성화한다. Commit은 원본 revision이 편집 시작과 동일한지와 service mapping 정합성을 실제 XML을 수정하기 전에 검사한다. 검사 실패 시 원본 XML은 바뀌지 않고 오류를 보여주며 pending projection을 남긴다. 검사 성공 시 단일 쓰기 명령으로 적용하고 projection을 재생성해 ELK를 다시 실행한다. 일반 모델 모드는 즉시 저장한다.

## ELK 선 교차 최소화 계약

서비스 그래프의 개별 포트는 각자 North/South 방향 면을 유지하지만 고정 좌표는 지정하지 않는다. ELK Layered는 LAYER_SWEEP 교차 최소화를 적용하고 입력 포트 순서는 transition 입력 순서로 결정적으로 유지한다. ELK가 접점 위치를 조정할 수 있게 하여 가능한 경우 교차를 줄이며, 위상상 불가피한 교차까지 없애는 보장은 하지 않는다. 좌표와 경로는 계속 메모리에만 둔다.
