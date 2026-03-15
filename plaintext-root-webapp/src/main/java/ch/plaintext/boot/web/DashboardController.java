package ch.plaintext.boot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard.html")
    public String dashboard() {
        return "redirect:/index.html";
    }

}
