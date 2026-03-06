package org.asymetrik.web.fairnsquare.split.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.Filename;
import org.asymetrik.web.fairnsquare.infrastructure.zipfile.ZipSerializer;
import org.asymetrik.web.fairnsquare.split.domain.InvalidSplitIdError;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.mapper.SplitPersistenceMapper;

@ApplicationScoped
public class SplitRepository {

    private final ZipSerializer zipSerializer;
    private final FileSystemService fileSystemService;
    private final SplitPersistenceMapper splitMapper;
    private final ObjectMapper objectMapper;

    @Inject
    public SplitRepository(final ZipSerializer zipSerializer, final FileSystemService fileSystemService,
            final SplitPersistenceMapper splitMapper) {
        this.zipSerializer = zipSerializer;
        this.fileSystemService = fileSystemService;
        this.splitMapper = splitMapper;
        this.objectMapper = createObjectMapper();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public void save(Split split) {
        if (split == null || split.getId() == null || split.getId().value() == null
                || split.getId().value().isBlank()) {
            throw new InvalidSplitIdError("Split ID cannot be null or blank");
        }
        byte[] data = serialize(splitMapper.toPersistenceDTO(split));
        byte[] zipBytes = zipSerializer.toZip(data);
        fileSystemService.saveFile(toFilename(split.getId().value()), zipBytes);
    }

    public Optional<Split> load(String splitId) {
        return fileSystemService.readFile(toFilename(splitId)).map(zipSerializer::fromZip)
                .map(bytes -> deserialize(bytes, SplitPersistenceDTO.class)).map(splitMapper::toDomain);
    }

    public boolean exists(final String splitId) {
        return fileSystemService.existsFile(toFilename(splitId));
    }

    public void delete(final String splitId) {
        fileSystemService.deleteFile(toFilename(splitId));
    }

    private Filename toFilename(String splitId) {
        return new Filename(splitId + ".zip");
    }

    private byte[] serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ZipSerializer.ZipException("Failed to serialize entity", e);
        }
    }

    private <T> T deserialize(byte[] bytes, Class<T> type) {
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new ZipSerializer.ZipException("Failed to deserialize entity", e);
        }
    }
}
