package com.ll.TeamProject.global.initData;

import com.ll.TeamProject.standard.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Profile("!test")
@Configuration
@RequiredArgsConstructor
public class DevInitData {
    @Autowired
    @Lazy
    private DevInitData self;
    @Bean
    public ApplicationRunner devInitDataApplicationRunner() {
        return args -> {
            try{

                Ut.file.downloadByHttp("http://localhost:8080/v3/api-docs", ".");

                String cmd = "yes | npx --package typescript --package openapi-typescript openapi-typescript api-docs.json -o ../frontend/src/lib/backend/schema.d.ts";
                Ut.cmd.runAsync(cmd);
            }catch(Exception e){
            }
        };
    }
}