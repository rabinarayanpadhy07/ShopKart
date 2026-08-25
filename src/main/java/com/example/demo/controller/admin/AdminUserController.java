// Admin Controller for Modify User functionality
package com.example.demo.controller.admin;
import com.example.demo.entity.User;
import com.example.demo.service.admin.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = adminUserService.getAllUsers();
            List<Map<String, Object>> responseList = new ArrayList<>();
            for (User u : users) {
                Map<String, Object> map = new HashMap<>();
                map.put("userId", u.getUserId());
                map.put("username", u.getUsername());
                map.put("email", u.getEmail());
                map.put("role", u.getRole().name());
                map.put("createdAt", u.getCreatedAt().toString());
                responseList.add(map);
            }
            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
        }
    }

    @PutMapping("/modify/{userId}")
    public ResponseEntity<?> modifyUser(@PathVariable("userId") Integer userId, @RequestBody Map<String, Object> userRequest) {
    	 try {
             String username = (String) userRequest.get("username");
             String email = (String) userRequest.get("email");
             String role = (String) userRequest.get("role");
             User updatedUser = adminUserService.modifyUser(userId, username, email, role);
             Map<String, Object> response = new HashMap<>();
             response.put("userId", updatedUser.getUserId());
             response.put("username", updatedUser.getUsername());
             response.put("email", updatedUser.getEmail());
             response.put("role", updatedUser.getRole().name());
             response.put("createdAt", updatedUser.getCreatedAt());
             response.put("updatedAt", updatedUser.getUpdatedAt());
             return ResponseEntity.status(HttpStatus.OK).body(response);
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
         }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable("userId") Integer userId) {
    	 try {
             User user = adminUserService.getUserById(userId);
             return ResponseEntity.status(HttpStatus.OK).body(user);
         } catch (IllegalArgumentException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
         } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong"));
         }
     }
}
