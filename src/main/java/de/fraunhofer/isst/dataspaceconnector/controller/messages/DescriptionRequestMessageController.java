package de.fraunhofer.isst.dataspaceconnector.controller.messages;

import de.fraunhofer.isst.dataspaceconnector.exceptions.MessageException;
import de.fraunhofer.isst.dataspaceconnector.exceptions.MessageResponseException;
import de.fraunhofer.isst.dataspaceconnector.exceptions.UnexpectedMessageType;
import de.fraunhofer.isst.dataspaceconnector.services.messages.MessageProcessingService;
import de.fraunhofer.isst.dataspaceconnector.services.messages.MessageService;
import de.fraunhofer.isst.dataspaceconnector.utils.ControllerUtils;
import de.fraunhofer.isst.dataspaceconnector.utils.MessageUtils;
import de.fraunhofer.isst.dataspaceconnector.utils.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Controller for sending description request messages.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ids")
@Tag(name = "IDS Messages", description = "Endpoints for invoke sending IDS messages")
public class DescriptionRequestMessageController {

    /**
     * Service for message handling;
     */
    private final @NonNull MessageService messageService;

    /**
     * Service for message responses.
     */
    private final @NonNull MessageProcessingService messageProcessor;

    /**
     * Requests metadata from an external connector by building an ArtifactRequestMessage.
     *
     * @param recipient The target connector url.
     * @param elementId The requested element id.
     * @return OK or error response.
     */
    @PostMapping("/description")
    @Operation(summary = "Send ids description request message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "417", description = "Expectation failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")})
    @PreAuthorize("hasPermission(#recipient, 'rw')")
    @ResponseBody
    public ResponseEntity<Object> sendDescriptionRequestMessage(
            @Parameter(description = "The recipient url.", required = true)
            @RequestParam("recipient") final URI recipient,
            @Parameter(description = "The id of the requested resource.")
            @RequestParam(value = "elementId", required = false) final URI elementId) {
        Map<String, String> response;
        try {
            response = messageService.sendDescriptionRequestMessage(recipient, elementId);
        } catch (MessageException exception) {
            return ControllerUtils.respondIdsMessageFailed(exception);
        }

        try {
            messageService.validateDescriptionResponseMessage(response);
        } catch (UnexpectedMessageType exception) {
            // If the response is not a description response message, show the response.
            return messageProcessor.returnResponseMessageContent(response);
        } catch (MessageResponseException exception) {
            return ControllerUtils.respondReceivedInvalidResponse(exception);
        }

        String payload = null;
        try {
            // Read and process the response message.
            payload = MessageUtils.extractPayloadFromMultipartMessage(response);

            if (!Utils.isEmptyOrNull(elementId)) {
                return new ResponseEntity<>(payload, HttpStatus.OK);
            } else {
                // Get payload as component.
                final var component = messageProcessor.getComponentFromPayload(payload);
                return new ResponseEntity<>(component, HttpStatus.OK);
            }
        } catch (IllegalArgumentException exception) {
            // If the response is not of type resource or base connector.
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (MessageResponseException exception) {
            return ControllerUtils.respondReceivedInvalidResponse(exception);
        } catch (Exception exception) {
            return ControllerUtils.respondGlobalException(exception);
        }
    }
}
