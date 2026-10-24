# 검증 절차

1. Gradle에서 ModelDiagramTest와 EditorMultiViewTest를 실행한다.
2. modeleditor/sample.glue_uml_ad를 열고 Design에서 노드 8개와 선 12개를 확인한다.
3. Source에서 Activity 위치와 이름을 변경하고 Design에 미저장 변경이 표시되는지 확인한다.
4. 종류별 연결, 가로·세로 겹침, 사분면, 다중 Bendpoint를 좌표 테스트로 확인한다.
5. 저장 전후 원문이 같고 weight가 유지되는지 확인한다.
6. 잘못된 XML과 끊어진 참조가 화면 예외를 일으키지 않는지 확인한다.
7. IDE 기본 배경, 회색 Wire·중앙 조건 라벨, 흰색 Activity 및 확대된 FinalState 안쪽 원을 밝은/어두운 배경에서 확인한다.
8. 어디에서든 우측 드래그하면 화면이 이동하는지 확인한다. 빈 공간 좌측 드래그는 전체 포함 Child만 선택하는 Box를 표시해야 한다.
9. Child 좌측 Down 및 Shift 추가 선택, 8방향 6×6 핸들과 마지막 대상의 4×4 표시, 다른 대상의 검정 테두리를 확인한다. 라벨 높이는 3px 줄고 글자는 1px 위로 이동하며 Child 글자는 회색이어야 한다.
10. Wire의 4px 이내 또는 조건 라벨을 클릭하면 선택되고 양 끝·꺾임점 6/4px 및 모든 선분 중점 4/2px 핸들이 표시되는지 확인한다.
11. Child 드래그 release 후 location만 갱신되고 Source·저장·Undo가 연동되는지 확인한다. 읽기 전용과 드래그 도중 Source 변경도 확인한다.

12. 마지막 선택 요소가 가장 앞에 표시되고 Navigation 좌표가 좌측 상단에 고정되는지 확인한다. Activity 좌우 중앙 핸들을 드래그해 반대 변 고정·최소 너비 16 및 size 높이 보존·Undo·저장을 확인한다.
13. Child 이동 후 음수 위치가 되어도 다른 Child의 화면 위치와 Navigation 좌표가 유지되는지 확인한다. Source 재파싱과 Undo 후에도 확인한다.
14. Activity 좌우 핸들의 resize 및 Wire 경로점·중점 핸들의 move 커서를 확인한다. Wire는 선분에서 4px 이내만 선택한다.
15. Wire 양 끝을 드래그하면 마우스 중심의 가상 State로 연결을 보정하고 저장된 상대 벡터와 시작·끝 중심으로 꺾임점이 비례 이동하는지 확인한다. 유효한 Child에 release하면 연결을 저장하고 빈 공간 또는 반대편과 같은 Child에 release하면 원복한다.

16. 여러 Child 선택 후 선택된 Child를 드래그하면 함께 이동하는지 확인한다. 너비 조절은 선택 Activity 전체에 적용하고 State는 유지해야 한다.
17. Wire 작은 핸들의 Hit가 6×6인지 확인한다. 기존 꺾임점 이동 및 중점 드래그가 원래 직선에서 5px를 초과해 삽입된 후 다른 점의 위치와 weight가 유지되고 Source·저장·Undo가 연동되는지 확인한다.
18. 재연결 전후 input/output의 해당 Wire 선언 인덱스만 이동하고 다른 참조와 Wire 순서는 유지되는지 확인한다. Source·읽기 전용 변경 도중의 드래그는 취소되어야 한다.

19. Wire 양 끝 드래그에서 마우스를 가상 State의 중심으로 취급하는지 확인한다. State↔State도 원 외곽 연결, Activity↔State는 원둘레·변·모서리 보정이며 가상 도형은 표시하지 않는다. 꺾임점의 비율 계산과 유효 재연결·잘못된 대상 원복도 확인한다.

20. 꺾임점 1~4개에서 t=(i+1)/(n+1)을 적용하고 시작 중심 변화량은 1-t, 끝 중심 변화량은 t만큼 반영되는지 확인한다. 중점 삽입의 기존 점 위치 보존과 끝점 이동의 상대 벡터 보존은 각각 검증한다.

21. 꺾임점 없는 Activity 연결에서 좌우 8px 제외 교집합이면 수직, 위아래 8px 제외 교집합이면 수평이며 가까운 변을 연결하는지 확인한다. 두 방향 모두 유효 구간이 없으면 대각선으로 보정하며 좁은 겹침에서도 범위 오류가 없어야 한다.

22. 작은 중점 핸들을 원래 직선에서 양쪽으로 5px까지 이동하면 분할되지 않고, 6px 이상 이동하면 분할되는지 확인한다. 같은 드래그 중 안쪽으로 복귀하면 병합되어야 하며 안쪽 release는 XML 및 Undo 기록을 변경하지 않아야 한다.

23. 기존 bendpoint 핸들을 제거 후보의 양옆 점 직선에서 양쪽 5px까지 옮기면 병합되고, 범위 밖으로 나오면 복원되는지 확인한다. 안쪽 release는 해당 XML 점만 삭제하고 다른 점의 위치·weight·속성을 유지하며 Undo/Redo로 복원되어야 한다.

24. 꺾임점 없는 Activity 직결에서 시작점·끝점 모두 대상 Activity 위로 드래그하면 공통 x/y가 따라오는지 확인한다. 수직·수평의 8px 구간 제한, 놓은 뒤 Source·저장·Undo/Redo 및 Navigation 후 드래그를 함께 확인한다.

## Child 관통 방지 병합 확인

1. Wire의 bendpoint를 이웃점 직선의 5px 이내로 드래그한다.
2. 마우스 위치 B와 양옆 점 A, C를 이은 A–B 또는 B–C 위에 Activity 또는 State가 있으면 꺾임점이 유지되는지 확인한다.
3. 장애물이 없거나 경계에 접하기만 하면 병합되는지 확인한다.
4. 기존점과 중점 핸들에서 반복하고 release·저장·Undo/Redo에도 같은 결과인지 확인한다.


연결 Child 검사 범위: 관통 검사 대상은 해당 Wire의 start·end만이며 다른 Child는 무시한다. 기존점·중점 모두 같은 규칙으로 동작한다.

## 작은 중점 핸들로 직결 Wire 이동 확인

1. 가로 범위가 겹치는 두 Activity의 수직 Wire를 선택하고 작은 핸들을 좌우로 드래그하여 양 끝 x가 함께 이동하는지 확인한다.
2. 각 Activity의 좌우 8px를 제외한 공통 구간 밖으로 드래그해 마우스 위치에 꺾임점이 생기고 두 선분으로 나뉘는지 확인한다.
3. 세로 범위가 겹치는 수평 Wire에서는 같은 방법으로 양 끝 y 이동 및 위아래 8px 범위 밖의 분할을 확인한다.
4. Navigation 후 드래그, Source·저장 및 한 번의 Undo/Redo를 확인한다. State와 기존 꺾임점 경로의 분할·병합도 유지되어야 한다.

## 제한 경계의 반복 전환

수평·수직 작은 핸들에서 공통 범위의 양끝 경계는 직선이고, 1px 밖에서는 마우스 위치에서 두 선분으로 나뉘어야 한다. 밖→안→밖을 반복한 뒤 범위 밖에서 release하고 Source에 bendpoint 하나가 생기는지 확인한다. Undo 한 번으로 원본, Redo 한 번으로 분할 경로가 복원되어야 한다.

## 범위 안의 관통 차단 확인

작은 중점 핸들을 공통 축 범위 안으로 유지한 채 start 또는 end Child 내부로 드래그한다. 두 선분이 유지되어야 한다. 두 Child 사이의 안전한 위치로 돌아오면 직선으로 복원되어야 한다. 수평·수직 각각 반복하고 관통 상태 release·저장·Undo/Redo 결과도 확인한다.

## Esc·복사·붙여넣기 확인

1. Child 이동·좌우 너비 조절 및 Wire 끝점·bendpoint·중점 드래그 중 Esc를 누르고 마우스를 놓는다. 미리보기가 원복되고 Source·Undo 기록이 추가되지 않아야 한다.
2. Navigation 및 범위 선택 중 Esc를 눌러 시작 화면 위치·선택으로 돌아오는지 확인한다. Source 변경으로 취소한 제스처는 Esc로 이전 Source를 복원하지 않아야 한다.
3. Child 둘을 선택하고 Ctrl+C/Ctrl+V로 복사한다. 두 Child를 잇는 Wire만 함께 붙고 다른 Child로 이어지는 참조는 없어야 한다. Wire만 선택한 복사는 비활성이다.
4. 원본 Child 속성·property·종류·크기와 Wire 조건·weight·기타 속성이 유지되고 ID는 대상 문서의 숫자 최댓값 다음부터 연속 부여되어야 한다. 참조는 새 선언 인덱스여야 한다.
5. 복사 후 Navigation·스크롤을 바꾸고 붙여넣는다. 복사 당시 화면 위치에서 첫 15px, 다음 30px씩 이동해야 하며 Navigation·스크롤 자체는 유지되어야 한다. 음수 좌표와 빈 문서도 확인한다.
6. 붙여넣은 Child가 선택되고 한 번의 Undo/Redo·저장으로 XML과 경로가 복원되는지 확인한다. 다른 열린 문서 붙여넣기와 읽기 전용·잘못된 XML·일반 텍스트 클립보드의 비활성도 확인한다.

## Delete 삭제 확인

1. Wire를 선택하고 Delete를 눌러 XML wire와 양 끝 Child의 해당 input/output 참조가 제거되는지 확인한다.
2. 첫·중간 Wire를 삭제해 뒤쪽 Wire의 ID·선언 순서는 유지되고 모든 남은 참조는 새 선언 인덱스로 바뀌는지 확인한다.
3. Child 하나 또는 여러 개를 선택하고 Delete를 눌러 연결 Wire가 모두 삭제되고 남은 Child 참조도 정리되는지 확인한다. 상대편 Child 자체는 남아야 한다.
4. 미지원 종류 Child에도 삭제 Wire 참조가 남지 않아야 한다. 관련 없는 property·조건·bendpoint·weight는 유지한다.
5. 한 번의 Undo/Redo·저장으로 삭제와 참조가 함께 복원되는지 확인한다. 화면 이동 위치와 복사본은 유지되어야 한다.
6. 선택 없음·읽기 전용·드래그 중·잘못된 XML·Source 갱신 대기 상태에서 Delete는 문서를 바꾸지 않아야 한다.

## 더블클릭 속성 편집 확인

1. Wire 선과 guardcondition 라벨을 각각 더블클릭해 현재 조건으로 창이 열리는지 확인한다. 확인·취소·동일 값 확인을 비교한다.
2. Activity를 더블클릭해 name 전용 입력, key/value 테이블과 행 추가/삭제, description textarea를 확인한다. State는 창을 열지 않아야 한다.
3. property key/value를 바꾸고 행을 추가·삭제한다. name과 description에 특수문자·여러 줄을 입력하여 확인 후 Source·저장·Undo/Redo에서도 유지되는지 확인한다. 기존 property의 추가 속성과 다른 XML 요소는 유지해야 한다.
4. 빈 key·중복 key·description을 테이블에 입력하면 안내하고 확인하지 않아야 한다. 취소는 아무 값도 바꾸지 않아야 한다.
5. 창이 열린 뒤 Source 또는 쓰기 가능 상태가 바뀌면 오래된 입력값으로 덮어쓰지 않아야 한다.

## Child 추가 툴바 확인

1. 통합 추가 버튼의 화살표 메뉴에서 Activity·시작 State·종료 State를 고르고 버튼 본체로 모드를 켠다. 마지막 종류를 기억하고 종류 변경만으로 토글을 켜지 않아야 한다.
2. 마우스 이동 시 실제 도형과 같은 ghost가 따라오고 캔버스 밖에서는 사라지는지 확인한다. 클릭 위치와 ghost 중심이 같은 Child를 생성해야 한다.
3. 연속 클릭과 종류 변경으로 배치하고 ID가 최대치+1부터 순서대로 생기는지 확인한다. 기존 Wire와 참조는 유지되어야 한다.
4. Navigation·스크롤 후 및 음수 위치에서 같은 좌표로 생성되는지 확인하고 클릭당 Undo/Redo·저장을 검증한다.
5. 토글 해제·Esc로 ghost와 추가 모드를 종료한다. 모드 중 일반 더블클릭 속성 편집·Copy/Paste/Delete가 충돌하지 않아야 한다. 읽기 전용·잘못된 XML에서는 추가하지 않아야 한다.

## 수정한 편집 창과 행 복사 확인

1. Wire 창이 Transition 편집 제목으로 열리고 조건 라벨이 없으며 이전 대비 약 절반 너비인지 확인한다.
2. Activity의 Name·Properties·Description 표시와 textarea 안쪽 여백, Add·Delete·Open Class·Open Query·Open Service의 균등 너비 및 Key 컬럼 축소를 확인한다.
3. 여러 property 행을 선택하고 Ctrl+C/Ctrl+V로 같은 창·다른 창에 붙인다. 새 행 선택·위치·key/value·줄바꿈이 보존되고 기존 XML은 확인 전 바뀌지 않아야 한다. TSV 외부 붙여넣기도 확인한다.
4. class 값을 현재 프로젝트의 클래스로 바꾸고 Open Class를 눌러 실제 에디터가 열리는지 확인한다. 빈 값·미발견 클래스는 입력을 유지하며 안내해야 한다. Open Query/Open Service는 비활성이다.

## 통합 추가 버튼과 실제 모양의 ghost 확인

1. 추가 버튼 본체로 모드를 켜고 끄며 오른쪽 화살표 또는 Alt+Down으로 3종류를 선택한다. 종류 선택만으로 꺼진 토글을 켜지 않아야 한다.
2. 종류 기억·모드 중 종류 변경·Esc·연속 배치를 확인한다. ghost는 실제 생성 도형과 같은 채움색·테두리·이름이어야 하며 선택 핸들은 없어야 한다.
3. Activity 패널 배치 검증 이미지는 build/reports/model-activity-properties.png에서 확인한다.

## 표준 툴바 및 접점 보완 확인

1. Child 추가가 IntelliJ 표준 분할 버튼 모양으로 표시되는지 확인한다. 본체 토글·종류 메뉴·체크·아이콘·툴팁과 마지막 종류 기억, Esc 및 읽기 전용 비활성을 확인한다.
2. Activity 왼쪽/오른쪽에 State 또는 마우스 끝점을 놓고 y를 옮긴다. 내부 y는 따라 움직이고 위아래 8px 여백에서 제한되어야 한다. 위·아래 수직 연결과 대각선 보정은 유지한다.
3. InitialState·FinalState의 양 끝 핸들을 원 안과 정확한 원 경계로 드래그한다. 접점은 중심이 아니라 반경 10 원둘레를 따라 움직여야 한다. 중심 클릭도 외곽을 유지해야 한다.
4. 접점 편집의 Source·저장·Undo/Redo와 State 자체 이동 후 상대적인 둘레 위치 보존을 확인한다. State↔State와 꺾임점 경로도 원 외곽에서 연결되어야 한다.

## 수직 접점과 미니맵 확인

1. Activity 위/아래에 있는 State·마우스·bendpoint의 x를 좌우 가장자리까지 옮긴다. 접점은 양쪽 8px 안쪽에 머물러야 하고 반대 방향 연결도 같아야 한다. 너비가 작은 기존 도형도 오류 없이 중앙에 연결되어야 한다.
2. 우측 상단 미니맵에 전체 Child·Wire·조건 라벨과 현재 화면 영역이 보이는지 확인한다. 이동·너비 조절·Wire 편집·추가/삭제·Source·Undo/Redo를 반영해야 한다.
3. Navigation·스크롤 후에도 미니맵은 화면 우측 상단에 고정되고 뷰포트 표시만 이동해야 한다. 음수 위치도 전체 범위에 포함되어야 한다.
4. 미니맵 왼쪽 클릭·더블클릭이 뒤의 Child 선택·창 열기·추가로 전달되지 않아야 한다. 오른쪽 Navigation과 이미 진행 중인 드래그는 유지한다.
5. 빈 문서·잘못된 문서·작은 화면에서는 숨기며 표시만으로 Source를 변경하지 않아야 한다. build/reports/model-minimap.png에서 배치를 확인한다.
6. 정렬 보조선/스냅은 이번에 적용하지 않았다. 구현 가능성과 후속 선택은 alignment-review.md를 참고한다.

## 실시간 스냅과 클래스 자동완성 확인

1. Child를 다른 Child의 변 또는 중심에서 5px 이내로 이동한다. 드래그 중 붙으면서 점선이 나타나고 멀어지면 자유 이동한다. Alt를 누르거나 놓으면 같은 마우스 위치에서 우회/복귀한다.
2. 다중 선택을 이동하여 간격을 확인한다. 여러 Activity의 좌우 너비 핸들을 조절하여 공통 변화량·고정 반대 변·최소 16px·같은 너비 스냅을 확인한다.
3. Esc·Source 변경·읽기 전용 전환 후 가이드와 미리보기가 제거되는지 확인하고, 놓은 스냅 결과를 한 번의 Undo/Redo로 되돌린다.
4. 미니맵 제목이 없고 도형/파란 뷰포트가 기본 맞춤 배율로 표시되는지 확인한다. 후속 요청으로 30% 추가 축소는 취소했다. 보조선은 미니맵에 그리지 않는다.
5. Activity 속성에서 class 행의 Value를 편집한다. 짧은 클래스명 입력 후 표준 자동완성(Ctrl+Space)으로 후보를 선택하면 전체 클래스명이 들어가는지 확인한다. 다른 행과 Key는 일반 입력이다.
6. Open Class가 현재 셀 값을 사용하고 확인 후 저장되며 취소 시 XML이 유지되는지 확인한다. 인덱싱 중에는 직접 전체 클래스명을 입력한다.

자동 검증은 모델 관련 테스트와 EditorMultiViewTest를 실행한다. 캔버스 렌더링 이미지는 build/reports/model-alignment-guides.png와 model-minimap.png다. 실제 실행 IDE에서의 마우스·테마·팝업 확인은 플러그인을 다시 로드한 후 수행한다.

## 균등 간격 및 sql-key 수동 확인

1. 같은 줄의 두 Child 사이로 세 번째 Child를 이동한다. 외곽 사이 두 간격이 같아지는 위치에서 붙고 같은 px 값의 치수선 두 개가 표시되는지 확인한다. 세로 열에서도 반복한다.
2. 두 기준 Child 바깥의 앞·뒤에 배치하여 기존 외곽 간격이 반복되는지 확인한다. 다른 크기·State·다중 선택에서도 확인하고 Alt·Esc·Navigation·Undo/Redo를 반복한다.
3. QueryEditor 파일이 있는 프로젝트를 열고 최초 메모리 수집을 기다린다. Activity 속성의 sql-key Value에서 ID 접두사 또는 Ctrl+Space로 자동완성한다. 파일·설명이 표시되고 선택하면 ID만 들어가는지 확인한다.
4. Query 파일을 열지 않아도 후보가 검색되는지 확인한다. Source에서 ID/desc를 바꾸되 저장하지 않은 상태, Design의 ID 변경, Undo 후 약 150ms 병합 갱신이 끝나면 최신 후보가 나타나는지 확인한다.
5. 파일 추가·외부 수정·삭제·이름/디렉터리 이동과 제외 폴더 변경에 따라 후보가 갱신되는지 확인한다. 잘못된 XML은 후보가 사라지고 복구하면 다시 나타난다.
6. class 행은 기존 Java 자동완성이고 일반 행은 일반 입력인지 확인한다. sql-key 후보를 고른 뒤 취소하면 Activity XML은 유지되어야 한다. 프로젝트 재실행 시 목록을 다시 수집한다.

자동 검증은 ModelDiagram*Test, Query*Test, EditorMultiViewTest를 실행한다. 균등 간격 렌더링 이미지는 build/reports/model-equal-spacing.png다. 실제 IDE의 팝업/테마 확인은 플러그인을 다시 로드하여 수행한다.

## 미니맵 현재 화면과 Navigation 자동 축소 확인

1. 작은 다이어그램을 넓은 화면과 세로로 긴 화면에서 연다. 파란 사각형이 실제 보이는 캔버스 비율을 유지하고 네 변이 모두 미니맵 안에 있는지 확인한다.
2. 에디터 크기 변경과 스크롤 후 사각형 크기·위치가 현재 화면과 함께 갱신되고 미니맵은 우측 상단에 고정되는지 확인한다.
3. 오른쪽 Navigation으로 상하좌우 멀리 이동한다. 도형과 사각형을 함께 보여주도록 배율이 줄어들고 사각형이 잘리거나 사라지지 않아야 한다.
4. 원래 위치에서 배율 복구와 음수 좌표를 확인한다. 표시만으로 XML이나 선택이 바뀌지 않아야 한다.

자동 검증은 ModelDiagramMiniMapTest 11개와 모델 편집 회귀로 수행한다. build/reports/model-minimap-navigation.png에서 Navigation 후 렌더링을 확인했다. 실제 IDE 창의 수동 조작 검증은 별도다.

## 탐색과 모델 설정 수동 확인

1. Query 파일 두 개에 같은 ID와 서로 다른 ID를 준비한다. Activity sql-key 편집 셀에서 Open Query를 눌러 해당 Query가 선택되는지, 중복 ID는 파일 chooser가 나타나는지 확인한다. Find in Files에서 ID·SQL 위치를 열 때 Design에서도 해당 Query가 선택되는지 확인한다.
2. GlueDiagram 직접 자식 property를 추가하고 Source에서 수정한다. 좌측 좌표 아래 key: value 표시가 갱신되고 Navigation 중 화면에 고정되는지 확인한다. Child property가 섞이지 않아야 한다.
3. Settings > GlueFramework > GlueModelEditor에서 Service 저장 폴더를 선택한다. Activity 프리셋을 Add/Delete하고 Name·Properties·Description·Color를 수정한다. 행 Copy/Paste와 Apply/Reset·재시작 후 보존을 확인한다. 프리셋 편집에는 Open 버튼이 없어야 한다.
4. 폴더에 alpha.xml과 alpha.glue_uml_ad를 준비한다. service-name 입력에서 alpha를 완성하고 Open Service가 UML 파일을 여는지 확인한다. 취소하면 Activity 원본 XML은 유지되어야 한다.
5. 툴바 자석 토글을 켜고 Wire 끝점을 Activity에 붙인다. 중심과 중심에서 좌우 30px 간격, 세로 가운데에 맞추되 8px 제한을 넘지 않는지 확인한다. off와 State 원 외곽, 저장·Undo/Redo도 확인한다.
6. 멀리 떨어진 Child가 있는 모델에서도 스크롤바가 없고 휠/정밀 휠은 세로, Ctrl+휠은 가로 Navigation인지 확인한다. 우클릭 이동과 좌표 표시를 확인한다.
7. 빈 공간에서 범위 선택을 드래그하여 반투명 하늘색 내부와 기존 포함 선택·Shift·Esc를 확인한다.
8. 미니맵에서 요소 위치를 클릭하면 화면 가운데로 이동하며 뒤의 선택·생성·속성 창은 실행되지 않아야 한다. 초기 배율은 화면 밖 요소 범위와 여백에 맞춰지고 파란 사각형 전체가 표시되어야 한다.

자동 검증은 Query*Test, ModelEditor*Test, ModelDiagram*Test, EditorMultiViewTest를 실행한다. model-settings.png와 model-navigation-features.png를 build/reports에서 확인했다. 실제 IDE의 Find in Files 검색 창·중복 chooser·컬러 팝업 조작은 별도 수동 확인 대상이다.

## 루트 속성·서비스·연속 드래그 후속 확인

1. 루트 property에 title과 description을 넣는다. HUD는 title=value만 표시해야 하며 툴바 Diagram 속성에서 Properties/Description을 별도로 수정한다. Name/Open 버튼은 없어야 한다. 취소·확인·Undo/Redo, Child/Wire 보존을 확인한다.
2. 자석 on에서 Wire의 4px 중간 핸들과 기존 꺾임점을 드래그한다. Activity 투영 범위 안에서 x는 중심±30n, y는 가운데로 맞고 밖에서는 자유롭게 움직여야 한다. off·Esc·관통 병합 금지도 확인한다.
3. 프로젝트 내부 services 폴더에 order-service.xml과 order.glue_uml_ad를 만든다. Settings에서 services를 선택하고 상대 경로로 저장되는지 확인한다. 프로젝트 밖 폴더나 ../ 경로는 적용할 수 없어야 한다.
4. service-name에 타이핑하여 자동 팝업을 확인하고 Ctrl+Space로도 order-service를 선택한다. 기존 order-service.xml 값에서도 후보를 선택할 수 있어야 한다. Open Service는 order.glue_uml_ad를 연다.
5. Settings 창을 줄여 목록/상세/색상/Description이 맞춰지고 외곽 가로·세로 스크롤바가 없는지 확인한다. 많은 행의 표/목록 내부 스크롤은 유지한다. build/reports/model-settings.png는 680×440 렌더링이다.
6. 미니맵 내부에서 누르고 계속 드래그해 이동한다. 끝 위치와 내부 경계 제한을 확인하고 Esc는 이전 Navigation으로 복원되는지 확인한다. 선택/생성/속성 창이나 XML 변경이 발생하면 안 된다.

자동 검증은 관련 모델 및 Query 테스트를 실행한다. 자동 팝업은 표준 컴포넌트 구성과 실제 후보 선택으로 검증하며 실제 IDE의 팝업 조작은 별도 수동 확인 대상이다.
- 확장 확인: 설정 프리셋을 Activity 추가 메뉴에서 선택하고 본체를 켜 이름·속성·Description·색상을 확인한다. Wire 추가는 시작 Child → 끝 Child 클릭으로 생성하며 Esc 취소·Undo/Redo를 확인한다. 임의 프로젝트 폴더의 sample2.glue_uml_ad를 service-name=sample2-service로 연다. 일반/다크 테마 자석 아이콘 색상을 비교한다. 모델·Query 테스트 206개가 통과했고 build/reports/model-preset-colors.png에서 Activity·미니맵 색상을 확인했다.
## 생성과 표시 개선 검증

1. Service의 Source를 선택해 둔 후 Open Service를 누르면 Design이 선택되는지 확인한다.
2. Activity 종류와 프리셋 메뉴를 선택하면 바로 생성 모드가 켜지고 같은 항목을 다시 골라도 켜지는지 확인한다.
3. Settings의 Background·Foreground를 바꿔 Apply하고 name 글자색·ghost·미니맵을 확인한다. Reset과 이전 설정의 기본 #919191도 확인한다.
4. 자석 on/off에서 빈 화면 Wire 미리보기가 직선이고 실제 Wire와 굵기·색·화살표가 같으며 목표 Child 위 미리보기와 생성 경로가 같은지 확인한다.

생성과 표시 개선 결과: 모델·Query 회귀 209개가 실패·오류·건너뜀 없이 통과했다. 이전 설정의 Foreground 기본값과 직렬화 왕복, Apply/Reset, name 글자색 렌더링, 메뉴 자동 활성화, Wire 미리보기/실제 선의 픽셀 및 목표 경로 일치를 검증했다. Open Service의 Design 선택은 실제 에디터와 기록용 관리자로 확인했으며 GUI 탭 전환은 위 수동 절차를 따른다.

## 텍스트 툴바·확대 및 서비스 저장 검증

1. 빈 Activity·InitialState·FinalState 또는 프리셋을 선택한다. 메뉴가 닫히고 본체에 선택 텍스트가 표시되며 생성 모드가 켜지는지 확인한다. Wire 버튼은 아이콘 대신 Wire로 표시한다.
2. 한 묶음인 [-][100%][+]로 25~200%를 조절한다. 가운데 버튼은 100%로 복원한다. 확대 후 이동·resize·범위 선택·Wire·ghost·Navigation·미니맵·붙여넣기를 확인한다. 확대만으로 XML은 바뀌지 않아야 한다.
3. 프로젝트 내부 Service 저장 경로를 지정하고 제공한 sample11을 수정·저장한다. sample11-service.xml의 service name·initial·transaction-manager·Activity·transition·property를 예시와 의미상 비교한다. 생성 순서는 원본 순서를 따른다.
4. transaction 값 false도 출력되는지, description은 모두 제외되는지, 특수문자·한글·줄바꿈이 보존되는지 확인한다. 열린 출력 에디터도 갱신되어야 한다. 잘못된 XML 저장은 기존 출력 파일을 보존하고 오류 알림을 표시한다. 경로 미설정은 생성을 건너뛴다.

검증 결과: 관련 모델·Query 회귀 219개가 통과했다. 이후 추가한 툴바 컴포넌트 렌더링 검사도 통과했으며 build/reports/model-text-toolbar.png에서 선택 텍스트 표시를 확인했다. 저장 이벤트로 Service 파일 생성·재저장·열린 출력 문서 동기화·실패 시 이전 출력 보존을 확인했다. 팝업은 표준 메뉴의 선택 후 닫기 정책을 검사했으며 실제 IDE에서 클릭해 닫히는 동작은 위 수동 절차를 따른다.

## 쓰기 불가 문맥의 Design 갱신 검증

1. Source에서 문서를 수정하고 Design으로 전환한다. 최신 문서가 표시되며 Write-unsafe context 로그가 없어야 한다.
2. Open Service로 새 파일을 열거나 이미 열린 Source를 Design으로 전환해 같은 결과를 확인한다.
3. 자동 재현은 ModelEditorRefreshTest에서 TransactionGuard.performActivity(false) 문맥으로 에디터 생성·selectNotify를 호출한다. 원래 구현은 세 테스트에서 해당 오류를 재현했으며 수정 후 최신 문서·중복 예약·폐기 보호 검사 세 개가 통과했다.

안전한 갱신 회귀 결과: 재현 테스트 세 개를 포함한 모델·Query·다중 뷰 테스트 222개가 실패·오류·건너뜀 없이 통과했다. 쓰기 불가 문맥의 생성·선택, 최신 변경 반영, 폐기 보호와 기존 편집 기능을 검증했다.

## 갱신 예약과 생성 클릭 검증

1. Design 탭을 선택하고 곧바로 Activity를 추가한다. 갱신이 예약 중이어도 한 번 생성되어야 한다.
2. 생성 모드를 유지하고 서로 다른 위치를 연속 클릭한다. 각 클릭에 서로 다른 ID가 생기는지 확인한다. 기존 더블클릭 제외 정책은 유지한다.
3. Source의 이름·ID를 수정하고 갱신 전에 생성 요청을 실행한다. 수정 내용과 새 Child가 모두 남으며 새 ID는 최신 최대값 다음이어야 한다.
4. 잘못된 Source에는 추가하지 않고 쓰기 불가 문맥에서는 비모달로 실행한다. 대기 중 모드를 취소하면 생성하지 않아야 한다. ModelEditorRefreshTest의 추가 다섯 검사와 기존 세 검사가 통과했다.

## 배율 Select 최신 검증

기존 확대 버튼 묶음은 단일 Select로 대체되었다. 목록은 20%, 30%, …, 150%이며 기본값은 100%이다. 20%·150%·100%를 선택하여 화면 중앙 유지와 원본 XML 무변경을 확인한다. 선택 배율에서도 이동·resize·생성·Wire·미니맵·붙여넣기를 검증한다.

생성 요청 보존·배율 Select 통합 검증 결과: 모델·Query·다중 뷰 테스트 227개가 실패·오류·건너뜀 없이 통과했다. 갱신 예약 중 생성, 최신 Source의 ID와 이름 보존, 쓰기 불가 생성/취소, 배율 14개 항목과 경계·복원, resize·미니맵·붙여넣기를 확인했다.

## 생성 요청 큐 검증

종류·프리셋 선택 후 생성 콜백에 도착한 요청은 FIFO 큐에서 비모달 실행한다. 연속 두 요청의 ID·위치 순서, 요청 프리셋 스냅샷, Source 최신 내용, 모드 취소 후 같은 종류 재활성화, 에디터 폐기와 기존 더블클릭 방지를 확인한다. 클릭 횟수 예외는 적용하지 않았다. 실제 팝업 직후 이벤트가 Canvas까지 전달되는지는 GUI 확인 항목으로 남기며 합성 이벤트 검증을 실제 보고 사례의 재현으로 표현하지 않는다.

요청 큐 통합 결과: 모델·Query·다중 뷰 테스트 228개가 실패·오류·건너뜀 없이 통과했다. 생성 요청 큐의 동작을 검증한 결과이며 실제 팝업 직후 클릭 지연의 원인을 확정한 결과는 아니다.

## 생성 클릭 진단 로그 수집

1. 변경된 플러그인으로 IDE를 다시 실행하고 Activity 종류 또는 프리셋을 바꾼 직후 클릭 누락을 재현한다. 잠시 기다렸다 정상 생성되는 클릭도 한 번 실행한다.
2. Help 메뉴의 Show Log in Explorer로 로그 폴더를 열고 idea.log에서 `[ModelChildCreation]`을 검색한다. 별도 Debug 설정은 필요 없다.
3. 같은 session의 POPUP_SHOW/POPUP_CLOSED·MODE_SET 이후 RAW_PRESS/RAW_RELEASE/RAW_CLICK와 CANVAS_PRESS/CANVAS_RELEASE/CANVAS_CLICK를 비교한다. 같은 input 식별자가 RAW에는 있고 Canvas에는 없으면 그 사이 IDE 전달 단계를 확인한다. press/release만 있고 click이 없으면 clicked 이벤트 생성/전달 문제로 구분한다.
4. IGNORE의 reason=click_count·minimap·read_only·invalid_diagram·no_ghost·no_handler 또는 ROUTE의 child_mode_off를 확인한다. SUBMIT 뒤에는 같은 input의 QUEUE_ACCEPT와 request 식별자가 이어져야 한다.
5. request별 CREATE_BEGIN·CREATE_DONE 또는 QUEUE_CANCEL/CREATE_CANCEL/CREATE_ERROR를 확인한다. mode_revision은 대기 중 모드 세대 변경이고 disposed는 에디터 종료이다. refreshPending·queue·stamp·docWritable·writeSafe·modality로 당시 상태를 확인한다.
6. 문제 발생 직전 메뉴 선택부터 정상 생성까지의 `[ModelChildCreation]` 줄을 공유하면 누락 단계를 판별할 수 있다. 속성 값·Description·XML 본문과 마우스 move는 기록하지 않는다.

진단 기록 검증 결과: 로그 거부 이유·입력/요청 식별자·큐 완료/취소·관찰자 폐기 해제 검사를 포함한 모델·Query·다중 뷰 테스트 232개가 실패·오류·건너뜀 없이 통과했다. 실제 idea.log의 INFO 기록도 확인했다. 원인 확정은 사용자 환경에서 문제 발생 시점의 기록을 수집한 뒤 진행한다.


## Wire 조건과 다중 선택 이동 검증 (2026-10-24)

조건 없는 기존 Wire는 success를 사용하고 InitialState 출발 조건 라벨은 표시하지 않는다. 새 Wire의 Source에는 success가 저장되어야 한다. 양 끝을 함께 이동한 뒤 경로 모양·기준점 이동·Undo 복원을 확인한다.

모델 에디터 전체 소스와 ModelDiagramTest는 로컬 IDEA 2025.3.6.1 SDK 및 Kotlin 2.3.20으로 직접 컴파일했다. 직선·bendpoint 경로 평행이동, 상대 bendpoint 보존, 한쪽 이동·resize 제외 회귀 메서드는 직접 실행하여 통과했다. git diff --check도 통과했다. Gradle 테스트는 캐시 쓰기 권한 및 오프라인 IDEA 의존성 해석 문제로 실행하지 못했다. 직접 JUnit 실행도 Java 플러그인 초기화 오류로 중단되어 전체 통합 테스트 통과는 확인하지 못했다. 정상 Gradle 환경에서 `./gradlew.bat test --tests com.look4counter.ModelDiagramTest`를 재실행한다.

## InitialState 편집 차단과 정수 좌표 검증 (2026-10-24)

InitialState 출발 Wire를 더블클릭하여 조건 편집 창이 열리지 않는지 확인한다. Activity 출발 Wire의 조건 편집은 유지한다. 소수 좌표가 생길 수 있는 배율에서 생성·이동·너비 변경·붙여넣기·끝점·bendpoint 편집을 수행하고 Source의 좌표 속성이 부호 있는 정수 쌍인지 확인한다. size=-1과 weight=0.5는 유지되며 Undo로 이전 값을 복원한다. 기존 파일을 열기만 해서는 변경하지 않는다.

모델 에디터 전체 소스와 ModelDiagramTest를 IDEA 2025.3.6.1 SDK·Kotlin 2.3.20으로 직접 컴파일하여 통과했다. 정수 직렬화(음수·0·반올림·size=-1), InitialState 조건 편집 정책, 기존 Wire 동반 이동 회귀 메서드를 직접 실행하여 통과했다. XML PSI 정규화에서 끝점·bendpoint·location·size 변환과 weight·조건 보존을 확인하는 테스트도 추가했다. 전체 JUnit 실행은 IDEA 서비스 초기화 오류(MessageBusFactory 미등록)로 중단되어 통합 테스트 통과를 확인하지 못했다. 정상 환경에서 `./gradlew.bat test --tests com.look4counter.ModelDiagramTest`로 재검증한다. `git diff --check`는 통과했다.

## 서비스 출력 순서 호환 검증 (2026-10-24)

Router의 success 중복은 마지막 SeachEmp-SQL2 대상 하나로 합쳐진다. transition 이름 순서는 success·find·custom·insert·delete·update2이다. property는 class·description을 포함한 원본 속성 개수로 버킷을 계산하여 사용자 예제의 모든 Activity 순서를 재현한다. Activity 순서와 기존 XML 직렬화·검증은 유지한다.

자동 검증: `./gradlew.bat test --tests com.look4counter.ModelServiceOutputOrderTest --tests com.look4counter.ModelEditorServiceExportTest`.

로컬 IDEA 2025.3.6.1 SDK와 Kotlin 2.3.20으로 모델 에디터 전체 소스·관련 테스트를 직접 컴파일하여 통과했다. ModelServiceOutputOrderTest의 3개 테스트(6개 Activity property 순서·값 보존·동일 버킷 순서·Router transition 순서·마지막 기본 success 우선·빈 입력)가 JUnit으로 통과했다. ModelEditorServiceExportTest에는 예제와 같은 연결 및 속성 값으로 전체 서비스의 순서·값·namespace·transaction-manager·반복 결과를 검사하는 통합 회귀를 추가했다. 통합 테스트 실행은 기존 GlueUmlAdFileType과 GlueSqlFileType의 displayName이 모두 XML인 오류로 IDE 초기화 중 중단되어 성공을 확인하지 못했다. `git diff --check`는 통과했다.

## 서비스 속성 순서와 빈 태그 검증 (2026-10-24)

service 속성은 name·initial·xmlns, transaction-manager는 id·commit, activity는 name·class, transition·property는 name·value 순서이다. 빈 요소는 공백 없는 />로 닫힌다. 속성 값을 정규식으로 치환하지 않고 JDOM으로 이스케이프하여 내부 공백을 보존한다.

검증 명령: `./gradlew.bat test --tests com.look4counter.ModelServiceOutputOrderTest --tests com.look4counter.ModelServiceXmlWriterTest`.

로컬 IDEA 2025.3.6.1 SDK와 Kotlin 2.3.20으로 모델 에디터 전체 소스·관련 테스트 컴파일이 통과했다. JUnit 직접 실행에서 기존 순서 회귀 3개와 직렬화 회귀 2개, 총 5개가 통과했다. 직렬화 회귀는 모든 태그의 역순 속성 추가, 정확한 전체 문자열 비교, 빈 activity 포함 빈 태그 형식, namespace, 반복 출력 일치, 특수문자·CR·LF·탭·값 내부 닫기 문자열·끝 공백의 재파싱 보존을 검사한다. git diff --check도 통과했다. 이번 검증은 IDE 초기화가 필요 없는 직렬화와 정렬 회귀이며 전체 IDE 통합 테스트는 이전에 확인된 파일 타입 표시 이름 충돌 때문에 재실행하지 않았다.

## 서비스 XML 후행 개행 제거 검증 (2026-10-24)

출력 마지막 LF 하나를 제거했다. 전체 소스와 관련 테스트 직접 컴파일 및 JUnit 회귀 5개가 통과했다. 전체 기대 문자열은 마지막 LF 없이 일치하며 기존 순서·특수문자·값 공백 보존 회귀도 통과했다. git diff --check도 통과했다.

## 모달 Service 열기와 IDE 폴더 선택 검증 (2026-10-24)

Activity 속성 창을 열고 service-name의 Open Service를 누른다. 속성 창을 닫기 전에 대상 Design 내용이 보이는지 확인한다. 이미 열린 대상에서 Source를 변경한 뒤 다시 Open Service를 실행해 최신 내용이 나타나는지도 확인한다. Settings의 Service 저장 경로 탐색 버튼은 IntelliJ 자체 폴더 선택 창을 열어야 하며 프로젝트 내부 폴더 선택·상대 경로 저장·취소 시 값 보존이 유지되어야 한다.

requestRefresh는 쓰기 안전한 현재 모달 상태를 캡처한다. 예약 상태가 바뀌면 세대를 교체하고 이전 콜백은 무시한다. 쓰기 안전하지 않은 모달 상태는 비모달 실행으로 대기한다. 기존 문서 커밋 안전성·생명주기·원점 보존 병합은 유지한다. 폴더 descriptor에는 isForcedToUseIdeaFileChooser=true를 설정했다.

모델 에디터 전체 소스와 관련 테스트를 로컬 IDEA 2025.3.6.1 SDK·Kotlin 2.3.20으로 컴파일하여 통과했다. ModelEditorRefreshTest 12개가 JUnit에서 통과했다. 신규 회귀 3개는 안전한 모달 안의 신규 편집기 표시, 기존 비모달 예약 중 Open Service의 최신 화면 반영, 쓰기 불가 모달 종료까지 지연을 검증한다. 기존 파일 타입 표시 이름 충돌을 피하려고 무시되는 검증 디렉터리에 테스트 전용 플러그인 리소스를 복사하고 사용하지 않는 Query 파일 타입 등록만 제외했다. 제품 소스나 플러그인 등록은 바꾸지 않았다. 실행 로그에는 기존 툴바 비동기 presentation 초기화 오류와 JNA 네이티브 라이브러리 로딩 오류가 별도로 있어 전체 플러그인 실행의 무오류 검증으로 간주하지 않는다. git diff --check는 통과했다.

## 모델 파일 미사용 체크박스 검증 (2026-10-24)

Settings > GlueFramework > GlueModelEditor 상단의 No use .glue_uml_ad file은 기본 해제이며 Apply로 프로젝트 설정에 저장한다. 미적용 변경은 Reset으로 복원되고 화면을 다시 열어도 저장된 값을 유지한다. 이번 단계에서는 체크 여부에 따른 모델 파일 동작은 연결하지 않는다.

전체 모델 에디터 소스와 관련 테스트 직접 컴파일이 통과했다. 격리된 IDEA 테스트 설정에서 체크박스 Apply·Reset·화면 재생성·설정 XML 왕복·기존 상태 기본값 회귀와 기존 프리셋 호환·설정 회귀 총 3개가 통과했다. 이전과 동일하게 사용하지 않는 Query 파일 타입 등록을 제외한 테스트 리소스를 사용했다. 실행 로그에는 환경의 인증서·네이티브 초기화 관련 오류가 별도로 있어 전체 IDE 실행 검증과 구분한다. 테스트 완료 메시지 확인 후 IDE 백그라운드 스레드가 남은 직접 실행 프로세스는 종료했다. git diff --check는 통과했다.

## Service XML 직접 Design 편집 검증 (2026-10-24)

1. Settings > GlueFramework > GlueModelEditor에서 No use .glue_uml_ad file을 체크하고 적용한다. 이미 기본 XML 편집기로 연 서비스 파일은 닫았다가 다시 연다.
2. 프로젝트의 -service.xml을 열어 기존 Design·Source 탭이 나타나는지 확인한다. Open Service도 해당 XML의 Design을 선택한다. 옵션 해제 상태에서는 기존 .glue_uml_ad 탐색을 유지한다.
3. 서비스 initial 기준으로 연결 단계를 위에서 아래로 배치하고 같은 단계는 XML 순서로 가로 배치한다. 순환·미연결 Activity도 표시한다. 긴 연결·역방향·같은 대상 다중 연결은 바깥쪽 경로를 사용한다.
4. Activity 이름·조건·연결·property·transaction-manager를 Design에서 편집하여 Source에 반영되는지 확인한다. Undo·Redo와 Source 수정 후 Design 갱신도 확인한다.
5. 위치·크기·wire 경로만 바꿔서는 서비스 XML이 변경되지 않아야 한다. 배치 파일이나 설정도 생성하지 않는다. 닫았다 다시 열거나 Source를 변경해 다시 읽으면 규칙으로 배치한다.
6. 주석·추가 속성·외부 namespace 요소는 편집 후에도 유지되어야 한다. 이름이 겹치는 새 Activity·붙여넣기에는 숫자 접미사를 붙인다. 잘못된 XML·존재하지 않는 대상·중복 이름은 Source 안내로 표시하고 읽기 전용 문서는 편집하지 않는다.
7. 직접 편집 모드에서는 기존 모델 저장의 자동 서비스 내보내기가 중지되어 실제 서비스 XML을 덮어쓰지 않아야 한다.

검증 명령: `./gradlew.bat test --tests com.look4counter.ModelServiceDirectEditorTest --tests com.look4counter.ServiceDiagramLayoutTest --tests com.look4counter.ModelEditorRefreshTest --tests com.look4counter.ModelEditorServiceExportTest --tests com.look4counter.ModelServiceOutputOrderTest --tests com.look4counter.ModelServiceXmlWriterTest`.

검증 결과: 로컬 IDEA 2025.3.6.1 SDK·Kotlin 2.3.20으로 모델 에디터 전체 소스와 관련 테스트를 컴파일했다. Service 직접 편집 9개, 자동 배치 2개, 기존 갱신 12개, 서비스 내보내기 6개, 순서 3개, 직렬화 2개로 총 34개가 통과했다. 직접 편집 회귀는 geometry만 편집할 때 문서 무변경, 이름·property·조건·transaction 갱신, 메타데이터·주석·타 namespace 보존, Source 오류 복구, 기존 UI 동작을 통한 편집·연결 대상 변경·Undo·Redo, 생성·복사·붙여넣기·삭제의 이름 고유성, 읽기 전용과 잘못된 참조 보호, 옵션별 Provider·Open Service 분기 및 자동 내보내기 중지를 검사한다. git diff --check도 통과했다.

테스트는 앞서 사용한 격리 플러그인 리소스로 실행했다(사용하지 않는 Query 파일 타입 등록 제외). 테스트 로그에는 기존 툴바 비동기 presentation 오류와 네이티브 초기화 오류가 별도로 있어 실제 IDE 전체 실행의 무오류 검증과 구분한다. 최종 수렴 검토에서 FR-181~FR-188과 구현·테스트의 대응을 확인했으며 이 범위의 추가 구현 작업은 없었다.

## ELK Layered 전환 검증 현황 (2026-10-24)

ServiceDiagramLayout은 고정 버전 ELK 0.12.0의 org.eclipse.elk.layered를 실제 호출하도록 변경했다. 아래 방향·직각 경로·고정 seed·중앙 고정 포트·시작/종료 계층 제약을 사용한다. ServiceDiagramAdapter는 반환 route를 메모리 firstRelativePoint·lastRelativePoint·상대 bendpoint로 반영한다. 이전 BFS 및 수동 lane 계산은 사용하지 않는다. 실제 서비스 XML과 프로젝트 설정에는 배치를 기록하지 않는다.

검증 명령: `./gradlew.bat test --tests com.look4counter.ServiceDiagramLayoutTest --tests com.look4counter.ModelServiceDirectEditorTest --tests com.look4counter.ModelEditorRefreshTest --tests com.look4counter.ModelEditorServiceExportTest`.

현재 이 변경의 컴파일·실행 검증은 완료하지 못했다. HTTP_PROXY·HTTPS_PROXY·ALL_PROXY가 127.0.0.1:9로 설정된 실행 환경에서 프록시 연결 실패로 Maven 다운로드가 불가능하며 로컬 ELK 캐시도 없다. 오프라인 Gradle도 기존 foojay 플러그인 캐시 해석 단계에서 실패했다. 별도 출력 디렉터리로 직접 컴파일을 시도했으나 실제 ELK 클래스가 없어 unresolved reference 오류로 실패했다. 이전 BFS 버전의 테스트 34개 통과 결과는 ELK 전환판의 검증 결과로 사용하지 않았다.

노드 비중첩·직각 경로·반복 결과·DAG 방향·순환·자기 연결·다중 연결·빈 그래프 및 Canvas 경로 검증을 갱신했다. 의존성 확보 후 실제 실행이 필요한 T218은 미완료로 유지한다. 정적 문서·구현 대조와 git diff --check는 통과했으나 실제 ELK의 API 호환·실행 결과는 아직 확인하지 못했다. IntelliJ에서 Maven 접근이 가능한 환경으로 Gradle 동기화한 뒤 위 검증을 실행해야 한다.

## ELK 런타임 패키징 수정 및 실제 검증 (2026-10-24)

ELK JAR가 다운로드된 뒤에도 Run IDE의 lib에는 프로젝트 합성 JAR만 있었고 LayoutMetaDataService를 찾지 못했다. 루트 모듈 합성 선언을 pluginComposedModule(implementation(project(...)))로 바꾸어 ELK 전이 의존성을 루트 runtimeClasspath와 배포 lib에 연결했다. 수정 후 기존 실패한 IDE 프로세스를 완전히 종료하고 Run IDE with Plugin을 다시 실행한다. 파일 재열기만으로는 JVM의 클래스 초기화 실패 상태가 복구되지 않는다.

올바른 공유 읽기 전용 캐시 경로와 로컬 JDK 21을 지정하여 실제 Gradle에서 설정 로딩·메인/테스트 Kotlin 컴파일·prepareSandbox·prepareSandbox_runIde·buildPlugin이 성공했다. 설정 검색 인덱스 생성 작업은 기존 IDE 디렉터리 잠금 때문에 실패하여 ZIP 패키징 검증에서는 buildSearchableOptions만 제외했다. 자동 배치 4개·순서 3개·직렬화 2개, 총 9개 테스트가 실제 Gradle에서 통과했다.

새 배포 ZIP과 Run IDE 샌드박스 lib에서 org.eclipse.elk.alg.layered·core·graph·alg.common 0.12.0, org.eclipse.emf.common·ecore·ecore.xmi 2.12.0 및 Guava 등 전이 의존성을 확인했다. 배포 ZIP의 실제 JAR를 사용해 Service 직접 편집·ELK 배치·기존 갱신·내보내기·순서·직렬화 격리 회귀 36개가 통과했다. 격리 리소스는 이전과 같이 사용하지 않는 Query 파일 타입 등록만 제외하며 제품 등록은 바꾸지 않았다. 기존 툴바·네이티브 진단 로그는 전체 IDE 수동 실행의 무오류 검증과 구분한다. git diff --check는 통과했다.

이 결과로 앞선 ELK 의존성 미확보와 컴파일·실행 미검증 상태를 해소했다. 실제 ELK 실행으로 분기·순환·자기 연결·다중 연결·비연결·빈 그래프, 비중첩·직각 경로·반복 재현성·원본 무변경이 검증되었다.

## 자동 wire 선분 분리 검증 (2026-10-24)

연결별 고정 포트와 선·도형·포트 간격을 적용하고 Activity 폭을 연결 수에 맞춰 확장했다. ELK 접점은 임시 PSI 사용자 데이터로만 보존하여 실제 Canvas에서도 중앙으로 합쳐지지 않는다. 그룹 이동은 접점을 함께 이동하고 한쪽 이동·크기 변경·재연결은 기존 경로 편집으로 전환한다. 서비스 XML에는 좌표나 임시 정책을 기록하지 않는다.

실제 Gradle 메인·테스트 컴파일, ELK 배치 테스트 6개, buildPlugin과 prepareSandbox_runIde가 성공했다. ZIP 생성은 기존 환경 제약과 동일하게 buildSearchableOptions를 제외했다. 실행 중인 테스트 IDE가 JAR를 잠근 상태에서는 샌드박스 갱신이 실패했으며 사용자 종료 후 재실행하여 성공했다.

최신 배포 ZIP의 JAR를 사용하는 격리 회귀 136개가 통과했다. 분기·합류·병렬 연결·자기 연결·순환에 대해 ELK 및 Canvas 경로의 양의 길이 선분 공유가 없음을 확인했다. 점 교차 제거까지 보장하지 않는다. 기존 라벨 클릭 테스트는 InitialState 라벨 숨김 요구에 맞게 FinalState 원형 도형으로 변경했고 확대 붙여넣기는 정수 반올림의 최대 반 픽셀 오차와 정수 좌표를 함께 검증한다. 격리 리소스는 기존 Query 파일 타입 충돌을 제외하는 용도이며 툴바 진단 로그를 포함한 전체 IDE 수동 검증과는 구분한다.

speckit-converge 최종 대조: FR-196~FR-200 및 T221~T225에 해당하는 미구현 항목은 없다. 원본 무변경·접점 보존·그룹 이동·단일 이동·기존 모델 회귀를 확인했다. git diff --check가 통과했다.

## 서비스 직접 모드 구조 편집 제한 (2026-10-24)

서비스 XML을 ServiceDiagramAdapter로 읽는 경우 구조 편집을 허용하지 않도록 했다. Activity/State와 Wire 생성 도구를 비활성화하고 이동·resize 입력을 막았다. Activity 속성, wire 조건·경로·연결, 삭제, 붙여넣기도 UI 활성 상태와 저장 콜백 양쪽에서 차단한다. 기존 glue_uml_ad 모드는 Adapter가 없으므로 계속 편집 가능하다. 선택·복사·탐색과 Diagram 메타데이터 편집은 유지한다.

`:modeleditor:compileKotlin` 실제 Gradle 컴파일이 통과했다. 변경 경로를 검토해 toolbar 생성, canvas 이동, property/condition 편집, 생성 콜백, paste, delete 모두 공통 `canEditStructure()` 정책으로 제한됨을 확인했다. 사용자 요청에 테스트 실행은 포함되지 않아 테스트는 실행하지 않았다. speckit 대조에서 FR-201~FR-203 및 T226~T228 구현 범위 누락은 없다.

## ELK Layered 교차 최소화 (2026-10-24)

고정 포트 좌표 때문에 Layered가 접점 위치를 선택할 수 없던 제한을 해소했다. 이제 각 edge는 독립 포트를 유지하고 North/South 면만 고정한다. 포트 좌표는 ELK가 계산한다. `LAYER_SWEEP`와 `INPUT_ORDER`를 명시하고 연결 생성 순서는 서비스 transition 입력 순서를 따른다. ELK의 crossing minimization은 레이어와 연결 순서를 바꾸어 교차 수를 줄이는 휴리스틱이며 모든 그래프의 교차 0개를 보장하지 않는다. 기존 route 변환은 ELK의 접점 위치를 그대로 받아 저장하지 않는다.

`:modeleditor:compileKotlin` 컴파일과 `:buildPlugin -x :buildSearchableOptions` 배포 빌드가 통과했다. Run IDE 전용 샌드박스 갱신은 현재 실행 중인 IntelliJ가 ELK JAR를 잠가 실패했다. 회귀 테스트는 실행하지 않았다. speckit 대조에서 FR-204~FR-206의 구현 누락은 없다.

## ELK 서비스 그래프 편집 활성화 (2026-10-24)

서비스 직접 편집 모드에서 공통 문서 쓰기 권한 gate를 사용하도록 바꾸어 Activity 추가, Wire 추가, child/wire 편집·이동·크기 변경·삭제·붙여넣기를 다시 활성화했다. 서비스 포맷은 Activity와 transition만 표현하므로 child 생성 메뉴는 Activity로 제한하고 Initial/Final State는 기존처럼 그래프에서 합성한다.

완성된 Wire는 양 끝의 유효한 child를 선택한 뒤에만 콜백을 호출한다. 시작점만 선택한 동안의 선은 캔버스 미리보기로 남아 XML/ELK 입력에 들어가지 않는다. Activity 및 Wire 구조가 바뀌면 변경을 service XML에 적용하고 projection을 무효화해 ELK 배치를 다시 계산한다. 위치·너비만 바꾸면 projection을 유지하므로 좌표는 XML에 기록되지 않는다.

`:modeleditor:compileKotlin`과 `:buildPlugin -x :buildSearchableOptions`가 통과했다. 회귀 테스트는 실행하지 않았다. FR-207~FR-212, T231~T233 구현이 완료됐고, 이전 US73 구조 차단 정책은 최신 요청으로 대체했다. `git diff --check`도 통과했다.

## Service XML Commit 전 검증 및 반영 (2026-10-24)

서비스 직접 편집의 변경은 비물리 projection에만 적용하고 pending 상태를 기록한다. 원본 문서는 Commit 클릭 전까지 변경되지 않는다. 서비스 모드 툴바에 Commit을 추가했으며 pending 변경과 쓰기 권한이 있을 때 활성화된다. Commit 전 원본 modification stamp, Diagram 파싱, Activity 이름 중복/예약어, InitialState 연결, FinalState 개수, service로 매핑 가능한 wire endpoint, child/root property 정합성을 검사한다. 검증 성공 시 단일 쓰기 명령으로 Service XML에 반영하고 projection을 무효화해 ELK를 다시 실행한다. 실패 시 메시지를 표시하고 기존 XML 및 pending 편집을 유지한다. source 파일 외부 수정이 감지되면 오래된 pending 변경을 덮어쓰지 않고 revision 오류를 낸다. Design editor의 isModified도 pending 상태를 반영한다.

`:modeleditor:compileKotlin` 및 `:buildPlugin -x :buildSearchableOptions`가 성공했다. 회귀 테스트는 실행하지 않았다. speckit 대조에서 FR-213~FR-218 및 T234~T236 구현 누락이 없다. `.glue_uml_ad`는 ServiceDiagramAdapter가 없으므로 기존 즉시 저장을 유지한다. `git diff --check`를 통과했다.

## 서비스 직접 편집의 대기 변경 관리 (2026-10-24)

`No use .glue_uml_ad file` 옵션에서 서비스 XML을 직접 편집하면 Commit 전 변경은 메모리 projection에만 남는다. 툴바의 Rollback은 이 변경을 버리고 원본 서비스 XML을 다시 읽어 ELK 배치를 갱신한다. 추가한 Activity와 Wire는 초록색, 삭제한 요소는 원래 위치에 빨간색 점선/외곽선으로 표시하고, 변경된 Activity와 Wire는 주황색으로 표시한다. 변경 없이 원본과 같아진 항목은 색상 표시가 사라진다.

미커밋 변경이 있을 때 파일을 닫으면 확인창이 열린다. `Rollback and Close`를 선택하면 대기 변경을 버리고 닫으며, `Keep Editing` 또는 창 닫기를 선택하면 닫기를 취소한다. 이 동작은 서비스 직접 편집 모드에만 적용된다.

`:modeleditor:compileKotlin` 및 `:buildPlugin -x :buildSearchableOptions` 빌드가 성공했다. 요청에 따라 테스트는 실행하지 않았다.
## 서비스 직접 편집 Undo/Redo 분리 (2026-10-24)

서비스 직접 편집의 staged 변경은 projection 스냅샷 기반 Undo/Redo 기록에 저장한다. 서비스 Design에서 Undo/Redo 버튼과 Ctrl+Z/Ctrl+Shift+Z(그리고 Ctrl+Y)를 사용하면 대기 변경만 이동한다. 이 편집 경로는 원본 Service XML을 대상으로 IntelliJ 쓰기 명령을 만들지 않는다. `.glue_uml_ad` 편집은 기존 일반 명령을 사용하고, Commit은 원본 XML에 하나의 일반 명령으로 반영한다. Source 탭의 Undo/Redo는 기본 XML 편집 기록을 유지한다.

Undo/Redo 뒤 Design을 다시 읽어 요소 색상과 Commit 가능 상태를 계산한다. Commit 성공과 Rollback은 대기 기록을 비운다. `:modeleditor:compileKotlin` 및 `:buildPlugin -x :buildSearchableOptions`가 성공했다. `git diff --check`도 통과했다. UI Undo/Redo 왕복은 실행 중인 IDE에서 수동 검증하지 않았고, 자동 테스트는 실행하지 않았다.
## 툴바 라벨과 Commit 전 Undo/Redo (2026-10-24)

자석 액션은 `Magnetic`, Diagram 속성 액션은 `Properties` 텍스트로 표시한다. 서비스 직접 편집의 Commit 전 변경은 projection에만 적용되고 Undo/Redo 명령 이력에 추가되지 않는다. 따라서 대기 변경은 Undo/Redo로 바뀌지 않으며 Commit 또는 Rollback으로만 반영하거나 폐기한다. staged Undo/Redo 버튼과 전용 단축키는 제공하지 않는다. Commit된 XML 변경과 `.glue_uml_ad` 편집의 일반 Undo/Redo 기록은 유지한다.

`:modeleditor:compileKotlin`, `:buildPlugin -x :buildSearchableOptions`, `git diff --check`가 통과했다. UI 수동 확인 및 테스트는 실행하지 않았다.
## Magnetic·Properties 아이콘 제거 (2026-10-24)

`Magnetic`과 `Properties` 툴바 액션은 아이콘 없이 텍스트만 표시한다. `:modeleditor:compileKotlin`과 `:buildPlugin -x :buildSearchableOptions`가 성공했고, `git diff --check`도 통과했다. UI 수동 확인과 테스트는 실행하지 않았다.