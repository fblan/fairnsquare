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

    private final JsonFileRepository jsonFileRepository;
    private final SplitPersistenceMapper splitMapper;

    @Inject
    public SplitRepository(final JsonFileRepository jsonFileRepository, final SplitPersistenceMapper splitMapper) {
        this.jsonFileRepository = jsonFileRepository;
        this.splitMapper = splitMapper;
    }

    public void save(Split split) {
        if (split == null || split.getId() == null || split.getId().value() == null
                || split.getId().value().isBlank()) {
            throw new InvalidSplitIdError("Split ID cannot be null or blank");
        }
        jsonFileRepository.save(split.getId().value(), splitMapper.toPersistenceDTO(split));
    }

    public Optional<Split> load(String splitId) {
        Optional<Split> split = jsonFileRepository.load(splitId, SplitPersistenceDTO.class).map(splitMapper::toDomain);
        return split;
    }

    public boolean exists(final String splitId) {
        return jsonFileRepository.exists(splitId);
    }

    public void delete(final String splitId) {
        jsonFileRepository.delete(splitId);
    }
}
