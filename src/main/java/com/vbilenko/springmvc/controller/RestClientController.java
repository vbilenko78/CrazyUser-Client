package com.vbilenko.springmvc.controller;

import com.vbilenko.springmvc.model.User;
import com.vbilenko.springmvc.model.UserProfile;
import com.vbilenko.springmvc.service.UserProfileService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sun.org.apache.xerces.internal.util.PropertyState.is;

@Controller
@RequestMapping("/")
@SessionAttributes("roles")
public class RestClientController {

    @Autowired
    MessageSource messageSource;

    @Autowired
    private UserProfileService userProfileService;

    private String plainClientCredentials = "sam:1";
    private String base64ClientCredentials = new String(Base64.encodeBase64(plainClientCredentials.getBytes()));

    /**
     * This method will list all existing users.
     */
    @RequestMapping(value = {"/", "/list"}, method = RequestMethod.GET)
    public String listUsers(ModelMap model) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + base64ClientCredentials);

        RestTemplate restTemplate = new RestTemplate();
        String resourceURL = "http://localhost:8080/users";
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<User[]> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, User[].class);

        List<User> users = Arrays.asList(response.getBody());
        model.addAttribute("users", users);
        model.addAttribute("loggedinuser", getPrincipal());
        return "userslist";
    }

    /**
     * This method will provide the medium to add a new user.
     */
    @RequestMapping(value = {"/newuser"}, method = RequestMethod.GET)
    public String newUser(ModelMap model) {

        User user = new User();
        model.addAttribute("user", user);
        model.addAttribute("edit", false);
        model.addAttribute("loggedinuser", getPrincipal());
        model.addAttribute("userProfiles", userProfileService.findAll());
        return "registration";
    }

    /**
     * This method will be called on form submission, handling POST request for
     * saving user in database. It also validates the user input
     */
    @RequestMapping(value = {"/newuser"})
    public String saveUser(@Valid User user, BindingResult result, ModelMap model) {

        if (result.hasErrors()) {
            model.addAttribute("allUsers", listUsers(model));
            return "registration";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + base64ClientCredentials);

        HttpEntity<User> request = new HttpEntity<>(user, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity("http://localhost:8080/users", request, User.class);

        model.addAttribute("user", user);
        model.addAttribute("success", "User " + user.getFirstName() + " " + user.getLastName() + " registered successfully");
        model.addAttribute("loggedinuser", getPrincipal());
        return "registrationsuccess";
    }


    /**
     * This method will provide the medium to update an existing user.
     */
    @RequestMapping(value = {"/edit-user-{id}"})
    public String editUser(@PathVariable int id, ModelMap model) {

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + base64ClientCredentials);

        RestTemplate restTemplate = new RestTemplate();
        String resourceURL = "http://localhost:8080/users/" + id;
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<User> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, User.class);

        User user = response.getBody();

        model.addAttribute("user", user);
        model.addAttribute("edit", true);
        model.addAttribute("loggedinuser", getPrincipal());
        return "registration";
    }

    /**
     * This method will be called on form submission, handling POST request for
     * updating user in database. It also validates the user input
     */
    @RequestMapping(value = {"/edit-user-{id}"}, method = RequestMethod.POST)
    public String updateUser(@Valid User user, BindingResult result,
                             ModelMap model, @PathVariable int id) {
        if (result.hasErrors()) {
            return "registration";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + base64ClientCredentials);

        HttpEntity<User> request = new HttpEntity<>(user, headers);
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8080/users/";
        restTemplate.put(url + id, request, User.class);

        model.addAttribute("success", "User " + user.getFirstName() + " " + user.getLastName() + " updated successfully");
        model.addAttribute("loggedinuser", getPrincipal());
        return "registrationsuccess";
    }

    /**
     * This method will delete an user by it's SSOID value.
     */
    @RequestMapping(value = {"/delete-user-{id}"})
    public String deleteUser(@PathVariable int id) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64ClientCredentials);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        String entityUrl = "http://localhost:8080/users" + "/" + id;
        restTemplate.exchange(entityUrl, HttpMethod.DELETE, entity, User.class);
        return "redirect:/";
    }


    /**
     * This method will provide UserProfile list to views
     */
    @ModelAttribute("roles")
    public List<UserProfile> initializeProfiles() {
        return userProfileService.findAll();
    }

    @ModelAttribute("userProfiles")
    public List<UserProfile> initializeUserProfiles() {
        return userProfileService.findAll();
    }

    /**
     * This method handles Access-Denied redirect.
     */
    @RequestMapping(value = "/Access_Denied", method = RequestMethod.GET)
    public String accessDeniedPage(ModelMap model) {
        model.addAttribute("loggedinuser", getPrincipal());
        return "accessDenied";
    }

    /**
     * This method handles login GET requests.
     * If users is already logged-in and tries to goto login page again, will be redirected to list page.
     */
    @RequestMapping(value = {"/login"})
    public String loginPage() {
        return "login";
    }

    /**
     * This method retrieves user's welcome page
     */
    @RequestMapping(value = "/userpage", method = RequestMethod.GET)
    public String userPage() {
        return "userpage";
    }

    /**
     * This method handles logout requests.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            SecurityContextHolder.getContext().setAuthentication(null);
        }
        return "redirect:/login?logout";
    }

    /**
     * This method returns the principal[user-name] of logged-in user.
     */
    private String getPrincipal() {
        String userName;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            userName = ((UserDetails) principal).getUsername();
        } else {
            userName = principal.toString();
        }
        return userName;
    }
}
