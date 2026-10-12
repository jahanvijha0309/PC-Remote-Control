package com.example.appington_city;

/** A PC server found on the network. */
public final class Device {
    public final String name;
    public final String ip;

    public Device(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    @Override
    public String toString() {
        return name + " - " + ip;
    }
}
