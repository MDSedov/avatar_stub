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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class AvatarController {

    private final AvatarPoolService avatarPoolService;
    private final AvatarGeneratorService avatarGeneratorService;
    private final AvatarProperties properties;

    public AvatarController(
            AvatarPoolService avatarPoolService,
            AvatarGeneratorService avatarGeneratorService,
            AvatarProperties properties
    ) {
        this.avatarPoolService = avatarPoolService;
        this.avatarGeneratorService = avatarGeneratorService;
        this.properties = properties;
    }

    @PostMapping("/admin/prepare")
    public ResponseEntity<Map<String, Object>> prepare(@RequestParam(defaultValue = "1000") int count) {
        if (count > properties.getAdminPrepareMaxCount()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "message", "HTTP generation is limited. Use command mode for large pools.",
                    "requestedCount", count,
                    "maxAllowedCount", properties.getAdminPrepareMaxCount()
            ));
        }

        avatarGeneratorService.generatePool(count);
        avatarPoolService.reloadPool();

        return ResponseEntity.ok(Map.of(
                "status", "DONE",
                "poolSize", avatarPoolService.size(),
                "dir", avatarPoolService.avatarDir().toAbsolutePath().toString()
        ));
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
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "poolSize", avatarPoolService.size(),
                "currentIndex", avatarPoolService.currentIndex(),
                "dir", avatarPoolService.avatarDir().toAbsolutePath().toString()
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
                    RequestMethod.HEAD
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
}
