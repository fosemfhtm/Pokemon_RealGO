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

[실시간 배틀 서버 Git](https://github.com/Sanu7D0/Pokemon_RealGO-server) - Node.js, Socket.io

### Login

- 회원가입
- 로그인

### Main

- 트레이너 정보
- 지닌포켓몬

### Home

- 포켓박스
- 포켓몬 상세 정보
- 지닌 포켓몬 변경

### Shop

- 일반 뽑기
- 고급 뽑기
- 슈퍼 프리미엄 뽑기

### Battle

- BattleRoom 입장
- 포켓몬 실시간 대전

## Database server

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
- 정강산 : JS로 객체지향프로그래밍은 하지 말자... + 설계를 잘 해놓자
