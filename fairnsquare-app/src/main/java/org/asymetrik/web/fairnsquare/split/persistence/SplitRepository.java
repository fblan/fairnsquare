package org.asymetrik.web.fairnsquare.split.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.Filename;
import org.asymetrik.web.fairnsquare.infrastructure.zipfile.ZipSerializer;
import org.asymetrik.web.fairnsquare.split.domain.InvalidSplitIdError;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.mapper.SplitPersistenceMapper;

@ApplicationScoped
public class SplitRepository {

    private final ZipSerializer zipSerializer;
    private final FileSystemService fileSystemService;
    private final SplitPersistenceMapper splitMapper;

    @Inject
    public SplitRepository(final ZipSerializer zipSerializer, final FileSystemService fileSystemService,
            final SplitPersistenceMapper splitMapper) {
        this.zipSerializer = zipSerializer;
        this.fileSystemService = fileSystemService;
        this.splitMapper = splitMapper;
    }

    public void save(Split split) {
        if (split == null || split.getId() == null || split.getId().value() == null
                || split.getId().value().isBlank()) {
            throw new InvalidSplitIdError("Split ID cannot be null or blank");
        }
        byte[] data = SplitJsonCodec.write(splitMapper.toPersistenceDTO(split));
        byte[] zipBytes = zipSerializer.toZip(data);
        fileSystemService.saveFile(toFilename(split.getId().value()), zipBytes);
    }

    /**
     * Loads all splits currently stored. Splits that cannot be deserialized are silently skipped.
     *
     * @return list of all valid splits
     */
    public List<Split> loadAll() {
        return fileSystemService.listAllSplitIds().stream().map(id -> {
            try {
                return load(id).orElse(null);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    public Optional<Split> load(String splitId) {
        return fileSystemService.readFile(toFilename(splitId)).map(zipSerializer::fromZip).map(SplitJsonCodec::read)
                .map(splitMapper::toDomain);
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
}
