package com.ticketblitz.auth.client;

import com.ticketblitz.auth.dto.UserDto;
import com.ticketblitz.auth.dto.UserRegistrationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    @PostMapping("/users/register")
    UserDto registerUser(@RequestBody UserRegistrationRequest request);

    @GetMapping("/users/internal/{username}")
    UserDto getUserByUsernameInternal(@PathVariable("username") String username);
}
