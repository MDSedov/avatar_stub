package ru.test.avatarstub;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class GenerationRunner implements ApplicationRunner {

    private final AvatarGeneratorService avatarGeneratorService;
    private final ConfigurableApplicationContext context;

    @Value("${avatar.mode:server}")
    private String mode;

    @Value("${avatar.generate-count:230000}")
    private int generateCount;

    public GenerationRunner(
            AvatarGeneratorService avatarGeneratorService,
            ConfigurableApplicationContext context
    ) {
        this.avatarGeneratorService = avatarGeneratorService;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"generate".equalsIgnoreCase(mode)) {
            return;
        }

        System.out.println("Avatar generation mode started. Count: " + generateCount);
        avatarGeneratorService.generatePool(generateCount);
        System.out.println("Avatar generation mode finished.");

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }
}
