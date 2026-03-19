/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web.nosec;

import ch.plaintext.boot.web.VersionBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nosec")
public class VersionController {

    private final VersionBean versionBean;

    public VersionController(VersionBean versionBean) {
        this.versionBean = versionBean;
    }

    @GetMapping("/version")
    public String getVersion() {
        return versionBean.getVersion();
    }
}
