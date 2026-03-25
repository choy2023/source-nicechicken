package com.example.nicechicken.user.security;

import com.example.nicechicken.user.entity.Role;
import com.example.nicechicken.user.entity.UserEntity;
import com.example.nicechicken.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. Retrieve user profile information from Google server
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 2. Extract email and name (Google standard)
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // 3. Register if new user, update name if already exists (Upsert)
        UserEntity user = userRepository.findByEmail(email)
                .map(entity -> entity.updateName(name))
                .orElse(UserEntity.builder()
                        .email(email)
                        .name(name)
                        .role(Role.ROLE_CUSTOMER) // Assign CUSTOMER role upon initial registration
                        .build());

        userRepository.save(user);

        // 4. Return for Spring Security session storage
        return oAuth2User;
    }
}