package com.framework.v25.dto;

import lombok.*;
import lombok.extern.jackson.Jacksonized;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class BuildLogDto {

    private UUID           id;
    private UUID           projectId;

    /** SUCCESS | FAILED | RUNNING */
    private String         status;

    /** Short git commit hash — e.g. a3f9c12 */
    private String         commitHash;

    /** Branch name — e.g. main, feature/login */
    private String         branch;

    /** Full build log text from Jenkins */
    private String         logs;

    /** Who triggered the build (email or userId) */
    private String         triggeredBy;

    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
