//package com.eams.controller;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/login")
//    public ResponseEntity<LoginResponse> login(
//            @Valid @RequestBody LoginRequest request) {
//
//        return ResponseEntity.ok(
//                authService.login(request)
//        );
//    }
//}