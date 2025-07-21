package com.chatroom.chatroombackend.service;

import com.chatroom.chatroombackend.dto.LocationDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class GeoIpService {
    /**
     * Mock implementation. In a real-world scenario, this service would use a
     * Geo-IP library (e.g., MaxMind GeoIP2) to look up the IP address.
     * @param request The incoming HTTP request.
     * @return A LocationDto with location data.
     */
    public LocationDto getLocationFromRequest(HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        // Here you would use a Geo-IP library to resolve the ipAddress.
        // For this example, we'll return a mock location for Frankfurt, Germany.
        System.out.println("Resolving IP for recommendation: " + ipAddress);
        return new LocationDto(50.1109, 8.6821, "Frankfurt");
    }
}