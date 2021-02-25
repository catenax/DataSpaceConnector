package de.fraunhofer.isst.dataspaceconnector.controller.v2;

import de.fraunhofer.isst.dataspaceconnector.model.Artifact;
import de.fraunhofer.isst.dataspaceconnector.model.ArtifactDesc;
import de.fraunhofer.isst.dataspaceconnector.services.resources.v2.backend.ArtifactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/artifacts")
public final class ArtifactController extends BaseResourceController<Artifact, ArtifactDesc, ArtifactService> {
    @RequestMapping(value = "{id}/data", method = RequestMethod.GET)
    public ResponseEntity<Object> getData(@Valid @PathVariable final UUID artifactId) {
        final var artifactService = ((ArtifactService) this.getService());
        return ResponseEntity.ok(artifactService.getData(artifactId, null));
    }
}
