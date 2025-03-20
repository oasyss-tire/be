// package com.inspection.config;

// import javax.sql.DataSource;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.io.ClassPathResource;
// import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Configuration
// @RequiredArgsConstructor
// @Slf4j
// public class DataInitializer {
    
//     private final DataSource dataSource;
    
//     /**
//      * 애플리케이션 시작 시 코드 데이터 초기화
//      */
//     @Bean
//     public CommandLineRunner initCodeData() {
//         return args -> {
//             log.info("코드 데이터 초기화 시작...");
            
//             try {
//                 ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
//                 populator.addScript(new ClassPathResource("data-code.sql"));
//                 populator.execute(dataSource);
                
//                 log.info("코드 데이터 초기화 완료");
//             } catch (Exception e) {
//                 log.error("코드 데이터 초기화 중 오류 발생: {}", e.getMessage(), e);
//             }
//         };
//     }
// } 