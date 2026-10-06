# NihongoLock v0.3.4

개인용 Galaxy Z Fold8 일본어 자기통제 앱.

## 학습 규칙

- 매일 00:00~23:59 실제 학습 20분
- 20분 미달 시 다음날 17:00~22:00 벌칙 모드
- 벌칙일 목표 35분(기본 20분 + 벌칙 15분)
- 35분 완료 시 즉시 해제
- 완료하지 못해도 22:00 자동 해제
- 10일 연속 성공 시 PASS 1장, 최대 2장
- 앱을 켜 놓기만 한 시간은 인정하지 않음. 90초 동안 조작이 없으면 타이머 정지
- 앱 삭제는 공부 포기로 간주. 삭제하면 앱의 제한과 데이터가 함께 사라짐
- 일본어 문제와 자유 첨삭 화면에 후리가나(한자 위 히라가나) 표시
- 문제/레벨 테스트에서 일본어 듣기 버튼 제공
- 레벨 기준: Lv.1 입문, Lv.2 JLPT N5, Lv.3 N4, Lv.4 N3, Lv.5 N2, Lv.6 N1

## 전화 / 안전 예외

벌칙 중에도 다음 전화·안전 계열 화면은 차단하지 않도록 설계했습니다.

- SKT 에이닷 전화: `com.skt.prod.dialer`
- Samsung / Android 인콜 UI
- 기본 전화 앱
- Samsung 시계/알람
- 긴급/안전 UI
- Android 패키지 설치/삭제 UI

실기기 One UI 버전에 따라 전화/긴급 기능의 실제 패키지명이 다를 수 있으므로 첫 설치 후 수신전화/발신전화/112·119 진입 동작은 반드시 테스트해야 합니다.

## OpenAI API

`API / 앱 설정`에서 사용자가 직접 설정합니다.

- API Key 입력/수정/삭제
- 모델명 변경
- 연결 테스트
- Responses API 기반 일본어 첨삭
- API Key는 Android Keystore의 AES-GCM 키를 이용해 기기 내 암호화 저장
- API가 없어도 기본 문제은행, 학습 타이머, 벌칙, PASS는 동작

개인용 사이드로드 앱이므로 기기에 API Key를 저장하는 구조입니다. 더 높은 보안이 필요하면 추후 별도 백엔드 프록시로 바꾸는 것을 권장합니다.

## 학습 기록 GitHub 백업

`API / 앱 설정 → 학습 기록 GitHub 백업`에서 GitHub 저장소와 fine-grained token을 한 번 설정할 수 있습니다.

- `Contents: Read and write` 권한이 있는 token이 필요합니다.
- 기본 저장소는 `bong40101-prog/NihongoLock`입니다.
- `data/study-record.json`에 정답/오답, 선택 답, 문제, 레벨, 학습 시간과 누적 통계가 저장됩니다.
- GitHub token은 Android Keystore로 암호화하고, OpenAI API Key와 학습 기록에는 포함하지 않습니다.
- 기록을 공개하고 싶지 않으면 GitHub private repository를 사용하세요.
- 답변 후 잠시 뒤 자동 업로드하며, 설정 화면에서 수동 업로드도 할 수 있습니다.

## 원격 업데이트

v0.2.0부터 GitHub Releases를 업데이트 서버로 사용할 수 있습니다.

1. 공개 GitHub 저장소를 하나 준비합니다.
2. 앱 `API / 앱 설정 → 앱 업데이트`에 `owner/repo` 또는 저장소 URL을 한 번 입력합니다.
3. 저장소의 최신 Release에 새 APK를 올리고 태그를 `v0.3.4`처럼 올립니다.
4. 앱은 실행 시 약 12시간 간격으로 최신 Release를 확인합니다.
5. 새 버전이 있으면 `업데이트 설치`를 누릅니다.
6. 앱이 APK를 다운로드한 뒤 Android 시스템 설치 화면을 엽니다. 최신 Android의 자체 업데이트 차단을 피하기 위해 시스템 설치 화면에서 `설치`를 한 번 눌러야 합니다.
7. 첫 업데이트 때 Android의 `알 수 없는 앱 설치` 권한을 이 앱에 한 번 허용합니다.
8. 이후 새 APK는 Android 설치 확인 화면을 거쳐 기존 앱 위에 덮어설치됩니다.

업데이트 APK는 반드시 최초 APK와 **같은 Android 서명키**로 서명해야 합니다. 그렇지 않으면 Android가 업데이트를 거부합니다.

## GitHub Actions 자동 빌드/Release

`.github/workflows/build-release.yml` 포함.

GitHub repository secrets에 아래 4개를 등록하면 태그를 push할 때 서명된 `NihongoLock.apk`를 자동 빌드하고 GitHub Release에 첨부합니다.

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

별도로 제공되는 signing backup의 `GITHUB_SECRETS.txt`에 최초 키와 값이 들어 있습니다. **절대 공개 저장소에 keystore 또는 secrets 파일을 commit하지 마세요.**

## Android Studio 로컬 빌드

- JDK 17
- Gradle 8.9
- Android SDK 35

로컬 서명 빌드를 하려면 프로젝트 루트에 `keystore.properties`를 만들고 `keystore.properties.example` 형식을 사용합니다.

```bash
gradle assembleRelease
```

결과:

`app/build/outputs/apk/release/app-release.apk`

## 최초 설치 후 필요한 설정

1. APK 설치
2. 앱 실행
3. Galaxy의 접근성 설정에서 `니혼고 락 강제 학습` 활성화
4. Android가 사이드로드 앱의 접근성 사용을 제한하는 경우 앱 정보 메뉴의 `제한된 설정 허용`을 먼저 켬
5. 전화 수신/발신, 긴급전화, 에이닷 전화 동작 확인
6. 필요하면 OpenAI API Key 설정
7. GitHub 업데이트 저장소 입력

## 중요한 현실적 제한

이 앱은 Device Owner가 아니라 사용자가 삭제할 수 있는 AccessibilityService 기반입니다. 따라서 사용자가 앱 자체를 삭제하면 제한은 종료됩니다. 이것이 의도된 `공부 포기` 경로입니다.

접근성 서비스는 벌칙 시간에 다른 앱 위를 덮어 학습 화면으로 돌아오게 하지만, 제조사/One UI 업데이트에 따라 예외 패키지 또는 동작을 조정해야 할 수 있습니다.
