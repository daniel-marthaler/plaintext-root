/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import ch.plaintext.settings.ISetupConfigService;
import ch.plaintext.settings.RootUserToggleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PlaintextInitLoader {

    private static final String ROOT_USERNAME = "root@root.root";

    private final MyUserRepository userRepository;
    private final ISetupConfigService setupConfigService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlaintextInitLoader(MyUserRepository userRepository, ISetupConfigService setupConfigService) {
        this.userRepository = userRepository;
        this.setupConfigService = setupConfigService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void createRootUserDelayed() {
        if (!setupConfigService.isRootUserEnabled("default")) {
            log.info("Root user creation disabled via setup config, skipping");
            return;
        }

        MyUserEntity existingRoot = userRepository.findByUsername(ROOT_USERNAME);
        if (existingRoot == null) {
            createRootUser();
        } else {
            log.info("Root user '{}' already exists, skipping creation", ROOT_USERNAME);
        }
    }

    @EventListener
    public void onRootUserToggle(RootUserToggleEvent event) {
        if (event.isEnabled()) {
            MyUserEntity existingRoot = userRepository.findByUsername(ROOT_USERNAME);
            if (existingRoot == null) {
                createRootUser();
            } else {
                log.info("Root user '{}' already exists", ROOT_USERNAME);
            }
        } else {
            MyUserEntity existingRoot = userRepository.findByUsername(ROOT_USERNAME);
            if (existingRoot != null) {
                userRepository.delete(existingRoot);
                log.info("Root user '{}' deleted", ROOT_USERNAME);
            }
        }
    }

    private void createRootUser() {
        log.info("Creating root user with username: {}", ROOT_USERNAME);
        MyUserEntity rootUser = new MyUserEntity();
        rootUser.addRole("root");
        rootUser.addRole("admin");
        rootUser.addRole("user");
        rootUser.setUsername(ROOT_USERNAME);
        rootUser.setPassword(passwordEncoder.encode("root"));
        rootUser.setMandat("default");
        userRepository.save(rootUser);
        log.info("Root user '{}' created successfully", ROOT_USERNAME);
    }
}
