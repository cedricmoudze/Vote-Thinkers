package com.vote.VotingSystem.Service;

import com.vote.VotingSystem.Dto.UserDto;
import com.vote.VotingSystem.Model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService {

    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    User registerNewUser(UserDto userDto) throws RuntimeException;

}
