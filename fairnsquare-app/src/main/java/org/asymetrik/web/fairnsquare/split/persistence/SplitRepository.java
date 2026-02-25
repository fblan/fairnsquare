package org.asymetrik.web.fairnsquare.split.persistence;

import java.util.Optional;

import org.asymetrik.web.fairnsquare.split.domain.InvalidSplitIdError;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.mapper.SplitPersistenceMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SplitRepository {

    private final ZipFileRepository zipFileRepository;
    private final SplitPersistenceMapper splitMapper;
    private final StorageConstraintsService storageConstraintsService;

    @Inject
    public SplitRepository(final ZipFileRepository zipFileRepository, final SplitPersistenceMapper splitMapper,
            final StorageConstraintsService storageConstraintsService) {
        this.zipFileRepository = zipFileRepository;
        this.splitMapper = splitMapper;
        this.storageConstraintsService = storageConstraintsService;
    }

    public void save(Split split) {
        if (split == null || split.getId() == null || split.getId().value() == null
                || split.getId().value().isBlank()) {
            throw new InvalidSplitIdError("Split ID cannot be null or blank");
        }
        zipFileRepository.save(split.getId().value(), splitMapper.toPersistenceDTO(split));
        storageConstraintsService.computeStorageStats();
    }

    public Optional<Split> load(String splitId) {
        Optional<Split> split = zipFileRepository.load(splitId, SplitPersistenceDTO.class).map(splitMapper::toDomain);
        return split;
    }

    public boolean exists(final String splitId) {
        return zipFileRepository.exists(splitId);
    }

    public void delete(final String splitId) {
        zipFileRepository.delete(splitId);
    }
}
