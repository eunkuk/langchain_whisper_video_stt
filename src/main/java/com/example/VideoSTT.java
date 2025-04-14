package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * VideoSTT - LangChain과 Whisper를 사용하여 비디오 파일에서 음성-텍스트 변환을 수행하는 프로그램.
 * 이 프로그램은 비디오 경로를 입력으로 받아 STT 결과를 출력합니다.
 */
public class VideoSTT {
    private static final Logger logger = LoggerFactory.getLogger(VideoSTT.class);

    private final String apiKey;
    private boolean cleanupTempFiles = false;

    /**
     * 환경 변수에서 OpenAI API 키를 가져와 새 VideoSTT 인스턴스를 생성합니다.
     */
    public VideoSTT() {
        this(System.getenv("OPENAI_API_KEY"));
    }

    /**
     * 지정된 OpenAI API 키로 새 VideoSTT 인스턴스를 생성합니다.
     *
     * @param apiKey OpenAI API 키
     */
    public VideoSTT(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 처리 후 임시 파일을 정리할지 여부를 설정합니다.
     *
     * @param cleanup 임시 파일을 정리하려면 true, 유지하려면 false
     * @return 메소드 체이닝을 위한 현재 인스턴스
     */
    public VideoSTT setCleanupTempFiles(boolean cleanup) {
        this.cleanupTempFiles = cleanup;
        return this;
    }

    /**
     * 비디오 파일을 텍스트로 변환합니다.
     *
     * @param videoPath 비디오 파일 경로
     * @return 변환된 텍스트
     * @throws Exception 처리 중 오류가 발생한 경우
     */
    public String transcribe(String videoPath) throws Exception {
        File videoFile = new File(videoPath);

        if (!videoFile.exists() || !videoFile.isFile()) {
            throw new IllegalArgumentException("The specified video file does not exist or is not a file: " + videoPath);
        }

        // 동시 사용 시 충돌을 방지하기 위해 고유한 임시 파일 이름 생성
        String uniqueTempFile = UUID.randomUUID().toString() + "_temp_audio.mp3";

        try {
            // 비디오에서 오디오 추출
            String audioFilePath = extractAudioFromVideo(videoPath, uniqueTempFile);

            // 추출된 오디오에서 STT 수행
            String transcription = performSpeechToText(audioFilePath);

            // 변환 결과를 파일로 저장
            saveTranscriptionToFile(videoPath, transcription);

            // 임시 파일 정리 기능이 활성화된 경우
            if (cleanupTempFiles) {
                cleanupTempFiles(audioFilePath);
            }

            return transcription;
        } catch (Exception e) {
            logger.error("Error processing video: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * VideoSTT의 명령줄 인터페이스입니다.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar video-stt.jar <video_file_path>");
            System.exit(1);
        }

        String videoPath = args[0];

        try {
            VideoSTT videoSTT = new VideoSTT();
            String transcription = videoSTT.transcribe(videoPath);

            // 변환 텍스트 출력
            System.out.println("변환 결과:");
            System.out.println(transcription);

        } catch (Exception e) {
            System.err.println("비디오 처리 오류: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * JavaCV/FFmpeg를 사용하여 비디오 파일에서 오디오를 추출합니다.
     *
     * @param videoPath 비디오 파일 경로
     * @param tempFileName 임시 오디오 파일 이름
     * @return 추출된 오디오 파일 경로
     * @throws Exception 오디오 추출에 실패한 경우
     */
    private String extractAudioFromVideo(String videoPath, String tempFileName) throws Exception {
        logger.info("Extracting audio from video: {}", videoPath);
        System.out.println("Extracting audio from video...");

        // 오디오를 위한 임시 파일 생성
        Path tempAudioPath = Paths.get(tempFileName);
        String outputPath = tempAudioPath.toAbsolutePath().toString();

        // 안정적인 오디오 추출을 위해 ProcessBuilder를 사용하여 FFmpeg 직접 호출
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", videoPath,
                "-vn",                  // 비디오 없음
                "-acodec", "libmp3lame", // MP3 코덱
                "-ar", "16000",         // 16kHz 샘플 레이트
                "-ac", "1",             // 모노 오디오
                "-y",                   // 출력 파일 덮어쓰기
                outputPath);

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 출력 읽기 및 로깅
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug(line);
            }
        }

        // 프로세스 완료 대기
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 프로세스가 코드 " + exitCode + "로 종료되었습니다");
        }

        // 파일이 생성되었는지 확인
        if (!Files.exists(tempAudioPath)) {
            throw new RuntimeException("Failed to extract audio: output file not created");
        }

        logger.info("Audio extraction completed: {}", tempAudioPath);
        System.out.println("Audio extraction completed.");

        return outputPath;
    }

    /**
     * OpenAI의 Whisper 모델을 사용하여 오디오 파일에서 음성-텍스트 변환을 수행합니다.
     *
     * @param audioFilePath 오디오 파일 경로
     * @return 변환된 텍스트
     * @throws IOException 파일 작업에 실패한 경우
     */
    private String performSpeechToText(String audioFilePath) throws IOException {
        logger.info("Performing speech-to-text on audio: {}", audioFilePath);
        System.out.println("Performing speech-to-text...");

        // ProcessBuilder를 사용하여 OpenAI의 Whisper API를 호출하는 간단한 구현
        ProcessBuilder pb = new ProcessBuilder(
                "curl",
                "-X", "POST",
                "-H", "Authorization: Bearer " + apiKey,
                "-H", "Content-Type: multipart/form-data",
                "-F", "file=@" + audioFilePath,
                "-F", "model=whisper-1",
                "https://api.openai.com/v1/audio/transcriptions");

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 응답 읽기
        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // 프로세스 완료 대기
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("API 요청이 종료 코드 " + exitCode + "로 실패했습니다");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("API 요청이 중단되었습니다", e);
        }

        // JSON 응답에서 변환 텍스트 추출
        String responseStr = response.toString();
        int textStart = responseStr.indexOf("\"text\":\"") + 8;
        int textEnd = responseStr.indexOf("\"", textStart);
        String transcription = responseStr.substring(textStart, textEnd);

        logger.info("Speech-to-text completed");
        System.out.println("Speech-to-text completed.");

        return transcription;
    }

    /**
     * 처리 중에 생성된 임시 파일을 정리합니다.
     *
     * @param audioFilePath 임시 오디오 파일 경로
     */
    private void cleanupTempFiles(String audioFilePath) {
        try {
            Files.deleteIfExists(Paths.get(audioFilePath));
            logger.info("Temporary files cleaned up");
        } catch (IOException e) {
            logger.warn("Failed to clean up temporary files: " + e.getMessage());
        }
    }

    /**
     * 변환 결과를 비디오 파일과 동일한 이름의 텍스트 파일로 저장합니다.
     *
     * @param videoPath 비디오 파일 경로
     * @param transcription 변환 텍스트
     */
    private static void saveTranscriptionToFile(String videoPath, String transcription) {
        try {
            // 비디오 파일 경로에서 파일 이름 추출
            Path path = Paths.get(videoPath);
            String fileName = path.getFileName().toString();

            // 파일 확장자 제거
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileName = fileName.substring(0, lastDotIndex);
            }

            // 텍스트 파일 경로 생성
            String textFilePath = path.getParent().toString() + File.separator + fileName + ".txt";

            // 텍스트 파일에 변환 결과 저장
            try (FileWriter writer = new FileWriter(textFilePath)) {
                writer.write(transcription);
            }

            System.out.println("변환 결과가 다음 파일에 저장되었습니다: " + textFilePath);
        } catch (IOException e) {
            System.err.println("파일 저장 오류: " + e.getMessage());
        }
    }
}
