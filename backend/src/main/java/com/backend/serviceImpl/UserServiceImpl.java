package com.backend.serviceImpl;

import com.backend.auth.AuthenticationRequest;
import com.backend.auth.AuthenticationResponse;
import com.backend.auth.RegistrationResponse;
import com.backend.config.JwtService;
import com.backend.entity.Role;
import com.backend.entity.User;
import com.backend.entity.VerificationToken;
import com.backend.exception.UserAlreadyExistsException;
import com.backend.model.UserModel;
import com.backend.repository.UserRepository;
import com.backend.repository.VerificationUserTokenRepository;
import com.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    @Autowired
    private VerificationUserTokenRepository verificationUserTokenRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VerificationUserTokenRepository verificationTokenRepository;
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void saveVerificationTokenForUser(String token, User user) {
        VerificationToken verificationToken
                = new VerificationToken(user, token);

        verificationTokenRepository.save(verificationToken);
    }

    @Override
    public RegistrationResponse registerTeacher(UserModel usermodel) {
        Optional<User> userExists = repository.findByEmail(usermodel.getEmail());
        if(userExists.isPresent()){
            throw new UserAlreadyExistsException(
                    "User with email "+usermodel.getEmail() + " already exists"
            );
        }
        User user = User.builder()
                .firstName(usermodel.getFirstName())
                .lastName(usermodel.getLastName())
                .email(usermodel.getEmail())
                .password(passwordEncoder.encode(usermodel.getPassword()))
                .role(Role.TEACHER)
                .build();
        repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
        return new RegistrationResponse(jwtToken, user);
    }

    @Override
    public Map<String, Object> registerStudent(UserModel userModel) {
        Optional<User> userExists = repository.findByEmail(userModel.getEmail());
        if(userExists.isPresent()){
            throw new UserAlreadyExistsException(
                    "User with email "+userModel.getEmail() + " already exists"
            );
        }
        User user = User.builder()
                .firstName(userModel.getFirstName())
                .lastName(userModel.getLastName())
                .email(userModel.getEmail())
                .password(passwordEncoder.encode(userModel.getPassword()))
                .role(Role.STUDENT)
                .build();
        repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
        Map<String, Object> response = new HashMap<>();
        response.put("jwtToken", jwtToken);
        response.put("user", user);
        return response;
    }

    @Override
    public String validateVerificationToken(String token) {
        VerificationToken verificationToken =
                verificationTokenRepository.findByToken(token);
        if(verificationToken == null){
            return "invalid";
        }

        User user = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();
        if(verificationToken.getExpirationTime().getTime() - cal.getTime().getTime() <= 0){
           verificationUserTokenRepository.delete(verificationToken);
           return "expired";
        }
        user.setEnabled(true);
        userRepository.save(user);
        return "valid";
    }

    @Override
    public VerificationToken generateNewVerificationToken(String oldToken) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(oldToken);
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationTokenRepository.save(verificationToken);

        return verificationToken;
    }
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

}
