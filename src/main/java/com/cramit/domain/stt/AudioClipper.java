package com.cramit.domain.stt;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AudioClipper {

    private static final long TIMEOUT_SEC = 60;

    /** source의 [startSec, endSec]를 모노 16kHz 64kbps mp3로 잘라 돌려준다. */
    public byte[] clip(Path source, double startSec, double endSec) throws IOException, InterruptedException {
        double durationSec = Math.max(endSec - startSec, 0.5);
        Process process = new ProcessBuilder(
                "ffmpeg", "-v", "error", "-y",
                "-ss", seconds(startSec), "-t", seconds(durationSec),
                "-i", source.toString(),
                "-ac", "1", "-ar", "16000", "-b:a", "64k", "-f", "mp3", "pipe:1")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        // ffmpeg가 출력을 닫지 않고 멈추면 readAllBytes가 끝나지 않아서 읽기까지 시간 제한 안에 넣는다.
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        byte[] mp3;
        try {
            mp3 = output.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!process.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        } catch (TimeoutException | ExecutionException e) {
            process.destroyForcibly();
            throw new IllegalStateException("ffmpeg 오디오 자르기 실패(시간 초과 또는 출력 읽기 오류)", e);
        }
        int exitCode = process.exitValue();
        if (exitCode != 0 || mp3.length == 0) {
            throw new IllegalStateException("ffmpeg 오디오 자르기 실패: exitCode=" + exitCode);
        }
        return mp3;
    }

    private String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
