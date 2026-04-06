package com.flightpathfinder.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用于定义当前类型或方法在模块内的职责边界。
 */
@ConfigurationProperties(prefix = "pathfinder.admin.data.etl")
public class AdminDataEtlProperties {

    private final OpenFlights openflights = new OpenFlights();
    private final Visa visa = new Visa();
    private final CityCost cityCost = new CityCost();

    public OpenFlights getOpenflights() {
        return openflights;
    }

    public Visa getVisa() {
        return visa;
    }

    public CityCost getCityCost() {
        return cityCost;
    }

    public static class OpenFlights {

        private String airportsLocation = "classpath:data/openflights/airports.dat";
        private String airlinesLocation = "classpath:data/openflights/airlines.dat";
        private String routesLocation = "classpath:data/openflights/routes.dat";

        public String getAirportsLocation() {
            return airportsLocation;
        }

        public void setAirportsLocation(String airportsLocation) {
            this.airportsLocation = airportsLocation;
        }

        public String getAirlinesLocation() {
            return airlinesLocation;
        }

        public void setAirlinesLocation(String airlinesLocation) {
            this.airlinesLocation = airlinesLocation;
        }

        public String getRoutesLocation() {
            return routesLocation;
        }

        public void setRoutesLocation(String routesLocation) {
            this.routesLocation = routesLocation;
        }
    }

    public static class Visa {

        private String location = "classpath:data/visa_policies.yaml";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    public static class CityCost {

        private String location = "classpath:data/city_costs.json";

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}



