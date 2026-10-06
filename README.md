# NihongoLock v0.2.0

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

## 원격 업데이트

v0.2.0부터 GitHub Releases를 업데이트 서버로 사용할 수 있습니다.

1. 공개 GitHub 저장소를 하나 준비합니다.
2. 앱 `API / 앱 설정 → 앱 업데이트`에 `owner/repo` 또는 저장소 URL을 한 번 입력합니다.
3. 저장소의 최신 Release에 새 APK를 올리고 태그를 `v0.3.0`처럼 올립니다.
4. 앱은 실행 시 약 12시간 간격으로 최신 Release를 확인합니다.
5. 새 버전이 있으면 `업데이트 설치`를 누릅니다.
6. 첫 업데이트 때 Android의 `알 수 없는 앱 설치` 권한을 이 앱에 한 번 허용합니다.
7. 이후 새 APK는 Android 설치 확인 화면을 거쳐 기존 앱 위에 덮어설치됩니다.

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
