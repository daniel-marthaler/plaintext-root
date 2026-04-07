/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings.service;

import ch.plaintext.settings.ISetupConfigService;
import ch.plaintext.settings.entity.SetupConfig;
import ch.plaintext.settings.repository.SetupConfigRepository;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Named("setupConfigService")
@Slf4j
@RequiredArgsConstructor
public class SetupConfigService implements ISetupConfigService {

    private final SetupConfigRepository repository;

    @Override
    public boolean isAutologinEnabled(String mandat) {
        return repository.findByMandat(mandat)
                .map(SetupConfig::isAutologinEnabled)
                .orElse(false);
    }

    @Override
    public boolean isOidcAutoRedirectEnabled(String mandat) {
        return repository.findByMandat(mandat)
                .map(SetupConfig::isOidcAutoRedirectEnabled)
                .orElse(false);
    }

    @Override
    public Long getOidcAutoRedirectConfigId(String mandat) {
        return repository.findByMandat(mandat)
                .map(SetupConfig::getOidcAutoRedirectConfigId)
                .orElse(null);
    }

    public Optional<SetupConfig> findByMandat(String mandat) {
        return repository.findByMandat(mandat);
    }

    public Optional<SetupConfig> findFirstWithOidcAutoRedirect() {
        return repository.findFirstByOidcAutoRedirectEnabledTrue();
    }

    public Optional<SetupConfig> findFirstWithAutologin() {
        return repository.findFirstByAutologinEnabledTrue();
    }

    @Transactional
    public SetupConfig save(SetupConfig config) {
        SetupConfig saved = repository.save(config);
        log.info("SetupConfig saved: id={}, mandat={}, autologin={}, oidcRedirect={}",
                saved.getId(), saved.getMandat(), saved.isAutologinEnabled(), saved.isOidcAutoRedirectEnabled());
        return saved;
    }

    @Transactional
    public SetupConfig getOrCreate(String mandat) {
        return repository.findByMandat(mandat).orElseGet(() -> {
            SetupConfig config = new SetupConfig();
            config.setMandat(mandat);
            return repository.save(config);
        });
    }
}
