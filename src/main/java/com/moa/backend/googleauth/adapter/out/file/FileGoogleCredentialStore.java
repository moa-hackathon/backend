package com.moa.backend.googleauth.adapter.out.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moa.backend.googleauth.application.port.out.GoogleCredentialStorePort;
import com.moa.backend.googleauth.domain.GoogleCredential;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * refresh token을 로컬 JSON 파일에 저장한다. POSIX 시스템에서는 소유자만 읽을 수 있게(600) 만든다.
 */
@Component
public class FileGoogleCredentialStore implements GoogleCredentialStorePort {

    private final Path file;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileGoogleCredentialStore(GoogleCredentialFileProperties properties) {
        this.file = Path.of(properties.file()).toAbsolutePath().normalize();
    }

    @Override
    public Optional<GoogleCredential> load() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            CredentialFile stored = objectMapper.readValue(file.toFile(), CredentialFile.class);
            Instant savedAt = stored.savedAt() == null ? null : Instant.parse(stored.savedAt());
            return Optional.of(new GoogleCredential(stored.refreshToken(), stored.scope(), savedAt));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Google 자격 증명 파일을 읽을 수 없습니다: " + file, e);
        }
    }

    @Override
    public void save(GoogleCredential credential) {
        String savedAt = credential.savedAt() == null ? null : credential.savedAt().toString();
        CredentialFile stored = new CredentialFile(credential.refreshToken(), credential.scope(), savedAt);
        Path temp = null;
        try {
            Files.createDirectories(file.getParent());
            temp = Files.createTempFile(file.getParent(), ".credential-", ".tmp");
            restrictToOwner(temp);
            objectMapper.writeValue(temp.toFile(), stored);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Google 자격 증명 파일을 저장할 수 없습니다: " + file, e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // 임시 파일 정리 실패는 무시한다.
                }
            }
        }
    }

    private static void restrictToOwner(Path path) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        }
    }

    record CredentialFile(String refreshToken, String scope, String savedAt) {
    }
}
