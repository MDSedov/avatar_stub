package ru.test.avatarstub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "avatar")
public class AvatarProperties {

    private String mode = "server";
    private String dir = "./data/avatars";
    private int generateCount = 230000;
    private int width = 550;
    private int height = 550;
    private int targetSizeKb = 40;
    private int adminPrepareMaxCount = 10000;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public int getGenerateCount() {
        return generateCount;
    }

    public void setGenerateCount(int generateCount) {
        this.generateCount = generateCount;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTargetSizeKb() {
        return targetSizeKb;
    }

    public void setTargetSizeKb(int targetSizeKb) {
        this.targetSizeKb = targetSizeKb;
    }

    public int getAdminPrepareMaxCount() {
        return adminPrepareMaxCount;
    }

    public void setAdminPrepareMaxCount(int adminPrepareMaxCount) {
        this.adminPrepareMaxCount = adminPrepareMaxCount;
    }
}
