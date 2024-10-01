package com.vote.VotingSystem.Service;

import com.vote.VotingSystem.Dto.UserDto;
import com.vote.VotingSystem.Model.User;
import com.vote.VotingSystem.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Tentative de chargement de l'utilisateur : " + username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.out.println("Utilisateur non trouvé : " + username);
                    return new UsernameNotFoundException("Utilisateur non trouvé: " + username);
                });

        System.out.println("Utilisateur trouvé : " + user.getUsername());
        System.out.println("Mot de passe encodé : " + user.getPassword());
        System.out.println("Rôle de l'utilisateur : " + (user.getRole() != null ? user.getRole() : "aucun"));


        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        }

        System.out.println("Autorités de l'utilisateur : " + authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    @Override
    public User registerNewUser(UserDto userDto) throws RuntimeException {
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("Un utilisateur avec ce nom existe déjà");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        // Le rôle n'est pas défini ici, il sera ajouté manuellement dans la BD
        return userRepository.save(user);
    }
}
