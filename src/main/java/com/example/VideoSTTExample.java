package com.example;

/**
 * VideoSTT를 프로그래밍 방식으로 사용하는 방법을 보여주는 예제 클래스입니다.
 */
public class VideoSTTExample {

    public static void main(String[] args) {
        // 예제 비디오 경로 - 실제 비디오 파일 경로로 대체하세요
        String videoPath = "C:\\Users\\LEGION\\Videos\\Captures\\3강.mp4";

        System.out.println("비디오 STT 예제");
        System.out.println("----------------");
        System.out.println("비디오 처리 중: " + videoPath);
        System.out.println();

        try {
            // VideoSTT 인스턴스 생성
            VideoSTT videoSTT = new VideoSTT("");

            // 선택 사항: 임시 파일 정리 여부 설정
            videoSTT.setCleanupTempFiles(true);

            // 변환 수행
            String transcription = videoSTT.transcribe(videoPath);

            // 결과 출력
            System.out.println("변환 결과:");
            System.out.println("--------------------");
            System.out.println(transcription);

        } catch (Exception e) {
            System.err.println("오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
