# 비디오 음성-텍스트 변환 (Video STT)

LangChain과 OpenAI의 Whisper를 사용하여 비디오 파일에서 음성-텍스트 변환(STT)을 수행하는 자바 프로그램입니다. 이 프로그램은 비디오 경로를 입력으로 받아 변환 결과를 출력합니다.

## 기능

- 비디오 파일에서 오디오 추출
- OpenAI의 Whisper 모델을 사용한 음성-텍스트 변환
- 다양한 비디오 형식 지원

## 사전 요구사항

- Java 11 이상
- FFmpeg 설치 및 시스템 PATH에 추가
- OpenAI API 키

참고: 이 프로젝트는 Gradle 래퍼를 사용하므로 Gradle을 별도로 설치할 필요가 없습니다.

### FFmpeg 설치하기

#### Windows
1. [ffmpeg.org](https://ffmpeg.org/download.html)에서 FFmpeg 다운로드
2. 다운로드한 아카이브 압축 해제
3. bin 폴더를 시스템 PATH에 추가

#### macOS
```
brew install ffmpeg
```

#### Linux
```
sudo apt update
sudo apt install ffmpeg
```

## 프로젝트 빌드하기

1. 이 저장소를 클론합니다
2. 프로젝트 디렉토리로 이동합니다
3. Gradle(래퍼 사용)로 빌드합니다:

```
# Windows
gradlew.bat clean build fatJar

# macOS/Linux
./gradlew clean build fatJar
```

이렇게 하면 `build/libs` 디렉토리에 의존성이 포함된 JAR 파일이 생성됩니다.

## 참고사항

- 프로그램은 임시 오디오 파일(`temp_audio.mp3`)을 생성 처리
- 대용량 비디오 파일의 경우 추출 및 변환 과정에 시간이 걸릴 수 있습니다
- 변환 품질은 비디오의 오디오 품질에 따라 달라집니다

## 문제 해결

- FFmpeg를 찾을 수 없다는 오류가 발생하면 제대로 설치되어 있고 시스템 PATH에 추가되어 있는지 확인하세요
- 인증 오류가 발생하면 OpenAI API 키가 환경 변수로 올바르게 설정되어 있는지 확인하세요
- 비디오 형식 문제가 있는 경우 FFmpeg를 사용하여 MP4와 같은 더 일반적인 형식으로 비디오를 변환해 보세요
