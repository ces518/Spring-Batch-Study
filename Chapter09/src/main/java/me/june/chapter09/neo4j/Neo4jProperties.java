package me.june.chapter09.neo4j;

import org.neo4j.ogm.config.AutoIndexMode;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.Configuration.Builder;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

@org.springframework.context.annotation.Configuration
@ConfigurationProperties(
    prefix = "spring.neo4j"
)
public class Neo4jProperties implements ApplicationContextAware {
    static final String EMBEDDED_DRIVER = "org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver";
    static final String HTTP_DRIVER = "org.neo4j.ogm.drivers.http.driver.HttpDriver";
    static final String DEFAULT_BOLT_URI = "bolt://localhost:7687";
    static final String BOLT_DRIVER = "org.neo4j.ogm.drivers.bolt.driver.BoltDriver";
    private String uri;
    private String username;
    private String password;
    private AutoIndexMode autoIndex;
    private boolean useNativeTypes;
    private final Neo4jProperties.Embedded embedded;
    private ClassLoader classLoader;

    public Neo4jProperties() {
        this.autoIndex = AutoIndexMode.NONE;
        this.useNativeTypes = false;
        this.embedded = new Neo4jProperties.Embedded();
        this.classLoader = Neo4jProperties.class.getClassLoader();
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AutoIndexMode getAutoIndex() {
        return this.autoIndex;
    }

    public void setAutoIndex(AutoIndexMode autoIndex) {
        this.autoIndex = autoIndex;
    }

    public boolean isUseNativeTypes() {
        return this.useNativeTypes;
    }

    public void setUseNativeTypes(boolean useNativeTypes) {
        this.useNativeTypes = useNativeTypes;
    }

    public Embedded getEmbedded() {
        return this.embedded;
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.classLoader = ctx.getClassLoader();
    }

    public Configuration createConfiguration() {
        Builder builder = new Builder();
        this.configure(builder);
        return builder.build();
    }

    private void configure(Builder builder) {
        if (this.uri != null) {
            builder.uri(this.uri);
        } else {
            this.configureUriWithDefaults(builder);
        }

        if (this.username != null && this.password != null) {
            builder.credentials(this.username, this.password);
        }

        builder.autoIndex(this.getAutoIndex().getName());
        if (this.useNativeTypes) {
            builder.useNativeTypes();
        }

    }

    private void configureUriWithDefaults(Builder builder) {
        if (!this.getEmbedded().isEnabled() || !ClassUtils.isPresent("org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver", this.classLoader)) {
            builder.uri("bolt://localhost:7687");
        }

    }

    public static class Embedded {
        private boolean enabled = true;

        public Embedded() {
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

