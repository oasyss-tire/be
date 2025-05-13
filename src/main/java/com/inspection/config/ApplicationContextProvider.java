package com.inspection.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 컨텍스트에 접근할 수 있는 유틸리티 클래스
 * 스레드 풀 상태 정보 등을 조회하는 데 사용됩니다.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 애플리케이션 컨텍스트를 반환합니다.
     * @return ApplicationContext 인스턴스
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * 빈 이름으로 빈을 가져옵니다.
     * @param beanName 빈 이름
     * @return 빈 객체
     */
    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }
    
    /**
     * 클래스 타입으로 빈을 가져옵니다.
     * @param <T> 빈 타입
     * @param beanClass 빈 클래스
     * @return 빈 객체
     */
    public static <T> T getBean(Class<T> beanClass) {
        return applicationContext.getBean(beanClass);
    }
    
    /**
     * 빈 이름과 클래스 타입으로 빈을 가져옵니다.
     * @param <T> 빈 타입
     * @param beanName 빈 이름
     * @param beanClass 빈 클래스
     * @return 빈 객체
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        return applicationContext.getBean(beanName, beanClass);
    }
} 