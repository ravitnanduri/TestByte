package com.testbyte.backend.admin;

import com.testbyte.backend.domain.AppSetting;
import com.testbyte.backend.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AppSettingsService {

    public static final String APPROVER_NOTIFICATION_EMAIL_KEY = "approver_notification_email";

    private final AppSettingRepository appSettingRepository;
    private final String approverEmailDefault;

    public AppSettingsService(AppSettingRepository appSettingRepository,
                               @Value("${app.approver-email-default}") String approverEmailDefault) {
        this.appSettingRepository = appSettingRepository;
        this.approverEmailDefault = approverEmailDefault;
    }

    public String getApproverNotificationEmail() {
        return appSettingRepository.findById(APPROVER_NOTIFICATION_EMAIL_KEY)
                .map(AppSetting::getValue)
                .orElse(approverEmailDefault);
    }

    public void setApproverNotificationEmail(String email) {
        AppSetting setting = appSettingRepository.findById(APPROVER_NOTIFICATION_EMAIL_KEY)
                .orElse(new AppSetting(APPROVER_NOTIFICATION_EMAIL_KEY, email));
        setting.setValue(email);
        appSettingRepository.save(setting);
    }
}
