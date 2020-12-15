package cn.sim.rbmq.controller;

import lombok.Data;

import java.util.Date;

@Data
public class CommonSmsJobPayload<T> {
    private String name;
    private String age;
    private String address;
    private String mobile;
}
