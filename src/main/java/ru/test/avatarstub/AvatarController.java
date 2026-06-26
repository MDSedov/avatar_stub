package ru.test.avatarstub;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class AvatarController {

    private final AvatarPoolService avatarPoolService;

    public AvatarController(AvatarPoolService avatarPoolService) {
        this.avatarPoolService = avatarPoolService;
    }

    @PostMapping("/admin/reload")
    public ResponseEntity<Map<String, Object>> reload() {
        avatarPoolService.reloadPool();

        return ResponseEntity.ok(Map.of(
                "status", "RELOADED",
                "poolSize", avatarPoolService.size(),
                "dir", avatarPoolService.avatarDir().toAbsolutePath().toString()
        ));
    }

    @GetMapping("/admin/status")
    public ResponseEntity<StatusResponse> status() {
        return ResponseEntity.ok(new StatusResponse(
                avatarPoolService.size(),
                avatarPoolService.nextIndex()
        ));
    }

    @RequestMapping(
            value = "/admin/**",
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.PATCH,
                    RequestMethod.DELETE,
                    RequestMethod.OPTIONS,
                    RequestMethod.HEAD,
                    RequestMethod.TRACE
            }
    )
    public ResponseEntity<Map<String, Object>> unknownAdminRequest(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", "NOT_FOUND",
                "path", request.getRequestURI()
        ));
    }

    @RequestMapping(
            value = "/**",
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST,
                    RequestMethod.PUT,
                    RequestMethod.PATCH,
                    RequestMethod.DELETE,
                    RequestMethod.OPTIONS,
                    RequestMethod.HEAD,
                    RequestMethod.TRACE
            }
    )
    public ResponseEntity<?> anyRestRequest(HttpServletRequest request) throws IOException {
        try {
            Path avatar = avatarPoolService.nextAvatar();
            FileSystemResource resource = new FileSystemResource(avatar);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(resource.contentLength())
                    .cacheControl(CacheControl.noStore())
                    .header("X-Avatar-File", avatar.getFileName().toString())
                    .body(resource);

        } catch (AvatarPoolEmptyException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Avatar pool is empty. Run generation first. Requested URI: " + request.getRequestURI());
        }
    }

    private record StatusResponse(int poolSize, long nextIndex) {
    }
}
