package ch.plaintext.boot.plugins.security.service;


import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import ch.plaintext.boot.plugins.security.persistence.MyUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MyUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final MyUserRepository userRepository;

    public MyUserDetailsService(MyUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("MyUserEntity not found");
        }

        List<SimpleGrantedAuthority> auth = new ArrayList<>();
        for (String role : user.getRoles()) {
            if(role.toLowerCase().contains("mandat")){
                auth.add(new SimpleGrantedAuthority( role.toUpperCase()));
            } else {
                auth.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        }
        auth.add(new SimpleGrantedAuthority("PROPERTY_MYUSERID_" + user.getId()));
        if (user.getStartpage() != null && !user.getStartpage().isEmpty()) {
            auth.add(new SimpleGrantedAuthority("PROPERTY_STARTPAGE_"+user.getStartpage()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                auth
        );
    }
}
