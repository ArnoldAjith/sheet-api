package com.task.sheet.controller;

import com.task.sheet.bean.MoniterBean;
import com.task.sheet.bean.UserBean;
import com.task.sheet.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(UserBean userBean) {
        return ResponseEntity.ok(loginService.userLogin(userBean));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserBean userBean) {
        return ResponseEntity.ok(loginService.userRegister(userBean));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logOutSession(@RequestBody MoniterBean moniterBean) {
        return ResponseEntity.ok(loginService.userLogOut(moniterBean));
    }

    @PostMapping("/monitoring")
    public ResponseEntity<?> monitoringData(@RequestBody MoniterBean moniterBean) {
        LocalDateTime endDateTime = null;
        LocalDateTime startDateTime = null;

        if (moniterBean.getStartDate() != null) {
            startDateTime = moniterBean.getStartDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        if (moniterBean.getEndDate() != null) {
            endDateTime = moniterBean.getEndDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return ResponseEntity.ok(loginService.getConsolidationOfUsersLoginAndLogout(startDateTime, endDateTime));
    }
}
