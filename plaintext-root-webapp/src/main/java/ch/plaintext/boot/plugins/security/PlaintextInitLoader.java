/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.plugins.security;

import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import jakarta.annotation.PostConstruct;
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

    private final MyUserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlaintextInitLoader(MyUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "";
        log.info(passwordEncoder.encode(rawPassword));
    }

    @PostConstruct
    public void init() {
        String name = "daniel.marthaler@plaintext.ch";
        MyUserEntity nr1 = userRepository.findByUsername(name);
        if (nr1 == null) {
            MyUserEntity admin = new MyUserEntity();
            admin.addRole("admin");
            admin.addRole("root");
            admin.addRole("user");
            admin.setMandat("plaintext");
            admin.setUsername(name);
            String defaultPassword = "admin";
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            admin.setAutologinKey("fHySOUPZo1N1zLOpviHmBukjSQUL1ivLkeM");
            userRepository.save(admin);
            log.info("Created user '{}' with autologin key", name);
        }
    }

    /**
     * Creates a root user 1 minute after application startup if it doesn't exist.
     * Username: root@root.root
     * Password: root
     * Roles: root, admin, user
     * Mandat: default
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void createRootUserDelayed() {

            log.info("Scheduling root user creation in 1 minute...");

            String rootUsername = "root@root.root";
            MyUserEntity existingRoot = userRepository.findByUsername(rootUsername);

            if (existingRoot == null) {
                log.info("Creating root user with username: {}", rootUsername);
                MyUserEntity rootUser = new MyUserEntity();
                rootUser.addRole("root");
                rootUser.addRole("admin");
                rootUser.addRole("user");
                rootUser.setUsername(rootUsername);
                rootUser.setPassword(passwordEncoder.encode("root"));
                rootUser.setMandat("default");
                userRepository.save(rootUser);
                log.info("Root user '{}' created successfully", rootUsername);
            } else {
                log.info("Root user '{}' already exists, skipping creation", rootUsername);
            }

    }
}
