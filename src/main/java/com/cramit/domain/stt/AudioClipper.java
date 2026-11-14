package com.cramit.domain.stt;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class AudioClipper {

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
        byte[] mp3 = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        if (exitCode != 0 || mp3.length == 0) {
            throw new IllegalStateException("ffmpeg 오디오 자르기 실패: exitCode=" + exitCode);
        }
        return mp3;
    }

    private String seconds(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
