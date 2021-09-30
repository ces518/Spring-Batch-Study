package me.nuc.chapter05;

import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;

public class CustomBatchConfigurer extends DefaultBatchConfigurer {

    @Autowired
    @Qualifier("repositoryDataSource")
    private DataSource dataSource;

    @Autowired
    @Qualifier("batchTransactionManager")
    private PlatformTransactionManager transactionManager;

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    protected JobRepository createJobRepository() throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDatabaseType(DatabaseType.MYSQL.getProductName());
        factoryBean.setTablePrefix("FOO_");
        factoryBean.setIsolationLevelForCreate("ISOLATION_REPEATABLE_READ");
        factoryBean.setDataSource(dataSource);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Override
    protected JobExplorer createJobExplorer() throws Exception {
        JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTablePrefix("FOO_");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
