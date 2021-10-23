package me.june.chapter09.neo4j;

import java.util.List;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

//@Configuration(
//    proxyBeanMethods = false
//)
//@ConditionalOnMissingBean({SessionFactory.class})
public class Neo4jOgmSessionFactoryConfiguration {
    Neo4jOgmSessionFactoryConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean
    org.neo4j.ogm.config.Configuration configuration(Neo4jProperties properties) {
        return properties.createConfiguration();
    }

    @Bean
    SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration, BeanFactory beanFactory, ObjectProvider<EventListener> eventListeners) {
        SessionFactory sessionFactory = new SessionFactory(configuration, this.getPackagesToScan(beanFactory));
        eventListeners.orderedStream().forEach(sessionFactory::register);
        return sessionFactory;
    }

    private String[] getPackagesToScan(BeanFactory beanFactory) {
        List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
        if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
            packages = AutoConfigurationPackages.get(beanFactory);
        }

        return StringUtils.toStringArray(packages);
    }
}
