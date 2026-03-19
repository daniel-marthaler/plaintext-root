/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Collection;

@org.springframework.web.bind.annotation.RestController
@Slf4j
public class Index {

    @GetMapping("/")
    public void getIndex(HttpServletResponse response) throws IOException {
        String redirect = "index.html";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (!authorities.isEmpty()) {
            for (GrantedAuthority authority : authorities) {
                String authStr = authority.getAuthority();
                if (authStr.contains("html")) {
                    // Remove PROPERTY_STARTPAGE_ prefix if present
                    if (authStr.startsWith("PROPERTY_STARTPAGE_")) {
                        redirect = authStr.substring("PROPERTY_STARTPAGE_".length());
                    } else {
                        redirect = authStr;
                    }
                }
            }
        }
        response.sendRedirect(redirect);
    }
}