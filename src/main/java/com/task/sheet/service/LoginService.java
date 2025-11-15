package com.task.sheet.service;

import com.task.sheet.bean.MoniterBean;
import com.task.sheet.bean.UserBean;
import com.task.sheet.model.Monitoring;
import com.task.sheet.model.Users;
import com.task.sheet.repository.MonitoringRepository;
import com.task.sheet.repository.UserRepository;
import com.task.sheet.utils.AesGcmCrypto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AesGcmCrypto aesGcmCrypto;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private UrlScrapperService urlScrapperService;


    public Map<String, Object> userRegister(UserBean userBean) {
        Map<String, Object> returnMap = new HashMap<>();
        Users user = userRepository.findByMailId(userBean.getMailId());
        if (user == null) {
            user = new Users();
            user.setUsername(userBean.getUsername());
            user.setPassword(aesGcmCrypto.encrypt(userBean.getPassword()));
            user.setMailId(userBean.getMailId());
            userRepository.save(user);
            returnMap.put("status", "success");
            returnMap.put("message", "User registered successfully");
        } else {
            returnMap.put("status", "error");
            returnMap.put("message", "User already exists with this email");
        }
        System.out.println(returnMap);
        return returnMap;
    }

    public Map<String, Object> userLogin(UserBean userBean) {
        Map<String, Object> returnMap = new HashMap<>();
        Users user = userRepository.findByMailId(userBean.getMailId());
        String password = null;
        if(user!=null){
            password = aesGcmCrypto.decrypt(user.getPassword());
        }

        Monitoring monitoring = new Monitoring();
        int monitorLenghth = (int) monitoringRepository.count();
        LocalDateTime now = LocalDateTime.now();
        if (user != null) {
            if (password.equals(userBean.getPassword())) {
                returnMap.put("status", "success");
                returnMap.put("message", "Login successful");
                returnMap.put("userName", user.getUsername());
                returnMap.put("mailID", user.getMailId());
                returnMap.put("role", user.getRole());
                returnMap.put("sessionID", urlScrapperService.createNewUploadSession());
                returnMap.put("fetch-account", urlScrapperService.fetchAccounts("/api/fetch-accounts/", returnMap.get("sessionID").toString()));
                monitoring.setId(monitorLenghth + 1);
                monitoring.setLoginTime(now);
                monitoring.setStatus("Login");
                monitoring.setMailId(user.getMailId());
                monitoring.setUserName(user.getUsername());
                monitoringRepository.save(monitoring);
                returnMap.put("monitoringID", monitorLenghth + 1);
            } else {
                returnMap.put("status", "error");
                returnMap.put("message", "Invalid credentials");
            }
        } else {
            returnMap.put("status", "error");
            returnMap.put("message", "User not found");
        }
        System.out.println(returnMap);
        return returnMap;
    }

    public Map<String, Object> userLogOut(MoniterBean moniterBean) {
        Map<String, Object> returnMap = new HashMap<>();
        Optional<Monitoring> monitoring = Optional.of(new Monitoring());
        monitoring = monitoringRepository.findById(Integer.valueOf(moniterBean.getMonitoring()));
        LocalDateTime now = LocalDateTime.now();
        monitoring.get().setLogOutTime(now);
        monitoring.get().setStatus("Logout");
        monitoringRepository.save(monitoring.get());
        returnMap.put("status", "success");
        return returnMap;
    }

    public List<Map<String, Object>> getConsolidationOfUsersLoginAndLogout(LocalDateTime startDate, LocalDateTime endDate) {
        List<Monitoring> monitoringList = new ArrayList<>();

        if (endDate == null) {
            LocalDateTime endOfDay = startDate.toLocalDate().atTime(23, 59, 59, 999999999);
            monitoringList = monitoringRepository.findByLoginTimeBetween(startDate, endOfDay);
        } else {
            monitoringList = monitoringRepository.findByLoginTimeBetween(startDate, endDate);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Monitoring m : monitoringList) {
            Map<String, Object> userData = new LinkedHashMap<>();
            userData.put("UserName", m.getUserName());
            userData.put("MailId", m.getMailId());
            userData.put("LoginDate", m.getLoginTime());
            userData.put("LogoutDate", m.getLogOutTime());

            if (m.getLogOutTime() != null && m.getLoginTime() != null) {
                Duration sessionDuration = Duration.between(m.getLoginTime(), m.getLogOutTime());
                double hours = sessionDuration.toMinutes() / 60.0; // in hours
                userData.put("SessionHours", String.format("%.2f", hours));
            } else {
                userData.put("SessionHours", "Active");
            }

            result.add(userData);
        }

        return result;

    }
}
