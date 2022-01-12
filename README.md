# Pokemon_RealGo
### AR 기반의 실시간 포켓몬 대전 게임
안드로이드 / 앱 / 게임
 
## Period
2022.1.5 ~ 2022.1.11 (몰입캠프 2주차)

## Contributors
#### 김수민 [fosemfhtm](https://github.com/fosemfhtm)
#### 박수빈 [psb0623](https://github.com/psb0623)
#### 정강산 [Sanu7D0](https://github.com/Sanu7D0)
 
## App
Made by kotlin

[실시간 배틀 서버](https://github.com/Sanu7D0/Pokemon_RealGO-server)

[DB 서버](https://github.com/psb0623/Pokemon-RealGO-Database)


### Login

- 회원가입 : 회원가입 버튼을 통해 포켓몬 리얼고 계정을 생성할 수 있다.
- 로그인 : 만든 본인의 계정으로 포켓몬 리얼고 게임에 접속하여 게임을 즐길 수 있다.
- 자동로그인 : 한번 로그인하면 정보가 로컬에 자동 저장되어, 로그아웃을 따로 하지 않은 경우에는 다음 접속시 로그인을 거치지 않고 바로 게임을 즐길 수 있다.

### Main

- 트레이너 정보 : DB 서버에 저장된 본인 계정의 이름, 게임머니, 전적, 보유 포켓몬의 숫자를 확인할 수 있다.
- 지닌 포켓몬 : 현재 지니고 있는 4마리의 포켓몬을 확인할 수 있다. 지닌 포켓몬으로 배틀에 참가하게 된다.
- 홈 : 버튼을 통해 지닌 포켓몬을 관리할 수 있는 Home 화면으로 이동할 수 있다.
- 상점 : 뽑기를 통해 새로운 포켓몬을 얻을 수 있는 Shop 화면으로 이동할 수 있다.
- 배틀 : 다른 사람과 대전할 수 있는 Battle 화면으로 이동할 수 있다.

### Home

- 포켓박스 : 자신이 지닌 모든 포켓몬을 확인할 수 있다.
- 포켓몬 상세 정보 : 포켓박스의 포켓몬을 꾹 누르면 포켓몬의 스탯과 같은 상세정보를 확인할 수 있고, 포켓몬을 놓아줄 수도 있다.
- 지닌 포켓몬 : 지닌 포켓몬과 포켓몬의 기술을 확인할 수 있고 지닌 포켓몬을 변경할 수 있다.

### Shop

- 일반 뽑기 : 포켓몬을 뽑을 수 있다.
- 고급 뽑기 : 좋은 포켓몬을 뽑을 수 있다.
- 슈퍼 프리미엄 뽑기 : 아주 좋은 포켓몬을 뽑을 수 있다.

### Battle

- BattleRoom 입장 : 메인에서 배틀 버튼을 누르면 방 번호를 입력할 수 있는 창이 뜬다. 상대방과 같은 방 번호를 입력하면 대전을 시작할 수 있다.
- 포켓몬 실시간 대전 : AR 포켓몬이 화면에 나타나고 포켓몬 대전을 실시간으로 할 수 있게 된다.

## Database server

Django와 SQLite3를 이용하여 사용자 데이터 및 포켓몬 배틀에 필요한 데이터들을 관리하는 데이터 서버를 구축하였다.

- User 데이터 : 사용자의 아이디와 비밀번호, 로그인 인증에 필요한 토큰, 가진 돈 등을 저장한다.
- Pokemon 데이터 : 포켓몬들의 이름과 타입, 스탯 등을 저장한다.
- Skill 데이터 : 스킬의 이름과 타입, 위력 등을 저장한다.
- Learnable 데이터 : 각 포켓몬이 배울 수 있는 스킬을 N:M 관계로 저장하였다.
- Box 데이터 : 사용자들이 가진 포켓몬과 그 포켓몬이 가진 스킬들을 ternary 관계로 저장하였다.
- 등등...

데이터베이스 서버와 클라이언트는 HTTP 통신 방식으로 통신하며, Restful API를 이용하여 필요한 기능들을 편리하게 구현하였다.
 

## Real-time battle server
Node.js 의 Socket.io 라이브러리 사용
 사용자가 선택한 스킬의 인덱스를 받으면, 서버가 가지고 있는 정보를 토대로
배틀 로직을 계산하여 결과를 보내준다
 소켓 통신으로 특정 Room에 있는 클라이언트들에게만 정보를 보낼 수 있기 때문에,
1:1 (또는 다수) 간의 실시간 대전에 적합하다.

### Issue
- 서버가 보내는 정보와 클라이언트의 정보가 꼬일 수 있다. -> 각 단계를 callback으로 관리하여 순서가 꼬이지 않게 해야한다
- 바닐라 JS로 배틀로직을 객체지향 설계하려니 문제가 많다. -> TS를 써야할듯?

## Reference / Libraries
- Minimum SDK level 24.
- Kotlin based, Coroutines + Flow for asynchronous.
- ARCore - ARCore SDK for Android.
- SceneForm - Sceneform SDK for Android.
- AR models & Renderer - [Pokedex-AR](https://github.com/skydoves/Pokedex-AR)

## Comments
- 김수민 :  
- 박수빈 : 
- 정강산 :  

## Todo...?
- 상대방과 AR 화면 공유
- 더 많은 포켓몬과 기술 추가
- 체력 게이지 색상 변화
- 카카오 로그인 기능
- 지도 기반 주변 트레이너 탐색 기능
- 공격시 애니메이션 기능
